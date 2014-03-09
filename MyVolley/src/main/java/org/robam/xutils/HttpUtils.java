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

package org.robam.xutils;

import android.text.TextUtils;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.robam.xutils.core.SimpleSSLSocketFactory;
import org.robam.xutils.http.HttpException;
import org.robam.xutils.http.HttpCache;
import org.robam.xutils.http.HttpHandler;
import org.robam.xutils.http.RequestParams;
import org.robam.xutils.http.ResponseStream;
import org.robam.xutils.http.SyncHttpHandler;
import org.robam.xutils.http.callback.HttpRedirectHandler;
import org.robam.xutils.http.callback.RequestCallBack;
import org.robam.xutils.http.client.HttpRequest;
import org.robam.xutils.http.client.RetryHandler;
import org.robam.xutils.http.client.entity.GZipDecompressingEntity;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Http请求获取数据..主要分四种.1.异步请求一般的字符串.2.同步请求字符串.3.下载.4.上传
 * 此类只要一创建出来,线程池就已经建好了.所以为了效率考虑,当不需要就不要随便new出来.避免资源的浪费.
 * <p/>
 * 以下是Http上传的示例
 * <p/>
 * RequestParams params = new RequestParams();
 * params.addHeader("name", "value");
 * params.addQueryStringParameter("name", "value");
 * <p/>
 * // 只包含字符串参数时默认使用BodyParamsEntity，
 * // 类似于UrlEncodedFormEntity（"application/x-www-form-urlencoded"）。
 * params.addBodyParameter("name", "value");
 * <p/>
 * // 加入文件参数后默认使用MultipartEntity（"multipart/form-data"），
 * // 如需"multipart/related"，xUtils中提供的MultipartEntity支持设置subType为"related"。
 * // 使用params.setBodyEntity(httpEntity)可设置更多类型的HttpEntity（如：
 * // MultipartEntity,BodyParamsEntity,FileUploadEntity,InputStreamUploadEntity,StringEntity）。
 * // 例如发送json参数：params.setBodyEntity(new StringEntity(jsonStr,charset));
 * params.addBodyParameter("file", new File("path"));
 * ...
 * <p/>
 * HttpUtils http = new HttpUtils();
 * http.send(HttpRequest.HttpMethod.POST,
 * "uploadUrl....",
 * params,
 * new RequestCallBack<String>() {
 *
 * @Override public void onStart() {
 * testTextView.setText("conn...");
 * }
 * @Override public void onLoading(long total, long current, boolean isUploading) {
 * if (isUploading) {
 * testTextView.setText("upload: " + current + "/" + total);
 * } else {
 * testTextView.setText("reply: " + current + "/" + total);
 * }
 * }
 * @Override public void onSuccess(ResponseInfo<String> responseInfo) {
 * testTextView.setText("reply: " + responseInfo.result);
 * }
 * @Override public void onFailure(HttpException error, String msg) {
 * testTextView.setText(error.getExceptionCode() + ":" + msg);
 * }
 * });
 */
public class HttpUtils {

    /**
     * HTTP缓存.都使用默认的设置
     */
    public final static HttpCache sHttpCache = new HttpCache();

    /**
     * Default  HTTP client.Http是使用HttpClient的方式请求.但是官方已经不推荐使用HttpClient,而是HttpUrlConnect
     */
    private final DefaultHttpClient httpClient;

    /**
     * TODO:???
     */
    private final HttpContext httpContext = new BasicHttpContext();

    /**
     * 重定向处理器.
     */
    private HttpRedirectHandler httpRedirectHandler;

    // ************************************    default settings & fields ****************************

    private String responseTextCharset = HTTP.UTF_8;

    private long currentRequestExpiry = HttpCache.getDefaultExpiryTime();

    private final static int DEFAULT_CONN_TIMEOUT = 1000 * 15; // 15s

    private final static int DEFAULT_RETRY_TIMES = 5;

    private static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
    private static final String ENCODING_GZIP = "gzip";
    private static final String USER_AGENT = "robam";

    /**
     * 线程池大小.只用3个.考虑到Http是长时间连接,如果太多的话,可能会消耗过多的CPU资源.
     */
    private static int threadPoolSize = 3;

    /**
     * ExecutorService是线程池服务.系统会自动维护.
     * Executors是工厂类,生产线程池.
     * newFixedThreadPool可以产生固定执行线程的线程池.参数:ThreadFactory就是传递一个可以产生线程的工厂类给他,
     * 它会使用这个工厂类产生新线程.
     * 当线程超过线程固定执行的数量时候,必须要等待执行中的结束.
     */
    private static ExecutorService executor;

