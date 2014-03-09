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

package org.robam.xutils.http.callback;

import android.text.TextUtils;

import org.apache.http.HttpEntity;
import org.robam.xutils.Utils.IOUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 其实这里才是真正下载数据的地方,所以不能让它阻塞了.
 * 另外:这根本不算是Callback,但是放在了Callback包下,是有点小问题哦
 */
public class FileDownloadHandler {

    /**
     * 处理下载文件.
     *
     * @param entity
     * @param callBackHandler
     * @param target
     * @param isResume
     * @param responseFileName
     * @return 文件名.还是得返回文件名.因为后面判断了如果存在了文件的话, 会获取当前时间添加进去的.
     * @throws java.io.IOException
     */
    public File handleEntity(HttpEntity entity,
                             RequestCallBackHandler callBackHandler,
                             String target,
                             boolean isResume,
                             String responseFileName) throws IOException {
        if (entity == null || TextUtils.isEmpty(target)) {
            return null;
        }

        File targetFile = new File(target);

        //如果文件不存在的话
        if (!targetFile.exists()) {
            File dir = targetFile.getParentFile();
            //先创建了目录
            if (!dir.exists()) {
                dir.mkdirs();
            }
            //再创建文件
            targetFile.createNewFile();
        }

        long currentFileSize = 0;
        InputStream inputStream = null;
        FileOutputStream fileOutputStream = null;

        try {
            if (isResume) {
                currentFileSize = targetFile.length();
                // true是追加
                fileOutputStream = new FileOutputStream(target, true);
            } else {
                fileOutputStream = new FileOutputStream(target);
            }

            // 文件总长度
            long totalFileSize = entity.getContentLength() + currentFileSize;

            //TODO:为什么就返回了呢?CallbackHandler控制着是否要结束,这样的方法不太好啊...
            if (callBackHandler != null && !callBackHandler.updateProgress(totalFileSize, currentFileSize, true)) {
                return targetFile;
            }

            // TODO:难道是到了这里entity.getContent才源源不断获取数据吗?前面获取到的只是一些大概信息?
            inputStream = entity.getContent();
            BufferedInputStream bis = new BufferedInputStream(inputStream);

            byte[] tmp = new byte[4096];
            int len;
            while ((len = bis.read(tmp)) != -1) {
                fileOutputStream.write(tmp, 0, len);
                currentFileSize += len;
                if (callBackHandler != null) {
                    if (!callBackHandler.updateProgress(totalFileSize, currentFileSize, false)) {
                        return targetFile;
                    }
                }
            }
            fileOutputStream.flush();
            if (callBackHandler != null) {
                callBackHandler.updateProgress(totalFileSize, currentFileSize, true);
            }
        } finally {
            //关闭文件
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(fileOutputStream);
        }


        // 重命名文件
        if (targetFile.exists() && !TextUtils.isEmpty(responseFileName)) {
            File newFile = new File(targetFile.getParent(), responseFileName);
            //如果文件名已经存在了,取当前的时间加到文件名中.
            while (newFile.exists()) {
                newFile = new File(targetFile.getParent(), System.currentTimeMillis() + responseFileName);
            }
            return targetFile.renameTo(newFile) ? newFile : targetFile;
        } else {
            return targetFile;
        }
    }

}
