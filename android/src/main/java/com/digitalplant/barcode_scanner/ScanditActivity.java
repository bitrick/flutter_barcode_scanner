package com.digitalplant.barcode_scanner;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.digitalplant.common.DataCaptureManager;
import com.scandit.datacapture.barcode.capture.BarcodeCapture;
import com.scandit.datacapture.barcode.capture.BarcodeCaptureListener;
import com.scandit.datacapture.barcode.capture.BarcodeCaptureSession;
import com.scandit.datacapture.barcode.capture.BarcodeCaptureSettings;
import com.scandit.datacapture.barcode.data.Barcode;
import com.scandit.datacapture.barcode.data.Symbology;
import com.scandit.datacapture.barcode.ui.overlay.BarcodeCaptureOverlay;
import com.scandit.datacapture.core.capture.DataCaptureContext;
import com.scandit.datacapture.core.common.feedback.Feedback;
import com.scandit.datacapture.core.common.geometry.FloatWithUnit;
import com.scandit.datacapture.core.common.geometry.MarginsWithUnit;
import com.scandit.datacapture.core.common.geometry.MeasureUnit;
import com.scandit.datacapture.core.data.FrameData;
import com.scandit.datacapture.core.source.Camera;
import com.scandit.datacapture.core.source.CameraSettings;
import com.scandit.datacapture.core.source.FrameSourceState;
import com.scandit.datacapture.core.source.TorchListener;
import com.scandit.datacapture.core.source.TorchState;
import com.scandit.datacapture.core.ui.DataCaptureView;
import com.scandit.datacapture.core.ui.viewfinder.RectangularViewfinder;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import androidx.annotation.NonNull;


public class ScanditActivity extends Activity implements BarcodeCaptureListener {
    private static final String TAG = ScanditActivity.class.getSimpleName();

    private FrameLayout frameLayout;
    private ImageView flushBtn;
    private ImageView backBtn;

    //The width and height of scan_view_finder is both 240 dp.
    final int SCAN_FRAME_SIZE = 240;

    private DataCaptureContext dataCaptureContext;
    private BarcodeCapture barcodeCapture;
    private Camera camera;

    private ArrayList<Integer> formats;

    //Declare the key. It is used to obtain the value returned from Scan Kit.
    public static final String SCAN_RESULT = "scanResult";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_scandit);

        frameLayout = findViewById(R.id.camera_holder);

        Intent intent = getIntent();
        formats = intent.getIntegerArrayListExtra(BarcodeScannerPlugin.FORMATS_KEY);

        dataCaptureContext = DataCaptureManager.dataCaptureContext;
        BarcodeCaptureSettings settings = new BarcodeCaptureSettings();
        if (formats == null) {
            settings.enableSymbologies(new HashSet<>(Arrays.asList(DataCaptureManager.BarcodeFormats)));
        } else {
            for (Integer f : formats) {
                settings.enableSymbology(DataCaptureManager.BarcodeFormats[f], true);
            }
        }

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

        //1. Obtain the screen density to calculate the viewfinder's rectangle.
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float density = dm.density;
        //2. Obtain the screen size.
        int mScreenWidth = getResources().getDisplayMetrics().widthPixels;
        int mScreenHeight = getResources().getDisplayMetrics().heightPixels;

        int scanFrameSize = (int) (SCAN_FRAME_SIZE * density);

        //3. Calculate the viewfinder's rectangle, which in the middle of the layout.
        //Set the scanning area. (Optional. Rect can be null. If no settings are specified, it will be located in the middle of the layout.)
        Rect rect = new Rect();
        rect.left = (mScreenWidth - scanFrameSize) / 2;
        rect.right = (mScreenWidth + scanFrameSize) / 2;
        rect.top = (mScreenHeight - scanFrameSize) / 2;
        rect.bottom = (mScreenHeight + scanFrameSize) / 2;

        DataCaptureView dataCaptureView = DataCaptureView.newInstance(this, dataCaptureContext);
        MarginsWithUnit margins = new MarginsWithUnit(
            new FloatWithUnit(0, MeasureUnit.FRACTION),
            new FloatWithUnit(0.3f, MeasureUnit.FRACTION),
            new FloatWithUnit(0, MeasureUnit.FRACTION),
            new FloatWithUnit(0.3f, MeasureUnit.FRACTION)
        );
        dataCaptureView.setScanAreaMargins(margins);

        // Add a barcode capture overlay to the data capture view to render the location of captured
        // barcodes on top of the video preview.
        // This is optional, but recommended for better visual feedback.
        BarcodeCaptureOverlay overlay = BarcodeCaptureOverlay.newInstance(barcodeCapture, dataCaptureView);
        RectangularViewfinder viewfinder = new RectangularViewfinder();
        viewfinder.setWidthAndAspectRatio(new FloatWithUnit(0.8f, MeasureUnit.FRACTION), 0.75f);
        overlay.setViewfinder(viewfinder);

        frameLayout.addView(dataCaptureView);

        setBackOperation();
        setFlashOperation();
    }

    @Override
    public void onBarcodeScanned(@NonNull BarcodeCapture barcodeCapture,
                                 @NonNull BarcodeCaptureSession session, @NonNull FrameData frameData) {
        List<Barcode> barcodes = session.getNewlyRecognizedBarcodes();
        if (barcodes.size() > 0) {
            Intent intent = new Intent();
            intent.putExtra(SCAN_RESULT, barcodes.get(0).getData());
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    private void setFlashOperation() {
        flushBtn = findViewById(R.id.flashlight_btn);
        if (camera == null) { return; }

        flushBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (camera.getDesiredTorchState() == TorchState.ON) {
                    camera.setDesiredTorchState(TorchState.OFF);
                } else {
                    camera.setDesiredTorchState(TorchState.ON);
                }
            }
        });

        camera.addTorchListener(new TorchListener() {
            @Override
            public void onTorchStateChanged(@NotNull TorchState torchState) {
                if (torchState == TorchState.ON) {
                    flushBtn.setImageResource(R.drawable.flashlight_on);
                } else {
                    flushBtn.setImageResource(R.drawable.flashlight_off);
                }
            }
        });
    }

    private void setBackOperation() {
        backBtn = findViewById(R.id.back_btn);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    /**
     * Call the lifecycle management method of the remoteView activity.
     */
    @Override
    protected void onStart() {
        super.onStart();

        camera.switchToDesiredState(FrameSourceState.ON);
    }

    @Override
    protected void onResume() {
        super.onResume();

        camera.switchToDesiredState(FrameSourceState.ON);
    }

    @Override
    protected void onPause() {
        super.onPause();

        camera.switchToDesiredState(FrameSourceState.OFF);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        camera.switchToDesiredState(FrameSourceState.OFF);
        barcodeCapture.removeListener(this);
        dataCaptureContext.removeMode(barcodeCapture);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        setResult(RESULT_CANCELED);
        finish();
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
