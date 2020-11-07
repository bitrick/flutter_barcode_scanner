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
  AnimationController _animtionController;
  Animation<double> _tween;

  int _maxScan;
  int _delay;
  bool _autoRestart;
  List<int> _formats;

  bool _visible = false;
  bool _flashOn = false;
  bool _okToScan = true;
  EmbeddedScannerController _controller;


  Function(String) onCode;
  VoidCallback onFinish;

  _EmbeddedViewState _viewState;

  @override
  initState() {
    super.initState();

    _animtionController = AnimationController(duration: Duration(milliseconds: 150), vsync: this);
    _animtionController.addStatusListener((status) {
      if (status == AnimationStatus.completed) {
        setState(() {
          _visible = true;
          _flashOn = false;
          _okToScan = true;
        });
      }
    });
    _tween = Tween(begin: 0.0, end: 200.0).animate(_animtionController);
  }

  _onReady() {
    setState(() {
      _okToScan = true;
    });
  }

  _onToggleFlash(bool on) {
    setState(() {
      _flashOn = on;
    });
  }

  _onResult(String code) {
    setState(() {
      _okToScan = false;
    });

    if (onCode != null) {
      onCode(code);
    }
  }

  show({int maxScan, int delay, bool autoRestart=false, List<int> formats}) async {
    this._maxScan = maxScan;
    this._delay = delay;
    this._autoRestart = autoRestart;
    this._formats = formats;

    if (_viewState == null) {
      _animtionController.forward();
      await _EmbeddedView.popup(context, onTap: () {
        _controller?.readyToScan();
      });

      // popup 紧跟着 finish
      _finish();
    } else {
      // 只是设置参数
      _setScanParams();
    }
  }

  hide() {
    if (_viewState != null) {
      // dismiss 会结束 show 中的阻塞，然后在 show 中调用 finish
      _viewState.dismiss();
    }
  }

  _finish() {
    if (onFinish != null) {
      onFinish();
    }

    Future.delayed(Duration(milliseconds: 100)).then((_) {
      _animtionController.reverse();
    });


    onCode = null;
    onFinish = null;
    _viewState = null;

    setState(() {
      _visible = false;
    });
  }

  _setScanParams() {
    _controller.setScanParams(
      delay: _delay,
      maxScan: _maxScan,
      autoRestart: _autoRestart,
      formats: _formats,
    );
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
            AnimatedBuilder(
              animation: _tween,
              child: Container(),
              builder: (context, Widget child) {
                return Container(
                  height: _tween.value,
                );
              }
            ),

            // camera view
            _visible ?
            Positioned(
              child: Container(
                width: screenSize.width,
                height: 200,
                child: EmbeddedScanner(
                  onCreated: (controller) async {
                    this._controller = controller;
                    setState(() {
                      controller.onReady = _onReady;
                      controller.onCode = _onResult;
                      controller.onFinish = hide;
                      controller.onToggleFlash = _onToggleFlash;
                    });

                    _setScanParams();
                  },
                )
              ),
            ) : Container(),

            // indicator
            _visible && _okToScan ?
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
            _visible ?
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
            _visible ?
            Positioned(
              left: 10,
              bottom: 10,
              child: GestureDetector(
                onTap: _controller?.toggleFlash,
                child: Container(
                  width: 46,
                  height: 46,
                  decoration: BoxDecoration(
                    color: Color(0xFF383838),
                    borderRadius: BorderRadius.all(Radius.circular(23)),
                  ),
                  child: Icon(_flashOn ? Icons.flash_on : Icons.flash_off, color: Colors.white, size: 25),
                ),
              ),
            ) : Container(),

          ],
        ),

        Expanded(
          flex: 1,
          child: widget.child,
        ),
      ],
    );
  }
}

// ----------------------------------------------------------------------------------------

class _EmbeddedView extends StatefulWidget {
  final Function onTap;
  final Function onCreate;

  _EmbeddedView({
    this.onTap,
    this.onCreate,
  });

  @override
  State<StatefulWidget> createState() {
    return _EmbeddedViewState();
  }

  static popup(BuildContext context, {Function onTap, Function onCreate}) {
    return showGeneralDialog(
      context: context,
      barrierColor: Color(0x00FFFFFF),
      barrierDismissible: false,
      transitionDuration: Duration(milliseconds: 150),
      transitionBuilder: (a,b,c, child) {
        return b.isCompleted ? child : Container();
      },
      pageBuilder: (context, Animation<double> animation1, Animation<double> animation2) {
        return _EmbeddedView(onTap: onTap, onCreate: onCreate);
      }
    );
  }
}

class _EmbeddedViewState extends State<_EmbeddedView> {
  @override
  void initState() {
    super.initState();

    if (widget.onCreate != null) {
      widget.onCreate(this);
    }
  }

  void dismiss() {
    Navigator.of(context).pop();
  }

  @override
  void dispose() {
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    var screenSize = MediaQuery.of(context).size;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisAlignment: MainAxisAlignment.start,
      children: <Widget>[
        Expanded(
          flex: 1,
          child: GestureDetector(
            onTap: widget.onTap,
            child: Container(
              height: screenSize.height-200,
              color: Color(0x3F000000),
            ),
          ),
        ),
      ],
    );
  }
}
