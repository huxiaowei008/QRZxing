package com.hxw.qr;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.ResultPointCallback;
import com.google.zxing.client.android.camera.CameraConfigurationUtils;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import timber.log.Timber;


/**
 * @author hxw
 * 主要控件和控制
 */
public class ZxingView extends FrameLayout implements SurfaceHolder.Callback, LifecycleObserver {
    private static final String TAG = ZxingView.class.getSimpleName();
    /**
     * 这里提供了预览框架，我们将其传递给注册的处理程序。确保清除处理程序，使它只接收一条消息。
     */
    private final PreviewCallback previewCallback = new PreviewCallback();
    private SurfaceView surfaceView;
    private Activity mActivity;
    /**
     * 开放出来的相机的设置属性
     */
    private CameraSetting mCameraSetting;
    /**
     * 相机
     */
    private Camera mCamera;
    /**
     * 相机的属性特性
     */
    private Camera.CameraInfo mCameraInfo;
    /**
     * 相机需要修正的角度
     */
    private int cwNeededRotation;
    /**
     * 相机的最佳分辨率，最佳预览尺寸
     */
    private Point cameraResolution;
    private boolean hasSurface;
    /**
     * 判断是否正在预览
     */
    private boolean previewing;
    /**
     * 判断相机方向修正角度和合适的预览大小是否计算过
     */
    private boolean initialized;

    private CaptureHandler handler;
    /**
     * 声音和震动管理
     */
    private BeepManager beepManager;
    /**
     * 灯光管理
     */
    private AmbientLightManager ambientLightManager;
    /**
     * 自动对焦管理
     */
    private AutoFocusManager autoFocusManager;

    public ZxingView(@NonNull Context context) {
        this(context, null);
    }

    public ZxingView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZxingView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * 初始化需要的视图
     */
    private void initView(Context context, ViewfinderView view) {
        surfaceView = new SurfaceView(context);
        ViewfinderView viewfinderView = view;
        if (viewfinderView == null) {
            viewfinderView = new ViewfinderView(context);
        }
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup
                .LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        addView(surfaceView, layoutParams);
        addView(viewfinderView, layoutParams);

    }

    /**
     * ######################模仿activity的生命周期##################################################
     */
    public void onCreate(FragmentActivity activity, CameraSetting cameraSetting) {
        mActivity = activity;
        activity.getLifecycle().addObserver(this);
        mCameraSetting = cameraSetting;
        beepManager = new BeepManager(activity, cameraSetting);
        ambientLightManager = new AmbientLightManager(activity, this,
                cameraSetting.getLightMode());
        initView(activity, cameraSetting.getViewfinderView());
        hasSurface = false;
    }

