package com.android.volley.toolbox;

import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;

import com.android.volley.FileError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by weiji.chen on 14-3-9.
 * 文件下载的请求.请求的队列会加到默认的RequestQueue中.
 * 由于是下载的文件,所以默认不使用缓存,因为默认缓存大小只有10M.除非RequestQueue配置大的缓存.
 * 如果需要下载小文件图片,比如头像,小文件可以缓存.
 * TODO:因为使用了路径,避免多余的网络请求,所以要进行路径拦截.首先进行的是:判断路径是否有效,如果无效,直接return了.
 */
public class DownloadRequest extends Request<File> {

    Response.Listener listener;

    private String filePath;

    public DownloadRequest(String url, String filePath) {
        this(url, filePath, null, null, null);
    }

    public DownloadRequest(String url, String filePath, Response.Listener listener, Response.ErrorListener errorListener) {
        this(Method.GET, url, filePath, listener, errorListener, null);
    }

    public DownloadRequest(String url, String filePath, Response.Listener listener, Response.ErrorListener errorListener, Response.ProgressListener progressListener) {
        this(Method.GET, url, filePath, listener, errorListener, progressListener);
    }

    /**
     * 创建下载请求.
     *
     * @param method
     * @param url
     * @param filePath
     * @param errorListener
     * @param progressListener 下载进度监听.默认1s刷新一次.
     */
    public DownloadRequest(int method, String url, String filePath, Response.Listener listener, Response.ErrorListener errorListener, Response.ProgressListener progressListener) {
        super(method, url, errorListener, progressListener);
        this.filePath = filePath;
        this.listener = listener;
        // 如果路径不可用,就要取消下载.
        if (!validateFilePath(filePath)) {
            VolleyLog.d("The file path is invalited.");
            cancel();
        }
    }

    /**
     * 判断文件的路径是否可用,且有合适的内存空间.粗鲁一点,直接判断10M可用空间.不可用就不仅要下载了.
     *
     * @param filePath 将要保存的文件及文件名.
     * @return true:路径可用,false:路径不可用.
     */
    private boolean validateFilePath(String filePath) {
        //检测是否存在SD卡
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return false;
        }
        String parentPath = "";

        // 根据文件名获取该文件的目录
        if (!TextUtils.isEmpty(filePath)) {
            int lastSeparater = filePath.lastIndexOf('/');
            if (lastSeparater >= 0) {
                parentPath = filePath.substring(0, lastSeparater);
            }
        }

        //检测是否存在下载目录,没有则创建，创建失败则返回.
        File downloadDir = new File(parentPath);
        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }
        if (!downloadDir.exists()) {
            return false;
        }

        //检查可用的空间
        final StatFs stats = new StatFs(parentPath);
        long size = (long) stats.getBlockSize() * (long) stats.getAvailableBlocks();
        return (size > 10 * 1024 * 1024);
    }

    @Override
    protected Response<File> parseNetworkResponse(NetworkResponse response) {
        // 将数据保存到文件了.
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(filePath);
            stream.write(response.data);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        File file = new File(filePath);
        if (file.exists()) {
            return Response.success(file, null);
        } else {
            return Response.error(new FileError(response));
        }
    }

    @Override
    protected void deliverResponse(File response) {
        if (listener != null) {
            listener.onResponse(response);
        }
    }
}
