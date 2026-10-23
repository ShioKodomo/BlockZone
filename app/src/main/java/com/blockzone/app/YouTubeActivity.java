package com.blockzone.app;

import android.app.Activity;
import android.graphics.Color;
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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.io.ByteArrayInputStream;
import java.util.Locale;

public class YouTubeActivity extends Activity {
    private WebView web;
    private ProgressBar progress;

    private static final String[] BLOCKED = new String[]{
            "doubleclick.net", "googleadservices.com", "googlesyndication.com",
            "googletagservices.com", "adservice.google.com", "pagead2.googlesyndication.com",
            "youtube.com/api/stats/ads", "youtube.com/pagead/", "youtube.com/ptracking",
            "youtube.com/get_midroll_info", "youtube.com/youtubei/v1/log_event"
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();
        configureWeb();
        web.loadUrl("https://m.youtube.com/");
    }

    private int dp(int v){ return (int)(v*getResources().getDisplayMetrics().density+.5f); }

    private void buildUi(){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(10,12,17));

        LinearLayout bar=new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(10),dp(8),dp(10),dp(8));
        bar.setBackgroundColor(Color.rgb(18,20,27));

        Button back=new Button(this);
        back.setText("‹"); back.setTextSize(25); back.setTextColor(Color.WHITE); back.setAllCaps(false);
        back.setBackgroundColor(Color.TRANSPARENT);
        bar.addView(back,new LinearLayout.LayoutParams(dp(52),dp(48)));
        back.setOnClickListener(v->{ if(web.canGoBack()) web.goBack(); else finish(); });

        TextView title=new TextView(this);
        title.setText("YouTube protegido"); title.setTextColor(Color.WHITE); title.setTextSize(18); title.setGravity(Gravity.CENTER_VERTICAL);
        bar.addView(title,new LinearLayout.LayoutParams(0,dp(48),1));

        Button reload=new Button(this);
        reload.setText("↻"); reload.setTextSize(22); reload.setTextColor(Color.WHITE); reload.setAllCaps(false);
        reload.setBackgroundColor(Color.TRANSPARENT);
        bar.addView(reload,new LinearLayout.LayoutParams(dp(52),dp(48)));
        reload.setOnClickListener(v->web.reload());

        root.addView(bar,new LinearLayout.LayoutParams(-1,-2));
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100); root.addView(progress,new LinearLayout.LayoutParams(-1,dp(3)));

        web=new WebView(this); root.addView(web,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
    }

    private void configureWeb(){
        WebSettings s=web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web,true);

        web.setWebChromeClient(new WebChromeClient(){
            @Override public void onProgressChanged(WebView view,int newProgress){
                progress.setProgress(newProgress);
                progress.setVisibility(newProgress>=100?View.GONE:View.VISIBLE);
            }
        });

        web.setWebViewClient(new WebViewClient(){
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request){
                String u=request.getUrl().toString().toLowerCase(Locale.ROOT);
                for(String x:BLOCKED){ if(u.contains(x)) return empty(); }
                return super.shouldInterceptRequest(view,request);
            }
            @Override public void onPageFinished(WebView view,String url){ super.onPageFinished(view,url); injectBlocker(); }
        });
    }

    private WebResourceResponse empty(){
        return new WebResourceResponse("text/plain","utf-8",new ByteArrayInputStream(new byte[0]));
    }

    private void injectBlocker(){
        String js="javascript:(function(){"+
                "if(window.__blockZoneInstalled)return;window.__blockZoneInstalled=true;"+
                "var css=document.createElement('style');css.innerHTML='"+
                "ytd-display-ad-renderer,ytd-promoted-sparkles-web-renderer,ytd-promoted-video-renderer,"+
                "ytd-ad-slot-renderer,ytm-promoted-sparkles-web-renderer,.ytp-ad-overlay-container,"+
                ".ytp-ad-player-overlay,.video-ads,.ytp-ad-text-overlay{display:none!important}';document.head.appendChild(css);"+
                "setInterval(function(){try{"+
                "var selectors=['.ytp-ad-skip-button','.ytp-ad-skip-button-modern','.ytp-skip-ad-button','.ytp-ad-skip-button-slot button'];"+
                "selectors.forEach(function(s){document.querySelectorAll(s).forEach(function(b){b.click();});});"+
                "var p=document.querySelector('.html5-video-player.ad-showing');"+
                "if(p){var v=p.querySelector('video');if(v&&isFinite(v.duration)&&v.duration>0){v.currentTime=v.duration;}}"+
                "document.querySelectorAll('ytm-promoted-sparkles-web-renderer,ytd-promoted-sparkles-web-renderer,ytd-display-ad-renderer,ytd-ad-slot-renderer').forEach(function(e){e.remove();});"+
                "}catch(e){}},500);"+
                "})()";
        web.evaluateJavascript(js,null);
    }

    @Override public void onBackPressed(){ if(web!=null&&web.canGoBack())web.goBack(); else super.onBackPressed(); }
    @Override protected void onDestroy(){ if(web!=null){ web.stopLoading(); web.destroy(); } super.onDestroy(); }
}
