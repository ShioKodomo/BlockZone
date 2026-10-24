package com.blockzone.app;

import android.app.*;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

public class MediaPlaybackService extends Service {
    private static final String CHANNEL_ID="blockzone_media";
    private static final int NOTIFICATION_ID=31;

    private PowerManager.WakeLock wakeLock;
    private AudioManager audioManager;
    private AudioFocusRequest focusRequest;
    private MediaSession mediaSession;

    @Override public void onCreate(){
        super.onCreate();

        if(Build.VERSION.SDK_INT>=26){
            NotificationChannel channel=new NotificationChannel(
                CHANNEL_ID,
                "Reprodução em segundo plano",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Mantém o áudio do YouTube protegido ativo com a tela apagada.");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);
        wakeLock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"BlockZone:MediaPlayback");
        wakeLock.setReferenceCounted(false);
        try{ wakeLock.acquire(); }catch(Exception ignored){}

        audioManager=(AudioManager)getSystemService(AUDIO_SERVICE);
        requestAudioFocus();

        mediaSession=new MediaSession(this,"BlockZoneYouTube");
        PlaybackState state=new PlaybackState.Builder()
            .setActions(PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE|PlaybackState.ACTION_PLAY_PAUSE)
            .setState(PlaybackState.STATE_PLAYING,PlaybackState.PLAYBACK_POSITION_UNKNOWN,1f)
            .build();
        mediaSession.setPlaybackState(state);
        mediaSession.setActive(true);
    }

    private void requestAudioFocus(){
        if(audioManager==null) return;
        try{
            if(Build.VERSION.SDK_INT>=26){
                AudioAttributes attrs=new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
                focusRequest=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(attrs)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(change->{})
                    .build();
                audioManager.requestAudioFocus(focusRequest);
            }else{
                audioManager.requestAudioFocus(change->{},AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);
            }
        }catch(Exception ignored){}
    }

    @Override public int onStartCommand(Intent intent,int flags,int startId){
        Notification.Builder builder=Build.VERSION.SDK_INT>=26
            ? new Notification.Builder(this,CHANNEL_ID)
            : new Notification.Builder(this);

        Notification notification=builder
            .setContentTitle("BlockZone")
            .setContentText("YouTube protegido em segundo plano")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setStyle(new Notification.MediaStyle().setMediaSession(mediaSession.getSessionToken()))
            .build();

        if(Build.VERSION.SDK_INT>=29){
            startForeground(NOTIFICATION_ID,notification,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        }else{
            startForeground(NOTIFICATION_ID,notification);
        }
        return START_STICKY;
    }

    @Override public void onDestroy(){
        if(mediaSession!=null){
            try{ mediaSession.setActive(false); mediaSession.release(); }catch(Exception ignored){}
        }
        if(audioManager!=null && Build.VERSION.SDK_INT>=26 && focusRequest!=null){
            try{ audioManager.abandonAudioFocusRequest(focusRequest); }catch(Exception ignored){}
        }
        if(wakeLock!=null && wakeLock.isHeld()){
            try{ wakeLock.release(); }catch(Exception ignored){}
        }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent){ return null; }
}
