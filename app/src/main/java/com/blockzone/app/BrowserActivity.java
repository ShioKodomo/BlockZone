package com.blockzone.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;

public class BrowserActivity extends Activity {
    private GeckoSession session;
    private boolean canGoBack=false;
    private ProgressBar progress;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.BLACK); getWindow().setNavigationBarColor(Color.BLACK);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.rgb(8,10,16));
        LinearLayout top=new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL); top.setPadding(dp(8),dp(5),dp(8),dp(5)); top.setBackgroundColor(Color.rgb(13,16,23));
        TextView back=button("‹",28); top.addView(back,new LinearLayout.LayoutParams(dp(48),dp(48))); back.setOnClickListener(v->onBackPressed());
        TextView title=new TextView(this); title.setText("YouTube protegido"); title.setTextColor(Color.WHITE); title.setTextSize(17); title.setGravity(Gravity.CENTER_VERTICAL); top.addView(title,new LinearLayout.LayoutParams(0,dp(48),1));
        TextView shield=button("✓",20); shield.setTextColor(Color.rgb(110,231,173)); top.addView(shield,new LinearLayout.LayoutParams(dp(48),dp(48)));
        root.addView(top,new LinearLayout.LayoutParams(-1,-2));
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal); progress.setMax(100); root.addView(progress,new LinearLayout.LayoutParams(-1,dp(2)));
        GeckoView view=new GeckoView(this); view.setBackgroundColor(Color.BLACK); root.addView(view,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root);

        GeckoRuntime runtime=BrowserRuntime.get(this); session=new GeckoSession(); session.setContentDelegate(new GeckoSession.ContentDelegate(){});
        session.setNavigationDelegate(new GeckoSession.NavigationDelegate(){
            @Override public void onCanGoBack(GeckoSession s, boolean value){ canGoBack=value; }
            @Override public GeckoResult<GeckoSession> onNewSession(GeckoSession s,String uri){ session.loadUri(uri); return null; }
        });
        session.setProgressDelegate(new GeckoSession.ProgressDelegate(){
            @Override public void onPageStart(GeckoSession s,String url){ progress.setVisibility(View.VISIBLE); progress.setProgress(15); }
            @Override public void onPageStop(GeckoSession s,boolean success){ progress.setProgress(100); progress.setVisibility(View.GONE); }
        });
        session.open(runtime); view.setSession(session); session.loadUri("https://m.youtube.com/");
    }
    private TextView button(String s,int sp){ TextView t=new TextView(this); t.setText(s); t.setTextColor(Color.WHITE); t.setTextSize(sp); t.setGravity(Gravity.CENTER); return t; }
    private int dp(int v){ return (int)(v*getResources().getDisplayMetrics().density+.5f); }
    @Override public void onBackPressed(){ if(session!=null && canGoBack) session.goBack(); else super.onBackPressed(); }
    @Override protected void onDestroy(){ if(session!=null && session.isOpen()) session.close(); super.onDestroy(); }
}
