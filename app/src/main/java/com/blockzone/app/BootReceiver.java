package com.blockzone.app;
import android.content.*;
import android.net.VpnService;
public class BootReceiver extends BroadcastReceiver {
  @Override public void onReceive(Context c, Intent i){
    if(Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())){
      android.content.SharedPreferences p=c.getSharedPreferences("blockzone",Context.MODE_PRIVATE);
      if(p.getBoolean("boot",false) && p.getBoolean("active",false) && VpnService.prepare(c)==null){
        c.startForegroundService(new Intent(c,BlockZoneVpnService.class));
      }
    }
  }
}
