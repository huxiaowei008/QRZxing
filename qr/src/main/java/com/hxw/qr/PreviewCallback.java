package com.hxw.qr;

import android.graphics.Point;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;

import timber.log.Timber;

/**
 * 相机预览的回掉
 *
 * @author hxw
 * @date 2018/4/11.
 */

final class PreviewCallback implements Camera.PreviewCallback {
    private static final String TAG = PreviewCallback.class.getSimpleName();
    private Handler previewHandler;
    private int previewMessage;
    private Point cameraResolution;

    void setHandler(Handler previewHandler, int previewMessage, Point cameraResolution) {
        this.previewHandler = previewHandler;
        this.previewMessage = previewMessage;
        this.cameraResolution = cameraResolution;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (cameraResolution != null && previewHandler != null) {
            Message message = previewHandler.obtainMessage(previewMessage, cameraResolution.x,
                    cameraResolution.y, data);
            message.sendToTarget();
            previewHandler = null;
        } else {
            Timber.tag(TAG).d("有预览回调，但没有处理程序的handler或屏幕分辨率可用!");
        }
    }
}
