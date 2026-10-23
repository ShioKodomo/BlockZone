package com.blockzone.app;

import android.app.*;
import android.content.*;
import android.net.VpnService;
import android.os.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class BlockZoneVpnService extends VpnService implements Runnable {
    private ParcelFileDescriptor tun;
    private Thread worker;
    private volatile boolean running;
    private final Set<String> blocked = new HashSet<>();
    private SharedPreferences prefs;
    private static final String DNS = "10.10.10.10";

    @Override public void onCreate(){ super.onCreate(); prefs=getSharedPreferences("blockzone",MODE_PRIVATE); loadBlocklist(); createChannel(); }
    private void loadBlocklist(){ try(BufferedReader r=new BufferedReader(new InputStreamReader(getAssets().open("blocked_domains.txt")))){ String s; while((s=r.readLine())!=null){s=s.trim().toLowerCase(Locale.ROOT); if(!s.isEmpty()&&!s.startsWith("#"))blocked.add(s);} }catch(Exception ignored){} }
    private void createChannel(){ if(Build.VERSION.SDK_INT>=26){ NotificationChannel c=new NotificationChannel("blockzone","BlockZone",NotificationManager.IMPORTANCE_LOW); getSystemService(NotificationManager.class).createNotificationChannel(c);} }
    private Notification note(){ Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,"blockzone"):new Notification.Builder(this); return b.setContentTitle("BlockZone ativo").setContentText("Bloqueando anúncios e rastreadores por DNS").setSmallIcon(android.R.drawable.ic_secure).build(); }

    @Override public int onStartCommand(Intent i,int f,int id){ startForeground(7,note()); if(worker==null){ running=true; worker=new Thread(this,"BlockZoneVPN"); worker.start(); } return START_STICKY; }
    @Override public void onDestroy(){ running=false; try{ if(tun!=null)tun.close(); }catch(Exception ignored){} worker=null; prefs.edit().putBoolean("active",false).apply(); super.onDestroy(); }
    @Override public void run(){ try{
        tun=new Builder().setSession("BlockZone").addAddress("10.10.10.2",32).addRoute(DNS,32).addDnsServer(DNS).setMtu(1500).establish();
        if(tun==null)return;
        FileInputStream in=new FileInputStream(tun.getFileDescriptor()); FileOutputStream out=new FileOutputStream(tun.getFileDescriptor()); byte[] packet=new byte[32767];
        while(running){ int len=in.read(packet); if(len<=0)continue; byte[] resp=handle(Arrays.copyOf(packet,len)); if(resp!=null)out.write(resp); }
    }catch(Exception ignored){} }

    private byte[] handle(byte[] p){ try{
        int ver=(p[0]>>4)&0xf; if(ver!=4)return null; int ihl=(p[0]&0xf)*4; if(ihl<20||p.length<ihl+8)return null; if((p[9]&0xff)!=17)return null;
        int srcPort=u16(p,ihl), dstPort=u16(p,ihl+2); if(dstPort!=53)return null; int dnsOff=ihl+8; byte[] q=Arrays.copyOfRange(p,dnsOff,p.length); String domain=readDomain(q); if(domain==null)return null;
        byte[] dnsResp;
        if(shouldBlock(domain)){ dnsResp=nxdomain(q); prefs.edit().putInt("blocked",prefs.getInt("blocked",0)+1).apply(); }
        else dnsResp=forward(q);
        if(dnsResp==null)return null;
        int total=20+8+dnsResp.length; byte[] r=new byte[total]; r[0]=0x45; r[1]=0; put16(r,2,total); put16(r,4,0); put16(r,6,0x4000); r[8]=64; r[9]=17;
        System.arraycopy(p,16,r,12,4); System.arraycopy(p,12,r,16,4); put16(r,20,53); put16(r,22,srcPort); put16(r,24,8+dnsResp.length); put16(r,26,0); System.arraycopy(dnsResp,0,r,28,dnsResp.length); put16(r,10,checksum(r,0,20)); put16(r,26,udpChecksum(r)); return r;
    }catch(Exception e){ return null; } }

    private boolean shouldBlock(String d){ String wl=prefs.getString("whitelist","").toLowerCase(Locale.ROOT); for(String x:wl.split(",")){x=x.trim(); if(!x.isEmpty()&&(d.equals(x)||d.endsWith("."+x)))return false;} for(String b:blocked)if(d.equals(b)||d.endsWith("."+b))return true; return false; }
    private String readDomain(byte[] q){ if(q.length<12)return null; int pos=12; StringBuilder s=new StringBuilder(); while(pos<q.length){int n=q[pos++]&0xff; if(n==0)break; if(n>63||pos+n>q.length)return null; if(s.length()>0)s.append('.'); for(int i=0;i<n;i++)s.append((char)(q[pos++]&0xff));} return s.toString().toLowerCase(Locale.ROOT); }
    private byte[] nxdomain(byte[] q){ byte[] r=q.clone(); r[2]=(byte)0x81; r[3]=(byte)0x83; r[6]=r[7]=r[8]=r[9]=r[10]=r[11]=0; return r; }
    private byte[] forward(byte[] q){ DatagramSocket s=null; try{ s=new DatagramSocket(); protect(s); s.setSoTimeout(2500); byte[] addr={(byte)1,(byte)1,(byte)1,(byte)1}; DatagramPacket req=new DatagramPacket(q,q.length,InetAddress.getByAddress(addr),53); s.send(req); byte[] b=new byte[4096]; DatagramPacket resp=new DatagramPacket(b,b.length); s.receive(resp); return Arrays.copyOf(b,resp.getLength()); }catch(Exception e){return nxdomain(q);} finally{if(s!=null)s.close();} }
    private int u16(byte[] b,int o){return ((b[o]&255)<<8)|(b[o+1]&255);} private void put16(byte[] b,int o,int v){b[o]=(byte)(v>>8);b[o+1]=(byte)v;}
    private int checksum(byte[] b,int o,int len){ long sum=0; int i=o; while(len>1){sum+=u16(b,i);i+=2;len-=2;} if(len>0)sum+=(b[i]&255)<<8; while((sum>>16)!=0)sum=(sum&0xffff)+(sum>>16); return (int)(~sum)&0xffff; }
    private int udpChecksum(byte[] p){ int udpLen=u16(p,24); long sum=0; for(int i=12;i<20;i+=2)sum+=u16(p,i); sum+=17; sum+=udpLen; for(int i=20;i<20+udpLen;i+=2){int v=(p[i]&255)<<8; if(i+1<20+udpLen)v|=p[i+1]&255; sum+=v;} while((sum>>16)!=0)sum=(sum&0xffff)+(sum>>16); int c=(int)(~sum)&0xffff; return c==0?0xffff:c; }
}
