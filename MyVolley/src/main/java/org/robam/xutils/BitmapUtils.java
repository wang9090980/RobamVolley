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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;

import org.robam.xutils.bitmap.BitmapCacheListener;
import org.robam.xutils.bitmap.BitmapCommonUtils;
import org.robam.xutils.bitmap.BitmapDisplayConfig;
import org.robam.xutils.bitmap.BitmapGlobalConfig;
import org.robam.xutils.bitmap.callback.BitmapLoadCallBack;
import org.robam.xutils.bitmap.callback.BitmapLoadFrom;
import org.robam.xutils.bitmap.callback.SimpleBitmapLoadCallBack;
import org.robam.xutils.bitmap.core.AsyncDrawable;
import org.robam.xutils.bitmap.core.BitmapSize;
import org.robam.xutils.bitmap.download.Downloader;
import org.robam.xutils.core.CompatibleAsyncTask;
import org.robam.xutils.core.LruDiskCache;

import java.io.File;
import java.lang.ref.WeakReference;


public class BitmapUtils {

    /**
     * 控制是否暂停Task的变量
     */
    private boolean pauseTask = false;

    /**
     * Task的控制锁.防止同时读写
     */
    private final Object pauseTaskLock = new Object();

    private Context context;

    /**
     * 全局配置.
     */
    private BitmapGlobalConfig globalConfig;

    /**
     * TODO:???
     */
    private BitmapDisplayConfig defaultDisplayConfig;

    /////////////////////////////////////////////// create ///////////////////////////////////////////////////

    /**
     * 构造方法.产生一个BitmapUtils对象.使用默认的磁盘缓存路径.
     *
     * @param context 上下文,最好是Application级别的.
     */
    public BitmapUtils(Context context) {
        this(context, null);
    }

    /**
     * 构造方法.其他的构造方法也都调用了这里.
     *
     * @param context       上下文.
     * @param diskCachePath 磁盘缓存路径.
     */
    public BitmapUtils(Context context, String diskCachePath) {
        if (context == null) {
            throw new IllegalArgumentException("context may not be null");
        }

        this.context = context;
        //初始化全局配置.
        globalConfig = new BitmapGlobalConfig(context, diskCachePath);
        //初始化显示配置.
        defaultDisplayConfig = new BitmapDisplayConfig();
    }

    /**
     * 构造方法.
     *
     * @param context         上下文.
     * @param diskCachePath   磁盘缓存路径.
     * @param memoryCacheSize 内存缓存大小.
     */
    public BitmapUtils(Context context, String diskCachePath, int memoryCacheSize) {
        this(context, diskCachePath);
        globalConfig.setMemoryCacheSize(memoryCacheSize);
    }

    /**
     * 构造方法.
     *
     * @param context         上下文.最好是Application级别的.
     * @param diskCachePath   磁盘缓存路径.
     * @param memoryCacheSize 内存缓存大小.
     * @param diskCacheSize   磁盘缓存大小.
     */
    public BitmapUtils(Context context, String diskCachePath, int memoryCacheSize, int diskCacheSize) {
        this(context, diskCachePath);
        globalConfig.setMemoryCacheSize(memoryCacheSize);
        globalConfig.setDiskCacheSize(diskCacheSize);
    }

    /**
     * 构造方法.
     *
     * @param context            上下文.
     * @param diskCachePath      磁盘缓存路径.
     * @param memoryCachePercent 内存缓存大小比重. 0.05 <= memoryCachePercent <= 0.8.(Default Memory >= 16MB).
     */
    public BitmapUtils(Context context, String diskCachePath, float memoryCachePercent) {
        this(context, diskCachePath);
        globalConfig.setMemCacheSizePercent(memoryCachePercent);
    }

    /**
     * 构造方法.
     *
     * @param context            上下文.
     * @param diskCachePath      磁盘缓存路径.
     * @param memoryCachePercent 内存缓存比重.0.05<= && <=0.8
     * @param diskCacheSize      磁盘缓存大小.
     */
    public BitmapUtils(Context context, String diskCachePath, float memoryCachePercent, int diskCacheSize) {
        this(context, diskCachePath);
        globalConfig.setMemCacheSizePercent(memoryCachePercent);
        globalConfig.setDiskCacheSize(diskCacheSize);
    }

    //////////////////////////////////////// config ////////////////////////////////////////////////////////////////////

    /**
     * 设置默认下载中图片.
     *
     * @param drawable 要显示的图片.
     * @return BitmapUtils当前实例.
     */
    public BitmapUtils configDefaultLoadingImage(Drawable drawable) {
        defaultDisplayConfig.setLoadingDrawable(drawable);
        return this;
    }

