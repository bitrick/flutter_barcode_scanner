package com.digitalplant.barcode_scanner.views;

import android.content.Context;

import android.util.Log;
import android.view.View;
import java.util.Map;

import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.plugin.platform.PlatformView;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;

public class EmbeddedScanner implements PlatformView, MethodCallHandler {
    public static final String TAG = EmbeddedScanner.class.getSimpleName();

    private Context context;
    private EmbeddedScanView scanView;
    private Registrar registrar;
    private MethodChannel channel;

    EmbeddedScanner(Context context, Registrar registrar, int id, Object o) {
        this.context = context;
        this.registrar = registrar;

        channel = new MethodChannel(registrar.messenger(), "embedded_scanner_" + id);
        channel.setMethodCallHandler(this);

        scanView = getScanView(registrar);
        scanView.setMethodChannel(channel);
        scanView.setViewId(id);

        if (o != null) {
            scanView.setScanParams((Map<String, Object>) o);
        }
    }

    @Override
    public View getView() {
        return scanView;
    }

    @Override
    public void dispose() {
        Log.d("EmbeddedScanner","dispose: "+scanView.getViewId());
    }

    private EmbeddedScanView getScanView(Registrar registrar) {
        return new EmbeddedScanView(registrar.context());
    }

    @Override
    public void onMethodCall(MethodCall call, MethodChannel.Result result) {
        switch (call.method) {
        case "EmbeddedScanner.setScanParams":
            scanView.setScanParams((Map <String, Object>) call.arguments);
            result.success(null);
            break;

        case "EmbeddedScanner.readyToScan":
            scanView.setReadyToScan();
            result.success(null);
            break;

        case "EmbeddedScanner.toggleFlash":
            scanView.toggleFlash();
            result.success(null);
            break;

        case "EmbeddedScanner.hasTorch":
            result.success(scanView.hasTorch());
            break;

        case "EmbeddedScanner.toggleVibrate":
            scanView.toggleVibrate();
            result.success(null);
            break;

        default:
            result.notImplemented();
        }

    }
}
