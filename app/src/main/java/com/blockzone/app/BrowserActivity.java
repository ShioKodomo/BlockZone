package com.blockzone.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.MediaSession;

public class BrowserActivity extends Activity {
    private GeckoSession session;
    private GeckoView geckoView;
    private boolean sessionAttached=false;
    private boolean canGoBack=false;
    private boolean youtubeLoaded=false;
    private ProgressBar progress;
    private TextView protectionStatus;
    private MediaSession currentMediaSession;
    private boolean mediaWasPlaying=false;
    private final Handler handler=new Handler(Looper.getMainLooper());

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

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
        protectionStatus.setText("Proteção iniciando…");
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

        geckoView=new GeckoView(this);
        geckoView.setBackgroundColor(Color.BLACK);
        root.addView(geckoView,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);

        GeckoRuntime runtime=BrowserRuntime.get(this);
        GeckoSessionSettings settings=new GeckoSessionSettings.Builder()
            .suspendMediaWhenInactive(false)
            .allowJavascript(true)
            .build();

        session=new GeckoSession(settings);
        session.setActive(true);
        session.setPriorityHint(GeckoSession.PRIORITY_HIGH);
        session.setContentDelegate(new GeckoSession.ContentDelegate(){});
        session.setNavigationDelegate(new GeckoSession.NavigationDelegate(){
            @Override public void onCanGoBack(GeckoSession s, boolean value){ canGoBack=value; }
            @Override public GeckoResult<GeckoSession> onNewSession(GeckoSession s,String uri){
                if(BrowserRuntime.isUboReady()) session.loadUri(uri);
                return null;
            }
        });
        session.setProgressDelegate(new GeckoSession.ProgressDelegate(){
            @Override public void onPageStart(GeckoSession s,String url){
                progress.setIndeterminate(false);
                progress.setVisibility(View.VISIBLE);
                progress.setProgress(15);
            }
            @Override public void onProgressChange(GeckoSession s,int value){
                progress.setIndeterminate(false);
                progress.setProgress(value);
            }
            @Override public void onPageStop(GeckoSession s,boolean success){
                progress.setProgress(100);
                progress.setVisibility(View.GONE);
            }
        });
        session.setMediaSessionDelegate(new MediaSession.Delegate(){
            @Override public void onActivated(GeckoSession s, MediaSession mediaSession){
                currentMediaSession=mediaSession;
            }
            @Override public void onDeactivated(GeckoSession s, MediaSession mediaSession){
                if(currentMediaSession==mediaSession) currentMediaSession=null;
                mediaWasPlaying=false;
            }
            @Override public void onPlay(GeckoSession s, MediaSession mediaSession){
                currentMediaSession=mediaSession;
                mediaWasPlaying=true;
            }
            @Override public void onPause(GeckoSession s, MediaSession mediaSession){
                currentMediaSession=mediaSession;
                boolean shouldResume=mediaWasPlaying && !isDeviceInteractive();
                if(shouldResume){
                    handler.postDelayed(()->{
                        try{
                            if(currentMediaSession!=null && currentMediaSession.isActive()) currentMediaSession.play();
                        }catch(Exception ignored){}
                    },300);
                }else{
                    mediaWasPlaying=false;
                }
            }
            @Override public void onStop(GeckoSession s, MediaSession mediaSession){
                mediaWasPlaying=false;
            }
        });

        session.open(runtime);
        geckoView.setSession(session);
        sessionAttached=true;

        BrowserRuntime.whenAdblockReady(this,new BrowserRuntime.ReadyListener(){
            @Override public void onReady(){
                runOnUiThread(()->{
                    protectionStatus.setText("Proteção ativa • uBlock Origin");
                    protectionStatus.setTextColor(Color.rgb(110,231,173));
                    if(!youtubeLoaded){
                        youtubeLoaded=true;
                        progress.setIndeterminate(false);
                        session.loadUri("https://m.youtube.com/");
                    }
                });
            }

            @Override public void onError(String message){
                runOnUiThread(()->{
                    protectionStatus.setText("Proteção indisponível");
                    protectionStatus.setTextColor(Color.rgb(255,92,119));
                    progress.setVisibility(View.GONE);
                    Toast.makeText(BrowserActivity.this,message,Toast.LENGTH_LONG).show();
                });
            }
        });

        Intent playback=new Intent(this, MediaPlaybackService.class);
        if(Build.VERSION.SDK_INT>=26) startForegroundService(playback); else startService(playback);
    }

    private boolean isDeviceInteractive(){
        PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);
        return pm==null || pm.isInteractive();
    }

    @Override protected void onStart(){
        super.onStart();
        if(session!=null){
            if(!sessionAttached && geckoView!=null){
                try{
                    geckoView.setSession(session);
                    sessionAttached=true;
                }catch(Exception ignored){}
            }
            session.setActive(true);
            session.setPriorityHint(GeckoSession.PRIORITY_HIGH);
        }
    }

    @Override protected void onResume(){
        super.onResume();
        if(session!=null){
            session.setActive(true);
            session.setFocused(true);
            session.setPriorityHint(GeckoSession.PRIORITY_HIGH);
        }
    }

    @Override protected void onPause(){
        if(session!=null){
            session.setFocused(false);
            session.setActive(true);
            session.setPriorityHint(GeckoSession.PRIORITY_HIGH);
        }
        super.onPause();
    }

    @Override protected void onStop(){
        if(session!=null && geckoView!=null && sessionAttached){
            try{
                GeckoSession released=geckoView.releaseSession();
                if(released!=null) session=released;
                sessionAttached=false;
            }catch(Exception ignored){}
        }
        if(session!=null){
            session.setActive(true);
            session.setPriorityHint(GeckoSession.PRIORITY_HIGH);
        }
        super.onStop();
    }

    private TextView button(String s,int sp){
        TextView t=new TextView(this);
        t.setText(s);
        t.setTextColor(Color.WHITE);
        t.setTextSize(sp);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private int dp(int v){ return (int)(v*getResources().getDisplayMetrics().density+.5f); }

    @Override public void onBackPressed(){
        if(session!=null && canGoBack) session.goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy(){
        handler.removeCallbacksAndMessages(null);
        stopService(new Intent(this, MediaPlaybackService.class));
        if(sessionAttached && geckoView!=null){
            try{ geckoView.releaseSession(); }catch(Exception ignored){}
            sessionAttached=false;
        }
        if(session!=null && session.isOpen()) session.close();
        super.onDestroy();
    }
}
