package com.hxw.qrzxing;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

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

        ((ToggleButton) findViewById(R.id.tbtn)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                zxingView.setTorch(isChecked);
            }
        });
    }
}
