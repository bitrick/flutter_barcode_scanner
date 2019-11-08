package com.digitalplant.barcode_scanner;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
//import androidx.annotation.CheckResult;
import android.util.Log;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;

import java.util.Map;
import java.util.ArrayList;

import io.flutter.app.FlutterActivity;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener;
import io.flutter.plugin.common.EventChannel.StreamHandler;
import io.flutter.plugin.common.EventChannel;


/** BarcodeScannerPlugin */
public class BarcodeScannerPlugin implements MethodCallHandler, ActivityResultListener, StreamHandler {
    private static final String CHANNEL = "barcode_scanner";
    private static final String TAG = BarcodeScannerPlugin.class.getSimpleName();

    public static final String FORMATS_KEY = "formats";
    public static final String IS_CONTINUOUS_KEY = "multi";
    public static final String MAX_SCAN_KEY = "max_scan";
    public static final String DELAY_KEY = "delay";

    public static final int RC_CAMERA = 0X01;

    public static final int SUCCESS = 1;
    public static final int NO_CAM_PERM = 2;

    public static final int RC_SCAN = 1;
    public static final int RC_SCAN_MULTI = 2;

    private static Result pendingResult;
    private static FlutterActivity activity;
    private final Registrar registrar;
    static EventChannel.EventSink barcodeStream;

    private BarcodeScannerPlugin(FlutterActivity activity, final Registrar registrar) {
        BarcodeScannerPlugin.activity = activity;
        this.registrar = registrar;

        ActivityLifecycleCallbacks activityLifecycleCallbacks =
                new ActivityLifecycleCallbacks() {
                    @Override
                    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

                    @Override
                    public void onActivityStarted(Activity activity) {}

                    @Override
                    public void onActivityResumed(Activity activity) {}

                    @Override
                    public void onActivityPaused(Activity activity) {}

                    @Override
                    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                        if (activity == registrar.activity()) {
                            // TODO
                        }
                    }

                    @Override
                    public void onActivityDestroyed(Activity activity) {
                        if (activity == registrar.activity()) {
                            ((Application) registrar.context()).unregisterActivityLifecycleCallbacks(this);
                        }
                    }

                    @Override
                    public void onActivityStopped(Activity activity) {}
                };

        if (this.registrar != null) {
            ((Application) this.registrar.context())
                    .registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
        }
    }

    /** Plugin registration. */
    public static void registerWith(Registrar registrar) {
        if (registrar.activity() == null) {
            return;
        }

        final MethodChannel channel = new MethodChannel(registrar.messenger(), CHANNEL);
        BarcodeScannerPlugin instance =
                new BarcodeScannerPlugin((FlutterActivity) registrar.activity(), registrar);
        registrar.addActivityResultListener(instance);
        channel.setMethodCallHandler(instance);

        final EventChannel eventChannel =
                new EventChannel(registrar.messenger(), "barcode_scanner_receiver");
        eventChannel.setStreamHandler(instance);
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        try {
            pendingResult = result;
            if (!(call.arguments instanceof Map)) {
                throw new IllegalArgumentException("Plugin not passing a map as parameter: " + call.arguments);
            }
            Map<String, Object> arguments = (Map <String, Object>) call.arguments;
            ArrayList<Integer> formats = (ArrayList<Integer>) arguments.get("formats");

            if (call.method.equals("scan")) {
                Intent intent = new Intent(activity, CaptureActivity.class);
                intent.putIntegerArrayListExtra(FORMATS_KEY, formats);
                activity.startActivityForResult(intent, RC_SCAN);

            } else if (call.method.equals("scanMulti")) {
                int maxScan = (int) arguments.get("maxscan");
                int delay = (int) arguments.get("delay");

                Intent intent = new Intent(activity, CaptureActivity.class);
                intent.putIntegerArrayListExtra(FORMATS_KEY, formats);
                intent.putExtra(IS_CONTINUOUS_KEY, true);
                intent.putExtra(MAX_SCAN_KEY, maxScan);
                intent.putExtra(DELAY_KEY, delay);
                activity.startActivityForResult(intent, RC_SCAN_MULTI);

            } else {
                result.notImplemented();
            }
        } catch (Exception e) {
            Log.e(TAG, "onMethodCall: " + e.getLocalizedMessage());
        }

    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_SCAN) {
            if (resultCode == SUCCESS) {
                if (data != null) {
                    try {
                        pendingResult.success(data.getStringExtra(CaptureActivity.BarcodeObject));
                    } catch (Exception e) {
                        pendingResult.success("");
                    }
                } else {
                    pendingResult.success("");
                }
                pendingResult = null;
                return true;
            } else {
                pendingResult.success("");
            }
        } else if (requestCode == RC_SCAN_MULTI) {
            if (barcodeStream != null) {
                barcodeStream.endOfStream();
            }
        }

        return false;
    }


    @Override
    public void onListen(Object o, EventChannel.EventSink eventSink) {
        try {
            barcodeStream = eventSink;
        } catch (Exception e) {
        }
    }

    @Override
    public void onCancel(Object o) {
        try {
            barcodeStream = null;
        } catch (Exception e) {

        }
    }

    /**
     * Continuous receive barcode
     *
     * @param barcode
     */
    public static void onBarcodeScanReceiver(final String barcode) {
        try {
            if (barcode != null && !barcode.isEmpty()) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        barcodeStream.success(barcode);
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "onBarcodeScanReceiver: " + e.getLocalizedMessage());
        }
    }
}