package com.blockzone.app;

import android.content.Context;
import android.util.Log;
import org.mozilla.geckoview.GeckoRuntime;

public final class BrowserRuntime {
    private static GeckoRuntime runtime;
    private static final String UBO_URI="resource://android/assets/ublock/";
    private static final String UBO_ID="uBlock0@raymondhill.net";
    private static final String BGPLAY_URI="resource://android/assets/video_bg_play/";
    private static final String BGPLAY_ID="video-bg-play@timdream.org";

    private BrowserRuntime(){}

    public static synchronized GeckoRuntime get(Context context){
        if(runtime==null){
            Context app=context.getApplicationContext();
            runtime=GeckoRuntime.create(app);

            runtime.getWebExtensionController().ensureBuiltIn(UBO_URI,UBO_ID).accept(
                ext->Log.i("BlockZone","uBlock Origin ready"),
                err->Log.e("BlockZone","uBlock Origin install failed",err)
            );

            runtime.getWebExtensionController().ensureBuiltIn(BGPLAY_URI,BGPLAY_ID).accept(
                ext->Log.i("BlockZone","Video Background Play Fix ready"),
                err->Log.e("BlockZone","Background playback extension install failed",err)
            );
        }
        return runtime;
    }
}
