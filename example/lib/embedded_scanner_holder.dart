import 'package:flutter/material.dart';
import 'package:barcode_scanner/barcode_scanner.dart';

class EmbeddedScannerHolder extends StatefulWidget {
  final Widget child;

  static _EmbeddedScannerHolderState instance;

  EmbeddedScannerHolder({this.child});

  @override
  State<StatefulWidget> createState() {
    instance = _EmbeddedScannerHolderState();
    return instance;
  }
}

class _EmbeddedScannerHolderState extends State<EmbeddedScannerHolder> with SingleTickerProviderStateMixin {
  bool visible = false;
  bool flashOn = false;
  bool okToScan = true;
  Function(String) onCode;
  EmbeddedScannerController controller;

  AnimationController animtionController;
  Animation<double> tween;

  int maxScan;
  int delay;
  bool autoRestart;
  List<int> formats;

  @override
  initState() {
    super.initState();

    animtionController = AnimationController(duration: Duration(milliseconds: 150), vsync: this);
    animtionController.addStatusListener((v) {
      if (v == AnimationStatus.completed) {
        setState(() {
          visible = true;
          okToScan = true;
          flashOn = false;
        });
      }
    });
    tween = Tween(begin: 0.0, end: 200.0).animate(animtionController);
  }

  show({int maxScan, int delay, bool autoRestart=false, List<int> formats}) {
    this.maxScan = maxScan;
    this.delay = delay;
    this.autoRestart = autoRestart;
    this.formats = formats;

    animtionController.forward();
  }

  hide() {
    setState(() {
      visible = false;
      maxScan = null;
      delay = null;
      autoRestart = null;
      formats = null;
    });

    Future.delayed(Duration(milliseconds: 100)).then((_) {
      animtionController.reverse();
    });
  }

  onReady() {
    setState(() {
      okToScan = true;
    });
  }

  onResult(String code) {
    setState(() {
      okToScan = false;
    });

    if (onCode != null) {
      onCode(code);
    }
  }

  onToggleFlash(bool on) {
    setState(() {
      flashOn = on;
    });
  }

  @override
  Widget build(BuildContext context) {
    var screenSize = MediaQuery.of(context).size;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisAlignment: MainAxisAlignment.start,
      children: <Widget>[
        Stack(
          children: <Widget>[
            Stack(
              children: <Widget>[
                AnimatedBuilder(
                  animation: tween,
                  child: Container(),
                  builder: (context, Widget child) {
                    return Container(
                      height: tween.value,
                    );
                  }
                ),

                visible ?
                Positioned(
                  child: Container(
                    height: 200,
                    child: EmbeddedScanner(
                      onCreated: (controller) async {
                        this.controller = controller;
                        await controller.setScanParams(
                          delay: delay,
                          maxScan: maxScan,
                          autoRestart: autoRestart,
                          formats: formats,
                        );
                        setState(() {
                          controller.onReady = onReady;
                          controller.onCode = onResult;
                          controller.onFinish = hide;
                          controller.onToggleFlash = onToggleFlash;
                        });
                      },
                    )
                  ),
                ) : Container(),

              ],
            ),

            // indicator
            visible && okToScan ?
            Positioned(
              top: 35,
              right: 10,
              child: Container(
                width: 36,
                height: 36,
                decoration: BoxDecoration(
                  color: Color(0xFFD43030),
                  borderRadius: BorderRadius.all(Radius.circular(18)),
                  boxShadow: [BoxShadow(color: Colors.white, blurRadius: 5, spreadRadius: 1)],
                ),
              ),
            ) : Container(),

            // close button
            visible ?
            Positioned(
              top: 30,
              left: 10,
              child: GestureDetector(
                onTap: hide,
                child: Container(
                  width: 46,
                  height: 46,
                  decoration: BoxDecoration(
                    color: Color(0xFF383838),
                    borderRadius: BorderRadius.all(Radius.circular(23)),
                  ),
                  child: Icon(Icons.close, color: Colors.white, size: 25),
                ),
              ),
            ) : Container(),

            // flash button
            visible ?
            Positioned(
              left: 10,
              bottom: 10,
              child: GestureDetector(
                onTap: controller?.toggleFlash,
                child: Container(
                  width: 46,
                  height: 46,
                  decoration: BoxDecoration(
                    color: Color(0xFF383838),
                    borderRadius: BorderRadius.all(Radius.circular(23)),
                  ),
                  child: Icon(flashOn ? Icons.flash_on : Icons.flash_off, color: Colors.white, size: 25),
                ),
              ),
            ) : Container(),

          ],
        ),

        Expanded(
          flex: 1,
          child: Stack(
            children: <Widget>[
              visible ?
              WillPopScope(
                onWillPop: () async {
                  hide();
                  return false;
                },
                child: widget.child,
              ) : widget.child,

              visible ?
              Positioned(
                child: GestureDetector(
                  onTap: controller?.readyToScan,
                  child: Container(
                    height: screenSize.height-tween.value,
                    color: Color(0x3F000000),
                  ),
                )
              ) : Container(),

            ],
          ),
        ),
      ],
    );
  }
}
