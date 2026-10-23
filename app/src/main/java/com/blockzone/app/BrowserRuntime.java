package com.blockzone.app;

import android.content.Context;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.WebExtension;
import org.mozilla.geckoview.WebExtensionController;

public final class BrowserRuntime {
    private static GeckoRuntime runtime;
    private static final String UBO_URI="resource://android/assets/ublock/";
    private static final String UBO_ID="uBlock0@raymondhill.net";
    private static final String BGPLAY_URI="resource://android/assets/video_bg_play/";
    private static final String BGPLAY_ID="video-bg-play@timdream.org";

    private static boolean uboReady=false;
    private static boolean bgReady=false;
    private static String initError=null;
    private static final List<ReadyListener> listeners=new ArrayList<>();

    public interface ReadyListener {
        void onReady();
        void onError(String message);
    }

    private BrowserRuntime(){}

    public static synchronized GeckoRuntime get(Context context){
        if(runtime==null){
            Context app=context.getApplicationContext();
            runtime=GeckoRuntime.create(app);
            WebExtensionController controller=runtime.getWebExtensionController();
            controller.enableExtensionProcessSpawning();

            controller.setAddonManagerDelegate(new WebExtensionController.AddonManagerDelegate(){
                @Override public void onReady(WebExtension extension){
                    if(extension==null) return;
                    if(UBO_ID.equals(extension.id)){
                        Log.i("BlockZone","uBlock Origin startup complete");
                        markUboReady();
                    }else if(BGPLAY_ID.equals(extension.id)){
                        bgReady=true;
                        Log.i("BlockZone","Background playback extension startup complete");
                    }
                }

                @Override public void onDisabled(WebExtension extension){
                    if(extension!=null && UBO_ID.equals(extension.id)){
                        uboReady=false;
                        setError("uBlock Origin foi desativado pelo GeckoView.");
                    }
                }

                @Override public void onInstallationFailed(WebExtension extension, WebExtension.InstallException exception){
                    String id=extension!=null?extension.id:"extensão";
                    setError("Falha ao instalar "+id+": "+exception.getMessage());
                }
            });

            controller.ensureBuiltIn(UBO_URI,UBO_ID).accept(ext->{
                if(ext.metaData!=null && !ext.metaData.enabled){
                    controller.enable(ext,WebExtensionController.EnableSource.APP).accept(
                        enabled->Log.i("BlockZone","uBlock Origin explicitly enabled"),
                        err->setError("Não foi possível habilitar o uBlock Origin: "+err.getMessage())
                    );
                }else{
                    Log.i("BlockZone","uBlock Origin installed and enabled; waiting for startup");
                }
            },err->setError("Falha ao instalar o uBlock Origin: "+err.getMessage()));

            controller.ensureBuiltIn(BGPLAY_URI,BGPLAY_ID).accept(ext->{
                if(ext.metaData!=null && !ext.metaData.enabled){
                    controller.enable(ext,WebExtensionController.EnableSource.APP).accept(
                        enabled->Log.i("BlockZone","Background playback extension explicitly enabled"),
                        err->Log.e("BlockZone","Could not enable background playback extension",err)
                    );
                }
            },err->Log.e("BlockZone","Background playback extension install failed",err));
        }
        return runtime;
    }

    public static void whenAdblockReady(Context context, ReadyListener listener){
        get(context);
        boolean ready;
        String error;
        synchronized(BrowserRuntime.class){
            ready=uboReady;
            error=initError;
            if(!ready && error==null) listeners.add(listener);
        }
        if(ready) listener.onReady();
        else if(error!=null) listener.onError(error);
    }

    public static synchronized boolean isUboReady(){ return uboReady; }
    public static synchronized boolean isBackgroundReady(){ return bgReady; }
    public static synchronized String getInitError(){ return initError; }

    private static void markUboReady(){
        List<ReadyListener> copy;
        synchronized(BrowserRuntime.class){
            uboReady=true;
            initError=null;
            copy=new ArrayList<>(listeners);
            listeners.clear();
        }
        for(ReadyListener listener:copy){
            try{ listener.onReady(); }catch(Exception ignored){}
        }
    }

    private static void setError(String message){
        Log.e("BlockZone",message);
        List<ReadyListener> copy;
        synchronized(BrowserRuntime.class){
            initError=message;
            copy=new ArrayList<>(listeners);
            listeners.clear();
        }
        for(ReadyListener listener:copy){
            try{ listener.onError(message); }catch(Exception ignored){}
        }
    }
}
