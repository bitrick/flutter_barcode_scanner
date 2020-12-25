import 'dart:async';

import 'package:flutter/services.dart';

class BarcodeScanner {
  static const MethodChannel _channel =
      const MethodChannel('barcode_scanner');

  static init(String licenseKey) {
    _channel.invokeMethod("init", licenseKey);
  }

  static Future<String> scan({List<int> formats}) async {
    Map params = <String, dynamic>{};
    if (formats != null) {
      params['formats'] = formats;
    }

    final String code = await _channel.invokeMethod('scan', params);
    return code;
  }

  static Future<String> immRestartInput() async {
    return _channel.invokeMethod('immRestartInput', {});
  }
}