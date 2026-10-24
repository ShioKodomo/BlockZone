package com.gothtime.optimizer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.io.File;

public class MaintenanceReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            deleteOldCache(context.getCacheDir(), System.currentTimeMillis() - (3L * 24L * 60L * 60L * 1000L));
            context.getSharedPreferences("gothtime_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putLong("last_maintenance", System.currentTimeMillis())
                    .apply();
        } catch (Exception ignored) {
        }
    }

    private void deleteOldCache(File dir, long cutoff) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                deleteOldCache(f, cutoff);
                File[] children = f.listFiles();
                if (children != null && children.length == 0) f.delete();
            } else if (f.lastModified() > 0 && f.lastModified() < cutoff) {
                f.delete();
            }
        }
    }
}
