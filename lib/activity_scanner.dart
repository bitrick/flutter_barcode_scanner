import 'dart:async';

import 'package:flutter/services.dart';

class BarcodeScanner {
  static const MethodChannel _channel =
      const MethodChannel('barcode_scanner');

  static const EventChannel _eventChannel =
      const EventChannel('barcode_scanner_receiver');

  static Future<String> scan({List<int> formats}) async {
    Map params = <String, dynamic>{};
    if (formats != null) {
      params['formats'] = formats;
    }

    final String code = await _channel.invokeMethod('scan', params);
    return code;
  }

  static Stream scanMulti({List<int> formats, int maxScan=-1, int delay=300}) {
    assert(delay>16);

    /// create params to be pass to plugin
    Map params = <String, dynamic>{
      "maxscan": maxScan,
      "delay": delay,
    };
    if (formats != null) {
      params['formats'] = formats;
    }

    /// Invoke method to open camera
    /// and then create event channel which will return stream
    _channel.invokeMethod('scanMulti', params);
    return _eventChannel.receiveBroadcastStream();
  }

  static Future<String> immRestartInput() async {
    return _channel.invokeMethod('immRestartInput', {});
  }
}