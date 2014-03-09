package org.robam.robamvolley.testvolley;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.DownloadRequest;
import com.android.volley.toolbox.Volley;

import java.io.File;

/**
 * Created by weiji.chen on 14-3-9.
 */
public class DownloadTest extends Activity {

    private String[] urls = {
            "http://yuzile.qiniudn.com/artworks_imgimg%20(10).jpg",
            "http://yuzile.qiniudn.com/artworks_imgimg%20(40).jpg",
            "http://yuzile.qiniudn.com/artworks_imgimg%20(41).jpg",
            "http://yuzile.qiniudn.com/artworks_imgimg%20(42).jpg",
            "http://yuzile.qiniudn.com/artworks_imgimg%20(43).jpg",
            "http://yuzile.qiniudn.com/artworks_imgimg%20(44).jpg",
            "http://yuzile.qiniudn.com/artworks_imgimg%20(45).jpg",
            "http://yuzile.qiniudn.com/artworks_imgimg%20(46).jpg",
            "http://yuzile.qiniudn.com/artworks_imgimg%20(47).jpg",
            "http://yuzile.qiniudn.com/artworks_imgimg%20(48).jpg",
            "http://yuzile.qiniudn.com/artworks_imgimg%20(49).jpg",
            "http://yuzile.qiniudn.com/artworks_imgimg%20(60).jpg",
            "http://yuzile.qiniudn.com/artworks_imgimg%20(61).jpg",
            "http://yuzile.qiniudn.com/artworks_imgimg%20(62).jpg",
            "http://yuzile.qiniudn.com/artworks_imgimg%20(63).jpg",
            "http://yuzile.qiniudn.com/artworks_imgimg%20(64).jpg",
            "http://yuzile.qiniudn.com/artworks_imgimg%20(65).jpg",
            "http://yuzile.qiniudn.com/artworks_imgimg%20(66).jpg",
            "http://yuzile.qiniudn.com/artworks_imgimg%20(67).jpg",
            "http://yuzile.qiniudn.com/artworks_imgimg%20(68).jpg",
            "http://yuzile.qiniudn.com/artworks_imgimg%20(69).jpg",
            "http://yuzile.qiniudn.com/artworks_imgimg%20(50).jpg",
            "http://yuzile.qiniudn.com/artworks_imgimg%20(51).jpg",
            "http://yuzile.qiniudn.com/artworks_imgimg%20(52).jpg",
            "http://yuzile.qiniudn.com/artworks_imgimg%20(53).jpg",
            "http://yuzile.qiniudn.com/artworks_imgimg%20(54).jpg",
            "http://yuzile.qiniudn.com/artworks_imgimg%20(55).jpg",
            "http://yuzile.qiniudn.com/artworks_imgimg%20(56).jpg",
            "http://yuzile.qiniudn.com/artworks_imgimg%20(57).jpg",
            "http://yuzile.qiniudn.com/artworks_imgimg%20(58).jpg",
            "http://yuzile.qiniudn.com/artworks_imgimg%20(59).jpg"
    };

    private final static String largePic = "http://yuzile.qiniudn.com/artworks_imgimg%20(91).jpg";

    RequestQueue mRequestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView textView = new TextView(this);
        setContentView(textView);
        mRequestQueue = Volley.newRequestQueue(this, 3);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //        DownloadRequest downloadRequest = new DownloadRequest(
//                largePic,
//                getActivity().getExternalFilesDir(null).getAbsolutePath() + File.separator + "Large" + ".jpg",
//                new Response.Listener() {
//                    @Override
//                    public void onResponse(Object response) {
//                        File file = (File) response;
//                        LogUtils.i("Download OK! file Path = " + file.getAbsolutePath());
//                    }
//                },
//                new Response.ErrorListener() {
//                    @Override
//                    public void onErrorResponse(VolleyError error) {
//                        LogUtils.i("Download error. error = " + error.toString());
//                    }
//                },
//                new Response.ProgressListener() {
//                    @Override
//                    public void onProgressing(long current, long total) {
//                        LogUtils.i("下载进度. current = " + current + ", total = " + total);
//                    }
//                }
//        );
//        mRequestQueue.add(downloadRequest);
        for (int i = 0; i < urls.length; i++) {
            DownloadRequest downloadRequest = new DownloadRequest(
                    urls[i],
                    getExternalFilesDir(null).getAbsolutePath() + File.separator + "hehe" + i + ".jpg",
                    new Response.Listener() {
                        @Override
                        public void onResponse(Object response) {
                            File file = (File) response;
                            VolleyLog.d("Download OK! file Path = " + file.getAbsolutePath());
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            VolleyLog.d("Download error. error = " + error.toString());
                        }
                    }
            );
            mRequestQueue.add(downloadRequest);
        }
    }
}
