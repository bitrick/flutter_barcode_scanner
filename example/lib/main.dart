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
                    child: Text("开启View"),
                    onPressed: () {
                      EmbeddedScannerHolder.instance.show();
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
                    child: Text("监听广播"),
                    onPressed: () async {
                      var fn = (code) {
                        print(code);
                      };

                      BarcodeScanner.onBroadcast(fn);
                      Future.delayed(Duration(seconds: 10)).then((_) {
                        BarcodeScanner.unBroadcast(fn);
                      });
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
