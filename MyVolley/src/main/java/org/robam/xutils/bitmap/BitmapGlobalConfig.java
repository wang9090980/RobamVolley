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

package org.robam.xutils.bitmap;

import android.app.ActivityManager;
import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;

import org.robam.xutils.Utils.LogUtils;
import org.robam.xutils.bitmap.core.BitmapCache;
import org.robam.xutils.bitmap.download.Downloader;
import org.robam.xutils.bitmap.download.SimpleDownloader;
import org.robam.xutils.core.LruDiskCache;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Bitmap的全局配置
 */
public class BitmapGlobalConfig {

    /**
     * 磁盘缓存路径
     */
    private String diskCachePath;

    /**
     * 最小内存缓存大小.2M
     */
    public final static int MIN_MEMORY_CACHE_SIZE = 1024 * 1024 * 2;

    /**
     * 默认内存缓存大小:4M
     */
    private int memoryCacheSize = 1024 * 1024 * 4;

    /**
     * 最小磁盘缓存:10M
     */
    public final static int MIN_DISK_CACHE_SIZE = 1024 * 1024 * 10;

    /**
     * 默认磁盘缓存大小:50M
     */
    private int diskCacheSize = 1024 * 1024 * 50;

    /**
     * 是否开启内存缓存.
     * 默认True
     */
    private boolean memoryCacheEnabled = true;

    /**
     * 是否开启磁盘缓存.
     * 默认True.使用
     */
    private boolean diskCacheEnabled = true;

    /**
     * 图片下载器
     */
    private Downloader downloader;

    /**
     * Bitmap缓存
     */
    private BitmapCache bitmapCache;

    /**
     * 线程池大小.
     * TODO:难道只开5个线程下载吗?
     */
    private int threadPoolSize = 5;

    /**
     * TODO:???
     */
    private boolean _dirty_params_bitmapLoadExecutor = true;

    /**
     * TODO:???
     */
    private ExecutorService bitmapLoadExecutor;

    /**
     * 默认缓存有效期.30天
     */
    private long defaultCacheExpiry = 1000L * 60 * 60 * 24 * 30;

    /**
     * 默认下载超时时间.15s
     */
    private int defaultConnectTimeout = 1000 * 15;

    /**
     * 默认读取超时时间.15s
     */
    private int defaultReadTimeout = 1000 * 15;

    /**
     * TODO:???
     */
    private LruDiskCache.DiskCacheFileNameGenerator diskCacheFileNameGenerator;

    /**
     * TODO:???
     */
    private BitmapCacheListener bitmapCacheListener;


