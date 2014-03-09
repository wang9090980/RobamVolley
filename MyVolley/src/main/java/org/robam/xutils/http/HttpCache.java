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

import android.text.TextUtils;

import org.robam.xutils.core.LruMemoryCache;
import org.robam.xutils.http.client.HttpRequest;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存缓存
 */
public class HttpCache {

    /**
     * key: url
     * value: response result
     */
    private final LruMemoryCache<String, String> mMemoryCache;

    private final static int DEFAULT_CACHE_SIZE = 1024 * 100;   // 100KB
    private final static long DEFAULT_EXPIRY_TIME = 1000 * 60; // 60 seconds

    private int cacheSize = DEFAULT_CACHE_SIZE;

    private static long defaultExpiryTime = DEFAULT_EXPIRY_TIME;

    /**
     * HttpCache(HttpCache.DEFAULT_CACHE_SIZE, HttpCache.DEFAULT_EXPIRY_TIME);
     */
    public HttpCache() {
        this(HttpCache.DEFAULT_CACHE_SIZE, HttpCache.DEFAULT_EXPIRY_TIME);
    }

    /**
     * 创建一个新的HttpCache,这个Cache使用了内存缓存技术.
     *
     * @param strLength
     * @param defaultExpiryTime
     */
    public HttpCache(int strLength, long defaultExpiryTime) {
        // Cache的大小
        this.cacheSize = strLength;
        // 有效期
        HttpCache.defaultExpiryTime = defaultExpiryTime;

        // 创建Lru内存缓存
        mMemoryCache = new LruMemoryCache<String, String>(this.cacheSize) {
            @Override
            protected int sizeOf(String key, String value) {
                if (value == null) return 0;
                return value.length();
            }
        };
    }

    public void setCacheSize(int strLength) {
        mMemoryCache.setMaxSize(strLength);
    }

    public static void setDefaultExpiryTime(long defaultExpiryTime) {
        HttpCache.defaultExpiryTime = defaultExpiryTime;
    }

    public static long getDefaultExpiryTime() {
        return HttpCache.defaultExpiryTime;
    }

    /**
     * 把数据放入到缓存中.
     *
     * @param url
     * @param result
     */
    public void put(String url, String result) {
        put(url, result, defaultExpiryTime);
    }

    /**
     * 把数据放入到缓存中.
     *
     * @param url
     * @param result
     * @param expiry
     */
    public void put(String url, String result, long expiry) {
        if (url == null || result == null || expiry < 1) return;
        mMemoryCache.put(url, result, System.currentTimeMillis() + expiry);
    }

    /**
     * 获取指定URL的缓存
     *
     * @param url
     * @return
     */
    public String get(String url) {
        return (url != null) ? mMemoryCache.get(url) : null;
    }

    /**
     * 清空所有的缓存
     */
    public void clear() {
        mMemoryCache.evictAll();
    }

    /**
     * 是否支持某个HTTP方法缓存
     *
     * @param method
     * @return
     */
    public boolean isEnabled(HttpRequest.HttpMethod method) {
        if (method == null) return false;

        Boolean enabled = httpMethod_enabled_map.get(method.toString());
        return enabled == null ? false : enabled;
    }

    /**
     * 是否支持某个HTTP方法缓存
     *
     * @param method
     * @return
     */
    public boolean isEnabled(String method) {
        if (TextUtils.isEmpty(method)) return false;

        Boolean enabled = httpMethod_enabled_map.get(method.toUpperCase());
        return enabled == null ? false : enabled;
    }

    /**
     * 设置某个方法缓存.
     *
     * @param method
     * @param enabled
     */
    public void setEnabled(HttpRequest.HttpMethod method, boolean enabled) {
        httpMethod_enabled_map.put(method.toString(), enabled);
    }

    /**
     * 保存当前支持的缓存HTTP方法.ConcurrentHashMap真是一个新的方法啊
     */
    private final static ConcurrentHashMap<String, Boolean> httpMethod_enabled_map;

    static {
        // 支持的HTTP方法(PUT,GET,POST).如果支持,就应该放在这里.大小为10,不难理解,怎么样HTTP方法也不会超过10个吧
        httpMethod_enabled_map = new ConcurrentHashMap<>(10);
        // 默认支持HTTP.GET方法缓存数据.
        httpMethod_enabled_map.put(HttpRequest.HttpMethod.GET.toString(), true);
    }
}
