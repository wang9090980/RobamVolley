/*******************************************************************************
 * Copyright 2011-2013 Sergey Tarasevich
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.robam.xutils.bitmap;

import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;

import org.robam.xutils.BitmapUtils;

public class PauseOnScrollListener implements OnScrollListener {

    private BitmapUtils bitmapUtils;

    private final boolean pauseOnScroll;
    private final boolean pauseOnFling;
    private final OnScrollListener externalListener;

    /**
     * Constructor
     *
     * @param bitmapUtils   {@linkplain BitmapUtils} instance for controlling
     * @param pauseOnScroll Whether {@linkplain BitmapUtils#pauseTasks() pause loading} during touch scrolling
     * @param pauseOnFling  Whether {@linkplain BitmapUtils#pauseTasks() pause loading} during fling
     */
    public PauseOnScrollListener(BitmapUtils bitmapUtils, boolean pauseOnScroll, boolean pauseOnFling) {
        this(bitmapUtils, pauseOnScroll, pauseOnFling, null);
    }

    /**
     * Constructor
     *
     * @param bitmapUtils    {@linkplain BitmapUtils} instance for controlling
     * @param pauseOnScroll  Whether {@linkplain BitmapUtils#pauseTasks() pause loading} during touch scrolling
     * @param pauseOnFling   Whether {@linkplain BitmapUtils#pauseTasks() pause loading} during fling
     * @param customListener Your custom {@link android.widget.AbsListView.OnScrollListener} for {@linkplain android.widget.AbsListView list view} which also will
     *                       be get scroll events
     */
    public PauseOnScrollListener(BitmapUtils bitmapUtils, boolean pauseOnScroll, boolean pauseOnFling, OnScrollListener customListener) {
        this.bitmapUtils = bitmapUtils;
        this.pauseOnScroll = pauseOnScroll;
        this.pauseOnFling = pauseOnFling;
        externalListener = customListener;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        switch (scrollState) {
            case OnScrollListener.SCROLL_STATE_IDLE:
                bitmapUtils.resumeTasks();
                break;
            case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
                if (pauseOnScroll) {
                    bitmapUtils.pauseTasks();
                }
                break;
            case OnScrollListener.SCROLL_STATE_FLING:
                if (pauseOnFling) {
                    bitmapUtils.pauseTasks();
                }
                break;
        }
        if (externalListener != null) {
            externalListener.onScrollStateChanged(view, scrollState);
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (externalListener != null) {
            externalListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
        }
    }
}