    /**
     * 设置下载中图片.
     *
     * @param resId 图片资源ID.
     * @return BitmapUtils实例.这样做的原因是:后面还可以接.号哦.经常看到的连起来操作的就是这样的.比如:Nofication
     */
    public BitmapUtils configDefaultLoadingImage(int resId) {
        defaultDisplayConfig.setLoadingDrawable(context.getResources().getDrawable(resId));
        return this;
    }

    /**
     * 设置下载中图片.
     *
     * @param bitmap 图片Bitmap
     * @return BitmapUtils实例.
     */
    public BitmapUtils configDefaultLoadingImage(Bitmap bitmap) {
        defaultDisplayConfig.setLoadingDrawable(new BitmapDrawable(context.getResources(), bitmap));
        return this;
    }

    /**
     * 设置下载失败图片.
     *
     * @param drawable 图片Drawable.
     * @return BitmapUtils实例.
     */
    public BitmapUtils configDefaultLoadFailedImage(Drawable drawable) {
        defaultDisplayConfig.setLoadFailedDrawable(drawable);
        return this;
    }

    /**
     * 设置下载失败图片.
     *
     * @param resId 图片资源ID.
     * @return BitmapUtils实例.
     */
    public BitmapUtils configDefaultLoadFailedImage(int resId) {
        defaultDisplayConfig.setLoadFailedDrawable(context.getResources().getDrawable(resId));
        return this;
    }

    /**
     * 设置下载失败图片.
     *
     * @param bitmap 图片Bitmap.
     * @return BitmapUtils实例.
     */
    public BitmapUtils configDefaultLoadFailedImage(Bitmap bitmap) {
        defaultDisplayConfig.setLoadFailedDrawable(new BitmapDrawable(context.getResources(), bitmap));
        return this;
    }

    /**
     * 限制图片最大大小
     *
     * @param maxWidth  最大宽度
     * @param maxHeight 最大高度
     * @return BitmapUtils实例
     */
    public BitmapUtils configDefaultBitmapMaxSize(int maxWidth, int maxHeight) {
        defaultDisplayConfig.setBitmapMaxSize(new BitmapSize(maxWidth, maxHeight));
        return this;
    }

    /**
     * 限制图片最大大小
     *
     * @param maxSize 大小
     * @return BitmapUtils实例
     */
    public BitmapUtils configDefaultBitmapMaxSize(BitmapSize maxSize) {
        defaultDisplayConfig.setBitmapMaxSize(maxSize);
        return this;
    }

    /**
     * 设置图片下载动画
     * TODO:下载中?
     *
     * @param animation
     * @return BitmapUtils实例
     */
    public BitmapUtils configDefaultImageLoadAnimation(Animation animation) {
        defaultDisplayConfig.setAnimation(animation);
        return this;
    }

    /**
     * TODO:图片翻转吗?
     *
     * @param autoRotation
     * @return BitmapUtils实例
     */
    public BitmapUtils configDefaultAutoRotation(boolean autoRotation) {
        defaultDisplayConfig.setAutoRotation(autoRotation);
        return this;
    }

    /**
     * TODO:???
     *
     * @param showOriginal
     * @return BitmapUtils实例
     */
    public BitmapUtils configDefaultShowOriginal(boolean showOriginal) {
        defaultDisplayConfig.setShowOriginal(showOriginal);
        return this;
    }

    /**
     * 好像就是图片显示模式吧,比如:ARGB_8888.
     *
     * @param config 显示模式.
     * @return BitmapUtils实例.
     */
    public BitmapUtils configDefaultBitmapConfig(Bitmap.Config config) {
        defaultDisplayConfig.setBitmapConfig(config);
        return this;
    }

    /**
     * 直接设置整个显示配置啊.
     *
     * @param displayConfig 显示配置.
     * @return BitmapUtils实例.
     */
    public BitmapUtils configDefaultDisplayConfig(BitmapDisplayConfig displayConfig) {
        defaultDisplayConfig = displayConfig;
        return this;
    }

    /**
     * 设置Downloader.
     *
     * @param downloader Downloader
     * @return BitmapUtils实例
     */
    public BitmapUtils configDownloader(Downloader downloader) {
        globalConfig.setDownloader(downloader);
        return this;
    }

    /**
     * 设置缓存有效期.
     *
     * @param defaultExpiry 有效期.单位:ms
     * @return BitmapUtils实例
     */
    public BitmapUtils configDefaultCacheExpiry(long defaultExpiry) {
        globalConfig.setDefaultCacheExpiry(defaultExpiry);
        return this;
    }

