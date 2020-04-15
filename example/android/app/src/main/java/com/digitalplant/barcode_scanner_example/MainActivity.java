package com.digitalplant.barcode_scanner_example;

import android.view.KeyEvent;

import android.os.Bundle;
import android.content.Intent;

import io.flutter.app.FlutterActivity;
import io.flutter.plugins.GeneratedPluginRegistrant;

import com.digitalplant.common.KeyboardListener;

public class MainActivity extends FlutterActivity {
  private KeyboardListener kbListener;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    kbListener = new KeyboardListener();

    Intent intent = getIntent();
    Bundle bundle = new Bundle();
    bundle.putBinder("keyboard_listener", kbListener);
    intent.putExtra("keyboard_listener_bundle", bundle);

    GeneratedPluginRegistrant.registerWith(this);
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (kbListener != null) {
      if (kbListener.dispatchKeyEvent(event)) {
        return true;
      }
    }
    return super.onKeyDown(keyCode, event);
    // return false;
  }
}
