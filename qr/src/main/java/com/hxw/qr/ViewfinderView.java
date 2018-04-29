package com.hxw.qr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.google.zxing.ResultPoint;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * @author hxw
 * 界面显示的视图
 */

public class ViewfinderView extends View {
    protected static final String TAG = ViewfinderView.class.getSimpleName();
    protected static final int MIN_FRAME_WIDTH = 240;
    protected static final int MIN_FRAME_HEIGHT = 240;
    protected static final int MAX_FRAME_WIDTH = 1200; // = 5/8 * 1920
    protected static final int MAX_FRAME_HEIGHT = 675; // = 5/8 * 1080

    private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
    private static final long ANIMATION_DELAY = 160L;
    private static final int CURRENT_POINT_OPACITY = 0xA0;
    private static final int MAX_RESULT_POINTS = 20;
    private static final int POINT_SIZE = 6;
    /**
     * 用于绘制的画笔
     */
    protected final Paint paint;
    /**
     * 预览时的遮罩颜色
     */
    protected final int maskColor;
    /**
     * 得到结果时的遮罩颜色
     */
    protected final int resultColor;
    /**
     * 激光扫描器的颜色
     */
    private final int laserColor;
    /**
     * 一些结果点的绘制颜色
     */
    private final int resultPointColor;
    /**
     * 屏幕捕捉窗口的尺寸
     */
    protected Rect framingRect;
    private int scannerAlpha;
    private List<ResultPoint> possibleResultPoints;
    private List<ResultPoint> lastPossibleResultPoints;
    /**
     * 获得结果的图片
     */
    private Bitmap resultBitmap;

    public ViewfinderView(Context context) {
        this(context, null);
    }

    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        maskColor = Color.parseColor("#60000000");
        resultColor = Color.parseColor("#b0000000");
        laserColor = Color.parseColor("#ffcc0000");
        resultPointColor = Color.parseColor("#c0ffbd21");
        scannerAlpha = 0;
        possibleResultPoints = new ArrayList<>(5);
        lastPossibleResultPoints = null;
    }

    @Override
    protected void onDraw(Canvas canvas) {

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        if (framingRect == null) {
            initFramingRect(width, height);
        }

        drawMask(canvas, width, height);

        //绘制结果图 基本是不会被调用到的
        if (resultBitmap != null) {
            paint.setAlpha(CURRENT_POINT_OPACITY);
            canvas.drawBitmap(resultBitmap, null, framingRect, paint);
        }

        drawResultPoint(canvas, width, height);

        drawLaser(canvas);


        // 请求在动画间隔内的另一个更新，但只重新绘制激光线，
        // 不是整个取景器的面具。这句看情况添加,自己设计动画就不一定需要,如果界面没有更新动画肯定是没添加这句
        postInvalidateDelayed(ANIMATION_DELAY,
                framingRect.left - POINT_SIZE,
                framingRect.top - POINT_SIZE,
                framingRect.right + POINT_SIZE,
                framingRect.bottom + POINT_SIZE);
    }

    /**
     * 绘制结果关键点,画不画都没什么关系
     */
    protected void drawResultPoint(Canvas canvas, float width, float height) {
        float scaleX = framingRect.width() / width;
        float scaleY = framingRect.height() / height;

        List<ResultPoint> currentPossible = possibleResultPoints;
        List<ResultPoint> currentLast = lastPossibleResultPoints;

        int frameLeft = framingRect.left;
        int frameTop = framingRect.top;
        if (currentPossible.isEmpty()) {
            lastPossibleResultPoints = null;
        } else {
            possibleResultPoints = new ArrayList<>(5);
            lastPossibleResultPoints = currentPossible;
            paint.setAlpha(CURRENT_POINT_OPACITY);
            paint.setColor(resultPointColor);
            synchronized (currentPossible) {
                for (ResultPoint point : currentPossible) {
                    canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
                            frameTop + (int) (point.getY() * scaleY),
                            POINT_SIZE, paint);
                }
            }
        }
        if (currentLast != null) {
            paint.setAlpha(CURRENT_POINT_OPACITY / 2);
            paint.setColor(resultPointColor);
            synchronized (currentLast) {
                float radius = POINT_SIZE / 2.0f;
                for (ResultPoint point : currentLast) {
                    canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
                            frameTop + (int) (point.getY() * scaleY),
                            radius, paint);
                }
            }
        }
    }

    /**
     * 绘制激光
     */
    protected void drawLaser(Canvas canvas) {
        // Draw a red "laser scanner" line through the middle to show decoding is active
        // 在中间画一个红色的“激光扫描器”，显示解码是活跃的
        paint.setColor(laserColor);
        paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
        scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
        int middle = framingRect.height() / 2 + framingRect.top;
        canvas.drawRect(framingRect.left + 2, middle - 1, framingRect.right - 1, middle + 2, paint);
    }

    /**
     * 绘制遮罩
     */
    protected void drawMask(Canvas canvas, int width, int height) {
        //画出外部(即框架的外面)变暗。
        paint.setColor(resultBitmap != null ? resultColor : maskColor);
        canvas.drawRect(0, 0, width, framingRect.top, paint);
        canvas.drawRect(0, framingRect.top, framingRect.left, framingRect.bottom + 1, paint);
        canvas.drawRect(framingRect.right + 1, framingRect.top, width, framingRect.bottom + 1, paint);
        canvas.drawRect(0, framingRect.bottom + 1, width, height, paint);
    }

    /**
     * 屏幕捕捉窗口的大小
     */
    protected void initFramingRect(int sWidth, int sHeight) {

        Point screenResolution = new Point(sWidth, sHeight);

        int width = findDesiredDimensionInRange(screenResolution.x, MIN_FRAME_WIDTH, MAX_FRAME_WIDTH);
        int height = findDesiredDimensionInRange(screenResolution.y, MIN_FRAME_HEIGHT, MAX_FRAME_HEIGHT);

        int leftOffset = (screenResolution.x - width) / 2;
        int topOffset = (screenResolution.y - height) / 2;
        framingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
        Timber.tag(TAG).d("计算出屏幕捕捉窗口的大小: " + framingRect);
    }

    protected int findDesiredDimensionInRange(int resolution, int hardMin, int hardMax) {
        int dim = 5 * resolution / 8; // Target 5/8 of each dimension
        if (dim < hardMin) {
            return hardMin;
        }
        if (dim > hardMax) {
            return hardMax;
        }
        return dim;
    }

    /**
     * 添加可能的结果点
     */
    void addPossibleResultPoint(ResultPoint point) {
        List<ResultPoint> points = possibleResultPoints;
        synchronized (points) {
            points.add(point);
            int size = points.size();
            if (size > MAX_RESULT_POINTS) {
                // trim it
                points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
            }
        }
    }

    /**
     * 清除获得结果的图像(通常无用)
     */
    void drawViewfinder() {
        Bitmap resultBitmap = this.resultBitmap;
        this.resultBitmap = null;
        if (resultBitmap != null) {
            resultBitmap.recycle();
        }
        invalidate();
    }

    /**
     * 设置获得结果的图像(测试时用)
     *
     * @param barcode 获得结果的图像
     */
    void drawResultBitmap(Bitmap barcode) {
        resultBitmap = barcode;
        invalidate();
    }
}
