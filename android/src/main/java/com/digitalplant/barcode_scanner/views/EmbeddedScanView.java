package com.digitalplant.barcode_scanner.views;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;

import com.digitalplant.barcode_scanner.R;
import com.google.zxing.BarcodeFormat;
import com.king.zxing.CaptureHelper;
import com.king.zxing.CaptureHelper2;
import com.king.zxing.OnCaptureFinishCallback;
import com.king.zxing.OnCaptureReadyCallback;
import com.king.zxing.ViewfinderView;
import com.king.zxing.OnCaptureCallback;
import com.king.zxing.camera.CameraManager;
import com.king.zxing.camera.CameraConfigurationUtils;

import java.util.ArrayList;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import io.flutter.app.FlutterApplication;
import io.flutter.plugin.common.MethodChannel;

public class EmbeddedScanView extends ConstraintLayout implements OnCaptureCallback, OnCaptureFinishCallback, OnCaptureReadyCallback {
    public static final String TAG = EmbeddedScanView.class.getSimpleName();

    private SurfaceView surfaceView;
    private ViewfinderView viewFinderView;
    private CaptureHelper2 mCaptureHelper;
    private Activity activity;
    private MethodChannel channel;
    private boolean flashOn = false;
    private boolean vibrateOn = true;
    private boolean okToStart = false;
    private int viewId;

    private static BarcodeFormat[] barcodeFormats = BarcodeFormat.values();

    public EmbeddedScanView(Context context) {
        super(context);

        init(context);
    }

    public EmbeddedScanView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }

    public EmbeddedScanView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context);
    }

    public void setMethodChannel(MethodChannel channel) {
        this.channel = channel;
    }

    public Activity getActivity(Context context) {
        return ((FlutterApplication) context).getCurrentActivity();
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.embedded_scan_view, this, true);
        activity = getActivity(context);
        initCaptureHelper();
    }

    public void setViewId(int viewId) {
        this.viewId = viewId;
    }

    public int getViewId() {
        return viewId;
    }

    public void setFormats(ArrayList<BarcodeFormat> formats) {
        mCaptureHelper.decodeFormats(formats);
    }

    public void setDelay(long delay) {
        mCaptureHelper.setDelay(delay);
    }

    public void setMaxScan(int maxScan) {
        mCaptureHelper.setMaxScan(maxScan);
    }

    public void setAutoRestart(boolean autoStart) {
        mCaptureHelper.setAutoRestart(autoStart);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        Log.d(TAG,"onAttachedToWindow: "+viewId);
        mCaptureHelper.onCreate();
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        Log.d(TAG,"onVisibilityChanged: "+viewId);
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == View.VISIBLE) {
            mCaptureHelper.onResume();
        } else {
            mCaptureHelper.onPause();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        Log.d(TAG,"onDetachedFromWindow: "+viewId);
        mCaptureHelper.onDestroy();
    }

    /**
     * 初始化相机，解码器
     */
    public void initCaptureHelper(){
        surfaceView = findViewById(getSurfaceViewId());
        viewFinderView = findViewById(getViewfinderViewId());
        // 没啥用的东西，但是库不能传NULL，只能隐藏掉了
        viewFinderView.setVisibility(View.GONE);
        // 初始化CaptureHelper
        mCaptureHelper = new CaptureHelper2(activity, surfaceView, viewFinderView);
        mCaptureHelper
                .setOnCaptureCallback(this)
                .setOnCaptureFinishCallback(this)
                .setOnCaptureReadyCallback(this)
                .supportAutoZoom(false)
                .vibrate(vibrateOn);
    }

    /**
     * 扫码结果回调
     * @param result 扫码结果
     */
    @Override
    public boolean onResultCallback(String result) {
        channel.invokeMethod("EmbeddedScanner.onCode", result);
        if (!mCaptureHelper.getAutoRestart()) {
            okToStart = true;
        }
        return true;
    }

    /**
     * 扫码结束了
     */
    @Override
    public void onFinishCallback() {
        channel.invokeMethod("EmbeddedScanner.onFinish", "");
    }

    /**
     * 准备好扫码
     */
    @Override
    public void onReadyCallback() {
        channel.invokeMethod("EmbeddedScanner.onReady", "");
    }

    /**
     * 打开闪光灯
     */
    public void toggleFlash() {
        if (hasTorch()) {
            flashOn = !flashOn;
            setTorch(flashOn);

            channel.invokeMethod("EmbeddedScanner.onToggleFlash", flashOn);
        }
    }

    /**
     * 打开震动
     */
    public void toggleVibrate() {
        vibrateOn = !vibrateOn;
        setVibrate(vibrateOn);

        channel.invokeMethod("EmbeddedScanner.onToggleVibrate", vibrateOn);
    }

    /**
     * {@link ViewfinderView} 的 id
     * @return
     */
    public int getViewfinderViewId(){
        return R.id.view_finder_view;
    }


    /**
     * 预览界面{@link #surfaceView} 的id
     * @return
     */
    public int getSurfaceViewId(){
        return R.id.camera_view;
    }

    /**
     * Get {@link CameraManager}
     * @return {@link #mCaptureHelper#getCameraManager()}
     */
    public CameraManager getCameraManager(){
        return mCaptureHelper.getCameraManager();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mCaptureHelper != null) {
            mCaptureHelper.onTouchEvent(event);
        }
        return super.onTouchEvent(event);
    }

    /**
     * 开启或关闭闪光灯（手电筒）
     * @param on {@code true}表示开启，{@code false}表示关闭
     */
    public void setTorch(boolean on){
        Camera camera = getCameraManager().getOpenCamera().getCamera();
        Camera.Parameters parameters = camera.getParameters();
        CameraConfigurationUtils.setTorch(parameters,on);
        camera.setParameters(parameters);
    }

    public void setVibrate(boolean vibrate) {
        mCaptureHelper.vibrate(vibrate);
    }

    /**
     * 检测是否支持闪光灯（手电筒）
     * @return
     */
    public boolean hasTorch(){
        return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    private void clickFlash(View v){
        boolean isSelected = v.isSelected();
        setTorch(!isSelected);
        v.setSelected(!isSelected);
    }

    public CaptureHelper2 getCaptureHelper() {
        return mCaptureHelper;
    }

    public void setScanParams(Map<String, Object> params) {
        Log.d(TAG, "setScanParams: "+params.toString());
        if (params.containsKey("formats")) {
            ArrayList<Integer> fs = (ArrayList<Integer>) params.get("formats");
            ArrayList<BarcodeFormat> formats = new ArrayList<>();
            for (int f : fs) {
                formats.add(barcodeFormats[f]);
            }
            setFormats(formats);
        }

        if (params.containsKey("maxScan")) {
            setMaxScan((int) params.get("maxScan"));
        }

        if (params.containsKey("delay")) {
            setDelay((long) params.get("delay"));
        }

        if (params.containsKey("autoRestart")) {
            setAutoRestart((boolean) params.get("autoRestart"));
        }
    }

    public void setReadyToScan() {
        Log.d(TAG,"okToStart: "+okToStart);
        if (okToStart) {
            mCaptureHelper.setReadyToScan();
            okToStart = false;
        }
    }
}
