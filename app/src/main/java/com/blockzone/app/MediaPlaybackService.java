package com.blockzone.app;

import android.app.*;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

public class MediaPlaybackService extends Service {
    private static final String CHANNEL_ID="blockzone_media";
    private static final int NOTIFICATION_ID=31;
    private PowerManager.WakeLock wakeLock;

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
            .build();

        if(Build.VERSION.SDK_INT>=29){
            startForeground(NOTIFICATION_ID,notification,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        }else{
            startForeground(NOTIFICATION_ID,notification);
        }
        return START_STICKY;
    }

    @Override public void onDestroy(){
        if(wakeLock!=null && wakeLock.isHeld()){
            try{ wakeLock.release(); }catch(Exception ignored){}
        }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent){ return null; }
}
