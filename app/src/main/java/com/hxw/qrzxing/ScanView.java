package com.hxw.qrzxing;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;

import com.hxw.qr.ViewfinderView;

/**
 * @author hxw
 * @date 2018/4/14.
 */

public class ScanView extends ViewfinderView {

    public ScanView(Context context) {
        super(context);
    }

    public ScanView(Context context, AttributeSet attrs) {
        super(context, attrs);


    }

    @Override
    protected void drawMask(Canvas canvas, int width, int height) {
        super.drawMask(canvas, width, height);
        //画角
        paint.setColor(Color.parseColor("#fcba3a"));
        paint.setStrokeWidth(8);
        paint.setStrokeCap(Paint.Cap.ROUND);
        int angleLength = 160;
        //左上
        canvas.drawLine(framingRect.left + 4, framingRect.top + 4, framingRect.left + angleLength, framingRect.top + 4, paint);
        canvas.drawLine(framingRect.left + 4, framingRect.top + 4, framingRect.left + 4, framingRect.top + angleLength, paint);
        //左下
        canvas.drawLine(framingRect.left + 4, framingRect.bottom - 4, framingRect.left + angleLength, framingRect.bottom - 4, paint);
        canvas.drawLine(framingRect.left + 4, framingRect.bottom - 4, framingRect.left + 4, framingRect.bottom - angleLength, paint);
        //右上
        canvas.drawLine(framingRect.right - 4, framingRect.top + 4, framingRect.right - angleLength, framingRect.top + 4, paint);
        canvas.drawLine(framingRect.right - 4, framingRect.top + 4, framingRect.right - 4, framingRect.top + angleLength, paint);
        //右下
        canvas.drawLine(framingRect.right - 4, framingRect.bottom - 4, framingRect.right - angleLength, framingRect.bottom - 4, paint);
        canvas.drawLine(framingRect.right - 4, framingRect.bottom - 4, framingRect.right - 4, framingRect.bottom - angleLength, paint);
    }

    @Override
    protected void initFramingRect(int sWidth, int sHeight) {
        Point screenResolution = new Point(sWidth, sHeight);

        int width = findDesiredDimensionInRange(screenResolution.x, MIN_FRAME_WIDTH, MAX_FRAME_WIDTH);
        int height = findDesiredDimensionInRange(screenResolution.y, MIN_FRAME_HEIGHT, MAX_FRAME_HEIGHT);

        int leftOffset = (screenResolution.x - width) / 2;
        int topOffset = (screenResolution.y - height) / 4;
        framingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
        Log.d(TAG, "计算出屏幕捕捉窗口的大小: " + framingRect);
    }
}
