package com.blockzone.app;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.text.NumberFormat;
import java.util.Locale;

public class HomeActivity extends Activity {
    private static final int VPN_REQ=2001;
    private SharedPreferences prefs;
    private TextView status, blocked;
    private Button vpnButton;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        prefs=getSharedPreferences("blockzone",MODE_PRIVATE);
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},10);
        draw();
    }
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}    
    private GradientDrawable box(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}
    private TextView tx(String s,int sp,int c,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(c);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private void gap(LinearLayout p,int h){Space s=new Space(this);p.addView(s,new LinearLayout.LayoutParams(1,dp(h)));}

    private void draw(){
        ScrollView scroll=new ScrollView(this); scroll.setBackgroundColor(Color.rgb(9,11,16));
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(18),dp(22),dp(18),dp(34)); scroll.addView(root);

        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);root.addView(head);
        ImageView icon=new ImageView(this);icon.setImageResource(R.mipmap.ic_launcher);icon.setScaleType(ImageView.ScaleType.CENTER_CROP);head.addView(icon,new LinearLayout.LayoutParams(dp(74),dp(74)));
        LinearLayout hc=new LinearLayout(this);hc.setOrientation(LinearLayout.VERTICAL);hc.setPadding(dp(14),0,0,0);head.addView(hc,new LinearLayout.LayoutParams(0,-2,1));
        hc.addView(tx("BlockZone",34,Color.WHITE,true));hc.addView(tx("Bloqueio local + modo protegido para YouTube.",14,Color.rgb(182,185,194),false));
        gap(root,18);

        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setPadding(dp(18),dp(18),dp(18),dp(18));hero.setBackground(box(Color.rgb(43,18,27),24));root.addView(hero);
        status=tx("Proteção desativada",27,Color.WHITE,true);hero.addView(status);
        hero.addView(tx("Proteção geral bloqueia domínios de anúncios e rastreadores em apps e sites.",14,Color.rgb(220,205,211),false));
        vpnButton=new Button(this);vpnButton.setTextColor(Color.WHITE);vpnButton.setTextSize(19);vpnButton.setTypeface(Typeface.DEFAULT,Typeface.BOLD);vpnButton.setAllCaps(false);vpnButton.setBackground(box(Color.rgb(236,38,76),28));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(58));bp.topMargin=dp(16);hero.addView(vpnButton,bp);vpnButton.setOnClickListener(v->toggleVpn());
        gap(root,14);

        LinearLayout yt=new LinearLayout(this);yt.setOrientation(LinearLayout.VERTICAL);yt.setPadding(dp(18),dp(18),dp(18),dp(18));yt.setBackground(box(Color.rgb(19,23,31),22));root.addView(yt);
        LinearLayout ytop=new LinearLayout(this);ytop.setGravity(Gravity.CENTER_VERTICAL);yt.addView(ytop);
        ImageView yi=new ImageView(this);yi.setImageResource(R.mipmap.ic_launcher);ytop.addView(yi,new LinearLayout.LayoutParams(dp(58),dp(58)));
        LinearLayout yc=new LinearLayout(this);yc.setOrientation(LinearLayout.VERTICAL);yc.setPadding(dp(12),0,0,0);ytop.addView(yc,new LinearLayout.LayoutParams(0,-2,1));
        yc.addView(tx("YouTube protegido",21,Color.WHITE,true));yc.addView(tx("Abra o YouTube dentro do BlockZone para uma filtragem mais forte.",13,Color.rgb(178,183,194),false));
        Button yb=new Button(this);yb.setText("Abrir YouTube protegido");yb.setTextColor(Color.WHITE);yb.setTextSize(17);yb.setTypeface(Typeface.DEFAULT,Typeface.BOLD);yb.setAllCaps(false);yb.setBackground(box(Color.rgb(255,64,91),26));LinearLayout.LayoutParams ybp=new LinearLayout.LayoutParams(-1,dp(56));ybp.topMargin=dp(14);yt.addView(yb,ybp);yb.setOnClickListener(v->startActivity(new Intent(this,YouTubeActivity.class)));
        TextView warn=tx("Importante: o aplicativo oficial do YouTube não pode ser filtrado como uma página comum porque anúncios e vídeos podem usar a mesma infraestrutura. Use este botão para assistir pelo modo protegido.",12,Color.rgb(160,165,176),false);warn.setPadding(0,dp(12),0,0);yt.addView(warn);

        gap(root,14);
        LinearLayout metrics=new LinearLayout(this);root.addView(metrics);
        blocked=metric(metrics,"Bloqueados","0",Color.rgb(255,108,130));
        metric(metrics,"Modo","DNS + Web",Color.rgb(172,124,255));
        metric(metrics,"Apps","Todos",Color.rgb(100,175,255));

        TextView cfg=tx("Configurações",25,Color.WHITE,true);cfg.setPadding(0,dp(22),0,0);root.addView(cfg);
        root.addView(tx("A proteção geral continua funcionando em paralelo.",14,Color.rgb(174,179,190),false));
        addSwitch(root,"Bloquear anúncios","Domínios conhecidos de publicidade.","block_ads",true);
        addSwitch(root,"Bloquear rastreadores","Domínios conhecidos de rastreamento.","block_trackers",true);
        addSwitch(root,"Iniciar com o celular","Reativa a proteção após reiniciar.","boot",false);

        TextView wl=tx("Lista branca",17,Color.WHITE,true);wl.setPadding(dp(16),dp(17),dp(16),dp(17));wl.setBackground(box(Color.rgb(20,23,31),18));LinearLayout.LayoutParams wlp=new LinearLayout.LayoutParams(-1,-2);wlp.topMargin=dp(10);root.addView(wl,wlp);wl.setOnClickListener(v->showWhitelist());
        setContentView(scroll);refresh();
    }

    private TextView metric(LinearLayout p,String l,String v,int c){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setPadding(dp(11),dp(13),dp(8),dp(13));x.setBackground(box(Color.rgb(20,23,31),18));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(105),1);lp.setMargins(dp(3),0,dp(3),0);p.addView(x,lp);x.addView(tx(l,12,Color.rgb(205,210,220),false));TextView val=tx(v,20,c,true);x.addView(val);return val;}
    private void addSwitch(LinearLayout root,String title,String sub,String key,boolean def){LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);r.setPadding(dp(15),dp(12),dp(8),dp(12));r.setBackground(box(Color.rgb(20,23,31),18));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(10);root.addView(r,lp);LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);r.addView(c,new LinearLayout.LayoutParams(0,-2,1));c.addView(tx(title,17,Color.WHITE,true));c.addView(tx(sub,13,Color.rgb(174,180,192),false));Switch sw=new Switch(this);sw.setChecked(prefs.getBoolean(key,def));sw.setOnCheckedChangeListener((b,v)->prefs.edit().putBoolean(key,v).apply());r.addView(sw);}
    private boolean active(){return prefs.getBoolean("active",false);}    
    private void refresh(){boolean a=active();status.setText(a?"Proteção geral ativa":"Proteção desativada");status.setTextColor(a?Color.rgb(255,115,133):Color.WHITE);vpnButton.setText(a?"Desativar proteção geral":"Ativar proteção geral");blocked.setText(NumberFormat.getIntegerInstance(new Locale("pt","BR")).format(prefs.getInt("blocked",0)));}
    private void toggleVpn(){if(active()){stopService(new Intent(this,BlockZoneVpnService.class));prefs.edit().putBoolean("active",false).apply();refresh();return;}Intent prep=VpnService.prepare(this);if(prep!=null)startActivityForResult(prep,VPN_REQ);else startProtection();}
    private void startProtection(){startForegroundService(new Intent(this,BlockZoneVpnService.class));prefs.edit().putBoolean("active",true).apply();refresh();}
    @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(req==VPN_REQ&&res==RESULT_OK)startProtection();}
    @Override protected void onResume(){super.onResume();if(status!=null)refresh();}
    private void showWhitelist(){final EditText e=new EditText(this);e.setHint("exemplo.com");e.setText(prefs.getString("whitelist",""));new android.app.AlertDialog.Builder(this).setTitle("Lista branca").setMessage("Digite domínios separados por vírgula.").setView(e).setPositiveButton("Salvar",(d,w)->prefs.edit().putString("whitelist",e.getText().toString()).apply()).setNegativeButton("Cancelar",null).show();}
}
