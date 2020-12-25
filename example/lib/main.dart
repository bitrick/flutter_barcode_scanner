import 'package:barcode_scanner_example/embedded_scanner_holder.dart';
import 'package:flutter/material.dart';
import 'package:barcode_scanner/barcode_scanner.dart';
import 'package:barcode_scanner/embedded_scanner.dart';
import 'package:flutter/services.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  MyApp() {
    SystemChrome.setPreferredOrientations([DeviceOrientation.portraitUp]);
  }

  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  List<String> codes = [];
  EmbeddedScannerController controller;

  @override
  void initState() {
    super.initState();

    BarcodeScanner.init("AbUfxSvuNMGiM+J7zDVH+AYiJm9gMkBSyFzCEM96/4zsUbSP9AKeM31LsY24YUymMBWu/6FKTK48am/LRTVt3FoWyZtjUWR75QRMaG9rriQVWP4OYHVtLudWGuCZfX0+qiTqlh4RPjZzKNMQ5g76WtOE1c6bqdIjjaR6OR4BOqfxuc9dkp8P19avc0UEeGE/QU/Olw8n/bLbhmwwpfIFx+cXXU77iPKj3RQkZ1+FlZ26MyFqbjHs68DnuBQ2xk9QvhZpRYvtZRzYH23ua0B/XkaaaezXrxmlbMPNVK5A3ujEKJlz3dyY8BA4YR2Z45ykpK0CtT2uiq3aRKduiRXE4FyBzQJKHSXw47vBMQCrj5vzwcNNqFlW0FFa5MAswRV09LaZq0rTfQx+ZYwigRMdnsFQexuQ92i2ldRJJis5bBbMaFlaAOje+k3WzIeWRBewsPurCzqQB63G4i/qnro0efmhcBcM/Axt74BvhH39Bw2bS5snANag06cEQRj5ZyCRU7fFiGQu93oC5TQQUWy9M2+LbSFvAKKZruuKfdSOrRHxYbCrxby1PL33olAVG97EtGBd2zp807ecdrRLHx0Qv3aOuzkx3wG7inTFeobKoj1fite+ERGGn4ViWi3JurCo2eEPtkFaorlYG3jPsp5KPOoUu2OaPwQWHqAGd6Yh1CYpA4Viv4cgzOhsdkSRlHIi0Dh5BVf7AcrVVACkPOMYdRJi3jUD+dXYdhBNdKmU15aen86AqNarQ93Q0YhT02GGwKQeN5WmllGFzNm5OTnBGgjdbhZtIShX5nWBWZA+Mys19FBZRnMU+dElIzaT0g==");
  }

  @override
  Widget build(BuildContext context) {
    var codeWidgets = <Widget>[];
    for (var code in codes) {
      codeWidgets.add(Text(code, textAlign: TextAlign.center));
    }

    return MaterialApp(
      home: EmbeddedScannerHolder(
        child: Scaffold(
          appBar: AppBar(
            title: const Text('条码扫描'),
          ),
          body: SingleChildScrollView(
            child: Container(
              child: Column(
                children: <Widget>[
                  RaisedButton(
                    child: Text("连续扫描"),
                    onPressed: () {
                      EmbeddedScannerHolder.instance.show(formats: [BarcodeFormat.QR_CODE, BarcodeFormat.CODE_128]);
                      EmbeddedScannerHolder.instance.onCode = (result) {
                        setState(() {
                          if (codes.contains(result)) {
                            return;
                          }
                          codes.add(result);
                        });
                      };
                    },
                  ),

                  RaisedButton(
                    child: Text("单次扫描"),
                    onPressed: () async {
                      EmbeddedScannerHolder.instance.show(autoRestart: true, maxScan: 1);
                      EmbeddedScannerHolder.instance.onCode = (result) {
                        setState(() {
                          codes = <String>[result];
                        });
                      };
                    },
                  ),

                  RaisedButton(
                    child: Text("全屏扫描"),
                    onPressed: () async {
                      var code = await BarcodeScanner.scan();
                      if (code != null) {
                        setState(() {
                          codes = <String>[code];
                        });
                      }
                    },
                  ),

                  Container(
                    padding: EdgeInsets.symmetric(horizontal: 15),
                    child: TextField(
                      onSubmitted: (v) {
                        Future.microtask((){
                          BarcodeScanner.immRestartInput();
                        });
                      },
                    ),
                  ),

                  RaisedButton(
                    child: Text("监听键盘"),
                    onPressed: () async {
                      RawKeyboard.instance.addListener(_onKey);
                    },
                  ),

                  RaisedButton(
                    child: Text("取消监听"),
                    onPressed: () async {
                      RawKeyboard.instance.removeListener(_onKey);
                    },
                  ),

                  SizedBox(height: 16),
                ] + codeWidgets,
              ),
            ),
          )
        ),
      )
    );
  }

  void _onKey(RawKeyEvent e) {
    print(e);
  }
}
