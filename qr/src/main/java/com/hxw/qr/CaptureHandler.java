package com.hxw.qr;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.ResultPointCallback;

import java.util.Collection;

/**
 * @author hxw
 * @date 2018/4/11.
 */

public final class CaptureHandler extends Handler {

    private final ZxingView zxingView;
    private final DecodeThread decodeThread;
    private State state;

    CaptureHandler(ZxingView zxingView,
                   ResultPointCallback resultPointCallback,
                   Collection<BarcodeFormat> decodeFormats) {
        this.zxingView = zxingView;

        decodeThread = new DecodeThread(zxingView, resultPointCallback, decodeFormats);
        decodeThread.start();
        state = State.SUCCESS;

        //开始捕捉预览和解码。
        zxingView.startPreview();
        restartPreviewAndDecode();
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case CameraConstant.restart_preview:
                restartPreviewAndDecode();
                break;
            case CameraConstant.decode_succeeded:
                state = State.SUCCESS;
//注释掉的是图片数据的获取,测试时有用,通常时用不到的
//                Bundle bundle = msg.getData();
                Bitmap barcode = null;
                float scaleFactor = 1.0f;
//                if (bundle != null) {
//                    byte[] compressedBitmap = bundle.getByteArray(DecodeHandler.BARCODE_BITMAP);
//                    if (compressedBitmap != null) {
//                        barcode = BitmapFactory.decodeByteArray(compressedBitmap, 0, compressedBitmap.length, null);
//                        // Mutable copy:
//                        barcode = barcode.copy(Bitmap.Config.ARGB_8888, true);
//                    }
//                    scaleFactor = bundle.getFloat(DecodeHandler.BARCODE_SCALED_FACTOR);
//                }

                zxingView.handleDecode((Result) msg.obj, barcode, scaleFactor);
                break;
            case CameraConstant.decode_failed:
                //我们正在尽可能快地解码，所以当一个解码失败时，启动另一个
                state = State.PREVIEW;
                zxingView.requestPreviewFrame(decodeThread.getHandler(), CameraConstant.decode);
                break;
            default:
                break;
        }
    }

    void quitSynchronously() {
        state = State.DONE;
        zxingView.stopPreview();
        Message quit = Message.obtain(decodeThread.getHandler(), CameraConstant.quit);
        quit.sendToTarget();
        try {
            // 最多等半秒;应该有足够的时间, onPause() 会很快超时的
            decodeThread.join(500L);
        } catch (InterruptedException e) {
            // continue
        }

        //确定我们不会发送任何排队的消息。
        removeMessages(CameraConstant.decode_succeeded);
        removeMessages(CameraConstant.decode_failed);
    }

    /**
     * 重新启动预览和解码
     */
    private void restartPreviewAndDecode() {
        if (state == State.SUCCESS) {
            state = State.PREVIEW;
            zxingView.requestPreviewFrame(decodeThread.getHandler(), CameraConstant.decode);
            zxingView.drawViewfinder();
        }
    }

    private enum State {
        PREVIEW,
        SUCCESS,
        DONE
    }
}
