import 'dart:async';

import 'package:flutter/services.dart';

class BarcodeFormat {

  /// Aztec 2D barcode format.
  static const int AZTEC = 0;

  /// CODABAR 1D format.
  static const int CODABAR = 1;

  /// Code 39 1D format.
  static const int CODE_39 = 2;

  /// Code 93 1D format.
  static const int CODE_93 = 3;

  /// Code 128 1D format.
  static const int CODE_128 = 4;

  /// Data Matrix 2D barcode format.
  static const int DATA_MATRIX = 5;

  /// EAN-8 1D format.
  static const int EAN_8 = 6;

  /// EAN-13 1D format.
  static const int EAN_13 = 7;

  /// ITF (Interleaved Two of Five) 1D format.
  static const int ITF = 8;

  /// MaxiCode 2D barcode format.
  static const int MAXICODE = 9;

  /// PDF417 format.
  static const int PDF_417 = 10;

  /// QR Code 2D barcode format.
  static const int QR_CODE = 11;

  /// RSS 14.
  static const int RSS_14 = 12;

  /// RSS EXPANDED.
  static const int RSS_EXPANDED = 13;

  /// UPC-A 1D format.
  static const int UPC_A = 14;

  /// UPC-E 1D format.
  static const int UPC_E = 15;

  /// UPC/EAN extension format. Not a stand-alone format.
  static const int UPC_EAN_EXTENSION = 16;
}

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

  static Future<String> getInputDeviceName() async {
    return _channel.invokeMethod('getInputDeviceName', {});
  }

  static Future<String> cancelGetInputDeviceName() async {
    return _channel.invokeMethod('cancelGetInputDeviceName', {});
  }

  static Future<String> setInputDeviceName(String deviceName) async {
    return _channel.invokeMethod('setInputDeviceName', {"device_name": deviceName});
  }

  static Stream startBarcodeListener() {
    _channel.invokeMethod('startBarcodeListener', {});
    return _eventChannel.receiveBroadcastStream();
  }

  static Future<String> stopBarcodeListener(String deviceName) async {
    return _channel.invokeMethod('stopBarcodeListener', {});
  }
}
