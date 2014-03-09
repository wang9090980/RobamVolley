package com.android.volley;

/**
 * Created by weiji.chen on 14-3-9.
 */
public class FileError extends VolleyError {
    public FileError() {
    }

    public FileError(NetworkResponse networkResponse) {
        super(networkResponse);
    }

    public FileError(Throwable cause) {
        super(cause);
    }
}
