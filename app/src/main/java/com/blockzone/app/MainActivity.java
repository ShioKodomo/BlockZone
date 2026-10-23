package com.blockzone.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.graphics.drawable.GradientDrawable;
import java.text.NumberFormat;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int VPN_REQ = 2001;
    private SharedPreferences prefs;
    private TextView status, blockedCount;
    private Button toggle;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("blockzone", MODE_PRIVATE);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 10);
        }
        render();
    }

    private int dp(int v){ return (int)(v*getResources().getDisplayMetrics().density+.5f); }
    private GradientDrawable bg(int color, int radius){ GradientDrawable g=new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(radius)); return g; }
    private TextView text(String s, int sp, int color, boolean bold){ TextView t=new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(color); if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t; }
    private void pad(View v,int a){ v.setPadding(dp(a),dp(a),dp(a),dp(a)); }

    private void render(){
        ScrollView scroll=new ScrollView(this); scroll.setBackgroundColor(Color.rgb(10,12,17));
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(18),dp(22),dp(18),dp(30)); scroll.addView(root);

        LinearLayout header=new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL); root.addView(header);
        ImageView icon=new ImageView(this); icon.setImageResource(com.blockzone.app.R.mipmap.ic_launcher); icon.setScaleType(ImageView.ScaleType.CENTER_CROP); icon.setBackground(bg(Color.rgb(255,82,106),18));
        header.addView(icon,new LinearLayout.LayoutParams(dp(74),dp(74)));
        LinearLayout htxt=new LinearLayout(this); htxt.setOrientation(LinearLayout.VERTICAL); htxt.setPadding(dp(14),0,0,0); header.addView(htxt,new LinearLayout.LayoutParams(0,-2,1));
        TextView title=text("BlockZone",34,Color.WHITE,true); htxt.addView(title);
        htxt.addView(text("Navegue com menos anúncios e rastreadores.",15,Color.rgb(180,184,193),false));

        Space s1=new Space(this); root.addView(s1,new LinearLayout.LayoutParams(1,dp(18)));
        LinearLayout hero=new LinearLayout(this); hero.setOrientation(LinearLayout.VERTICAL); hero.setBackground(bg(Color.rgb(38,18,25),24)); pad(hero,18); root.addView(hero,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); hero.addView(row);
        LinearLayout copy=new LinearLayout(this); copy.setOrientation(LinearLayout.VERTICAL); row.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
        status=text("Proteção desativada",27,Color.WHITE,true); copy.addView(status);
        copy.addView(text("Toque no botão abaixo para ativar o filtro local.",15,Color.rgb(220,205,210),false));
        ImageView heroIcon=new ImageView(this); heroIcon.setImageResource(com.blockzone.app.R.mipmap.ic_launcher); heroIcon.setScaleType(ImageView.ScaleType.CENTER_CROP); row.addView(heroIcon,new LinearLayout.LayoutParams(dp(112),dp(112)));
        toggle=new Button(this); toggle.setTextColor(Color.WHITE); toggle.setTextSize(19); toggle.setTypeface(Typeface.DEFAULT,Typeface.BOLD); toggle.setAllCaps(false); toggle.setBackground(bg(Color.rgb(255,82,106),28)); LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(58)); bp.topMargin=dp(14); hero.addView(toggle,bp); toggle.setOnClickListener(v->toggleVpn());

        LinearLayout metrics=new LinearLayout(this); metrics.setPadding(0,dp(14),0,0); root.addView(metrics);
        blockedCount=metric(metrics,"Anúncios\nbloqueados","0",Color.rgb(255,112,129));
        metric(metrics,"Rastreadores\nbloqueados",String.valueOf(prefs.getInt("trackers",0)),Color.rgb(176,125,255));
        metric(metrics,"Apps\nprotegidos","Todos",Color.rgb(103,176,255));

        TextView cfg=text("Configurações",26,Color.WHITE,true); cfg.setPadding(0,dp(22),0,dp(4)); root.addView(cfg);
        root.addView(text("Personalize sua proteção do seu jeito.",15,Color.rgb(174,180,192),false));
        addSwitch(root,"Bloquear anúncios","Filtra domínios conhecidos de publicidade.","block_ads",true);
        addSwitch(root,"Bloquear rastreadores","Reduz rastreamento por domínios conhecidos.","block_trackers",true);
        addSwitch(root,"Iniciar com o celular","Tenta reativar a proteção após reiniciar.","boot",false);

        TextView wl=text("Lista branca",18,Color.WHITE,true); wl.setPadding(dp(16),dp(18),dp(16),dp(18)); wl.setBackground(bg(Color.rgb(21,24,32),18)); LinearLayout.LayoutParams wlp=new LinearLayout.LayoutParams(-1,-2); wlp.topMargin=dp(10); root.addView(wl,wlp); wl.setOnClickListener(v->showWhitelist());

        TextView apps=text("Apps protegidos",23,Color.WHITE,true); apps.setPadding(0,dp(22),0,dp(8)); root.addView(apps);
        LinearLayout y=new LinearLayout(this); y.setGravity(Gravity.CENTER_VERTICAL); y.setBackground(bg(Color.rgb(21,24,32),18)); pad(y,14); root.addView(y,new LinearLayout.LayoutParams(-1,-2));
        ImageView yi=new ImageView(this); yi.setImageResource(com.blockzone.app.R.mipmap.ic_launcher); y.addView(yi,new LinearLayout.LayoutParams(dp(46),dp(46)));
        LinearLayout yt=new LinearLayout(this); yt.setOrientation(LinearLayout.VERTICAL); yt.setPadding(dp(12),0,0,0); y.addView(yt,new LinearLayout.LayoutParams(0,-2,1)); yt.addView(text("Proteção de rede",17,Color.WHITE,true)); yt.addView(text("Sites e apps que usam DNS do sistema.",13,Color.rgb(174,180,192),false));
        TextView badge=text("Protegido",13,Color.rgb(113,232,168),true); badge.setPadding(dp(12),dp(8),dp(12),dp(8)); badge.setBackground(bg(Color.rgb(20,60,43),20)); y.addView(badge);

        TextView note=text("Observação: anúncios do YouTube podem usar os mesmos servidores do vídeo. Por isso, nenhum bloqueador DNS consegue garantir a remoção de todos eles no app oficial.",13,Color.rgb(160,164,174),false); note.setPadding(0,dp(18),0,0); root.addView(note);
        setContentView(scroll); refresh();
    }

    private TextView metric(LinearLayout p,String label,String value,int accent){ LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setBackground(bg(Color.rgb(21,24,32),18)); c.setPadding(dp(12),dp(14),dp(10),dp(14)); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(112),1); lp.setMargins(dp(3),0,dp(3),0); p.addView(c,lp); c.addView(text(label,12,Color.rgb(205,210,220),false)); TextView v=text(value,25,accent,true); c.addView(v); return v; }
    private void addSwitch(LinearLayout root,String title,String sub,String key,boolean def){ LinearLayout r=new LinearLayout(this); r.setGravity(Gravity.CENTER_VERTICAL); r.setBackground(bg(Color.rgb(21,24,32),18)); r.setPadding(dp(15),dp(12),dp(8),dp(12)); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.topMargin=dp(10); root.addView(r,lp); LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); r.addView(c,new LinearLayout.LayoutParams(0,-2,1)); c.addView(text(title,17,Color.WHITE,true)); c.addView(text(sub,13,Color.rgb(174,180,192),false)); Switch sw=new Switch(this); sw.setChecked(prefs.getBoolean(key,def)); sw.setOnCheckedChangeListener((b,v)->prefs.edit().putBoolean(key,v).apply()); r.addView(sw); }

    private boolean active(){ return prefs.getBoolean("active",false); }
    private void refresh(){ boolean a=active(); status.setText(a?"Proteção ativa!":"Proteção desativada"); status.setTextColor(a?Color.rgb(255,116,132):Color.WHITE); toggle.setText(a?"Desativar proteção":"Ativar proteção"); blockedCount.setText(NumberFormat.getIntegerInstance(new Locale("pt","BR")).format(prefs.getInt("blocked",0))); }
    private void toggleVpn(){ if(active()){ stopService(new Intent(this,BlockZoneVpnService.class)); prefs.edit().putBoolean("active",false).apply(); refresh(); return; } Intent prep=VpnService.prepare(this); if(prep!=null) startActivityForResult(prep,VPN_REQ); else startProtection(); }
    private void startProtection(){ startForegroundService(new Intent(this,BlockZoneVpnService.class)); prefs.edit().putBoolean("active",true).apply(); refresh(); }
    @Override protected void onActivityResult(int req,int res,Intent data){ super.onActivityResult(req,res,data); if(req==VPN_REQ && res==RESULT_OK) startProtection(); }
    @Override protected void onResume(){ super.onResume(); if(status!=null)refresh(); }
    private void showWhitelist(){ final EditText e=new EditText(this); e.setHint("exemplo.com"); e.setText(prefs.getString("whitelist","")); new AlertDialog.Builder(this).setTitle("Lista branca").setMessage("Digite domínios separados por vírgula.").setView(e).setPositiveButton("Salvar",(d,w)->prefs.edit().putString("whitelist",e.getText().toString()).apply()).setNegativeButton("Cancelar",null).show(); }
}
