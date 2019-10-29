package com.digitalplant.barcode_scanner;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.content.Intent;

import com.google.zxing.BarcodeFormat;
import com.king.zxing.CaptureHelper;
import com.king.zxing.OnCaptureCallback;
import com.king.zxing.ViewfinderView;
import com.king.zxing.camera.CameraConfigurationUtils;
import com.digitalplant.barcode_scanner.util.StatusBarUtils;
import com.king.zxing.camera.CameraManager;

import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;


public class CaptureActivity extends AppCompatActivity implements OnCaptureCallback, EasyPermissions.PermissionCallbacks, View.OnClickListener {

    private SurfaceView surfaceView;
    private ViewfinderView viewfinderView;
    private CaptureHelper mCaptureHelper;

    public static final String BarcodeObject = "barcodes";

    private ArrayList<BarcodeFormat> formats;
    private boolean isContinuousScan;
    private int maxScan = 1;
    private int delay = 300;

    private static BarcodeFormat[] barcodeFormats = BarcodeFormat.values();

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.capture_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        StatusBarUtils.immersiveStatusBar(this,toolbar,0.2f);
        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText(R.string.toolbar_title);

        View ivFlash = findViewById(R.id.ivFlash);
        if(!hasTorch()){
            ivFlash.setVisibility(View.GONE);
        }

        Intent intent = getIntent();
        isContinuousScan = intent.getBooleanExtra(BarcodeScannerPlugin.IS_CONTINUOUS_KEY, false);
        maxScan = intent.getIntExtra(BarcodeScannerPlugin.MAX_SCAN_KEY, 1);
        delay = intent.getIntExtra(BarcodeScannerPlugin.DELAY_KEY, 300);
        ArrayList<Integer> fs = intent.getIntegerArrayListExtra(BarcodeScannerPlugin.FORMATS_KEY);
        if (fs == null) {
            formats = null;
        } else {
            formats = new ArrayList<>();
            for (int f : fs) {
                formats.add(barcodeFormats[f]);
            }
        }

        checkCameraPermissions();
    }

    /**
     * 检测拍摄权限
     */
    @AfterPermissionGranted(BarcodeScannerPlugin.RC_CAMERA)
    private void checkCameraPermissions(){
        String[] perms = {Manifest.permission.CAMERA};
        if (EasyPermissions.hasPermissions(this, perms)) {//有权限
            initCaptureHelper();
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, getString(R.string.permission_camera),
                    BarcodeScannerPlugin.RC_CAMERA, perms);
        }
    }

    /**
     * 初始化
     */
    public void initCaptureHelper(){
        surfaceView = findViewById(getSurfaceViewId());
        viewfinderView = findViewById(getViewfinderViewId());
        mCaptureHelper = new CaptureHelper(this,surfaceView,viewfinderView);
        mCaptureHelper.setOnCaptureCallback(this);

        mCaptureHelper
                .playBeep(false)//播放音效
                .vibrate(false)//震动
                .autoRestartPreviewAndDecode(false)
//                .framingRectRatio(0.9f)//设置识别区域比例，范围建议在0.625 ~ 1.0之间。非全屏识别时才有效
//                .framingRectVerticalOffset(0)//设置识别区域垂直方向偏移量，非全屏识别时才有效
//                .framingRectHorizontalOffset(0)//设置识别区域水平方向偏移量，非全屏识别时才有效
                .continuousScan(isContinuousScan);//是否连扫

        if (formats != null) {
            mCaptureHelper.decodeFormats(formats);
        }

        mCaptureHelper.onCreate();
    }

    /**
     * 返回true时会自动初始化{@link #setContentView(int)}，返回为false是需自己去初始化{@link #setContentView(int)}
     * @param layoutId
     * @return 默认返回true
     */
    public boolean isContentView(@LayoutRes int layoutId){
        return true;
    }

    /**
     * {@link ViewfinderView} 的 id
     * @return
     */
    public int getViewfinderViewId(){
        return R.id.viewfinderView;
    }


    /**
     * 预览界面{@link #surfaceView} 的id
     * @return
     */
    public int getSurfaceViewId(){
        return R.id.surfaceView;
    }

    /**
     * Get {@link CaptureHelper}
     * @return {@link #mCaptureHelper}
     */
    public CaptureHelper getCaptureHelper(){
        return mCaptureHelper;
    }

    /**
     * Get {@link CameraManager}
     * @return {@link #mCaptureHelper#getCameraManager()}
     */
    public CameraManager getCameraManager(){
        return mCaptureHelper.getCameraManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        mCaptureHelper.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mCaptureHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCaptureHelper.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        setResult(BarcodeScannerPlugin.SUCCESS);
        finish();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mCaptureHelper.onTouchEvent(event);
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

    /**
     * 检测是否支持闪光灯（手电筒）
     * @return
     */
    public boolean hasTorch(){
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    /**
     * 扫码结果回调
     * @param result 扫码结果
     * @return
     */
    @Override
    public boolean onResultCallback(String result) {
        if (isContinuousScan) {
            BarcodeScannerPlugin.onBarcodeScanReceiver(result);
            if (maxScan > 0 && --maxScan <= 0) {
                finish();
            } else {
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mCaptureHelper.restartPreviewAndDecode();
                    }
                }, delay);
            }
        } else {
            Intent data = new Intent();
            data.putExtra(BarcodeObject, result);
            setResult(BarcodeScannerPlugin.SUCCESS, data);
            finish();
        }

        return true;
    }

    private void clickFlash(View v){
        boolean isSelected = v.isSelected();
        setTorch(!isSelected);
        v.setSelected(!isSelected);
    }

    public void onClick(View v){
        int vid = v.getId();
        if (vid == R.id.ivLeft) {
            onBackPressed();
        } else if (vid == R.id.ivFlash) {
            clickFlash(v);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        setResult(BarcodeScannerPlugin.NO_CAM_PERM);
        finish();
    }
}
