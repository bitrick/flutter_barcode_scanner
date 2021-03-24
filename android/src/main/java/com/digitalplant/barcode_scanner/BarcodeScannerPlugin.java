package com.digitalplant.barcode_scanner;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
//import androidx.annotation.CheckResult;
import android.util.Log;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import android.view.View;

import com.digitalplant.barcode_scanner.views.EmbeddedScannerFactory;
import com.digitalplant.common.KeyboardListener;

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
    private static InputMethodManager imm;

    public static String deviceName = "";
    public static final String FORMATS_KEY       = "formats";
    public static final String IS_CONTINUOUS_KEY = "multi";
    public static final String MAX_SCAN_KEY      = "max_scan";
    public static final String DELAY_KEY         = "delay";

    public static final int RC_CAMERA = 0X01;

    public static final int SUCCESS = 1;
    public static final int NO_CAM_PERM = 2;

    public static final int RC_SCAN = 1;
    public static final int RC_SCAN_MULTI = 2;

    private static Result pendingResult;
    private static FlutterActivity activity;
    private final Registrar registrar;
    private static EventChannel.EventSink barcodeStream;

    private BarcodeScannerPlugin(FlutterActivity activity, final Registrar registrar) {
        BarcodeScannerPlugin.activity = activity;
        this.registrar = registrar;

        View flutterView = activity.getFlutterView();
        imm = (InputMethodManager) flutterView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

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
        BarcodeScannerPlugin instance = new BarcodeScannerPlugin((FlutterActivity) registrar.activity(), registrar);
        registrar.addActivityResultListener(instance);
        channel.setMethodCallHandler(instance);

        final EventChannel eventChannel = new EventChannel(registrar.messenger(), "barcode_scanner_receiver");
        eventChannel.setStreamHandler(instance);

        registrar.platformViewRegistry().registerViewFactory("embedded_scanner", new EmbeddedScannerFactory(registrar));
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        try {
            if (pendingResult != null) {
                pendingResult.success("PREVIOUS_CALL_INTERRUPTED");
            }
            pendingResult = result;
            if (!(call.arguments instanceof Map)) {
                throw new IllegalArgumentException("Plugin not passing a map as parameter: " + call.arguments);
            }

            switch (call.method) {
            case "scan":
                scan(call, result);
                break;

            case "scanMulti":
                scanMulti(call, result);
                break;

            case "immRestartInput":
                immRestartInput(call, result);
                break;

            default:
                result.notImplemented();
                break;
            }
        } catch (Exception e) {
            Log.e(TAG, "onMethodCall: " + e.getMessage());
        }
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case RC_SCAN:
            return scanResult(requestCode, resultCode, data);

        case RC_SCAN_MULTI:
            return scanMultiResult(requestCode, resultCode, data);

        }
        return false;
    }


    @Override
    public void onListen(Object o, EventChannel.EventSink eventSink) {
        barcodeStream = eventSink;
    }

    @Override
    public void onCancel(Object o) {
        barcodeStream = null;
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
            Log.e(TAG, "onBarcodeScanReceiver: " + e.getMessage());
        }
    }

    private void scan(MethodCall call, Result result) {
        Map<String, Object> arguments = (Map <String, Object>) call.arguments;
        ArrayList<Integer> formats = (ArrayList<Integer>) arguments.get("formats");

        Intent intent = new Intent(activity, CaptureActivity.class);
        intent.putIntegerArrayListExtra(FORMATS_KEY, formats);
        activity.startActivityForResult(intent, RC_SCAN);
    }

    private boolean scanResult(int requestCode, int resultCode, Intent data) {
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
        } else {
            pendingResult.success("");
        }

        pendingResult = null;
        return true;
    }

    private void scanMulti(MethodCall call, Result result) {
        Map<String, Object> arguments = (Map <String, Object>) call.arguments;
        ArrayList<Integer> formats = (ArrayList<Integer>) arguments.get("formats");

        int maxScan = (int) arguments.get("maxscan");
        int delay = (int) arguments.get("delay");

        Intent intent = new Intent(activity, CaptureActivity.class);
        intent.putIntegerArrayListExtra(FORMATS_KEY, formats);
        intent.putExtra(IS_CONTINUOUS_KEY, true);
        intent.putExtra(MAX_SCAN_KEY, maxScan);
        intent.putExtra(DELAY_KEY, delay);
        activity.startActivityForResult(intent, RC_SCAN_MULTI);
    }

    private boolean scanMultiResult(int requestCode, int resultCode, Intent data) {
        if (barcodeStream != null) {
            barcodeStream.endOfStream();
        }
        return true;
    }

    private void immRestartInput(MethodCall call, Result result) {
        imm.restartInput(activity.getFlutterView());
    }
}