    /**
     * 用来新建线程出来,工厂类.参考:DefaultThreadFactory.
     */
    private static ThreadFactory sThreadFactory;

    static {
        sThreadFactory = new ThreadFactory() {
            //自增的 int 值
            private final AtomicInteger mCount = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {

                Thread thread = new Thread(r, "HttpUtils #" + mCount.getAndIncrement());
                //设置优先级.都比默认的低1.
                thread.setPriority(Thread.NORM_PRIORITY - 1);
                return thread;
            }
        };
        executor = Executors.newFixedThreadPool(threadPoolSize, sThreadFactory);
    }


    public HttpClient getHttpClient() {
        return this.httpClient;
    }


    public HttpUtils() {
        this(HttpUtils.DEFAULT_CONN_TIMEOUT);
    }

    /**
     * 创建一个HTTPUtils
     *
     * @param connTimeout
     */
    public HttpUtils(int connTimeout) {
        // HTTP请求参数.Notice:这写参数只是设置到params存起来,暂时还没有应用,所以后面要调用,否则不会生效.
        HttpParams params = new BasicHttpParams();

        // 设置HTTP请求参数.
        // 这是设置连接超时
        ConnManagerParams.setTimeout(params, connTimeout);
        HttpConnectionParams.setConnectionTimeout(params, connTimeout);
        // Socket超时
        HttpConnectionParams.setSoTimeout(params, connTimeout);
        //UserAgent
        HttpProtocolParams.setUserAgent(params, USER_AGENT);

        ConnManagerParams.setMaxConnectionsPerRoute(params, new ConnPerRouteBean(10));
        ConnManagerParams.setMaxTotalConnections(params, 10);

        HttpConnectionParams.setTcpNoDelay(params, true);
        HttpConnectionParams.setSocketBufferSize(params, 1024 * 8);
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);

        // 支持的协议集合,比如http,https等. Schemes are identified by lowercase names.
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        // 注册支持的协议
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        schemeRegistry.register(new Scheme("https", SimpleSSLSocketFactory.getSocketFactory(), 443));

        // ThreadSafeClientConnManager:Manages a pool of client connections
        // 创建新的Http客户端
        httpClient = new DefaultHttpClient(new ThreadSafeClientConnManager(params, schemeRegistry), params);

        // 重试
        httpClient.setHttpRequestRetryHandler(new RetryHandler(DEFAULT_RETRY_TIMES));

        /**
         * 请求拦截器.
         */
        httpClient.addRequestInterceptor(new HttpRequestInterceptor() {
            /**
             *拦截器里面对Http增加一个请求头,对即将发送出去的数据,如果没有增加压缩方式,就添加"GZIP".
             * @param httpRequest
             * @param httpContext
             * @throws org.apache.http.HttpException
             * @throws java.io.IOException
             */
            @Override
            public void process(org.apache.http.HttpRequest httpRequest, HttpContext httpContext) throws org.apache.http.HttpException, IOException {
                // 添加HTTP请求头.压缩方法+ GZIP
                if (!httpRequest.containsHeader(HEADER_ACCEPT_ENCODING)) {
                    httpRequest.addHeader(HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
                }
            }
        });

        /**
         * HTTP响应拦截器.为的是添加一个GZIP压缩标志
         */
        httpClient.addResponseInterceptor(new HttpResponseInterceptor() {
            @Override
            public void process(HttpResponse response, HttpContext httpContext) throws org.apache.http.HttpException, IOException {
                final HttpEntity entity = response.getEntity();
                if (entity == null) {
                    return;
                }
                final Header encoding = entity.getContentEncoding();
                if (encoding != null) {
                    // 遍历每个元素
                    for (HeaderElement element : encoding.getElements()) {
                        if (element.getName().equalsIgnoreCase("gzip")) {
                            // 添加压缩头
                            response.setEntity(new GZipDecompressingEntity(response.getEntity()));
                            return;
                        }
                    }
                }
            }
        });
    }


    // ***************************************** config *******************************************

    /**
     * 设置响应的字符集.默认为UTF-8..
     *
     * @param charSet
     * @return
     */
    public HttpUtils configResponseTextCharset(String charSet) {
        if (!TextUtils.isEmpty(charSet)) {
            this.responseTextCharset = charSet;
        }
        return this;
    }

    /**
     * 重试次数.
     *
     * @param count
     * @return
     */
    public HttpUtils configRequestRetryCount(int count) {
        this.httpClient.setHttpRequestRetryHandler(new RetryHandler(count));
        return this;
    }