    /**
     * TODO:???开启线程的吗?
     */
    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "BitmapUtils #" + mCount.getAndIncrement());
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            return thread;
        }
    };

    private Context mContext;

    /**
     * @param context       上下文
     * @param diskCachePath If null, use default appCacheDir+"/xBitmapCache"
     */
    public BitmapGlobalConfig(Context context, String diskCachePath) {
        if (context == null) {
            throw new IllegalArgumentException("context may not be null");
        }
        this.mContext = context;
        this.diskCachePath = diskCachePath;
        initBitmapCache();
    }


    private void initBitmapCache() {
        new BitmapCacheManagementTask().execute(BitmapCacheManagementTask.MESSAGE_INIT_MEMORY_CACHE);
        new BitmapCacheManagementTask().execute(BitmapCacheManagementTask.MESSAGE_INIT_DISK_CACHE);
    }

    public String getDiskCachePath() {
        if (TextUtils.isEmpty(diskCachePath)) {
            diskCachePath = BitmapCommonUtils.getDiskCacheDir(mContext, "xBitmapCache");
        }
        return diskCachePath;
    }

    public Downloader getDownloader() {
        if (downloader == null) {
            downloader = new SimpleDownloader();
        }
        downloader.setContext(mContext);
        downloader.setDefaultExpiry(getDefaultCacheExpiry());
        downloader.setDefaultConnectTimeout(getDefaultConnectTimeout());
        downloader.setDefaultReadTimeout(getDefaultReadTimeout());
        return downloader;
    }

    public void setDownloader(Downloader downloader) {
        this.downloader = downloader;
    }

    public long getDefaultCacheExpiry() {
        return defaultCacheExpiry;
    }

    public void setDefaultCacheExpiry(long defaultCacheExpiry) {
        this.defaultCacheExpiry = defaultCacheExpiry;
    }

    public int getDefaultConnectTimeout() {
        return defaultConnectTimeout;
    }

    public void setDefaultConnectTimeout(int defaultConnectTimeout) {
        this.defaultConnectTimeout = defaultConnectTimeout;
    }

    public int getDefaultReadTimeout() {
        return defaultReadTimeout;
    }

    public void setDefaultReadTimeout(int defaultReadTimeout) {
        this.defaultReadTimeout = defaultReadTimeout;
    }

    public BitmapCache getBitmapCache() {
        if (bitmapCache == null) {
            bitmapCache = new BitmapCache(this);
        }
        return bitmapCache;
    }

    public int getMemoryCacheSize() {
        return memoryCacheSize;
    }

    public void setMemoryCacheSize(int memoryCacheSize) {
        if (memoryCacheSize >= MIN_MEMORY_CACHE_SIZE) {
            this.memoryCacheSize = memoryCacheSize;
            if (bitmapCache != null) {
                bitmapCache.setMemoryCacheSize(this.memoryCacheSize);
            }
        } else {
            this.setMemCacheSizePercent(0.3f);// Set default memory cache size.
        }
    }

    /**
     * @param percent between 0.05 and 0.8 (inclusive)
     */
    public void setMemCacheSizePercent(float percent) {
        if (percent < 0.05f || percent > 0.8f) {
            throw new IllegalArgumentException("percent must be between 0.05 and 0.8 (inclusive)");
        }
        this.memoryCacheSize = Math.round(percent * getMemoryClass() * 1024 * 1024);
        if (bitmapCache != null) {
            bitmapCache.setMemoryCacheSize(this.memoryCacheSize);
        }
    }

    public int getDiskCacheSize() {
        return diskCacheSize;
    }

    public void setDiskCacheSize(int diskCacheSize) {
        if (diskCacheSize >= MIN_DISK_CACHE_SIZE) {
            this.diskCacheSize = diskCacheSize;
            if (bitmapCache != null) {
                bitmapCache.setDiskCacheSize(this.diskCacheSize);
            }
        }
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        if (threadPoolSize > 0 && threadPoolSize != this.threadPoolSize) {
            _dirty_params_bitmapLoadExecutor = true;
            this.threadPoolSize = threadPoolSize;
        }
    }

    public ExecutorService getBitmapLoadExecutor() {
        if (_dirty_params_bitmapLoadExecutor || bitmapLoadExecutor == null) {
            bitmapLoadExecutor = Executors.newFixedThreadPool(getThreadPoolSize(), sThreadFactory);
            _dirty_params_bitmapLoadExecutor = false;
        }
        return bitmapLoadExecutor;
    }

    public boolean isMemoryCacheEnabled() {
        return memoryCacheEnabled;
    }

    public void setMemoryCacheEnabled(boolean memoryCacheEnabled) {
        this.memoryCacheEnabled = memoryCacheEnabled;
    }

    public boolean isDiskCacheEnabled() {
        return diskCacheEnabled;
    }

    public void setDiskCacheEnabled(boolean diskCacheEnabled) {
        this.diskCacheEnabled = diskCacheEnabled;
    }

    public LruDiskCache.DiskCacheFileNameGenerator getDiskCacheFileNameGenerator() {
        return diskCacheFileNameGenerator;
    }

    public void setDiskCacheFileNameGenerator(LruDiskCache.DiskCacheFileNameGenerator diskCacheFileNameGenerator) {
        this.diskCacheFileNameGenerator = diskCacheFileNameGenerator;
        if (bitmapCache != null) {
            bitmapCache.setDiskCacheFileNameGenerator(diskCacheFileNameGenerator);
        }
    }

    public BitmapCacheListener getBitmapCacheListener() {
        return bitmapCacheListener;
    }

    public void setBitmapCacheListener(BitmapCacheListener bitmapCacheListener) {
        this.bitmapCacheListener = bitmapCacheListener;
    }

    /**
     * 获取APP默认内存大小.根据手机内存大小返回的值也不一样.最少是16MB.其他可能是24MB或者更大.
     * PS:似乎是 8 的倍数.
     *
     * @return
     */
    private int getMemoryClass() {
        return ((ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
    }


    public void clearCache() {
        new BitmapCacheManagementTask().execute(BitmapCacheManagementTask.MESSAGE_CLEAR);
    }

    public void clearMemoryCache() {
        new BitmapCacheManagementTask().execute(BitmapCacheManagementTask.MESSAGE_CLEAR_MEMORY);
    }

    public void clearDiskCache() {
        new BitmapCacheManagementTask().execute(BitmapCacheManagementTask.MESSAGE_CLEAR_DISK);
    }

    public void clearCache(String uri) {
        new BitmapCacheManagementTask().execute(BitmapCacheManagementTask.MESSAGE_CLEAR_BY_KEY, uri);
    }

    public void clearMemoryCache(String uri) {
        new BitmapCacheManagementTask().execute(BitmapCacheManagementTask.MESSAGE_CLEAR_MEMORY_BY_KEY, uri);
    }

    public void clearDiskCache(String uri) {
        new BitmapCacheManagementTask().execute(BitmapCacheManagementTask.MESSAGE_CLEAR_DISK_BY_KEY, uri);
    }

    public void flushCache() {
        new BitmapCacheManagementTask().execute(BitmapCacheManagementTask.MESSAGE_FLUSH);
    }

    public void closeCache() {
        new BitmapCacheManagementTask().execute(BitmapCacheManagementTask.MESSAGE_CLOSE);
    }

    /**
     * 管理Bitmap缓存类
     */
    private class BitmapCacheManagementTask extends AsyncTask<Object, Void, Object[]> {
        public static final int MESSAGE_INIT_MEMORY_CACHE = 0;
        public static final int MESSAGE_INIT_DISK_CACHE = 1;
        public static final int MESSAGE_FLUSH = 2;
        public static final int MESSAGE_CLOSE = 3;
        public static final int MESSAGE_CLEAR = 4;
        public static final int MESSAGE_CLEAR_MEMORY = 5;
        public static final int MESSAGE_CLEAR_DISK = 6;
        public static final int MESSAGE_CLEAR_BY_KEY = 7;
        public static final int MESSAGE_CLEAR_MEMORY_BY_KEY = 8;
        public static final int MESSAGE_CLEAR_DISK_BY_KEY = 9;

        @Override
        protected Object[] doInBackground(Object... params) {
            if (params == null || params.length == 0) return params;
            BitmapCache cache = getBitmapCache();
            if (cache == null) return params;
            try {
                switch ((Integer) params[0]) {
                    case MESSAGE_INIT_MEMORY_CACHE:
                        cache.initMemoryCache();
                        break;
                    case MESSAGE_INIT_DISK_CACHE:
                        cache.initDiskCache();
                        break;
                    case MESSAGE_FLUSH:
                        cache.flush();
                        break;
                    case MESSAGE_CLOSE:
                        cache.clearMemoryCache();
                        cache.close();
                        break;
                    case MESSAGE_CLEAR:
                        cache.clearCache();
                        break;
                    case MESSAGE_CLEAR_MEMORY:
                        cache.clearMemoryCache();
                        break;
                    case MESSAGE_CLEAR_DISK:
                        cache.clearDiskCache();
                        break;
                    case MESSAGE_CLEAR_BY_KEY:
                        if (params.length != 2) {
                            return params;
                        }
                        cache.clearCache(String.valueOf(params[1]));
                        break;
                    case MESSAGE_CLEAR_MEMORY_BY_KEY:
                        if (params.length != 2) return params;
                        cache.clearMemoryCache(String.valueOf(params[1]));
                        break;
                    case MESSAGE_CLEAR_DISK_BY_KEY:
                        if (params.length != 2) return params;
                        cache.clearDiskCache(String.valueOf(params[1]));
                        break;
                    default:
                        break;
                }
            } catch (Throwable e) {
                LogUtils.e(e.getMessage(), e);
            }
            return params;
        }

        @Override
        protected void onPostExecute(Object[] params) {
            if (bitmapCacheListener == null || params == null || params.length == 0) return;
            try {
                switch ((Integer) params[0]) {
                    case MESSAGE_INIT_MEMORY_CACHE:
                        bitmapCacheListener.onInitMemoryCacheFinished();
                        break;
                    case MESSAGE_INIT_DISK_CACHE:
                        bitmapCacheListener.onInitDiskFinished();
                        break;
                    case MESSAGE_FLUSH:
                        bitmapCacheListener.onFlushCacheFinished();
                        break;
                    case MESSAGE_CLOSE:
                        bitmapCacheListener.onCloseCacheFinished();
                        break;
                    case MESSAGE_CLEAR:
                        bitmapCacheListener.onClearCacheFinished();
                        break;
                    case MESSAGE_CLEAR_MEMORY:
                        bitmapCacheListener.onClearMemoryCacheFinished();
                        break;
                    case MESSAGE_CLEAR_DISK:
                        bitmapCacheListener.onClearDiskCacheFinished();
                        break;
                    case MESSAGE_CLEAR_BY_KEY:
                        if (params.length != 2) return;
                        bitmapCacheListener.onClearCacheFinished(String.valueOf(params[1]));
                        break;
                    case MESSAGE_CLEAR_MEMORY_BY_KEY:
                        if (params.length != 2) return;
                        bitmapCacheListener.onClearMemoryCacheFinished(String.valueOf(params[1]));
                        break;
                    case MESSAGE_CLEAR_DISK_BY_KEY:
                        if (params.length != 2) return;
                        bitmapCacheListener.onClearDiskCacheFinished(String.valueOf(params[1]));
                        break;
                    default:
                        break;
                }
            } catch (Throwable e) {
                LogUtils.e(e.getMessage(), e);
            }
        }
    }
}