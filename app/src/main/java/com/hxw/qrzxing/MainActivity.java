package com.hxw.qrzxing;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.hxw.qr.CameraSetting;
import com.hxw.qr.FrontLightMode;
import com.hxw.qr.ZxingView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    ZxingView zxingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate");
        zxingView = findViewById(R.id.zxing);

        zxingView.onCreate(this, new CameraSetting()
                .setOnResultListener(new CameraSetting.ZxingResultListener() {
                    @Override
                    public void result(CharSequence result) {
                        Log.d(TAG, "解码结果：" + result.toString());
                        Toast.makeText(MainActivity.this, result.toString(), Toast.LENGTH_SHORT).show();

                    }
                }).setVibrate(true)
                .setPlayBeep(false)
                .setLightMode(FrontLightMode.OFF)
                .setViewfinderView(new ScanView(this)));

        findViewById(R.id.btn_restart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                zxingView.restartPreviewAfterDelay(500);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        zxingView.onDestroy();
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    protected void onPause() {
        zxingView.onPause();
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        zxingView.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        zxingView.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
