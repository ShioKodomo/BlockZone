package com.blockzone.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.json.JSONObject;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.WebExtension;

public final class BrowserRuntime {
    private static GeckoRuntime runtime;
    private static final String UBO_URI="resource://android/assets/ublock/";
    private static final String UBO_ID="uBlock0@raymondhill.net";
    private static final String HELPER_URI="resource://android/assets/blockzone_adblock/";
    private static final String HELPER_ID="blockzone-adblock@local";

    private BrowserRuntime(){}

    public static synchronized GeckoRuntime get(Context context){
        if(runtime==null){
            Context app=context.getApplicationContext();
            runtime=GeckoRuntime.create(app);

            runtime.getWebExtensionController().ensureBuiltIn(UBO_URI,UBO_ID).accept(
                ext->Log.i("BlockZone","uBlock Origin ready"),
                err->Log.e("BlockZone","uBlock Origin install failed",err)
            );

            runtime.getWebExtensionController().ensureBuiltIn(HELPER_URI,HELPER_ID).accept(ext->{
                ext.setMessageDelegate(new WebExtension.MessageDelegate(){
                    @Override public org.mozilla.geckoview.GeckoResult<Object> onMessage(
                        String nativeApp,
                        Object message,
                        WebExtension.MessageSender sender
                    ){
                        if(message instanceof JSONObject){
                            JSONObject j=(JSONObject)message;
                            if("blocked".equals(j.optString("type"))){
                                SharedPreferences p=app.getSharedPreferences("blockzone",Context.MODE_PRIVATE);
                                p.edit().putInt(
                                    "browser_blocked",
                                    p.getInt("browser_blocked",0)+Math.max(1,j.optInt("count",1))
                                ).apply();
                            }
                        }
                        return null;
                    }
                },"blockzone");
                Log.i("BlockZone","Media helper ready");
            },err->Log.e("BlockZone","Media helper install failed",err));
        }
        return runtime;
    }
}
