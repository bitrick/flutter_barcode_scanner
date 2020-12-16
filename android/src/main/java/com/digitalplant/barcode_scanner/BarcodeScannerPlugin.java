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
import com.huawei.hms.ml.scan.HmsScan;

import java.util.Map;
import java.util.ArrayList;

import io.flutter.app.FlutterActivity;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener;


/** BarcodeScannerPlugin */
public class BarcodeScannerPlugin implements MethodCallHandler, ActivityResultListener {
    private static final String CHANNEL = "barcode_scanner";
    private static final String TAG = BarcodeScannerPlugin.class.getSimpleName();
    private static InputMethodManager imm;

    public static String deviceName = "";
    public static final String FORMATS_KEY       = "formats";
    public static final int RC_SCAN = 1;

    private static Result pendingResult;
    private static FlutterActivity activity;
    private final Registrar registrar;

    public static int[] BarcodeFormats = {
            HmsScan.AZTEC_SCAN_TYPE,
            HmsScan.CODABAR_SCAN_TYPE,
            HmsScan.CODE39_SCAN_TYPE,
            HmsScan.CODE93_SCAN_TYPE,
            HmsScan.CODE128_SCAN_TYPE,
            HmsScan.DATAMATRIX_SCAN_TYPE,
            HmsScan.EAN8_SCAN_TYPE,
            HmsScan.EAN13_SCAN_TYPE,
            HmsScan.ITF14_SCAN_TYPE,
            HmsScan.PDF417_SCAN_TYPE,
            HmsScan.QRCODE_SCAN_TYPE,
            HmsScan.UPCCODE_A_SCAN_TYPE,
            HmsScan.UPCCODE_E_SCAN_TYPE,
    };

    private BarcodeScannerPlugin(FlutterActivity activity, final Registrar registrar) {
        BarcodeScannerPlugin.activity = activity;
        this.registrar = registrar;

        View flutterView = activity.getFlutterView();
        imm = (InputMethodManager) flutterView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        ActivityLifecycleCallbacks activityLifecycleCallbacks = new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}
            @Override
            public void onActivityStarted(Activity activity) {}
            @Override
            public void onActivityResumed(Activity activity) {}
            @Override
            public void onActivityPaused(Activity activity) {}
            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

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
        return scanResult(resultCode, data);
    }

    private void scan(MethodCall call, Result result) {
        Map<String, Object> arguments = (Map <String, Object>) call.arguments;
        ArrayList<Integer> fs = (ArrayList<Integer>) arguments.get("formats");
        ArrayList<Integer> formats = new ArrayList<>();
        for (int f : fs) {
            formats.add(BarcodeFormats[f]);
        }

        Intent intent = new Intent(activity, DefinedActivity.class);
        intent.putIntegerArrayListExtra(FORMATS_KEY, formats);
        activity.startActivityForResult(intent, RC_SCAN);
    }

    private boolean scanResult(int resultCode, Intent data) {
        if (resultCode == DefinedActivity.RESULT_OK) {
            pendingResult.success(data.getStringExtra(DefinedActivity.SCAN_RESULT));
        } else {
            pendingResult.success("");
        }

        pendingResult = null;
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
