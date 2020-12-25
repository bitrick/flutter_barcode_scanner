package com.digitalplant.barcode_scanner.views;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.digitalplant.barcode_scanner.R;
import com.digitalplant.barcode_scanner.util.JobManager;
import com.digitalplant.common.DataCaptureManager;
import com.scandit.datacapture.barcode.capture.BarcodeCapture;
import com.scandit.datacapture.barcode.capture.BarcodeCaptureListener;
import com.scandit.datacapture.barcode.capture.BarcodeCaptureSession;
import com.scandit.datacapture.barcode.capture.BarcodeCaptureSettings;
import com.scandit.datacapture.barcode.data.Barcode;
import com.scandit.datacapture.core.capture.DataCaptureContext;
import com.scandit.datacapture.core.common.feedback.Feedback;
import com.scandit.datacapture.core.data.FrameData;
import com.scandit.datacapture.core.source.Camera;
import com.scandit.datacapture.core.source.CameraSettings;
import com.scandit.datacapture.core.source.FrameSourceState;
import com.scandit.datacapture.core.source.TorchState;
import com.scandit.datacapture.core.ui.DataCaptureView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import io.flutter.app.FlutterApplication;
import io.flutter.plugin.common.MethodChannel;

public class EmbeddedScanView extends ConstraintLayout implements BarcodeCaptureListener {
    public static final String TAG = EmbeddedScanView.class.getSimpleName();

    private Activity activity;
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

    private DataCaptureContext dataCaptureContext;
    private BarcodeCapture barcodeCapture;
    private Camera camera;

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

        frameLayout = findViewById(R.id.camera_view_holder);

        dataCaptureContext = DataCaptureManager.dataCaptureContext;
        BarcodeCaptureSettings settings = new BarcodeCaptureSettings();
        settings.enableSymbologies(new HashSet<>(Arrays.asList(DataCaptureManager.BarcodeFormats)));

        barcodeCapture = BarcodeCapture.forDataCaptureContext(dataCaptureContext, settings);
        barcodeCapture.getFeedback().setSuccess(new Feedback(null, null));
        barcodeCapture.addListener(this);

        CameraSettings cameraSettings = BarcodeCapture.createRecommendedCameraSettings();
        camera = Camera.getDefaultCamera();

        if (camera != null) {
            camera.applySettings(cameraSettings);
            dataCaptureContext.setFrameSource(camera);
            camera.switchToDesiredState(FrameSourceState.ON);
        }

        frameLayout.addView(DataCaptureView.newInstance(activity, dataCaptureContext));
    }

    public void setViewId(int viewId) {
        this.viewId = viewId;
    }

    public int getViewId() {
        return viewId;
    }

    public void setFormats(ArrayList<Integer> formats) {
        BarcodeCaptureSettings settings = new BarcodeCaptureSettings();
        if (formats == null) {
            settings.enableSymbologies(new HashSet<>(Arrays.asList(DataCaptureManager.BarcodeFormats)));
        } else {
            for (Integer f : formats) {
                settings.enableSymbology(DataCaptureManager.BarcodeFormats[f], true);
            }
        }

        barcodeCapture.applySettings(settings);
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

        barcodeCapture.setEnabled(true);
        if (camera != null) {
            camera.switchToDesiredState(FrameSourceState.ON);
        }
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        Log.d(TAG,"onVisibilityChanged: "+viewId);

        if (barcodeCapture == null) { return; }
        barcodeCapture.setEnabled(visibility == View.VISIBLE);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        Log.d(TAG,"onDetachedFromWindow: "+viewId);
        if (barcodeCapture != null) {
            barcodeCapture.removeListener(this);
            dataCaptureContext.removeMode(barcodeCapture);
        }

        if (camera != null) {
            camera.switchToDesiredState(FrameSourceState.OFF);
        }

        vibrator.cancel();
    }

    /**
     * 扫码结果回调
     */
    @Override
    public void onBarcodeScanned(@NonNull BarcodeCapture barcodeCapture,
                                 @NonNull BarcodeCaptureSession session, @NonNull FrameData frameData) {
        List<Barcode> barcodes = session.getNewlyRecognizedBarcodes();
        if (barcodes.size() == 0) {
            return;
        }

        if (vibrateOn && vibrator.hasVibrator()) {
            vibrator.vibrate(80);
        }

        final String result = barcodes.get(0).getData();
        jobManager.runOnUiThread(new JobManager.Task() {
            @Override
            public void run() {
                channel.invokeMethod("EmbeddedScanner.onCode", result);
            }
        });

        barcodeCapture.setEnabled(false);
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

        jobManager.runOnUiThread(new JobManager.Task() {
            @Override
            public void run() {
                onFinishCallback();
            }
        });
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

            if (camera != null) {
                camera.setDesiredTorchState(flashOn ? TorchState.ON : TorchState.OFF);
            }

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
            setFormats((ArrayList<Integer>) params.get("formats"));
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
            barcodeCapture.setEnabled(true);
            okToStart = false;

            onReadyCallback();
        }
    }

    @Override
    public void onObservationStarted(@NotNull BarcodeCapture barcodeCapture) {}

    @Override
    public void onObservationStopped(@NotNull BarcodeCapture barcodeCapture) {}

    @Override
    public void onSessionUpdated(@NotNull BarcodeCapture barcodeCapture,
                                 @NotNull BarcodeCaptureSession barcodeCaptureSession,
                                 @NotNull FrameData frameData) {}
}
