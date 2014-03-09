package org.robam.robamvolley.testvolley;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Created by weiji.chen on 14-3-8.
 */
public class BitmapTestActivity extends Activity {

    private ListView listview;

    private RequestQueue mRequestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bitmap_test_activity);
        listview = (ListView) findViewById(R.id.listview);

        mRequestQueue = Volley.newRequestQueue(this);
    }
}
