/*
 * Copyright (c) 2013. wyouflf (wyouflf@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.robam.xutils.http;

import android.os.SystemClock;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.protocol.HttpContext;
import org.robam.xutils.HttpUtils;
import org.robam.xutils.Utils.LogUtils;
import org.robam.xutils.Utils.OtherUtils;
import org.robam.xutils.core.CompatibleAsyncTask;
import org.robam.xutils.http.callback.DefaultHttpRedirectHandler;
import org.robam.xutils.http.callback.FileDownloadHandler;
import org.robam.xutils.http.callback.HttpRedirectHandler;
import org.robam.xutils.http.callback.RequestCallBack;
import org.robam.xutils.http.callback.RequestCallBackHandler;
import org.robam.xutils.http.callback.StringDownloadHandler;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;

/**
 * 最关键的HTTP请求.
 *
 * @param <T>
 */
public class HttpHandler<T> extends CompatibleAsyncTask<Object, Object, Void> implements RequestCallBackHandler {

    private final AbstractHttpClient client;
    private final HttpContext context;

    private final StringDownloadHandler mStringDownloadHandler = new StringDownloadHandler();
    private final FileDownloadHandler mFileDownloadHandler = new FileDownloadHandler();

    private HttpRedirectHandler httpRedirectHandler;

    private String requestUrl;
    private String requestMethod;
    private HttpRequestBase request;
    private boolean isUploading = true;
    private final RequestCallBack<T> callback;

    private int retriedTimes = 0;
    private String fileSavePath = null;

    private boolean isDownloadingFile = false;

    /**
     * 自动从断点处下载
     */
    private boolean autoResume = false;

    /**
     * 是否根据响应头的重命名文件
     */
    private boolean autoRename = false;

    /**
     * 响应头的字符集
     */
    private String charset; // The default charset of response header info.

    public HttpHandler(AbstractHttpClient client, HttpContext context, String charset, RequestCallBack<T> callback) {
        this.client = client;
        this.context = context;
        this.callback = callback;
        this.charset = charset;
    }

    private State state = State.WAITING;

    public State getState() {
        return state;
    }

    private long expiry = HttpCache.getDefaultExpiryTime();

    public void setExpiry(long expiry) {
        this.expiry = expiry;
    }

    public void setHttpRedirectHandler(HttpRedirectHandler httpRedirectHandler) {
        if (httpRedirectHandler != null) {
            this.httpRedirectHandler = httpRedirectHandler;
        }
    }

    /**
     * 这是请求,看准了.
     * 先查询是否断点续传,如果续传,先获取已有文件的长度,然后设置请求头从断点处开始.
     *
     * @param request Http请求客户端
     * @return 返回的是响应信息
     * @throws HttpException
     */
    @SuppressWarnings("unchecked")
    private ResponseInfo<T> sendRequest(HttpRequestBase request) throws HttpException {

        if (autoResume && isDownloadingFile) {

            File downloadFile = new File(fileSavePath);
            long fileLen = 0;
            // 文件存在,先获取文件的长度
            if (downloadFile.isFile() && downloadFile.exists()) {
                fileLen = downloadFile.length();
            }

            if (fileLen > 0) {
                //文件长度大于0的,就从断点处开始,原来这么简单,就一个设置请求头RANGE
                request.setHeader("RANGE", "bytes=" + fileLen + "-");
            }
        }

        // 重试
        boolean retry = true;
        HttpRequestRetryHandler retryHandler = client.getHttpRequestRetryHandler();


        //一直在重复请求,直到返回了.
        while (retry) {
            IOException exception = null;
            try {
                requestMethod = request.getMethod();
                // 查询是否支持请求方法的缓存
                if (HttpUtils.sHttpCache.isEnabled(requestMethod)) {
                    String result = HttpUtils.sHttpCache.get(requestUrl);
                    if (result != null) {
                        // TODO:难道只要查询到了就马上返回了吗?
                        return new ResponseInfo<T>(null, (T) result, true);
                    }
                }

                ResponseInfo<T> responseInfo = null;
                if (!isCancelled()) {
                    // 这就是真正的请求了!得到了Response
                    HttpResponse response = client.execute(request, context);
                    // 得到了响应
                    responseInfo = handleResponse(response);
                }
                return responseInfo;
            } catch (UnknownHostException e) {
                exception = e;
                // 注意:这只是根据错误和HttpContext判断是否需要重试.
                retry = retryHandler.retryRequest(exception, ++retriedTimes, context);
            } catch (IOException e) {
                exception = e;
                retry = retryHandler.retryRequest(exception, ++retriedTimes, context);
            } catch (NullPointerException e) {
                exception = new IOException(e.getMessage());
                exception.initCause(e);
                retry = retryHandler.retryRequest(exception, ++retriedTimes, context);
            } catch (HttpException e) {
                throw e;
            } catch (Throwable e) {
                exception = new IOException(e.getMessage());
                exception.initCause(e);
                retry = retryHandler.retryRequest(exception, ++retriedTimes, context);
            }
            if (!retry && exception != null) {
                throw new HttpException(exception);
            }
        }
        return null;
    }

