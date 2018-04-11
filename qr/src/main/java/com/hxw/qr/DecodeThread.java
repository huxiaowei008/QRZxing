package com.hxw.qr;

import android.os.Handler;
import android.os.Looper;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.ResultPointCallback;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * 用于处理解码的异步线程
 *
 * @author hxw
 * @date 2018/4/11.
 */

final class DecodeThread extends Thread {

    private final CountDownLatch handlerInitLatch;
    private final ZxingView zxingView;
    private Map<DecodeHintType, Object> decodeHints;
    private Handler handler;

    DecodeThread(ZxingView zxingView, ResultPointCallback resultPointCallback,
                 Collection<BarcodeFormat> decodeFormats) {
        this.zxingView = zxingView;
        handlerInitLatch = new CountDownLatch(1);

        decodeHints = new EnumMap<>(DecodeHintType.class);
        //指定解码时使用的字符编码（如果适用）（字符串类型）
//        decodeHints.put(DecodeHintType.CHARACTER_SET, "utf-8");
        //当找到可能的{@link ResultPoint}时，调用者需要通过回调来通知
        decodeHints.put(DecodeHintType.NEED_RESULT_POINT_CALLBACK, resultPointCallback);

        decodeHints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
    }

    Handler getHandler() {
        try {
            handlerInitLatch.await();
        } catch (InterruptedException ie) {
            // continue?
        }
        return handler;
    }

    @Override
    public void run() {
        Looper.prepare();
        handler = new DecodeHandler(zxingView, decodeHints);
        handlerInitLatch.countDown();
        Looper.loop();
    }
}