    /**
     * 设置下载超时时间
     *
     * @param connectTimeout 下载超时时间.单位:ms
     * @return BitmapUtils实例
     */
    public BitmapUtils configDefaultConnectTimeout(int connectTimeout) {
        globalConfig.setDefaultConnectTimeout(connectTimeout);
        return this;
    }

    /**
     * 设置读取超时时间
     *
     * @param readTimeout 读取超时时间.单位:ms
     * @return BitmapUtils实例
     */
    public BitmapUtils configDefaultReadTimeout(int readTimeout) {
        globalConfig.setDefaultReadTimeout(readTimeout);
        return this;
    }

    /**
     * 配置线程池大小.即同时可以开多少个线程.
     *
     * @param threadPoolSize 线程池大小
     * @return BitmapUtils实例
     */
    public BitmapUtils configThreadPoolSize(int threadPoolSize) {
        globalConfig.setThreadPoolSize(threadPoolSize);
        return this;
    }

    /**
     * 是否开启内存缓存
     *
     * @param enabled true:开启.false:关闭
     * @return BitmapUtils实例
     */
    public BitmapUtils configMemoryCacheEnabled(boolean enabled) {
        globalConfig.setMemoryCacheEnabled(enabled);
        return this;
    }

    /**
     * 是否开启磁盘缓存
     *
     * @param enabled true:开启.false:关闭
     * @return BitmapUtils实例
     */
    public BitmapUtils configDiskCacheEnabled(boolean enabled) {
        globalConfig.setDiskCacheEnabled(enabled);
        return this;
    }

    /**
     * TODO:???
     *
     * @param diskCacheFileNameGenerator
     * @return BitmapUtils实例
     */
    public BitmapUtils configDiskCacheFileNameGenerator(LruDiskCache.DiskCacheFileNameGenerator diskCacheFileNameGenerator) {
        globalConfig.setDiskCacheFileNameGenerator(diskCacheFileNameGenerator);
        return this;
    }

    /**
     * 设置图片缓存监听器
     *
     * @param listener 缓存监听器
     * @return BitmapUtils实例
     */
    public BitmapUtils configBitmapCacheListener(BitmapCacheListener listener) {
        globalConfig.setBitmapCacheListener(listener);
        return this;
    }

    /**
     * 直接设置整个配置
     *
     * @param globalConfig 全局配置
     * @return BitmapUtils实例
     */
    public BitmapUtils configGlobalConfig(BitmapGlobalConfig globalConfig) {
        this.globalConfig = globalConfig;
        return this;
    }

    /*************************** 显示 ***************************************/

    /**
     * 显示图片
     *
     * @param container 图片控件,如ImageView,extends View
     * @param uri       图片Url
     * @param <T>       ???
     */
    public <T extends View> void display(T container, String uri) {
        display(container, uri, null, null);
    }

    /**
     * 显示图片
     *
     * @param container     图片控件,如ImageView,extends View
     * @param uri           图片Url
     * @param displayConfig 显示配置
     */
    public <T extends View> void display(T container, String uri, BitmapDisplayConfig displayConfig) {
        display(container, uri, displayConfig, null);
    }

    public <T extends View> void display(T container, String uri, BitmapLoadCallBack<T> callBack) {
        display(container, uri, null, callBack);
    }

