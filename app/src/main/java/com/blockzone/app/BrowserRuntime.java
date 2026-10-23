package com.blockzone.app;

import android.content.Context;

/** Compatibility shim kept for the existing home screen. */
public final class BrowserRuntime {
    private BrowserRuntime(){}
    public static NativeAdblockManager get(Context context){
        return NativeAdblockManager.get(context);
    }
}
