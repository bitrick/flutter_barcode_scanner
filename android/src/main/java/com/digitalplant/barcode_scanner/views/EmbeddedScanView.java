package com.digitalplant.barcode_scanner.views;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import com.digitalplant.barcode_scanner.BarcodeScannerPlugin;
import com.digitalplant.barcode_scanner.R;
import com.digitalplant.barcode_scanner.util.JobManager;

import com.huawei.hms.hmsscankit.OnResultCallback;
import com.huawei.hms.hmsscankit.RemoteView;
import com.huawei.hms.ml.scan.HmsScan;


import java.util.ArrayList;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import io.flutter.app.FlutterApplication;
import io.flutter.plugin.common.MethodChannel;

public class EmbeddedScanView extends ConstraintLayout implements OnResultCallback {
    public static final String TAG = EmbeddedScanView.class.getSimpleName();

    private Activity activity;
    private RemoteView remoteView;
    private FrameLayout frameLayout;
    private Vibrator vibrator;
    private MethodChannel channel;
    private boolean flashOn = false;
    private boolean vibrateOn = true;
    private boolean okToStart = false;
    private boolean autoStart = false;
    private int maxScan = -1;
    private int delay = 0;
    private int viewId;
    private ArrayList<Integer> formats;

    private JobManager jobManager;

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
        jobManager = new JobManager(activity);

        vibrator = (Vibrator) activity.getApplication().getSystemService(Service.VIBRATOR_SERVICE);
    }

    public void setViewId(int viewId) {
        this.viewId = viewId;
    }

    public int getViewId() {
        return viewId;
    }

    public void setFormats(ArrayList<Integer> formats) {
        this.formats = formats;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public void setMaxScan(int maxScan) {
        this.maxScan = maxScan;
    }

    public void setAutoRestart(boolean autoStart) {
        this.autoStart = autoStart;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        frameLayout = findViewById(R.id.camera_view_holder);
        ViewTreeObserver observer = frameLayout.getViewTreeObserver();
        if (observer.isAlive()) {
            observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    frameLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    initRemoteView();
                    remoteView.onCreate(null);
                    remoteView.onStart();
                }
            });
        }
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        Log.d(TAG,"onVisibilityChanged: "+viewId);

        if (remoteView == null) { return; }
        if (visibility == View.VISIBLE) {
            remoteView.onResume();
        } else {
            remoteView.onPause();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        Log.d(TAG,"onDetachedFromWindow: "+viewId);
        if (remoteView != null) {
            remoteView.onStop();
            remoteView.onDestroy();
        }

        vibrator.cancel();
    }

    /**
     * 初始化相机，解码器
     */
    public void initRemoteView(){
        DisplayMetrics dm = getResources().getDisplayMetrics();
        //2. Obtain the screen size.
        int screenWidth = dm.widthPixels;
        int screenHeight = dm.heightPixels;
        int viewHeight = frameLayout.getHeight();

        //3. Calculate the viewfinder's rectangle, which in the middle of the layout.
        //Set the scanning area. (Optional. Rect can be null. If no settings are specified, it will be located in the middle of the layout.)
        Rect rect = new Rect(0, (screenHeight-viewHeight)/2, 0, (screenHeight+viewHeight)/2);

        int scanType1 = HmsScan.ALL_SCAN_TYPE;
        int[] scanTypes = new int[0];
        if (formats != null && formats.size() > 0) {
            scanType1 = formats.get(0).intValue();

            scanTypes = new int[formats.size()-1];
            for (int i=1; i<formats.size(); ++i) {
                scanTypes[i-1] = formats.get(i).intValue();
            }
        }

        //Initialize the RemoteView instance, and set callback for the scanning result.
        remoteView = new RemoteView.Builder()
                .setContext(activity)
                .setBoundingBox(rect)
                .setFormat(scanType1, scanTypes)
                .build();

        remoteView.setOnResultCallback(this);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(screenWidth, screenHeight);
        params.gravity = Gravity.CENTER;

        frameLayout.addView(remoteView, params);
    }

    /**
     * 扫码结果回调
     * @param result 扫码结果
     */
    @Override
    public void onResult(HmsScan[] result) {
        if (result == null || result.length == 0 || result[0] == null || TextUtils.isEmpty(result[0].getOriginalValue())) {
            return;
        }

        channel.invokeMethod("EmbeddedScanner.onCode", result[0].getOriginalValue());
        if (vibrateOn && vibrator.hasVibrator()) {
            vibrator.vibrate(80);
        }

        remoteView.pauseContinuouslyScan();
        if (autoStart) {
            if(--maxScan != 0 ){
                if (delay <= 0) {
                    delay = 300;
                }

                jobManager.postDelayed(new JobManager.Task() {
                    @Override
                    public void run() {
                        okToStart = true;
                        setReadyToScan();
                    }
                }, delay);
                return;
            }
        } else {
            okToStart = true;
            return;
        }

        onFinishCallback();
    }

    public void onFinishCallback() {
        channel.invokeMethod("EmbeddedScanner.onFinish", "");
    }

    public void onReadyCallback() {
        channel.invokeMethod("EmbeddedScanner.onReady", "");
    }

    /**
     * 打开闪光灯
     */
    public void toggleFlash() {
        if (hasTorch()) {
            flashOn = !flashOn;

            remoteView.switchLight();
            channel.invokeMethod("EmbeddedScanner.onToggleFlash", flashOn);
        }
    }

    /**
     * 打开震动
     */
    public void toggleVibrate() {
        vibrateOn = !vibrateOn;
        channel.invokeMethod("EmbeddedScanner.onToggleVibrate", vibrateOn);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    /**
     * 检测是否支持闪光灯（手电筒）
     * @return
     */
    public boolean hasTorch(){
        return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    public void setScanParams(Map<String, Object> params) {
        Log.d(TAG, "setScanParams: "+params.toString());
        if (params.containsKey("formats")) {
            ArrayList<Integer> fs = (ArrayList<Integer>) params.get("formats");
            ArrayList<Integer> _formats = new ArrayList<>();
            for (int f : fs) {
                _formats.add(BarcodeScannerPlugin.BarcodeFormats[f]);
            }
            setFormats(_formats);
        }

        if (params.containsKey("maxScan")) {
            setMaxScan((int) params.get("maxScan"));
        }

        if (params.containsKey("delay")) {
            setDelay((int) params.get("delay"));
        }

        if (params.containsKey("autoRestart")) {
            setAutoRestart((boolean) params.get("autoRestart"));
        }
    }

    public void setReadyToScan() {
        if (okToStart) {
            remoteView.resumeContinuouslyScan();
            okToStart = false;

            onReadyCallback();
        }
    }
}
