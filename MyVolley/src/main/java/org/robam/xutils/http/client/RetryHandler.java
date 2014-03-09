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

package org.robam.xutils.http.client;

import android.os.SystemClock;

import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.RequestWrapper;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.robam.xutils.Utils.LogUtils;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashSet;

import javax.net.ssl.SSLHandshakeException;


/**
 * HTTP重试处理器.其实只是判断是否需要重试而已的,根本没做重试操作.
 */
public class RetryHandler implements HttpRequestRetryHandler {

    private static final int RETRY_SLEEP_INTERVAL = 500;

    /**
     * 错误重试的白名单
     */
    private static HashSet<Class<?>> exceptionWhiteList = new HashSet<Class<?>>();

    /**
     * 错误重试的黑名单
     */
    private static HashSet<Class<?>> exceptionBlackList = new HashSet<Class<?>>();

    /**
     * 添加错误白名单和黑名单
     */
    static {
        exceptionWhiteList.add(NoHttpResponseException.class);
        exceptionWhiteList.add(UnknownHostException.class);
        exceptionWhiteList.add(SocketException.class);

        exceptionBlackList.add(InterruptedIOException.class);
        exceptionBlackList.add(SSLHandshakeException.class);
    }

    /**
     * 最大重试次数
     */
    private final int maxRetries;

    public RetryHandler(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    /**
     * @param exception
     * @param retriedTimes
     * @param context      HTTP请求的上下文,原来什么东西都包含了,就跟Activity的Context一样
     * @return
     */
    @Override
    public boolean retryRequest(IOException exception, int retriedTimes, HttpContext context) {
        boolean retry = true;

        if (exception == null || context == null) {
            return false;
        }

        // 获取是否已经发送了请求,如果没法送,当然是True了
        Object isReqSent = context.getAttribute(ExecutionContext.HTTP_REQ_SENT);
        boolean sent = isReqSent == null ? false : (Boolean) isReqSent;

        // 判断是否还要重试
        if (retriedTimes > maxRetries) {
            // 次数已经达到最大限制,返回了
            retry = false;
        } else if (exceptionBlackList.contains(exception.getClass())) {
            // 遇到BlackList中包含的错误就不重试.
            retry = false;
        } else if (exceptionWhiteList.contains(exception.getClass())) {
            // 在白名单内的需要重试.
            retry = true;
        } else if (!sent) {
            // 如果没发送过
            retry = true;
        }

        if (retry) {
            try {
                Object currRequest = context.getAttribute(ExecutionContext.HTTP_REQUEST);
                if (currRequest != null) {
                    if (currRequest instanceof HttpRequestBase) {
                        // 还要根据请求是不是GET方法才retry啊,如果不是GET,根本就不重试了
                        HttpRequestBase requestBase = (HttpRequestBase) currRequest;
                        retry = "GET".equals(requestBase.getMethod());
                    } else if (currRequest instanceof RequestWrapper) {
                        RequestWrapper requestWrapper = (RequestWrapper) currRequest;
                        retry = "GET".equals(requestWrapper.getMethod());
                    }
                } else {
                    retry = false;
                    LogUtils.e("retry error, curr request is null");
                }
            } catch (Throwable e) {
                retry = false;
                LogUtils.e("retry error", e);
            }
        }

        if (retry) {
            // sleep a while and retry http request again.
            SystemClock.sleep(RETRY_SLEEP_INTERVAL);
        }

        return retry;
    }

}