    /**
     * 显示图片.其他都是调用这里的,所以这才是重点
     *
     * @param container     图片控件,如ImageView,extends View
     * @param uri           图片Url
     * @param displayConfig 显示配置
     * @param callBack      下载回调函数
     * @param <T>           ???
     */
    public <T extends View> void display(T container, String uri, BitmapDisplayConfig displayConfig, BitmapLoadCallBack<T> callBack) {
        // 连显示的View都为空,那肯定返回了
        if (container == null) {
            return;
        }

        // 清除所有的动画
        container.clearAnimation();

        // TODO:为什么当CallBack是空的时候需要new一个呢?
        if (callBack == null) {
            callBack = new SimpleBitmapLoadCallBack<T>();
        }

        // 为什么要克隆一份?难道是:不同的图片可能有不同的配置,所以不能混用
        if (displayConfig == null || displayConfig == defaultDisplayConfig) {
            displayConfig = defaultDisplayConfig.cloneNew();
        }

        // Optimize Max Size
        // TODO:难点
        BitmapSize size = displayConfig.getBitmapMaxSize();
        displayConfig.setBitmapMaxSize(BitmapCommonUtils.optimizeMaxSizeByView(container, size.getWidth(), size.getHeight()));

        // 调用回调方法
        callBack.onPreLoad(container, uri, displayConfig);

        // 累了半天,发现uri是空就直接返回啊?
        // TODO:应该优化一下,判断uri是否为空
        if (TextUtils.isEmpty(uri)) {
            // 其实调用失败的回调,还设置了失败的之后的图片.
            callBack.onLoadFailed(container, uri, displayConfig.getLoadFailedDrawable());
            return;
        }

        // 先从内存缓存获取图片,注意:只是从内存而已.
        Bitmap bitmap = globalConfig.getBitmapCache().getBitmapFromMemCache(uri, displayConfig);


        if (bitmap != null) {
            // 说明从缓存已经获取到了.直接设置.
            callBack.onLoadStarted(container, uri, displayConfig);
            callBack.onLoadCompleted(container, uri, bitmap, displayConfig, BitmapLoadFrom.MEMORY_CACHE);
        } else if (!bitmapLoadTaskExist(container, uri, callBack)) {

            // 如果不在下载线程,当然需要下载了.BitmapLoadTask是重点.
            final BitmapLoadTask<T> loadTask = new BitmapLoadTask<T>(container, uri, displayConfig, callBack);

            // 异步设置图片,这也很重要,为什么能异步设置呢?
            final AsyncDrawable<T> asyncDrawable = new AsyncDrawable<T>(
                    displayConfig.getLoadingDrawable(),
                    loadTask);
            // TODO:???
            callBack.setDrawable(container, asyncDrawable);

            // 从网络下载或者是从磁盘缓存读取.磁盘缓存真的也要开一个线程?
            loadTask.executeOnExecutor(globalConfig.getBitmapLoadExecutor());
        }
    }

    /////////////////////////////////////////////// cache /////////////////////////////////////////////////////////////////

    /**
     * 清空 内存 & 磁盘的缓存
     */
    public void clearCache() {
        globalConfig.clearCache();
    }

    /**
     * 清空内存缓存
     */
    public void clearMemoryCache() {
        globalConfig.clearMemoryCache();
    }

    /**
     * 清空磁盘缓存
     */
    public void clearDiskCache() {
        globalConfig.clearDiskCache();
    }


    /**
     * 清除指定Uri的图片,磁盘和内存缓存的.
     *
     * @param uri
     */
    public void clearCache(String uri) {
        globalConfig.clearCache(uri);
    }

    /**
     * 清除指定Uri的图片,内存缓存.
     *
     * @param uri
     */
    public void clearMemoryCache(String uri) {
        globalConfig.clearMemoryCache(uri);
    }

    /**
     * 清除指定Uri的图片,磁盘缓存的.
     *
     * @param uri
     */
    public void clearDiskCache(String uri) {
        globalConfig.clearDiskCache(uri);
    }


    /**
     * Flushes the disk cache
     * TODO:难道只是清除磁盘的未下载完的临时文件?
     */
    public void flushCache() {
        globalConfig.flushCache();
    }

    /**
     * 关闭缓存.其实是先清空了内存缓存,再加磁盘缓存一把锁.磁盘缓存并没有清除.(内存 + 磁盘).
     */
    public void closeCache() {
        globalConfig.closeCache();
    }

    /**
     * 根据Uri生成File,相当于只是一个路径而已.
     *
     * @param uri
     * @return
     */
    public File getBitmapFileFromDiskCache(String uri) {
        return globalConfig.getBitmapCache().getBitmapFileFromDiskCache(uri);
    }


    public Bitmap getBitmapFromMemCache(String uri, BitmapDisplayConfig config) {
        if (config == null) {
            config = defaultDisplayConfig;
        }
        return globalConfig.getBitmapCache().getBitmapFromMemCache(uri, config);
    }

    ////////////////////////////////////////// tasks //////////////////////////////////////////////////////////////////////

    /**
     * 恢复 Task
     */
    public void resumeTasks() {
        pauseTask = false;
        synchronized (pauseTaskLock) {
            // 通知所有在pauseTaskLock阻塞停止的线程,唤醒他们.
            pauseTaskLock.notifyAll();
        }
    }

    /**
     * 暂停 Task.意味着没下载完的但是暂停了就需要同时下载?太不人性了吧.
     */
    public void pauseTasks() {
        pauseTask = true;
        // 好像只是清除未下载完的缓存文件哦
        flushCache();
    }

