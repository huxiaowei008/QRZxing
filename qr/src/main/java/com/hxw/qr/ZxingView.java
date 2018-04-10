package com.hxw.qr;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.google.zxing.client.android.camera.CameraConfigurationUtils;

import java.io.IOException;


/**
 * @author hxw
 * @date 2018/4/9.
 */

public class ZxingView extends FrameLayout implements SurfaceHolder.Callback {
    private static final String TAG = ZxingView.class.getSimpleName();
    private static final int REQUEST_CODE = 1008;

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
     * 屏幕的分辨率
     */
    private Point screenResolution;
    /**
     * 相机的最佳分辨率，最佳预览尺寸
     */
    private Point bestPreviewSize;
    /**
     * 在屏幕上预览的尺寸
     */
    private Point previewSizeOnScreen;
    private boolean hasSurface;
    private boolean previewing;//判断是否正在预览
    private boolean initialized;//判断相机方向修正角度和合适的预览大小是否计算过

    public ZxingView(@NonNull Context context) {
        this(context, null);
    }

    public ZxingView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZxingView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs);
    }

    /**
     * 初始化需要的视图
     */
    private void initView(Context context, AttributeSet attrs) {
        surfaceView = new SurfaceView(context, attrs);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup
                .LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        addView(surfaceView, layoutParams);
    }

    /**
     * 模仿activity的生命周期########################################################################
     */
    public void onCreate(Activity activity, CameraSetting cameraSetting) {
        mActivity = activity;
        mCameraSetting = cameraSetting;
        hasSurface = false;
    }

    public void onResume() {

        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceView.getHolder());
        } else {
            // Install the callback and wait for surfaceCreated() to init the camera.
            surfaceView.getHolder().addCallback(this);
        }
    }

    public void onPause() {

        closeDriver();
        if (!hasSurface) {
            surfaceView.getHolder().removeCallback(this);
        }
    }

    public void onDestroy() {
        mCameraSetting = null;
        mActivity = null;
    }

    //##############################################################################################


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated");
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
        hasSurface = false;
    }

    /**
     * #######################################相机操作###############################################
     */
    /**
     * 初始化相机
     */
    private void initCamera(SurfaceHolder surfaceHolder) {
        if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            //没有权限
            ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CODE);
            return;
        }
        try {
            if (surfaceHolder == null) {
                throw new IllegalStateException("没有提供 SurfaceHolder ");
            }
            if (mCamera == null) {
                openDriver(surfaceHolder);
            }


        } catch (Exception e) {
            Log.w(TAG, e);
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
            Log.w(TAG, "相机设置参数被拒。只设置最小安全模式的参数" + re.getMessage());
            Log.i(TAG, "重新设置保存的相机参数: " + parametersFlattened);
            //重置
            if (parametersFlattened != null) {
                parameters = mCamera.getParameters();
                parameters.unflatten(parametersFlattened);
                try {
                    mCamera.setParameters(parameters);
                    setDesiredCameraParameters(mCamera, true, mCameraSetting);
                } catch (RuntimeException re2) {
                    // 好吧,该死的。放弃
                    Log.w(TAG, "相机甚至拒绝安全模式参数!无法配置");
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
            Log.w(TAG, "没有摄像头!");
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
            Log.i(TAG, "打开摄像头为 #" + index);
            mCamera = Camera.open(index);
        } else {
            if (explicitRequest) {
                Log.w(TAG, "请求的摄像头不存在: " + cameraId);
                mCamera = null;
            } else {
                Log.i(TAG, "没有摄像头的facing是 " +
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
        Log.i(TAG, "显示角度 Display at: " + cwRotationFromNaturalToDisplay);
        int cwRotationFromNaturalToCamera = cameraInfo.orientation;
        Log.i(TAG, "相机角度 Camera at: " + cwRotationFromNaturalToCamera);

        // 如果是前置摄像头,我们需要把它翻转过来。:
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            cwRotationFromNaturalToCamera = (360 - cwRotationFromNaturalToCamera) % 360;
            Log.i(TAG, "前置摄像头重载 to: " + cwRotationFromNaturalToCamera);
        }

        int cwRotationFromDisplayToCamera =
                (360 + cwRotationFromNaturalToCamera - cwRotationFromNaturalToDisplay) % 360;
        Log.i(TAG, "最终显示的方向: " + cwRotationFromDisplayToCamera);

        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            Log.i(TAG, "前置摄像头的补偿旋转");
            cwNeededRotation = (360 - cwRotationFromDisplayToCamera) % 360;
        } else {
            cwNeededRotation = cwRotationFromDisplayToCamera;
        }
        Log.i(TAG, "从显示到相机的顺时针旋转: " + cwNeededRotation);
    }

    /**
     * 寻找合适的预览尺寸
     */
    private void initPreviewSize(Display display, Camera camera) {
        screenResolution = new Point();
        display.getSize(screenResolution);
        Log.i(TAG, "当前方向的屏幕分辨率: " + screenResolution);

        bestPreviewSize = CameraConfigurationUtils.findBestPreviewSizeValue(camera.getParameters(), screenResolution);
        Log.i(TAG, "最好的预览大小: " + bestPreviewSize);

        boolean isScreenPortrait = screenResolution.x < screenResolution.y;
        boolean isPreviewSizePortrait = bestPreviewSize.x < bestPreviewSize.y;

        if (isScreenPortrait == isPreviewSizePortrait) {
            previewSizeOnScreen = bestPreviewSize;
        } else {
            previewSizeOnScreen = new Point(bestPreviewSize.y, bestPreviewSize.x);
        }
        //previewSizeOnScreen是我们理解方向的尺寸,不一定是相机支持的尺寸,所以设置尺寸时还是要设置bestPreviewSize
        Log.i(TAG, "在屏幕上的预览尺寸: " + previewSizeOnScreen);
    }

    /**
     * 设置相机属性
     */
    private void setDesiredCameraParameters(Camera camera, boolean safeMode, CameraSetting cameraSetting) {
        Camera.Parameters parameters = camera.getParameters();
        if (parameters == null) {
            Log.w(TAG, "设备错误:没有可以设置的相机参数。无法进行配置。");
            return;
        }
        Log.i(TAG, "最初的相机参数: " + parameters.flatten());
        if (safeMode) {
            Log.w(TAG, "在相机配置安全模式下----大多数设置都不会被授予");
        }

        boolean currentSetting = cameraSetting.getLightMode().equals(Camera.Parameters.FLASH_MODE_ON);
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
        parameters.setPreviewSize(bestPreviewSize.x, bestPreviewSize.y);

        camera.setParameters(parameters);

        camera.setDisplayOrientation(cwNeededRotation);

        Camera.Parameters afterParameters = camera.getParameters();
        Camera.Size afterSize = afterParameters.getPreviewSize();
        if (afterSize != null && (bestPreviewSize.x != afterSize.width || bestPreviewSize.y != afterSize.height)) {
            Log.w(TAG, "摄像头说它支持预览尺寸 " + bestPreviewSize.x + 'x' + bestPreviewSize.y +
                    ", but after setting it, preview size is " + afterSize.width + 'x' + afterSize.height);
            bestPreviewSize.x = afterSize.width;
            bestPreviewSize.y = afterSize.height;
        }

    }

    private void doSetTorch(Camera.Parameters parameters, boolean newSetting,
                            boolean safeMode, boolean disExposure) {
        CameraConfigurationUtils.setTorch(parameters, newSetting);
        if (!safeMode && !disExposure) {
            CameraConfigurationUtils.setBestExposure(parameters, newSetting);
        }
    }

    /**
     * 开始预览
     */
    private synchronized void startPreview(){

    }

    /**
     * 停止预览
     */
    public synchronized void stopPreview() {

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

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initCamera(surfaceView.getHolder());
            } else {
                displayFrameworkBugMessageAndExit();
            }
        }
    }

    //##############################################################################################
}
