package com.hxw.qr;

import android.hardware.Camera;

import com.google.zxing.BarcodeFormat;

import java.util.Collection;
import java.util.Set;

/**
 * 相机和码的扫描设置
 *
 * @author hxw
 * @date 2018/4/9.
 */

public class CameraSetting {
    /**
     * 解码后的结果监听
     */
    public ZxingResultListener listener = null;
    /**
     * 不曝光
     */
    private boolean disExposure = true;
    /**
     * 自动对焦
     */
    private boolean autoFocus = true;
    /**
     * 反色
     */
    private boolean invertScan = false;
    /**
     * 不持续对焦
     */
    private boolean disContinuousFocus = true;
    /**
     * 不使用距离测量
     */
    private boolean disMetering = true;
    /**
     * 不进行条形码场景匹配
     */
    private boolean disBarcodeSceneMode = true;
    /**
     * 打开哪个摄像头
     */
    private int requestedCameraId = CameraConstant.NO_REQUESTED_CAMERA;
    /**
     * 灯光模式
     */
    private String lightMode = Camera.Parameters.FLASH_MODE_OFF;
    /**
     * 解码类型,默认就一个二维码
     */
    private Collection<BarcodeFormat> decodeFormats = DecodeFormatManager.QR_CODE_FORMATS;

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

    public Collection<BarcodeFormat> getDecodeFormats() {
        return decodeFormats;
    }

    /**
     * 添加扫描的码类型，默认一个二维码
     *
     * @param formats 码类型 {@link DecodeFormatManager}
     */
    public CameraSetting addDecodeFormats(Set<BarcodeFormat> formats) {
        this.decodeFormats.addAll(formats);
        return this;
    }

    public ZxingResultListener getListener() {
        return listener;
    }

    public CameraSetting setOnResultListener(ZxingResultListener listener) {
        this.listener = listener;
        return this;
    }

    public interface ZxingResultListener {
        void result(CharSequence result);
    }
}
