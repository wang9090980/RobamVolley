package org.robam.robamvolley.testvolley;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends ActionBarActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        findViewById(R.id.bitmap_test).setOnClickListener(this);
        findViewById(R.id.download_test).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent();
        switch (v.getId()) {
            case R.id.bitmap_test:
                intent.setClass(this, BitmapTestActivity.class);
                break;
            case R.id.download_test:
                intent.setClass(this, DownloadTest.class);
                break;
            default:
                return;
        }
        startActivity(intent);
    }
}
