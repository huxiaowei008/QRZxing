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
 * 主线程的handler,用于结果和操作
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
            case CameraConstant.RESTART_PREVIEW:
                restartPreviewAndDecode();
                break;
            case CameraConstant.DECODE_SUCCEEDED:
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
            case CameraConstant.DECODE_FAILED:
                //我们正在尽可能快地解码，所以当一个解码失败时，启动另一个
                state = State.PREVIEW;
                zxingView.requestPreviewFrame(decodeThread.getHandler());
                break;
            default:
                break;
        }
    }

    void quitSynchronously() {
        state = State.DONE;
        zxingView.stopPreview();
        Message quit = Message.obtain(decodeThread.getHandler(), CameraConstant.QUIT);
        quit.sendToTarget();
        try {
            // 最多等半秒;应该有足够的时间, onPause() 会很快超时的
            decodeThread.join(500L);
        } catch (InterruptedException e) {
            // continue
        }

        //确定我们不会发送任何排队的消息。
        removeMessages(CameraConstant.DECODE_SUCCEEDED);
        removeMessages(CameraConstant.DECODE_FAILED);
    }

    /**
     * 重新启动预览和解码
     */
    private void restartPreviewAndDecode() {
        if (state == State.SUCCESS) {
            state = State.PREVIEW;
            zxingView.requestPreviewFrame(decodeThread.getHandler());
        }
    }

    private enum State {
        /**
         * 在预览
         */
        PREVIEW,

        /**
         * 成功
         */
        SUCCESS,
        /**
         * 在解析
         */
        DONE
    }
}
