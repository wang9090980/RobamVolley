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

import org.apache.http.HttpEntity;
import org.robam.xutils.Utils.IOUtils;
import org.robam.xutils.Utils.OtherUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * 处理下载字符串
 */
public class StringDownloadHandler {

    /**
     * 处理下载字符串.
     *
     * @param entity
     * @param callBackHandler
     * @param charset
     * @return
     * @throws java.io.IOException
     */
    public String handleEntity(HttpEntity entity, RequestCallBackHandler callBackHandler, String charset) throws IOException {
        if (entity == null) {
            return null;
        }

        long current = 0;
        long total = entity.getContentLength();

        if (callBackHandler != null && !callBackHandler.updateProgress(total, current, true)) {
            return null;
        }

        InputStream inputStream = null;
        StringBuilder sb = new StringBuilder();
        try {
            inputStream = entity.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset));
            String line = "";
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                current += OtherUtils.sizeOfString(line, charset);
                if (callBackHandler != null) {
                    if (!callBackHandler.updateProgress(total, current, false)) {
                        return sb.toString();
                    }
                }
            }
            if (callBackHandler != null) {
                callBackHandler.updateProgress(total, current, true);
            }
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return sb.toString();
    }

}
