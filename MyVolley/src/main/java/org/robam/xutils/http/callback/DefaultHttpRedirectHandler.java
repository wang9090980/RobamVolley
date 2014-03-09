package org.robam.xutils.http.callback;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

/**
 * 重定向处理器.当URL返回301或302代码,就会再次请求.
 */
public class DefaultHttpRedirectHandler implements HttpRedirectHandler {
    @Override
    public HttpRequestBase getDirectRequest(HttpResponse response) {
        if (response.containsHeader("Location")) {
            String location = response.getFirstHeader("Location").getValue();
            HttpGet request = new HttpGet(location);
            if (response.containsHeader("Set-Cookie")) {
                String cookie = response.getFirstHeader("Set-Cookie").getValue();
                request.addHeader("Cookie", cookie);
            }
            return request;
        }
        return null;
    }
}