    /**
     * 反正先会执行到这里
     *
     * @param params The parameters of the task.
     * @return
     */
    @Override
    protected Void doInBackground(Object... params) {
        if (this.state == State.STOPPED || params == null || params.length == 0) {
            return null;
        }

        if (params.length > 3) {
            // 文件保存路径.如果是空判定为不是下载.
            fileSavePath = String.valueOf(params[1]);
            isDownloadingFile = fileSavePath != null;
            autoResume = (Boolean) params[2];
            autoRename = (Boolean) params[3];
        }

        try {
            if (this.state == State.STOPPED) {
                return null;
            }
            // init request & requestUrl
            request = (HttpRequestBase) params[0];
            requestUrl = request.getURI().toString();
            if (callback != null) {
                callback.setRequestUrl(requestUrl);
            }

            // 开始下载
            this.publishProgress(UPDATE_START);

            lastUpdateTime = SystemClock.uptimeMillis();

            //关键,调用发送请求的.
            ResponseInfo<T> responseInfo = sendRequest(request);

            if (responseInfo != null) {
                // 得到了结果从这里返回
                this.publishProgress(UPDATE_SUCCESS, responseInfo);
                return null;
            }
        } catch (HttpException e) {
            this.publishProgress(UPDATE_FAILURE, e, e.getMessage());
        }

        return null;
    }

    private final static int UPDATE_START = 1;
    private final static int UPDATE_LOADING = 2;
    private final static int UPDATE_FAILURE = 3;
    private final static int UPDATE_SUCCESS = 4;

