package com.blockzone.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.json.JSONObject;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.WebExtension;

public final class BrowserRuntime {
    private static GeckoRuntime runtime;
    private static final String EXTENSION_URI="resource://android/assets/blockzone_adblock/";
    private static final String EXTENSION_ID="blockzone-adblock@local";
    private BrowserRuntime(){}
    public static synchronized GeckoRuntime get(Context context){
        if(runtime==null){
            Context app=context.getApplicationContext(); runtime=GeckoRuntime.create(app);
            runtime.getWebExtensionController().ensureBuiltIn(EXTENSION_URI,EXTENSION_ID).accept(ext->{
                ext.setMessageDelegate(new WebExtension.MessageDelegate(){
                    @Override public GeckoResult<Object> onMessage(String nativeApp,Object message,WebExtension.MessageSender sender){
                        if(message instanceof JSONObject){ JSONObject j=(JSONObject)message; if("blocked".equals(j.optString("type"))){ SharedPreferences p=app.getSharedPreferences("blockzone",Context.MODE_PRIVATE); p.edit().putInt("browser_blocked",p.getInt("browser_blocked",0)+Math.max(1,j.optInt("count",1))).apply(); } }
                        return null;
                    }
                },"blockzone");
                Log.i("BlockZone","Built-in adblock ready");
            },err->Log.e("BlockZone","Adblock install failed",err));
        }
        return runtime;
    }
}