/**
 * 部分代码来源于 https://www.jianshu.com/p/5c1bf3e968e6
 */
class BarcodeListener implements KeyboardListener.Callback {
    private StringBuilder result;
    private boolean capsOn = false;

    BarcodeListener() {
        result = new StringBuilder();
    }

    @Override
    public boolean run(KeyEvent e) {
        String deviceName = BarcodeScannerPlugin.deviceName;
        if (!(deviceName.isEmpty() || deviceName.equals(e.getDevice().getName()))) {
            return false;
        }

        int keyCode = e.getKeyCode();
        switch (keyCode) {
        case KeyEvent.KEYCODE_SHIFT_RIGHT:
        case KeyEvent.KEYCODE_SHIFT_LEFT:
            capsOn = e.getAction() == KeyEvent.ACTION_DOWN;
            break;

        case KeyEvent.KEYCODE_ENTER:
            if (e.getAction() == KeyEvent.ACTION_DOWN) {
                BarcodeScannerPlugin.onBarcodeScanReceiver(result.toString());
                result.delete(0, result.length());
            }
            break;

        default:
            if (e.getAction() == KeyEvent.ACTION_DOWN) {
                char aChar = getInputCode(capsOn, keyCode);
                if (aChar != 0) {
                    result.append(aChar);
                }
            }
            break;
        }
        return false;
    }

    /**
     * 将keyCode转为char
     *
     * @param caps    是不是大写
     * @param keyCode 按键
     * @return 按键对应的char
     */
    private char getInputCode(boolean caps, int keyCode) {
        if (keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z) {
            return (char) ((caps ? 'A' : 'a') + keyCode - KeyEvent.KEYCODE_A);
        } else {
            return keyValue(caps, keyCode);
        }
    }

    /**
     * 按键对应的char表
     */
    private char keyValue(boolean caps, int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_0:
                return caps ? ')' : '0';
            case KeyEvent.KEYCODE_1:
                return caps ? '!' : '1';
            case KeyEvent.KEYCODE_2:
                return caps ? '@' : '2';
            case KeyEvent.KEYCODE_3:
                return caps ? '#' : '3';
            case KeyEvent.KEYCODE_4:
                return caps ? '$' : '4';
            case KeyEvent.KEYCODE_5:
                return caps ? '%' : '5';
            case KeyEvent.KEYCODE_6:
                return caps ? '^' : '6';
            case KeyEvent.KEYCODE_7:
                return caps ? '&' : '7';
            case KeyEvent.KEYCODE_8:
                return caps ? '*' : '8';
            case KeyEvent.KEYCODE_9:
                return caps ? '(' : '9';
            case KeyEvent.KEYCODE_NUMPAD_SUBTRACT:
                return '-';
            case KeyEvent.KEYCODE_MINUS:
                return '_';
            case KeyEvent.KEYCODE_EQUALS:
                return '=';
            case KeyEvent.KEYCODE_NUMPAD_ADD:
                return '+';
            case KeyEvent.KEYCODE_GRAVE:
                return caps ? '~' : '`';
            case KeyEvent.KEYCODE_BACKSLASH:
                return caps ? '|' : '\\';
            case KeyEvent.KEYCODE_LEFT_BRACKET:
                return caps ? '{' : '[';
            case KeyEvent.KEYCODE_RIGHT_BRACKET:
                return caps ? '}' : ']';
            case KeyEvent.KEYCODE_SEMICOLON:
                return caps ? ':' : ';';
            case KeyEvent.KEYCODE_APOSTROPHE:
                return caps ? '"' : '\'';
            case KeyEvent.KEYCODE_COMMA:
                return caps ? '<' : ',';
            case KeyEvent.KEYCODE_PERIOD:
                return caps ? '>' : '.';
            case KeyEvent.KEYCODE_SLASH:
                return caps ? '?' : '/';
            default:
                return 0;
        }
    }
}