    private void onResume() {
        ambientLightManager.start();
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceView.getHolder());
        } else {
            // Install the callback and wait for surfaceCreated() to init the camera.
            surfaceView.getHolder().addCallback(this);
        }
    }

    private void onPause() {
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }

        ambientLightManager.stop();
        beepManager.close();
        closeDriver();
        if (!hasSurface) {
            surfaceView.getHolder().removeCallback(this);
        }
    }

    private void onDestroy() {
        mCameraSetting = null;
        mActivity = null;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
    public void onLifecycleChanged(@NonNull LifecycleOwner owner,
                                   @NonNull Lifecycle.Event event) {
        switch (event) {
            case ON_RESUME:
                onResume();
                break;
            case ON_PAUSE:
                onPause();
                break;
            case ON_DESTROY:
                owner.getLifecycle().removeObserver(this);
                onDestroy();
                break;
            default:
                break;
        }
    }
    //##############################################################################################

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Timber.tag(TAG).d("surfaceCreated");
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Timber.tag(TAG).d("surfaceChanged");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Timber.tag(TAG).d("surfaceDestroyed");
        hasSurface = false;
    }

    /**
     * #######################################相机操作###############################################
     */

    /**
     * 初始化相机
     */
    private void initCamera(SurfaceHolder surfaceHolder) {
        try {
            if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                //没有权限
                throw new IllegalAccessException("没有相机权限");
            }

            if (surfaceHolder == null) {
                throw new IllegalStateException("没有提供 SurfaceHolder ");
            }
            if (mCamera == null) {
                openDriver(surfaceHolder);
            }
            if (handler == null) {
                handler = new CaptureHandler(this, new ResultPointCallback() {
                    @Override
                    public void foundPossibleResultPoint(ResultPoint point) {
//                        viewfinderView.addPossibleResultPoint(point);
                    }
                }, mCameraSetting.getDecodeFormats());
            }
        } catch (Exception e) {
            Timber.tag(TAG).w(e);
            displayFrameworkBugMessageAndExit();
        }
    }

    /**
     * Opens the camera driver and initializes the hardware parameters.
     * 打开相机驱动程序并初始化硬件参数
     *
     * @param surfaceHolder The surface object which the camera will draw preview frames into.
     * @throws IOException Indicates the camera driver failed to open.
     */
    private synchronized void openDriver(SurfaceHolder surfaceHolder) throws IOException {
        openCamera(mCameraSetting.getRequestedCameraId());
        if (mCamera == null) {
            throw new IOException("打开相机失败！！");
        }
        if (!initialized) {
            initialized = true;
            WindowManager manager = (WindowManager) mActivity.getSystemService(Context.WINDOW_SERVICE);
            if (manager == null) {
                throw new NullPointerException("WindowManager获取为空");
            }
            Display display = manager.getDefaultDisplay();
            initRotationFromDisplayToCamera(display, mCameraInfo);
            initPreviewSize(display, mCamera);
        }

        Camera.Parameters parameters = mCamera.getParameters();
        String parametersFlattened = parameters == null ? null : parameters.flatten();
        try {
            //设置相机属性
            setDesiredCameraParameters(mCamera, false, mCameraSetting);
        } catch (RuntimeException re) {
            //驱动程序失败
            Timber.tag(TAG).w("相机设置参数被拒。只设置最小安全模式的参数%s", re.getMessage());
            Timber.tag(TAG).i("重新设置保存的相机参数: %s", parametersFlattened);
            //重置
            if (parametersFlattened != null) {
                parameters = mCamera.getParameters();
                parameters.unflatten(parametersFlattened);
                try {
                    mCamera.setParameters(parameters);
                    setDesiredCameraParameters(mCamera, true, mCameraSetting);
                } catch (RuntimeException re2) {
                    // 好吧,该死的。放弃
                    Timber.tag(TAG).w("相机甚至拒绝安全模式参数!无法配置");
                }
            }
        }
        mCamera.setPreviewDisplay(surfaceHolder);
    }

    /**
     * 打开相机
     */
    private void openCamera(int cameraId) {
        //获取手机所拥有的摄像头
        int numCameras = Camera.getNumberOfCameras();
        if (numCameras == 0) {
            Timber.tag(TAG).w("没有摄像头!");
            return;
        }

        boolean explicitRequest = cameraId >= 0;
        int index;
        //获取相机的属性特性
        if (explicitRequest) {
            index = cameraId;
            mCameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(index, mCameraInfo);
        } else {
            index = 0;
            while (index < numCameras) {
                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                Camera.getCameraInfo(index, cameraInfo);
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    mCameraInfo = cameraInfo;
                    break;
                }
                index++;
            }
        }

        //打开指定的摄像头
        if (index < numCameras) {
            Timber.tag(TAG).i("打开摄像头为 #%s", index);
            mCamera = Camera.open(index);
        } else {
            if (explicitRequest) {
                Timber.tag(TAG).w("请求的摄像头不存在: %s", cameraId);
                mCamera = null;
            } else {
                Timber.tag(TAG).i("没有摄像头的facing是 " +
                        Camera.CameraInfo.CAMERA_FACING_BACK + "; 默认返回摄像头 #0");
                mCamera = Camera.open(0);
                mCameraInfo = new Camera.CameraInfo();
                Camera.getCameraInfo(0, mCameraInfo);
            }
        }
    }

    /**
     * Closes the camera driver if still in use.
     * 如果仍在使用，则关闭相机驱动程序。
     */
    private synchronized void closeDriver() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * 寻找合适的修正相机方向
     */
    private void initRotationFromDisplayToCamera(Display display, Camera.CameraInfo cameraInfo) {

        int displayRotation = display.getRotation();
        int cwRotationFromNaturalToDisplay;
        switch (displayRotation) {
            case Surface.ROTATION_0:
                cwRotationFromNaturalToDisplay = 0;
                break;
            case Surface.ROTATION_90:
                cwRotationFromNaturalToDisplay = 90;
                break;
            case Surface.ROTATION_180:
                cwRotationFromNaturalToDisplay = 180;
                break;
            case Surface.ROTATION_270:
                cwRotationFromNaturalToDisplay = 270;
                break;
            default:
                //看到过这样的返回错误值，比如-90吗?
                if (displayRotation % 90 == 0) {
                    cwRotationFromNaturalToDisplay = (360 + displayRotation) % 360;
                } else {
                    throw new IllegalArgumentException("不好的旋转角度 Bad rotation: " + displayRotation);
                }
        }
        Timber.tag(TAG).i("显示角度 Display at: %s", cwRotationFromNaturalToDisplay);
        int cwRotationFromNaturalToCamera = cameraInfo.orientation;
        Timber.tag(TAG).i("相机角度 Camera at: %s", cwRotationFromNaturalToCamera);

        // 如果是前置摄像头,我们需要把它翻转过来。:
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            cwRotationFromNaturalToCamera = (360 - cwRotationFromNaturalToCamera) % 360;
            Timber.tag(TAG).i("前置摄像头重载 to: %s", cwRotationFromNaturalToCamera);
        }

        int cwRotationFromDisplayToCamera =
                (360 + cwRotationFromNaturalToCamera - cwRotationFromNaturalToDisplay) % 360;
        Timber.tag(TAG).i("最终显示的方向: %s", cwRotationFromDisplayToCamera);

        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            Timber.tag(TAG).i("前置摄像头的补偿旋转");
            cwNeededRotation = (360 - cwRotationFromDisplayToCamera) % 360;
        } else {
            cwNeededRotation = cwRotationFromDisplayToCamera;
        }
        Timber.tag(TAG).i("从显示到相机的顺时针旋转: %s", cwNeededRotation);
    }

    /**
     * 寻找合适的预览尺寸
     */
    private void initPreviewSize(Display display, Camera camera) {
        Point screenResolution = new Point();
        display.getSize(screenResolution);
        Timber.tag(TAG).i("当前方向的屏幕分辨率: %s", screenResolution);

        cameraResolution = CameraConfigurationUtils.findBestPreviewSizeValue(camera.getParameters(), screenResolution);
        Timber.tag(TAG).i("最好的预览大小: %s", cameraResolution);
    }

    /**
     * 设置相机属性
     */
    private void setDesiredCameraParameters(Camera camera, boolean safeMode, CameraSetting cameraSetting) {
        Camera.Parameters parameters = camera.getParameters();
        if (parameters == null) {
            Timber.tag(TAG).w("设备错误:没有可以设置的相机参数。无法进行配置。");
            return;
        }
        Timber.tag(TAG).i("最初的相机参数: %s", parameters.flatten());
        if (safeMode) {
            Timber.tag(TAG).w("在相机配置安全模式下----大多数设置都不会被授予");
        }

        boolean currentSetting = cameraSetting.getLightMode() == FrontLightMode.ON;
        doSetTorch(parameters, currentSetting, safeMode, cameraSetting.isDisExposure());

        CameraConfigurationUtils.setFocus(
                parameters,
                cameraSetting.isAutoFocus(),
                cameraSetting.isDisContinuousFocus(),
                safeMode);

        if (!safeMode) {
            if (cameraSetting.isInvertScan()) {
                CameraConfigurationUtils.setInvertColor(parameters);
            }

            if (!cameraSetting.isDisBarcodeSceneMode()) {
                CameraConfigurationUtils.setBarcodeSceneMode(parameters);
            }

            if (!cameraSetting.isDisMetering()) {
                CameraConfigurationUtils.setVideoStabilization(parameters);
                CameraConfigurationUtils.setFocusArea(parameters);
                CameraConfigurationUtils.setMetering(parameters);
            }
        }
        //SetRecordingHint to true also a workaround for low framerate on Nexus 4
        //https://stackoverflow.com/questions/14131900/extreme-camera-lag-on-nexus-4
        parameters.setRecordingHint(true);

        //注意！！！这里设置的大小需要是相机支持的,交换x,y可能会导致不支持尺寸从而设置失败
        parameters.setPreviewSize(cameraResolution.x, cameraResolution.y);

        camera.setParameters(parameters);

        camera.setDisplayOrientation(cwNeededRotation);

        Camera.Parameters afterParameters = camera.getParameters();
        Camera.Size afterSize = afterParameters.getPreviewSize();
        if (afterSize != null && (cameraResolution.x != afterSize.width || cameraResolution.y != afterSize.height)) {
            Timber.tag(TAG).w("摄像头说它支持预览尺寸 " + cameraResolution.x + 'x' + cameraResolution.y +
                    ", but after setting it, preview size is " + afterSize.width + 'x' + afterSize.height);
            cameraResolution.x = afterSize.width;
            cameraResolution.y = afterSize.height;
        }

    }

    private void doSetTorch(Camera.Parameters parameters, boolean newSetting,
                            boolean safeMode, boolean disExposure) {
        CameraConfigurationUtils.setTorch(parameters, newSetting);
        if (!safeMode && !disExposure) {
            CameraConfigurationUtils.setBestExposure(parameters, newSetting);
        }
    }

    private boolean getTorchState(Camera camera) {
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            if (parameters != null) {
                String flashMode = parameters.getFlashMode();
                return flashMode != null &&
                        (Camera.Parameters.FLASH_MODE_ON.equals(flashMode) ||
                                Camera.Parameters.FLASH_MODE_TORCH.equals(flashMode));
            }
        }
        return false;
    }

    /**
     * 开始预览
     */
    synchronized void startPreview() {
        if (mCamera != null && !previewing) {
            mCamera.startPreview();
            previewing = true;
            autoFocusManager = new AutoFocusManager(mCamera, mCameraSetting.isAutoFocus());
            autoFocusManager.start();
        }
    }

    /**
     * 停止预览
     */
    synchronized void stopPreview() {
        if (autoFocusManager != null) {
            autoFocusManager.stop();
            autoFocusManager = null;
        }
        if (mCamera != null && previewing) {
            mCamera.stopPreview();
            previewCallback.setHandler(null, 0, null);
            previewing = false;
        }
    }

    /**
     * 无法启动相机，提示并退出
     */
    private void displayFrameworkBugMessageAndExit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle("温馨提示");
        builder.setMessage("很遗憾，Android 相机出现问题。你可能需要重启设备。请检查是否具有相机权限！！！");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mActivity.finish();
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                mActivity.finish();
            }
        });
        builder.show();
    }

    //##############################################################################################

    /**
     * #######################提供给外部和内部操作的方法####################################################
     */

    /**
     * A single preview frame will be returned to the handler supplied. The data will arrive as byte[]
     * in the message.obj field, with width and height encoded as message.arg1 and message.arg2,
     * respectively.
     *
     * @param handler The handler to send the message to.
     */
    synchronized void requestPreviewFrame(Handler handler) {
        if (mCamera != null && previewing) {
            previewCallback.setHandler(handler, CameraConstant.DECODE, cameraResolution);
            mCamera.setOneShotPreviewCallback(previewCallback);
        }
    }

    /**
     * A factory method to build the appropriate LuminanceSource object based on the format
     * of the preview buffers, as described by Camera.Parameters.
     * 一种工厂方法，它基于预览缓冲区的格式来构建合适的LuminanceSource对象，如Camera.Parameters所描述的那样。
     *
     * @param data   A preview frame.
     * @param width  The width of the image.
     * @param height The height of the image.
     * @return A PlanarYUVLuminanceSource instance.
     */
    PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
        // Go ahead and assume it's YUV rather than die.
        return new PlanarYUVLuminanceSource(data, width, height, 0, 0,
                width, height, false);
    }

    Handler getCaptureHandler() {
        return handler;
    }

    /**
     * 处理解码
     *
     * @param rawResult 解码结果
     */
    void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
        beepManager.playBeepSoundAndVibrate();

