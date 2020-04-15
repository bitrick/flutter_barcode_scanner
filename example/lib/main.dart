import 'package:flutter/material.dart';
import 'package:barcode_scanner/barcode_scanner.dart';
import 'package:flutter/services.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  List<String> codes = [];

  @override
  Widget build(BuildContext context) {
    var codeWidgets = <Widget>[];
    for (var code in codes) {
      codeWidgets.add(Text(code, textAlign: TextAlign.center));
    }

    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('条码扫描'),
        ),
        body: Container(
          child: Column(
            children: <Widget>[
              RaisedButton(
                child: Text("单次扫描"),
                onPressed: () async {
                  var code = await BarcodeScanner.scan(formats: [BarcodeFormat.CODE_128]);
                  setState(() {
                    codes = <String>[code];
                  });
                },
              ),

              RaisedButton(
                child: Text("连续扫描"),
                onPressed: () async {
                  codes = [];
                  var receiver = BarcodeScanner.scanMulti(formats: [BarcodeFormat.CODE_128], maxScan: -1, delay: 100);
                  receiver.listen((result){
                    if (codes.contains(result)) {
                      return;
                    }
                    codes.add(result);
                  }, onDone: () {
                    print("done");
                    setState(() {
                    });
                  });
                },
              ),

              Container(
                padding: EdgeInsets.symmetric(horizontal: 15),
                child: TextField(),
              ),

              RaisedButton(
                child: Text("初始化扫码枪"),
                onPressed: () async {
                  var name = await BarcodeScanner.getInputDeviceName();
                  print(name);
                },
              ),

              SizedBox(height: 16),
            ] + codeWidgets,
          ),
        ),
      ),
    );
  }
}
