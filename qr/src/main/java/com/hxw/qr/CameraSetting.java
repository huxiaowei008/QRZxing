package com.hxw.qr;

import android.hardware.Camera;

/**
 * @author hxw
 * @date 2018/4/9.
 */

public class CameraSetting {
    private boolean disExposure = true;//不曝光
    private boolean autoFocus = true;//自动对焦
    private boolean invertScan = false;//反色
    private boolean disContinuousFocus = true;//不持续对焦
    private boolean disMetering = true;//不使用距离测量
    private boolean disBarcodeSceneMode = true;//不进行条形码场景匹配
    private int requestedCameraId = CameraConstant.NO_REQUESTED_CAMERA;//打开哪个摄像头
    private String lightMode = Camera.Parameters.FLASH_MODE_OFF;

    public boolean isDisExposure() {
        return disExposure;
    }

    public CameraSetting setDisExposure(boolean disExposure) {
        this.disExposure = disExposure;
        return this;
    }

    public boolean isAutoFocus() {
        return autoFocus;
    }

    public CameraSetting setAutoFocus(boolean autoFocus) {
        this.autoFocus = autoFocus;
        return this;
    }

    public boolean isInvertScan() {
        return invertScan;
    }

    public CameraSetting setInvertScan(boolean invertScan) {
        this.invertScan = invertScan;
        return this;
    }

    public boolean isDisContinuousFocus() {
        return disContinuousFocus;
    }

    public CameraSetting setDisContinuousFocus(boolean disContinuousFocus) {
        this.disContinuousFocus = disContinuousFocus;
        return this;
    }

    public boolean isDisMetering() {
        return disMetering;
    }

    public CameraSetting setDisMetering(boolean disMetering) {
        this.disMetering = disMetering;
        return this;
    }

    public boolean isDisBarcodeSceneMode() {
        return disBarcodeSceneMode;
    }

    public CameraSetting setDisBarcodeSceneMode(boolean disBarcodeSceneMode) {
        this.disBarcodeSceneMode = disBarcodeSceneMode;
        return this;
    }

    public int getRequestedCameraId() {
        return requestedCameraId;
    }

    public CameraSetting setRequestedCameraId(int requestedCameraId) {
        this.requestedCameraId = requestedCameraId;
        return this;
    }

    public String getLightMode() {
        return lightMode;
    }

    public CameraSetting setLightMode(String lightMode) {
        this.lightMode = lightMode;
        return this;
    }
}
