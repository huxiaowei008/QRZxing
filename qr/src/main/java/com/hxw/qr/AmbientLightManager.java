package com.hxw.qr;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * 探测周围的光，当非常暗的时候，打开灯光，在有足够的光线下再次关闭。
 *
 * @author hxw
 * @date 2018/4/13.
 */

final class AmbientLightManager implements SensorEventListener {

    private static final float TOO_DARK_LUX = 45.0f;
    private static final float BRIGHT_ENOUGH_LUX = 450.0f;

    private final Context context;
    private final ZxingView zxingView;

    @FrontLightMode.Mode
    private int ligthMode;
    private Sensor lightSensor;

    AmbientLightManager(Context context, ZxingView zxingView, @FrontLightMode.Mode int lightMode) {
        this.context = context;
        this.zxingView = zxingView;
        this.ligthMode = lightMode;
    }

    void start() {
        if (ligthMode == FrontLightMode.AUTO) {
            SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            if (lightSensor != null) {
                sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    void stop() {
        if (lightSensor != null) {
            SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            sensorManager.unregisterListener(this);
            lightSensor = null;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float ambientLightLux = sensorEvent.values[0];
        if (ambientLightLux <= TOO_DARK_LUX) {
            zxingView.setTorch(true);
        } else if (ambientLightLux >= BRIGHT_ENOUGH_LUX) {
            zxingView.setTorch(false);
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // do nothing
    }
}
