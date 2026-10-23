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
import org.mlm.adblock.BlockDecision;
import org.mlm.adblock.CosmeticResources;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class BrowserActivity extends Activity {
    private WebView webView;
    private ProgressBar progress;
    private TextView protectionStatus;
    private NativeAdblockManager adblock;
    private volatile String currentUrl="https://m.youtube.com/";

    private static final String BACKGROUND_FIX=
        "(function(){try{"+
        "Object.defineProperty(document,'hidden',{configurable:true,get:function(){return false;}});"+
        "Object.defineProperty(document,'visibilityState',{configurable:true,get:function(){return 'visible';}});"+
        "document.addEventListener('visibilitychange',function(e){e.stopImmediatePropagation();},true);"+
        "window.__bzPlaying=false;"+
        "document.addEventListener('play',function(e){if(e.target&&e.target.tagName==='VIDEO')window.__bzPlaying=true;},true);"+
        "document.addEventListener('pause',function(e){if(e.target&&e.target.tagName==='VIDEO'&&document.visibilityState==='visible')window.__bzPlaying=false;},true);"+
        "setInterval(function(){var v=document.querySelector('video');if(v&&window.__bzPlaying&&v.paused){v.play().catch(function(){});}},900);"+
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
        protectionStatus.setText("Inicializando motor nativo…");
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

        webView=new WebView(this);
        webView.setBackgroundColor(Color.BLACK);
        root.addView(webView,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);

        configureWebView();

        adblock.whenReady((ok,message)->runOnUiThread(()->{
            protectionStatus.setText(message);
            protectionStatus.setTextColor(ok?Color.rgb(110,231,173):Color.rgb(255,92,119));
            progress.setVisibility(ok?View.VISIBLE:View.GONE);
            if(ok) webView.loadUrl("https://m.youtube.com/");
            else Toast.makeText(BrowserActivity.this,message,Toast.LENGTH_LONG).show();
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
        s.setUserAgentString(s.getUserAgentString().replace("; wv","") + " BlockZone/3.4");

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
                injectFixes(view,url);
            }

            @Override public void onPageFinished(WebView view,String url){
                currentUrl=url;
                injectFixes(view,url);
                CookieManager.getInstance().flush();
            }

            @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request){
                try{
                    String url=request.getUrl().toString();
                    if(request.isForMainFrame()) return super.shouldInterceptRequest(view,request);
                    String accept=request.getRequestHeaders().get("Accept");
                    String source=request.getRequestHeaders().get("Referer");
                    if(source==null || source.isEmpty()) source=currentUrl;
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
                return super.shouldInterceptRequest(view,request);
            }

            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){
                return false;
            }
        });
    }

    private void injectFixes(WebView view,String url){
        view.evaluateJavascript(BACKGROUND_FIX,null);
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