//        if (barcode != null) {
//            drawResultPoints(barcode, scaleFactor, rawResult);
//        }
        CameraSetting.ZxingResultListener listener = mCameraSetting.getListener();
        if (listener != null) {
            listener.result(rawResult.getText());
        }
    }

    /**
     * 延迟重启预览
     *
     * @param delayMS 延迟时间
     */
    public void restartPreviewAfterDelay(long delayMS) {
        if (handler != null) {
            handler.sendEmptyMessageDelayed(CameraConstant.RESTART_PREVIEW, delayMS);
        }
    }

    /**
     * 灯光的开关
     *
     * @param newSetting if {@code true}, light should be turned on if currently off. And vice versa.
     */
    public synchronized void setTorch(boolean newSetting) {
        if (mCamera != null && newSetting != getTorchState(mCamera)) {
            boolean wasAutoFocusManager = autoFocusManager != null;
            if (wasAutoFocusManager) {
                autoFocusManager.stop();
                autoFocusManager = null;
            }
            Camera.Parameters parameters = mCamera.getParameters();
            doSetTorch(parameters, newSetting, false, mCameraSetting.isDisExposure());
            mCamera.setParameters(parameters);

            if (wasAutoFocusManager) {
                autoFocusManager = new AutoFocusManager(mCamera, mCameraSetting.isAutoFocus());
                autoFocusManager.start();
            }
        }
    }
    //##############################################################################################

    /**
     * 绘制关键特征点
     *
     * @param barcode     A bitmap of the captured image.
     * @param scaleFactor amount by which thumbnail was scaled
     * @param rawResult   The decoded results which contains the points to draw.
     */
    private void drawResultPoints(Bitmap barcode, float scaleFactor, Result rawResult) {
        ResultPoint[] points = rawResult.getResultPoints();
        if (points != null && points.length > 0) {
            Canvas canvas = new Canvas(barcode);
            Paint paint = new Paint();
            paint.setColor(Color.parseColor("#c099cc00"));
            if (points.length == 2) {
                paint.setStrokeWidth(4.0f);
                drawLine(canvas, paint, points[0], points[1], scaleFactor);
            } else if (points.length == 4 &&
                    (rawResult.getBarcodeFormat() == BarcodeFormat.UPC_A ||
                            rawResult.getBarcodeFormat() == BarcodeFormat.EAN_13)) {
                // Hacky special case -- draw two lines, for the barcode and metadata
                drawLine(canvas, paint, points[0], points[1], scaleFactor);
                drawLine(canvas, paint, points[2], points[3], scaleFactor);
            } else {
                paint.setStrokeWidth(10.0f);
                for (ResultPoint point : points) {
                    if (point != null) {
                        canvas.drawPoint(scaleFactor * point.getX(), scaleFactor * point.getY(), paint);
                    }
                }
            }
        }
    }

    private void drawLine(Canvas canvas, Paint paint, ResultPoint a, ResultPoint b, float scaleFactor) {
        if (a != null && b != null) {
            canvas.drawLine(scaleFactor * a.getX(),
                    scaleFactor * a.getY(),
                    scaleFactor * b.getX(),
                    scaleFactor * b.getY(),
                    paint);
        }
    }
}
