package com.augray.visionx.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import com.augray.visionx.R;


public class GuidelineActivity extends Activity {
    private RelativeLayout takePicLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guideline);
        takePicLayout = findViewById(R.id.takeAPicLayout);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Button doneText = (Button) findViewById(R.id.skipVideo);
        doneText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GuidelineActivity.this.finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
