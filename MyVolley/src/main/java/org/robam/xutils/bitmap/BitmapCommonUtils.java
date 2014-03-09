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

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.robam.xutils.Utils.LogUtils;
import org.robam.xutils.bitmap.core.BitmapSize;

import java.io.File;
import java.lang.reflect.Field;

public class BitmapCommonUtils {

    /**
     * @param context
     * @param dirName Only the folder name, not full path.
     * @return app_cache_path/dirName
     */
    public static String getDiskCacheDir(Context context, String dirName) {
        String cachePath = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File externalCacheDir = context.getExternalCacheDir();
            if (externalCacheDir != null) {
                cachePath = externalCacheDir.getPath();
            }
        }
        if (cachePath == null) {
            cachePath = context.getCacheDir().getPath();
        }

        return cachePath + File.separator + dirName;
    }

    public static long getAvailableSpace(File dir) {
        try {
            final StatFs stats = new StatFs(dir.getPath());
            return (long) stats.getBlockSize() * (long) stats.getAvailableBlocks();
        } catch (Throwable e) {
            LogUtils.e(e.getMessage(), e);
            return -1;
        }

    }

    private static BitmapSize screenSize = null;

    /**
     * 获取屏幕大小
     *
     * @param context
     * @return
     */
    public static BitmapSize getScreenSize(Context context) {
        if (screenSize == null) {
            screenSize = new BitmapSize();
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            screenSize.setWidth(displayMetrics.widthPixels);
            screenSize.setHeight(displayMetrics.heightPixels);
        }
        return screenSize;
    }

    /**
     * 根据View,最优化图片显示大小.1.如果传递过来的Width或Height大于0,就直接用.2.获取view的Width,>0,直接使用.3.view==Match_parent,view.getwidth(),如>
     * 0,OK.4.利用反射获取width或者height.5.实在没办法了,就用屏幕的分辨率啊!
     *
     * @param view
     * @param maxImageWidth
     * @param maxImageHeight
     * @return
     */
    public static BitmapSize optimizeMaxSizeByView(View view, int maxImageWidth, int maxImageHeight) {
        int width = maxImageWidth;
        int height = maxImageHeight;

        //Why只要传递过来的大于0就返回了?
        if (width > 0 && height > 0) {
            return new BitmapSize(width, height);
        }

        final ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params != null) {
            if (params.width > 0) {
                //Width有确定的值
                width = params.width;
            } else if (params.width != ViewGroup.LayoutParams.WRAP_CONTENT) {
                //当等于MatchParent
                width = view.getWidth();
            }

            if (params.height > 0) {
                height = params.height;
            } else if (params.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
                height = view.getHeight();
            }
        }

        //view == wrap_content就要这种非常手段?
        if (width <= 0) {
            width = getImageViewFieldValue(view, "mMaxWidth");
        }
        if (height <= 0) {
            height = getImageViewFieldValue(view, "mMaxHeight");
        }

        //获取屏幕分辩率
        BitmapSize screenSize = getScreenSize(view.getContext());
        if (width <= 0) {
            width = screenSize.getWidth();
        }
        if (height <= 0) {
            height = screenSize.getHeight();
        }

        return new BitmapSize(width, height);
    }

    /**
     * 利用反射获取ImageView的宽度或高度.
     *
     * @param object    ImageView
     * @param fieldName 字段名 mMaxHeight || mMaxWidth
     * @return height or width
     */
    private static int getImageViewFieldValue(Object object, String fieldName) {
        int value = 0;
        if (object instanceof ImageView) {
            try {
                //通过反射获取
                Field field = ImageView.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                int fieldValue = (Integer) field.get(object);
                if (fieldValue > 0 && fieldValue < Integer.MAX_VALUE) {
                    value = fieldValue;
                }
            } catch (Throwable e) {
            }
        }
        return value;
    }
}
