package com.digitalplant.barcode_scanner.views;

import android.content.Context;
import io.flutter.plugin.common.StandardMessageCodec;
import io.flutter.plugin.platform.PlatformView;
import io.flutter.plugin.platform.PlatformViewFactory;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import com.digitalplant.barcode_scanner.views.EmbeddedScanner;

public class EmbeddedScannerFactory extends PlatformViewFactory {

    private final Registrar mPluginRegistrar;

    public EmbeddedScannerFactory(Registrar registrar) {
        super(StandardMessageCodec.INSTANCE);
        mPluginRegistrar = registrar;
    }

    @Override
    public PlatformView create(Context context, int i, Object o) {
        return new EmbeddedScanner(context, mPluginRegistrar, i, o);
    }
}
