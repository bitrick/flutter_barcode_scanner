package com.digitalplant.barcode_scanner_example;

import android.os.Binder;
import android.view.KeyEvent;

import java.util.ArrayList;
import java.util.List;

public class KeyboardListener extends Binder {
    private List<Callback> callbacks;
    private boolean running;
    private List<Callback> toAdd;
    private List<Callback> toDel;

    public KeyboardListener() {
        callbacks = new ArrayList<>();
        toAdd     = new ArrayList<>();
        toDel     = new ArrayList<>();
        running   = false;
    }

    public void addCallback(Callback cb) {
        if (running) {
            toAdd.add(cb);
        } else {
            callbacks.add(cb);
        }
    }

    public void removeCallback(Callback cb) {
        if (running) {
            toDel.add(cb);
        } else {
            callbacks.remove(cb);
        }
    }

    private void postProcess() {
        callbacks.addAll(toAdd);
        callbacks.removeAll(toDel);
        toAdd.clear();
        toDel.clear();

        running = false;
    }

    public boolean dispatchKeyEvent(KeyEvent e) {
        running = true;
        for (int i=0; i!=callbacks.size(); ++i) {
            Callback cb = callbacks.get(i);
            if (cb.run(e)) {
                postProcess();
                return true;
            }
        }

        postProcess();
        return false;
    }

    public interface Callback {
        boolean run(KeyEvent e);
    }
}
