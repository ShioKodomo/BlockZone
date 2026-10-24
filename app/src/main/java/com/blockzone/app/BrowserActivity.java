package com.blockzone.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;

import androidx.webkit.ServiceWorkerClientCompat;
import androidx.webkit.ServiceWorkerControllerCompat;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import org.mlm.adblock.BlockDecision;
import org.mlm.adblock.CosmeticResources;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class BrowserActivity extends Activity {
    private WebView webView;
    private ProgressBar progress;
    private TextView protectionStatus;
    private NativeAdblockManager adblock;
    private volatile String currentUrl="https://m.youtube.com/";
    private boolean documentStartSupported=false;

    private static final String BACKGROUND_EARLY=
        "(function(){try{"+
        "if(window.__bzBgInstalled)return;window.__bzBgInstalled=true;"+
        "var pd=Object.getOwnPropertyDescriptor(Document.prototype,'visibilityState');"+
        "var ph=Object.getOwnPropertyDescriptor(Document.prototype,'hidden');"+
        "var nativeVis=function(){try{return pd&&pd.get?pd.get.call(document):'visible';}catch(e){return 'visible';}};"+
        "var wasPlaying=false;"+
        "var realPause=HTMLMediaElement.prototype.pause;"+
        "HTMLMediaElement.prototype.pause=function(){try{if(nativeVis()!=='visible'&&wasPlaying&&!this.ended)return;}catch(e){}return realPause.apply(this,arguments);};"+
        "try{Object.defineProperty(document,'hidden',{configurable:true,get:function(){return false;}});}catch(e){}"+
        "try{Object.defineProperty(document,'visibilityState',{configurable:true,get:function(){return 'visible';}});}catch(e){}"+
        "try{document.hasFocus=function(){return true;};}catch(e){}"+
        "var realAdd=EventTarget.prototype.addEventListener;"+
        "EventTarget.prototype.addEventListener=function(t,l,o){if((this===document||this===window)&&(t==='visibilitychange'||t==='webkitvisibilitychange'||t==='freeze'||t==='pagehide'))return;return realAdd.call(this,t,l,o);};"+
        "realAdd.call(document,'play',function(e){if(e.target&&e.target.tagName==='VIDEO')wasPlaying=true;},true);"+
        "realAdd.call(document,'pause',function(e){if(e.target&&e.target.tagName==='VIDEO'&&nativeVis()==='visible')wasPlaying=false;},true);"+
        "setInterval(function(){try{var v=document.querySelector('video');if(v&&wasPlaying&&nativeVis()!=='visible'&&v.paused&&!v.ended)v.play().catch(function(){});}catch(e){}},500);"+
        "}catch(e){}})();";

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        adblock=NativeAdblockManager.get(this);

        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(8,10,16));

        LinearLayout top=new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(8),dp(5),dp(8),dp(5));
        top.setBackgroundColor(Color.rgb(13,16,23));

        TextView back=button("‹",28);
        top.addView(back,new LinearLayout.LayoutParams(dp(48),dp(48)));
        back.setOnClickListener(v->onBackPressed());

        LinearLayout titleBox=new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setGravity(Gravity.CENTER_VERTICAL);

        TextView title=new TextView(this);
        title.setText("YouTube protegido");
        title.setTextColor(Color.WHITE);
        title.setTextSize(17);
        titleBox.addView(title,new LinearLayout.LayoutParams(-1,-2));

        protectionStatus=new TextView(this);
        protectionStatus.setText("Inicializando proteção…");
        protectionStatus.setTextColor(Color.rgb(255,191,105));
        protectionStatus.setTextSize(11);
        titleBox.addView(protectionStatus,new LinearLayout.LayoutParams(-1,-2));
        top.addView(titleBox,new LinearLayout.LayoutParams(0,dp(52),1));

        TextView shield=button("✓",20);
        shield.setTextColor(Color.rgb(110,231,173));
        top.addView(shield,new LinearLayout.LayoutParams(dp(48),dp(48)));
        root.addView(top,new LinearLayout.LayoutParams(-1,-2));

        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true);
        root.addView(progress,new LinearLayout.LayoutParams(-1,dp(2)));

        configureServiceWorker();

        webView=new WebView(this);
        webView.setBackgroundColor(Color.BLACK);
        root.addView(webView,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);

        configureWebView();
        installEarlyScripts();

        adblock.whenReady((ok,message)->runOnUiThread(()->{
            if(ok){
                protectionStatus.setText(documentStartSupported
                    ? "Proteção ativa • filtro antecipado do YouTube"
                    : "Proteção ativa • WebView sem filtro antecipado");
                protectionStatus.setTextColor(documentStartSupported?Color.rgb(110,231,173):Color.rgb(255,191,105));
                progress.setVisibility(View.VISIBLE);
                webView.loadUrl("https://m.youtube.com/");
            }else{
                protectionStatus.setText(message);
                protectionStatus.setTextColor(Color.rgb(255,92,119));
                progress.setVisibility(View.GONE);
                Toast.makeText(BrowserActivity.this,message,Toast.LENGTH_LONG).show();
            }
        }));

        Intent playback=new Intent(this,MediaPlaybackService.class);
        if(Build.VERSION.SDK_INT>=26) startForegroundService(playback); else startService(playback);
    }

    private void configureWebView(){
        WebSettings s=webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadsImagesAutomatically(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        String ua=s.getUserAgentString().replace("; wv","").replace(" Version/4.0","");
        s.setUserAgentString(ua);

        CookieManager cookies=CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView,true);

        webView.setWebChromeClient(new WebChromeClient(){
            @Override public void onProgressChanged(WebView view,int newProgress){
                progress.setIndeterminate(false);
                progress.setProgress(newProgress);
                progress.setVisibility(newProgress>=100?View.GONE:View.VISIBLE);
            }
        });

        webView.setWebViewClient(new WebViewClient(){
            @Override public void onPageStarted(WebView view,String url,android.graphics.Bitmap favicon){
                currentUrl=url;
                if(!documentStartSupported) view.evaluateJavascript(BACKGROUND_EARLY,null);
            }

            @Override public void onPageFinished(WebView view,String url){
                currentUrl=url;
                injectCosmetics(view,url);
                CookieManager.getInstance().flush();
            }

            @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request){
                if(request.isForMainFrame()) return super.shouldInterceptRequest(view,request);
                WebResourceResponse r=intercept(request,currentUrl);
                return r!=null?r:super.shouldInterceptRequest(view,request);
            }

            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){
                return false;
            }
        });
    }

    private void installEarlyScripts(){
        if(!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) return;
        try{
            String ytScript=readAsset("adblock/yt_mobile_player_request.js");
            String all=ytScript+"\ntry{ytMobilePlayerRequest('lactmilli');}catch(e){}\n"+BACKGROUND_EARLY;
            Set<String> origins=new HashSet<>();
            origins.add("https://m.youtube.com");
            origins.add("https://www.youtube.com");
            WebViewCompat.addDocumentStartJavaScript(webView,all,origins);
            documentStartSupported=true;
        }catch(Throwable e){
            documentStartSupported=false;
        }
    }

    private void configureServiceWorker(){
        if(!WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_BASIC_USAGE) ||
           !WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST)) return;
        try{
            ServiceWorkerControllerCompat controller=ServiceWorkerControllerCompat.getInstance();
            controller.setServiceWorkerClient(new ServiceWorkerClientCompat(){
                @Override public WebResourceResponse shouldInterceptRequest(WebResourceRequest request){
                    return intercept(request,"https://m.youtube.com/");
                }
            });
        }catch(Throwable ignored){}
    }

    private WebResourceResponse intercept(WebResourceRequest request,String fallbackSource){
        try{
            String url=request.getUrl().toString();
            String accept=request.getRequestHeaders().get("Accept");
            String source=request.getRequestHeaders().get("Referer");
            if(source==null || source.isEmpty()) source=fallbackSource;
            BlockDecision d=adblock.check(url,source,accept);
            if(d!=null && d.getMatched() && !d.getException()){
                adblock.countBlocked();
                String redirect=d.getRedirect();
                if(redirect!=null && redirect.startsWith("data:")){
                    int comma=redirect.indexOf(',');
                    if(comma>0){
                        String data=redirect.substring(comma+1);
                        return new WebResourceResponse("text/plain","utf-8",new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
                    }
                }
                return new WebResourceResponse("text/plain","utf-8",new ByteArrayInputStream(new byte[0]));
            }
        }catch(Throwable ignored){}
        return null;
    }

    private void injectCosmetics(WebView view,String url){
        CosmeticResources c=adblock.cosmetics(url);
        if(c==null) return;
        String css=c.css();
        String js=c.getJs();
        if(css!=null && !css.isEmpty()){
            String safe=css.replace("\\","\\\\").replace("`","\\`").replace("${","\\${");
            String inject="(function(){var s=document.getElementById('bz-css');if(!s){s=document.createElement('style');s.id='bz-css';document.documentElement.appendChild(s);}s.textContent=`"+safe+"`;})();";
            view.evaluateJavascript(inject,null);
        }
        if(js!=null && !js.isEmpty()) view.evaluateJavascript(js,null);
    }

    private String readAsset(String path) throws Exception {
        try(InputStream in=getAssets().open(path)){
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            byte[] buf=new byte[8192]; int n;
            while((n=in.read(buf))!=-1) out.write(buf,0,n);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    private TextView button(String s,int sp){
        TextView t=new TextView(this);
        t.setText(s); t.setTextColor(Color.WHITE); t.setTextSize(sp); t.setGravity(Gravity.CENTER);
        return t;
    }

    private int dp(int v){ return (int)(v*getResources().getDisplayMetrics().density+.5f); }

    @Override public void onBackPressed(){
        if(webView!=null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override protected void onPause(){
        CookieManager.getInstance().flush();
        super.onPause();
    }

    @Override protected void onDestroy(){
        stopService(new Intent(this,MediaPlaybackService.class));
        if(webView!=null){
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
        }
        super.onDestroy();
    }
}
