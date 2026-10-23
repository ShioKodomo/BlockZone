package com.blockzone.app;

import android.content.Context;
import android.util.Log;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;

public final class BrowserRuntime {
    private static GeckoRuntime runtime;
    private static final String EXTENSION_URI = "resource://android/assets/blockzone_adblock/";
    private static final String EXTENSION_ID = "blockzone-adblock@local";

    private BrowserRuntime() {}

    public static synchronized GeckoRuntime get(Context context) {
        if (runtime == null) {
            GeckoRuntimeSettings settings = new GeckoRuntimeSettings.Builder()
                    .javaScriptEnabled(true)
                    .remoteDebuggingEnabled(false)
                    .build();
            runtime = GeckoRuntime.create(context.getApplicationContext(), settings);
            runtime.getWebExtensionController()
                    .ensureBuiltIn(EXTENSION_URI, EXTENSION_ID)
                    .accept(
                            ext -> Log.i("BlockZone", "Adblock extension ready: " + ext.id),
                            err -> Log.e("BlockZone", "Adblock extension failed", err)
                    );
        }
        return runtime;
    }
}
