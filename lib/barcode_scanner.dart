library barcode_scanner;

export "package:barcode_scanner/activity_scanner.dart";
export "package:barcode_scanner/embedded_scanner.dart";

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
