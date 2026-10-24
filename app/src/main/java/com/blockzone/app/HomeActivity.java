package com.blockzone.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class HomeActivity extends Activity {
    private WebView web;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(5,3,9));
        getWindow().setNavigationBarColor(Color.rgb(5,3,9));
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        web.setBackgroundColor(Color.rgb(5,3,9));
        web.setWebViewClient(new WebViewClient());
        web.addJavascriptInterface(new GothTimeBridge(this), "GothTime");
        setContentView(web);
        web.loadUrl("file:///android_asset/gothtime/index.html");
    }

    @Override protected void onResume() {
        super.onResume();
        if (web != null) web.evaluateJavascript("try{refresh()}catch(e){}", null);
    }

    @Override public void onBackPressed() {
        if (web == null) { super.onBackPressed(); return; }
        web.evaluateJavascript("(function(){var a=document.querySelector('.screen.active');if(a&&a.id!=='home'&&a.id!=='splash'){go('home');return 'home'}return 'exit'})()", value -> {
            if ("\"exit\"".equals(value)) HomeActivity.super.onBackPressed();
        });
    }

    @Override protected void onDestroy() {
        if (web != null) {
            web.removeJavascriptInterface("GothTime");
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
