import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class EmbeddedScanParams {
  static const int InfiniteScan = -1;
}

class EmbeddedScannerController {
  final int viewId;
  VoidCallback onReady;
  VoidCallback onFinish;
  Function(bool) onToggleFlash;
  Function(bool) onToggleVibrate;
  Function(String) onCode;

  MethodChannel _channel;

  EmbeddedScannerController({
    @required
    this.viewId,
    this.onReady,
    this.onFinish,
    this.onCode,
    this.onToggleFlash,
    this.onToggleVibrate,
  }){
    _channel = MethodChannel("embedded_scanner_$viewId");
    _channel.setMethodCallHandler(_onMethodCall);
  }

  Future<dynamic> _onMethodCall(MethodCall methodCall) async {
    final String method = methodCall.method;

    switch (method) {
    case 'EmbeddedScanner.onReady':
      if (onReady != null) {
        onReady();
      }
      break;

    case 'EmbeddedScanner.onCode':
      if (onCode != null) {
        onCode(methodCall.arguments);
      }
      break;

    case 'EmbeddedScanner.onFinish':
      if (onFinish != null) {
        onFinish();
      }
      break;

    case 'EmbeddedScanner.onToggleFlash':
      if (onToggleFlash != null) {
        onToggleFlash(methodCall.arguments);
      }
      break;
    
    case 'EmbeddedScanner.onToggleVibrate':
      if (onToggleVibrate != null) {
        onToggleVibrate(methodCall.arguments);
      }
      break;

    }
  }

  setScanParams({bool autoRestart=false, int maxScan, int delay) async {
    Map<String, dynamic> params = {};
    if (autoRestart != null) {
      params["autoRestart"] = autoRestart;
    }
    if (maxScan != null) {
      params["maxScan"] = maxScan;
    }
    if (delay != null) {
      params["delay"] = delay;
    }

    if (params.length == 0) {
      return null;
    }
    return _channel.invokeMethod("EmbeddedScanner.setScanParams", params);
  }

  toggleFlash() async {
    return _channel.invokeMethod("EmbeddedScanner.toggleFlash");
  }

  toggleVibrate() async {
    return _channel.invokeMethod("EmbeddedScanner.toggleVibrate");
  }

  readyToScan() async {
    return _channel.invokeMethod("EmbeddedScanner.readyToScan");
  }
}

class EmbeddedScanner extends StatefulWidget {
  final Function(EmbeddedScannerController controller) onCreated;
  final List<int> formats;
  final int delay;
  final int maxScan;
  final bool autoRestart;

  EmbeddedScanner({
    @required
    this.onCreated,
    this.formats,
    this.delay,
    this.maxScan,
    this.autoRestart,
  });

  @override
  State<StatefulWidget> createState() {
    return _EmbededScannerState();
  }
}

class _EmbededScannerState extends State<EmbeddedScanner> {
  @override
  Widget build(BuildContext context) {
    Map<String, dynamic> params = {};
    if (widget.autoRestart != null) {
      params["autoRestart"] = widget.autoRestart;
    }
    if (widget.maxScan != null) {
      params["maxScan"] = widget.maxScan;
    }
    if (widget.delay != null) {
      params["delay"] = widget.delay;
    }
    if (widget.formats != null) {
      params["formats"] = widget.formats;
    }

    return AndroidView(
      viewType: "embedded_scanner",
      onPlatformViewCreated: onPlatformViewCreated,
      creationParamsCodec: const StandardMessageCodec(),
      creationParams: params,
    );
  }

  Future onPlatformViewCreated(id) async {
    widget.onCreated(EmbeddedScannerController(viewId: id));
  }
}
