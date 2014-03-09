package org.robam.xutils.bitmap;

public interface BitmapCacheListener {
    void onInitMemoryCacheFinished();

    void onInitDiskFinished();

    void onClearCacheFinished();

    void onClearMemoryCacheFinished();

    void onClearDiskCacheFinished();

    void onClearCacheFinished(String uri);

    void onClearMemoryCacheFinished(String uri);

    void onClearDiskCacheFinished(String uri);

    void onFlushCacheFinished();

    void onCloseCacheFinished();
}
