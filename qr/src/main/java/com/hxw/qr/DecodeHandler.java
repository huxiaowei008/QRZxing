package com.hxw.qr;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.util.Map;

/**
 * @author hxw
 * @date 2018/4/11.
 */

final class DecodeHandler extends Handler {
    private static final String TAG = DecodeHandler.class.getSimpleName();
    private final MultiFormatReader multiFormatReader;
    private final ZxingView zxingView;
    private boolean running = true;

    DecodeHandler(ZxingView zxingView, Map<DecodeHintType, Object> hints) {
        multiFormatReader = new MultiFormatReader();
        multiFormatReader.setHints(hints);

        this.zxingView = zxingView;
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg == null || !running) {
            return;
        }
        switch (msg.what) {
            case CameraConstant.decode:
                decode((byte[]) msg.obj, msg.arg1, msg.arg2);
                break;
            case CameraConstant.quit:
                running = false;
                Looper.myLooper().quit();
                break;
            default:
                break;
        }
    }

    /**
     * Decode the data within the viewfinder rectangle, and time how long it took. For efficiency,
     * reuse the same reader objects from one decode to the next.
     * 解码在矩形取景器内的数据,还有解码花费的时间。为了提高效率,将相同的阅读器对象从一个解码到下一个
     *
     * @param data   The YUV preview frame.
     * @param width  The width of the preview frame.
     * @param height The height of the preview frame.
     */
    private void decode(byte[] data, int width, int height) {
        long start = System.currentTimeMillis();
        Result rawResult = null;
        PlanarYUVLuminanceSource source = zxingView.buildLuminanceSource(data, width, height);
        if (source != null) {
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            try {
                rawResult = multiFormatReader.decodeWithState(bitmap);
            } catch (ReaderException re) {
                // continue
            } finally {
                multiFormatReader.reset();
            }
        }
        long end = System.currentTimeMillis();
        Log.d(TAG, "解码时间: " + (end - start) + " ms");
        Handler handler = zxingView.getCaptureHandler();
        if (handler!=null) {
            if (rawResult != null) {
                Message message = Message.obtain(handler, CameraConstant.decode_succeeded, rawResult);
                message.sendToTarget();
            }else {
                Message message = Message.obtain(handler, CameraConstant.decode_failed);
                message.sendToTarget();
            }
        }
    }
}
