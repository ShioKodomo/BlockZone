package com.blockzone.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import org.mlm.adblock.AdblockEngine;
import org.mlm.adblock.BlockDecision;
import org.mlm.adblock.CosmeticResources;
import org.mlm.adblock.RequestTypeMapper;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class NativeAdblockManager {
    private static NativeAdblockManager instance;
    private final Context app;
    private final AdblockEngine engine;
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final SharedPreferences prefs;
    private volatile boolean ready=false;
    private volatile String status="Inicializando motor nativo…";

    private static final String[] FILTER_URLS={
        "https://easylist.to/easylist/easylist.txt",
        "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/filters.txt",
        "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/quick-fixes.txt",
        "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/filters-2026.txt",
        "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/privacy.txt"
    };

    public interface ReadyCallback { void onResult(boolean ok,String message); }

    private NativeAdblockManager(Context context){
        app=context.getApplicationContext();
        prefs=app.getSharedPreferences("blockzone",Context.MODE_PRIVATE);
        engine=AdblockEngine.Companion.create();
        io.execute(this::initialize);
    }

    public static synchronized NativeAdblockManager get(Context context){
        if(instance==null) instance=new NativeAdblockManager(context);
        return instance;
    }

    private void initialize(){
        try{
            String rules=readSavedOrBundledRules();
            if(rules==null || rules.trim().isEmpty()) throw new IOException("Lista de filtros vazia");
            if(!engine.loadFilterList(rules)) throw new IOException("O motor recusou a lista de filtros");
            loadResources();
            ready=true;
            status="Proteção ativa • Brave adblock-rust";
            maybeRefreshRules();
        }catch(Throwable e){
            ready=false;
            status="Proteção indisponível: "+e.getMessage();
            Log.e("BlockZone","Native adblock init failed",e);
        }
    }

    private String readSavedOrBundledRules() throws IOException {
        File saved=new File(app.getFilesDir(),"blockzone_filters.txt");
        if(saved.exists() && saved.length()>1000) return readFile(saved);
        try(InputStream in=app.getAssets().open("adblock/blockzone_filters.txt")){
            return readStream(in);
        }
    }

    private void loadResources(){
        try(InputStream in=app.getAssets().open("adblock/resources.json")){
            engine.loadResources(readStream(in));
        }catch(Throwable e){
            Log.w("BlockZone","Scriptlet resources unavailable",e);
        }
    }

    private void maybeRefreshRules(){
        long last=prefs.getLong("adblock_filters_updated",0L);
        if(System.currentTimeMillis()-last < 12L*60L*60L*1000L) return;
        io.execute(()->{
            try{
                StringBuilder all=new StringBuilder(2_000_000);
                for(String u:FILTER_URLS){
                    String text=download(u);
                    if(text!=null && text.length()>1000) all.append(text).append('\n');
                }
                if(all.length()<10000) return;
                File out=new File(app.getFilesDir(),"blockzone_filters.txt");
                try(FileOutputStream fos=new FileOutputStream(out)){
                    fos.write(all.toString().getBytes(StandardCharsets.UTF_8));
                }
                if(engine.loadFilterList(all.toString())){
                    loadResources();
                    prefs.edit().putLong("adblock_filters_updated",System.currentTimeMillis()).apply();
                    status="Proteção ativa • filtros atualizados";
                }
            }catch(Throwable e){
                Log.w("BlockZone","Filter refresh failed; bundled rules remain active",e);
            }
        });
    }

    private String download(String address) throws IOException {
        HttpURLConnection c=(HttpURLConnection)new URL(address).openConnection();
        c.setConnectTimeout(8000); c.setReadTimeout(12000);
        c.setRequestProperty("User-Agent","BlockZone/3.4 Android");
        try{
            if(c.getResponseCode()!=200) return null;
            try(InputStream in=c.getInputStream()){ return readStream(in); }
        }finally{ c.disconnect(); }
    }

    public boolean isReady(){ return ready; }
    public String getStatus(){ return status; }

    public void whenReady(ReadyCallback callback){
        io.execute(()->{
            for(int i=0;i<100 && !ready && status.startsWith("Inicializando");i++){
                try{ Thread.sleep(50); }catch(InterruptedException ignored){}
            }
            callback.onResult(ready,status);
        });
    }

    public BlockDecision check(String url,String source,String accept){
        if(!ready || url==null) return null;
        try{
            String type=RequestTypeMapper.INSTANCE.from(Uri.parse(url),accept);
            return engine.checkNetworkRequest(url,source==null?url:source,type);
        }catch(Throwable e){ return null; }
    }

    public CosmeticResources cosmetics(String pageUrl){
        if(!ready || pageUrl==null) return null;
        try{ return engine.cosmeticResources(pageUrl); }catch(Throwable e){ return null; }
    }

    public void countBlocked(){
        int n=prefs.getInt("browser_blocked",0);
        prefs.edit().putInt("browser_blocked",n+1).apply();
    }

    private static String readFile(File f) throws IOException {
        try(InputStream in=new FileInputStream(f)){ return readStream(in); }
    }
    private static String readStream(InputStream in) throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        byte[] buf=new byte[16384]; int n;
        while((n=in.read(buf))!=-1) out.write(buf,0,n);
        return out.toString(StandardCharsets.UTF_8.name());
    }
}
