/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.volley.toolbox;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.http.AndroidHttpClient;
import android.os.Build;

import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyLog;

import java.io.File;

/**
 * Modify By weiji.chen 2014/03/09
 * 用来创建RequestQueue的Helper.实际上,所有的请求都是经过RequestQueue.
 * 注意:Volley不适合下载大文件,只能下载小文件.建议不要超过5M.因为下载的时候直接到内存的.暂用的是APP的内存.同时,注意并发线程的控制.
 * 如果需要下载大文件,请使用xUtils.
 * TODO:流量统计.
 */
public class Volley {

    /**
     * Default on-disk cache directory..
     */
    private static final String DEFAULT_CACHE_DIR = "robamVolley";

    /**
     * Creates a default instance of the worker pool and calls {@link com.android.volley.RequestQueue#start()} on it.
     *
     * @param context        A {@link android.content.Context} to use for creating the cache dir.
     *                       这个context只是创建缓存目录的,跟执行的线程没关系,以前一直以为就在这个Context中执行了.
     * @param stack          An {@link HttpStack} to use for the network, or null for default.
     * @param threadPoolSize 执行任务的线程池大小.最好根据任务多少来分配,否则分配过多会浪费资源.默认是4.
     * @return A started {@link com.android.volley.RequestQueue} instance.
     */
    public static RequestQueue newRequestQueue(Context context, HttpStack stack, int threadPoolSize, int cacheSize) {

        File cacheDir = new File(context.getCacheDir(), DEFAULT_CACHE_DIR);
        VolleyLog.d("Cache file dir = " + cacheDir.getAbsolutePath());

        String userAgent = "volley/robam";
        try {
            String packageName = context.getPackageName();
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            userAgent = packageName + "/" + info.versionCode;
        } catch (NameNotFoundException e) {
        }

        if (stack == null) {
            if (Build.VERSION.SDK_INT >= 9) {
                //这里可以指定stack的SSL或者是URL验证
                stack = new HurlStack();
            } else {
                // Prior to Gingerbread, HttpUrlConnection was unreliable.
                // See: http://android-developers.blogspot.com/2011/09/androids-http-clients.html
                stack = new HttpClientStack(AndroidHttpClient.newInstance(userAgent));
            }
        }

        Network network = new BasicNetwork(stack);

        // 默认开启磁盘缓存.我觉得这么小的缓存,只要下载一张图片就塞满了,所以这么小不适合下载图片和文件.
        // 如果需要下载文件,可以用大缓存.
        // 如果为了下载Json等比较小的数据,可以换成MemoryCache,这更快.
        RequestQueue queue = new RequestQueue(new DiskBasedCache(cacheDir, cacheSize), network, threadPoolSize);
        queue.start();

        return queue;
    }

    public static RequestQueue newRequestQueue(Context context, int threadPoolSize) {
        return newRequestQueue(context, threadPoolSize, DiskBasedCache.DEFAULT_DISK_USAGE_BYTES);
    }

    public static RequestQueue newRequestQueue(Context context, int threadPoolSize, int cacheSize) {
        return newRequestQueue(context, null, threadPoolSize, cacheSize);
    }

    /**
     * Creates a default instance of the worker pool and calls {@link com.android.volley.RequestQueue#start()} on it.
     *
     * @param context A {@link android.content.Context} to use for creating the cache dir.
     *                这个context只是创建缓存目录的,跟执行的线程没关系,以前一直以为就在这个Context中执行了.
     * @return A started {@link com.android.volley.RequestQueue} instance.
     * 注意:这个RequestQueue已经去开始了,只要往这个Queue里Add任务,就会自动执行了.所以,最好在需要的时候再打开,不需要的时候关闭.
     */
    public static RequestQueue newRequestQueue(Context context) {
        return newRequestQueue(context, RequestQueue.DEFAULT_NETWORK_THREAD_POOL_SIZE);
    }
}