    /**
     * 停止 Task
     */
    public void stopTasks() {
        pauseTask = true;
        synchronized (pauseTaskLock) {
            pauseTaskLock.notifyAll();
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("unchecked")
    private static <T extends View> BitmapLoadTask<T> getBitmapTaskFromContainer(T container, BitmapLoadCallBack<T> callBack) {
        if (container != null) {
            final Drawable drawable = callBack.getDrawable(container);
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable<T> asyncDrawable = (AsyncDrawable<T>) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    private static <T extends View> boolean bitmapLoadTaskExist(T container, String uri, BitmapLoadCallBack<T> callBack) {
        final BitmapLoadTask<T> oldLoadTask = getBitmapTaskFromContainer(container, callBack);

        if (oldLoadTask != null) {
            final String oldUrl = oldLoadTask.uri;
            if (TextUtils.isEmpty(oldUrl) || !oldUrl.equals(uri)) {
                oldLoadTask.cancel(true);
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * 图片加载.关键任务就在这里了.
     *
     * @param <T>
     */
    public class BitmapLoadTask<T extends View> extends CompatibleAsyncTask<Object, Object, Bitmap> {

        private static final int PROGRESS_LOAD_STARTED = 0;
        private static final int PROGRESS_LOADING = 1;

        private final String uri;

        /**
         * 这是弱引用.有可能已经释放掉了.ImageVeiw
         */
        private final WeakReference<T> containerReference;
        private final BitmapLoadCallBack<T> callBack;
        private final BitmapDisplayConfig displayConfig;

        /**
         * 从哪里来的.有三个地方.默认是磁盘缓存
         */
        private BitmapLoadFrom from = BitmapLoadFrom.DISK_CACHE;

        /**
         * 构造方法,返回一个线程对象,但是还没启动
         *
         * @param container 显示图片的控件.=ImageView
         * @param uri       图片Uri
         * @param config    显示图片的配置
         * @param callBack  回调.
         */
        public BitmapLoadTask(T container, String uri, BitmapDisplayConfig config, BitmapLoadCallBack<T> callBack) {
            if (container == null || uri == null || config == null || callBack == null) {
                throw new IllegalArgumentException("args may not be null");
            }

            this.containerReference = new WeakReference<T>(container);
            this.callBack = callBack;
            this.uri = uri;
            this.displayConfig = config;
        }

        @Override
        protected Bitmap doInBackground(Object... params) {

            //先看看是否停止了任务.
            synchronized (pauseTaskLock) {
                while (pauseTask && !this.isCancelled()) {
                    try {
                        //如果暂停了需要等待.这里又学会了一招
                        pauseTaskLock.wait();
                    } catch (Throwable e) {
                    }
                }
            }

            Bitmap bitmap = null;

            // 从磁盘缓存获取图片
            if (!this.isCancelled() && this.getTargetContainer() != null) {
                this.publishProgress(PROGRESS_LOAD_STARTED);
                bitmap = globalConfig.getBitmapCache().getBitmapFromDiskCache(uri, displayConfig);
            }

            // 下载图片
            if (bitmap == null && !this.isCancelled() && this.getTargetContainer() != null) {
                bitmap = globalConfig.getBitmapCache().downloadBitmap(uri, displayConfig, this);
                from = BitmapLoadFrom.URI;
            }

            return bitmap;
        }

        public void updateProgress(long total, long current) {
            this.publishProgress(PROGRESS_LOADING, total, current);
        }

        @Override
        protected void onProgressUpdate(Object... values) {
            if (values == null || values.length == 0) return;

            final T container = this.getTargetContainer();
            if (container == null) return;

            switch ((Integer) values[0]) {
                case PROGRESS_LOAD_STARTED:
                    callBack.onLoadStarted(container, uri, displayConfig);
                    break;
                case PROGRESS_LOADING:
                    if (values.length != 3) return;
                    callBack.onLoading(container, uri, displayConfig, (Long) values[1], (Long) values[2]);
                    break;
                default:
                    break;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            final T container = this.getTargetContainer();
            if (container != null) {
                if (bitmap != null) {
                    callBack.onLoadCompleted(
                            container,
                            this.uri,
                            bitmap,
                            displayConfig,
                            from);
                } else {
                    callBack.onLoadFailed(
                            container,
                            this.uri,
                            displayConfig.getLoadFailedDrawable());
                }
            }
        }

        @Override
        protected void onCancelled(Bitmap bitmap) {
            synchronized (pauseTaskLock) {
                pauseTaskLock.notifyAll();
            }
        }

        /**
         * TODO:???
         *
         * @return
         */
        public T getTargetContainer() {

            // 先看看有没有Container引用,可能被释放掉了.
            final T container = containerReference.get();

            final BitmapLoadTask<T> bitmapWorkerTask = getBitmapTaskFromContainer(container, callBack);

            if (this == bitmapWorkerTask) {
                return container;
            }

            return null;
        }
    }
}