    /**
     * 更新当前的进度.
     *
     * @param values The values indicating progress.
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void onProgressUpdate(Object... values) {
        //为了防止资源的浪费,一般都先进行判断再往下执行.
        if (this.state == State.STOPPED || values == null || values.length == 0 || callback == null)
            return;
        // values[0]是当前的状态
        switch ((Integer) values[0]) {
            case UPDATE_START:
                this.state = State.STARTED;
                callback.onStart();
                break;
            case UPDATE_LOADING:
                if (values.length != 3) return;
                this.state = State.LOADING;
                callback.onLoading(
                        Long.valueOf(String.valueOf(values[1])),
                        Long.valueOf(String.valueOf(values[2])),
                        isUploading);
                break;
            case UPDATE_FAILURE:
                if (values.length != 3) return;
                this.state = State.FAILURE;
                callback.onFailure((HttpException) values[1], (String) values[2]);
                break;
            case UPDATE_SUCCESS:
                if (values.length != 2) return;
                this.state = State.SUCCESS;
                // 原来就是从这里返回啊
                callback.onSuccess((ResponseInfo<T>) values[1]);
                break;
            default:
                break;
        }
    }

    /**
     * 处理响应
     * TODO:疑惑:如果下载一个很大的文件,缓存的文件存在哪里?按照现在的想法,HttpResponse有一个缓存的地方,而不占用当前APP的内存.如果占用APP内存,下载100M的早就崩了
     *
     * @param response
     * @return
     * @throws HttpException
     * @throws java.io.IOException
     */
    @SuppressWarnings("unchecked")
    private ResponseInfo<T> handleResponse(HttpResponse response) throws HttpException, IOException {
        if (response == null) {
            throw new HttpException("response is null");
        }
        if (isCancelled()) return null;

        // 获取状态码
        StatusLine status = response.getStatusLine();
        int statusCode = status.getStatusCode();

        if (statusCode < 300) {
            // 只有小于300的状态码是正常的
            Object result = null;
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                isUploading = false;
                //从这里可以看出,这个类的设计并不好,扩展性太差了,这就限定了只能下载文件和字符串.应该学学Volley吧一切都抽象出去.今天的感悟.
                if (isDownloadingFile) {
                    // 下载文件就用FileDownloadHandler处理.
                    // 判断是否支持续传.
                    autoResume = autoResume && OtherUtils.isSupportRange(response);
                    // 从响应头获取文件名
                    String responseFileName = autoRename ? OtherUtils.getFileNameFromHttpResponse(response) : null;
                    result = mFileDownloadHandler.handleEntity(entity, this, fileSavePath, autoResume, responseFileName);
                } else {
                    // 下载字符串就用StringDownLoadHandler处理.字符串可以缓存
                    result = mStringDownloadHandler.handleEntity(entity, this, charset);
                    if (HttpUtils.sHttpCache.isEnabled(requestMethod)) {
                        HttpUtils.sHttpCache.put(requestUrl, (String) result, expiry);
                    }
                }
            }
            return new ResponseInfo<T>(response, (T) result, false);
        } else if (statusCode == 301 || statusCode == 302) {
            // 301 & 302 重定向.所以需要再一次请求.考虑很周到,之前一直没意识到.
            if (httpRedirectHandler == null) {
                httpRedirectHandler = new DefaultHttpRedirectHandler();
            }
            HttpRequestBase request = httpRedirectHandler.getDirectRequest(response);
            if (request != null) {
                // 又发起请求
                return this.sendRequest(request);
            }
        } else if (statusCode == 416) {

            // 请求头的Range范围不合法
            throw new HttpException(statusCode, "maybe the file has downloaded completely");
        } else {
            throw new HttpException(statusCode, status.getReasonPhrase());
        }
        return null;
    }

    /**
     * stop request task.
     */
    public void stop() {
        this.state = State.STOPPED;

        if (request != null && !request.isAborted()) {
            try {
                request.abort();
            } catch (Throwable e) {
            }
        }
        if (!this.isCancelled()) {
            try {
                this.cancel(true);
            } catch (Throwable e) {
            }
        }

        if (callback != null) {
            callback.onStopped();
        }
    }

    public boolean isStopped() {
        return this.state == State.STOPPED;
    }

    public RequestCallBack<T> getRequestCallBack() {
        return this.callback;
    }

    private long lastUpdateTime;

    /**
     * 这是实现RequestCallbackHandler接口的.目的是用来更新进度.根据返回值决定是否还要继续执行..
     * 不过我觉得这样控制有点不好.应该分开控制的.
     *
     * @param total
     * @param current
     * @param forceUpdateUI
     * @return true:继续,false:停止.
     */
    @Override
    public boolean updateProgress(long total, long current, boolean forceUpdateUI) {
        LogUtils.i("update progress......");
        if (callback != null && this.state != State.STOPPED) {
            if (forceUpdateUI) {
                this.publishProgress(UPDATE_LOADING, total, current);
            } else {
                // 刷新的频率是非常快的,就是通过这里用实际那来控制频率
                long currTime = SystemClock.uptimeMillis();
                if (currTime - lastUpdateTime >= callback.getRate()) {
                    lastUpdateTime = currTime;
                    this.publishProgress(UPDATE_LOADING, total, current);
                }
            }
        }
        return this.state != State.STOPPED;
    }

    public enum State {
        WAITING(0), STARTED(1), LOADING(2), FAILURE(3), STOPPED(4), SUCCESS(5);
        private int value = 0;

        State(int value) {
            this.value = value;
        }

        public static State valueOf(int value) {
            switch (value) {
                case 0:
                    return WAITING;
                case 1:
                    return STARTED;
                case 2:
                    return LOADING;
                case 3:
                    return FAILURE;
                case 4:
                    return STOPPED;
                case 5:
                    return SUCCESS;
                default:
                    return FAILURE;
            }
        }

        public int value() {
            return this.value;
        }
    }
}
