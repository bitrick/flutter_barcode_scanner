package com.digitalplant.barcode_scanner.util;

import java.util.Timer;
import java.util.TimerTask;
import android.app.Activity;

public class JobManager {
    private Timer timer = new Timer();
    private Activity activity;

    public JobManager(Activity activity) {
        this.activity = activity;
    }

    public synchronized void postDelayed(final Task task, long delay) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        task.run();
                    }
                });
            }
        }, delay);
    }

    public synchronized void runOnUiThread(final Task task) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                task.run();
            }
        });
    }

    public interface Task {
        void run();
    }
}