    /**
     * 线程池大小.这个很重要
     *
     * @param threadPoolSize
     * @return
     */
    public HttpUtils configRequestThreadPoolSize(int threadPoolSize) {
        if (threadPoolSize > 0 && threadPoolSize != HttpUtils.threadPoolSize) {
            HttpUtils.threadPoolSize = threadPoolSize;
            HttpUtils.executor = Executors.newFixedThreadPool(threadPoolSize, sThreadFactory);
        }
        return this;
    }

    // ***************************************** send request *******************************************

    /**
     * 最简单的GET请求.异步
     *
     * @param url      URL
     * @param callBack 回调方法.
     * @param <T>
     * @return
     */
    public <T> HttpHandler<T> send(String url, RequestCallBack<T> callBack) {
        return send(HttpRequest.HttpMethod.GET, url, null, callBack);
    }

    /**
     * 异步请求.
     *
     * @param method
     * @param url
     * @param params
     * @param callBack
     * @param <T>
     * @return
     */
    public <T> HttpHandler<T> send(HttpRequest.HttpMethod method, String url, RequestParams params,
                                   RequestCallBack<T> callBack) {
        if (url == null) throw new IllegalArgumentException("url may not be null");

        HttpRequest request = new HttpRequest(method, url);
        return sendRequest(request, params, callBack);
    }

    /**
     * 同步请求.没有使用线程.
     *
     * @param method
     * @param url
     * @param params
     * @return
     * @throws HttpException
     */
    public ResponseStream sendSync(HttpRequest.HttpMethod method, String url, RequestParams params) throws HttpException {
        if (url == null) throw new IllegalArgumentException("url may not be null");

        HttpRequest request = new HttpRequest(method, url);
        return sendSyncRequest(request, params);
    }

    // ***************************************** download *******************************************

    public HttpHandler<File> download(String url, String target,
                                      RequestParams params, boolean autoResume, boolean autoRename, RequestCallBack<File> callback) {
        return download(HttpRequest.HttpMethod.GET, url, target, params, autoResume, autoRename, callback);
    }

    /**
     * 这才是下载的关键.
     *
     * @param method     HTTP方法,Get,Post
     * @param url        下载Url
     * @param target     路径.包括文件名.如果需要重命名的话也需要传一个名称过来.后面是以Parent为准的.
     * @param params     自定义的请求参数
     * @param autoResume 断点续传
     * @param autoRename 重命名,根据响应头.
     * @param callback   下载状况监听器.
     * @return
     */
    public HttpHandler<File> download(HttpRequest.HttpMethod method, String url, String target,
                                      RequestParams params, boolean autoResume, boolean autoRename, RequestCallBack<File> callback) {

        if (url == null) throw new IllegalArgumentException("url may not be null");
        if (target == null) throw new IllegalArgumentException("target may not be null");

        HttpRequest request = new HttpRequest(method, url);

        HttpHandler<File> handler = new HttpHandler<>(httpClient, httpContext, responseTextCharset, callback);

        handler.setExpiry(currentRequestExpiry);
        handler.setHttpRedirectHandler(httpRedirectHandler);
        request.setRequestParams(params, handler);

        handler.executeOnExecutor(executor, request, target, autoResume, autoRename);
        return handler;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 异步请求.
     * 异步请求和同步请求设计得不好.HttpHandler应该设计成一个接口,需要实现请求的就实现这个接口.
     *
     * @param request
     * @param params
     * @param callBack
     * @param <T>
     * @return
     */
    private <T> HttpHandler<T> sendRequest(HttpRequest request, RequestParams params, RequestCallBack<T> callBack) {

        HttpHandler<T> handler = new HttpHandler<T>(httpClient, httpContext, responseTextCharset, callBack);

        handler.setExpiry(currentRequestExpiry);
        handler.setHttpRedirectHandler(httpRedirectHandler);
        request.setRequestParams(params, handler);

        handler.executeOnExecutor(executor, request);
        return handler;
    }

    /**
     * 同步请求.
     *
     * @param request
     * @param params
     * @return
     * @throws HttpException
     */
    private ResponseStream sendSyncRequest(HttpRequest request, RequestParams params) throws HttpException {

        SyncHttpHandler handler = new SyncHttpHandler(httpClient, httpContext, responseTextCharset);

        handler.setExpiry(currentRequestExpiry);
        handler.setHttpRedirectHandler(httpRedirectHandler);
        request.setRequestParams(params);

        return handler.sendRequest(request);
    }
}
