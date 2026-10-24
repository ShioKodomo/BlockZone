package com.blockzone.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import java.io.File;

public class MaintenanceReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!context.getSharedPreferences("gothtime", Context.MODE_PRIVATE).getBoolean("auto", false)) return;
        deleteChildren(context.getCacheDir());
        deleteChildren(context.getExternalCacheDir());
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su","-c","pm trim-caches 999G; am kill-all"});
            p.waitFor();
        } catch (Throwable ignored) { }
    }

    private static void deleteChildren(File f) {
        if (f == null || !f.exists()) return;
        File[] files = f.listFiles();
        if (files == null) return;
        for (File x : files) deleteRec(x);
    }

    private static void deleteRec(File f) {
        if (f == null) return;
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null) for (File x : files) deleteRec(x);
        }
        try { f.delete(); } catch (Throwable ignored) { }
    }
}
