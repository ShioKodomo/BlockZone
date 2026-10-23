package com.blockzone.app;

import android.Manifest;
import android.app.*;
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
    private static final int VPN_REQ = 2001;
    private static final int BG = Color.rgb(8,10,16);
    private static final int CARD = Color.rgb(17,22,33);
    private static final int PINK = Color.rgb(255,84,116);
    private static final int PINK_SOFT = Color.rgb(255,126,150);
    private static final int MUTED = Color.rgb(172,176,190);
    private SharedPreferences prefs;
    private TextView statusTitle, statusText, adCount, trackerCount, appsCount;
    private Button protectionButton;
    private Switch adSwitch, bootSwitch;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("blockzone", MODE_PRIVATE);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 10);
        }
        BrowserRuntime.get(this);
        buildUi();
    }

    private int dp(int v){ return (int)(v * getResources().getDisplayMetrics().density + .5f); }
    private GradientDrawable rounded(int color, int radius){ GradientDrawable g=new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(radius)); return g; }
    private TextView text(String s,int sp,int c,boolean bold){ TextView t=new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(c); if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t; }
    private void gap(LinearLayout p,int h){ Space s=new Space(this); p.addView(s,new LinearLayout.LayoutParams(1,dp(h))); }
    private ImageView icon(int res,int size){ ImageView v=new ImageView(this); v.setImageResource(res); v.setScaleType(ImageView.ScaleType.CENTER_INSIDE); v.setPadding(dp(4),dp(4),dp(4),dp(4)); v.setLayoutParams(new LinearLayout.LayoutParams(dp(size),dp(size))); return v; }

    private void buildUi(){
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(Color.BLACK);
        LinearLayout shell=new LinearLayout(this); shell.setOrientation(LinearLayout.VERTICAL); shell.setBackgroundColor(BG);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(12),dp(14),dp(12),dp(12)); scroll.addView(root);

        LinearLayout head=new LinearLayout(this); head.setGravity(Gravity.CENTER_VERTICAL); root.addView(head);
        ImageView avatar=new ImageView(this); avatar.setImageResource(R.drawable.blockzone_avatar); avatar.setScaleType(ImageView.ScaleType.CENTER_CROP); avatar.setBackground(rounded(PINK,20)); head.addView(avatar,new LinearLayout.LayoutParams(dp(74),dp(74)));
        LinearLayout brandCol=new LinearLayout(this); brandCol.setOrientation(LinearLayout.VERTICAL); brandCol.setPadding(dp(12),0,0,0); head.addView(brandCol,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout brand=new LinearLayout(this); brand.setGravity(Gravity.CENTER_VERTICAL); brandCol.addView(brand); brand.addView(text("Block",31,Color.WHITE,true)); brand.addView(text("Zone",31,PINK,true)); ImageView verified=icon(R.drawable.ic_shield,22); verified.setColorFilter(PINK); brand.addView(verified); brandCol.addView(text("Navegue sem anúncios,\ncom mais liberdade.",13,MUTED,false));
        ImageButton settings=new ImageButton(this); settings.setImageResource(R.drawable.ic_settings); settings.setColorFilter(Color.rgb(222,226,236)); settings.setBackgroundColor(Color.TRANSPARENT); settings.setPadding(dp(9),dp(9),dp(9),dp(9)); settings.setOnClickListener(v->showWhitelist()); head.addView(settings,new LinearLayout.LayoutParams(dp(44),dp(44)));
        gap(root,10);

        FrameLayout hero=new FrameLayout(this); hero.setBackground(rounded(Color.rgb(34,16,24),24)); root.addView(hero,new LinearLayout.LayoutParams(-1,dp(288)));
        ImageView girl=new ImageView(this); girl.setImageResource(R.drawable.blockzone_hero); girl.setScaleType(ImageView.ScaleType.CENTER_CROP); FrameLayout.LayoutParams gp=new FrameLayout.LayoutParams(dp(205),dp(212),Gravity.TOP|Gravity.RIGHT); gp.topMargin=dp(2); hero.addView(girl,gp);
        LinearLayout copy=new LinearLayout(this); copy.setOrientation(LinearLayout.VERTICAL); copy.setPadding(dp(18),dp(17),0,0); FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(dp(176),dp(198),Gravity.TOP|Gravity.LEFT); hero.addView(copy,cp);
        FrameLayout shieldGlow=new FrameLayout(this); shieldGlow.setBackground(rounded(Color.rgb(67,27,41),30)); copy.addView(shieldGlow,new LinearLayout.LayoutParams(dp(76),dp(76))); ImageView shield=icon(R.drawable.ic_shield,48); shield.setColorFilter(PINK_SOFT); shieldGlow.addView(shield,new FrameLayout.LayoutParams(dp(50),dp(50),Gravity.CENTER)); gap(copy,9);
        statusTitle=text("Proteção ativa!",22,Color.WHITE,true); copy.addView(statusTitle); statusText=text("Você está navegando sem anúncios e com mais privacidade.",12,Color.rgb(215,207,214),false); statusText.setPadding(0,dp(4),0,0); copy.addView(statusText);
        protectionButton=new Button(this); protectionButton.setAllCaps(false); protectionButton.setTextSize(17); protectionButton.setTextColor(Color.WHITE); protectionButton.setTypeface(Typeface.DEFAULT,Typeface.BOLD); protectionButton.setBackground(rounded(PINK,28)); protectionButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_power,0,0,0); protectionButton.setCompoundDrawablePadding(dp(10)); protectionButton.setOnClickListener(v->toggleVpn()); FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-1,dp(60),Gravity.BOTTOM); bp.setMargins(dp(14),0,dp(14),dp(14)); hero.addView(protectionButton,bp);
        gap(root,8);

        LinearLayout metrics=new LinearLayout(this); root.addView(metrics,new LinearLayout.LayoutParams(-1,dp(110)));
        adCount=metric(metrics,"Anúncios\nbloqueados hoje","0",R.drawable.ic_megaphone,Color.rgb(255,112,136),Color.rgb(44,18,27)); trackerCount=metric(metrics,"Rastreadores\nbloqueados","0",R.drawable.ic_eyeoff,Color.rgb(180,126,255),Color.rgb(25,23,43)); appsCount=metric(metrics,"Apps protegidos","12",R.drawable.ic_grid,Color.rgb(105,176,255),Color.rgb(15,29,43));

        TextView cfg=text("Configurações",22,Color.WHITE,true); cfg.setPadding(0,dp(12),0,0); root.addView(cfg); root.addView(text("Personalize sua proteção do seu jeito.",12,MUTED,false)); gap(root,7);
        LinearLayout panel=new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL); panel.setBackground(rounded(CARD,19)); panel.setPadding(dp(4),dp(2),dp(4),dp(2)); root.addView(panel);
        adSwitch=setting(panel,R.drawable.ic_shield,"Bloquear anúncios","Impede anúncios em apps e sites.",true,false,v->{}); divider(panel); action(panel,R.drawable.ic_list,"Lista branca","Permita sites e apps de sua confiança.",v->showWhitelist()); divider(panel); bootSwitch=setting(panel,R.drawable.ic_power,"Iniciar com o celular","Ativa a proteção automaticamente.",prefs.getBoolean("boot",false),true,(button,isChecked)->prefs.edit().putBoolean("boot",isChecked).apply());

        LinearLayout appsHeader=new LinearLayout(this); appsHeader.setGravity(Gravity.CENTER_VERTICAL); appsHeader.setPadding(0,dp(12),0,dp(5)); root.addView(appsHeader); appsHeader.addView(text("Apps protegidos",18,Color.WHITE,true),new LinearLayout.LayoutParams(0,-2,1)); TextView see=text("Ver todos ›",12,PINK,false); see.setPadding(dp(8),dp(6),0,dp(6)); see.setOnClickListener(v->Toast.makeText(this,"A VPN/DNS protege apps compatíveis. YouTube usa o navegador protegido.",Toast.LENGTH_LONG).show()); appsHeader.addView(see);
        LinearLayout yt=new LinearLayout(this); yt.setGravity(Gravity.CENTER_VERTICAL); yt.setPadding(dp(10),dp(9),dp(10),dp(9)); yt.setBackground(rounded(CARD,18)); root.addView(yt); ImageView ytIcon=new ImageView(this); ytIcon.setImageResource(R.drawable.ic_youtube); ytIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE); yt.addView(ytIcon,new LinearLayout.LayoutParams(dp(48),dp(48))); LinearLayout yc=new LinearLayout(this); yc.setOrientation(LinearLayout.VERTICAL); yc.setPadding(dp(10),0,0,0); yt.addView(yc,new LinearLayout.LayoutParams(0,-2,1)); yc.addView(text("YouTube",15,Color.WHITE,true)); yc.addView(text("Abra dentro do BlockZone com bloqueio web.",11,MUTED,false)); TextView badge=text("✓ Protegido",10,Color.rgb(108,230,172),true); badge.setPadding(dp(9),dp(6),dp(9),dp(6)); badge.setBackground(rounded(Color.rgb(16,55,40),14)); yt.addView(badge); TextView arrow=text("›",24,MUTED,false); arrow.setPadding(dp(7),0,0,0); yt.addView(arrow); yt.setOnClickListener(v->startActivity(new Intent(this,BrowserActivity.class)));

        LinearLayout nav=new LinearLayout(this); nav.setGravity(Gravity.CENTER); nav.setBackgroundColor(Color.rgb(12,15,22)); nav.setPadding(dp(4),dp(3),dp(4),dp(5)); shell.addView(nav,new LinearLayout.LayoutParams(-1,dp(68))); navItem(nav,R.drawable.ic_home,"Início",true,v->{}); navItem(nav,R.drawable.ic_grid,"Apps",false,v->Toast.makeText(this,"Apps protegidos pela VPN/DNS local.",Toast.LENGTH_SHORT).show()); navItem(nav,R.drawable.ic_bars,"Relatórios",false,v->showReport()); navItem(nav,R.drawable.ic_settings,"Configurações",false,v->showWhitelist());
        setContentView(shell); refresh();
    }

    private TextView metric(LinearLayout p,String label,String value,int iconRes,int accent,int bg){ LinearLayout x=new LinearLayout(this); x.setOrientation(LinearLayout.VERTICAL); x.setPadding(dp(9),dp(9),dp(8),dp(8)); x.setBackground(rounded(bg,16)); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1); lp.setMargins(dp(2),0,dp(2),0); p.addView(x,lp); LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.TOP); x.addView(row); TextView l=text(label,10,Color.rgb(213,217,226),false); row.addView(l,new LinearLayout.LayoutParams(0,-2,1)); ImageView iv=icon(iconRes,21); iv.setColorFilter(accent); row.addView(iv); TextView v=text(value,21,Color.WHITE,true); v.setPadding(0,dp(6),0,0); x.addView(v); return v; }
    private Switch setting(LinearLayout root,int iconRes,String title,String sub,boolean checked,boolean enabled,android.widget.CompoundButton.OnCheckedChangeListener listener){ LinearLayout r=new LinearLayout(this); r.setGravity(Gravity.CENTER_VERTICAL); r.setPadding(dp(8),dp(9),dp(6),dp(9)); root.addView(r,new LinearLayout.LayoutParams(-1,-2)); FrameLayout tile=new FrameLayout(this); tile.setBackground(rounded(Color.rgb(57,29,40),12)); r.addView(tile,new LinearLayout.LayoutParams(dp(44),dp(44))); ImageView ic=icon(iconRes,24); ic.setColorFilter(PINK_SOFT); tile.addView(ic,new FrameLayout.LayoutParams(dp(28),dp(28),Gravity.CENTER)); LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(10),0,0,0); r.addView(c,new LinearLayout.LayoutParams(0,-2,1)); c.addView(text(title,15,Color.WHITE,true)); c.addView(text(sub,11,MUTED,false)); Switch sw=new Switch(this); sw.setChecked(checked); sw.setEnabled(enabled); sw.setOnCheckedChangeListener(listener); r.addView(sw); return sw; }
    private void action(LinearLayout root,int iconRes,String title,String sub,View.OnClickListener click){ LinearLayout r=new LinearLayout(this); r.setGravity(Gravity.CENTER_VERTICAL); r.setPadding(dp(8),dp(9),dp(6),dp(9)); r.setOnClickListener(click); root.addView(r,new LinearLayout.LayoutParams(-1,-2)); FrameLayout tile=new FrameLayout(this); tile.setBackground(rounded(Color.rgb(57,29,40),12)); r.addView(tile,new LinearLayout.LayoutParams(dp(44),dp(44))); ImageView ic=icon(iconRes,24); ic.setColorFilter(PINK_SOFT); tile.addView(ic,new FrameLayout.LayoutParams(dp(28),dp(28),Gravity.CENTER)); LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(10),0,0,0); r.addView(c,new LinearLayout.LayoutParams(0,-2,1)); c.addView(text(title,15,Color.WHITE,true)); c.addView(text(sub,11,MUTED,false)); r.addView(text("›",24,MUTED,false)); }
    private void divider(LinearLayout p){ View d=new View(this); d.setBackgroundColor(Color.rgb(35,40,51)); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(1)); lp.leftMargin=dp(60); p.addView(d,lp); }
    private void navItem(LinearLayout nav,int iconRes,String label,boolean selected,View.OnClickListener click){ LinearLayout item=new LinearLayout(this); item.setOrientation(LinearLayout.VERTICAL); item.setGravity(Gravity.CENTER); item.setOnClickListener(click); nav.addView(item,new LinearLayout.LayoutParams(0,-1,1)); ImageView iv=icon(iconRes,23); iv.setColorFilter(selected?PINK:Color.rgb(185,190,202)); item.addView(iv); item.addView(text(label,9,selected?PINK:Color.rgb(185,190,202),false)); }

    private boolean active(){ return prefs.getBoolean("active",false); }
    private void refresh(){ boolean a=active(); statusTitle.setText(a?"Proteção ativa!":"Proteção desativada"); statusText.setText(a?"Você está navegando sem anúncios e com mais privacidade.":"Ative o BlockZone para navegar com menos anúncios e mais privacidade."); protectionButton.setText(a?"Desativar proteção":"Ativar proteção"); adSwitch.setChecked(a); adCount.setText(fmt(prefs.getInt("blocked",0)+prefs.getInt("browser_blocked",0))); trackerCount.setText(fmt(prefs.getInt("trackers",0))); appsCount.setText("12"); }
    private String fmt(long n){ return NumberFormat.getIntegerInstance(new Locale("pt","BR")).format(n); }
    private void toggleVpn(){ if(active()){ stopService(new Intent(this,BlockZoneVpnService.class)); prefs.edit().putBoolean("active",false).apply(); refresh(); return; } Intent prep=VpnService.prepare(this); if(prep!=null) startActivityForResult(prep,VPN_REQ); else startProtection(); }
    private void startProtection(){ if(Build.VERSION.SDK_INT>=26) startForegroundService(new Intent(this,BlockZoneVpnService.class)); else startService(new Intent(this,BlockZoneVpnService.class)); prefs.edit().putBoolean("active",true).apply(); refresh(); }
    @Override protected void onActivityResult(int req,int res,Intent data){ super.onActivityResult(req,res,data); if(req==VPN_REQ && res==RESULT_OK) startProtection(); }
    @Override protected void onResume(){ super.onResume(); if(statusTitle!=null)refresh(); }
    private void showWhitelist(){ final EditText e=new EditText(this); e.setHint("exemplo.com"); e.setText(prefs.getString("whitelist","")); new AlertDialog.Builder(this).setTitle("Lista branca").setMessage("Domínios separados por vírgula.").setView(e).setPositiveButton("Salvar",(d,w)->prefs.edit().putString("whitelist",e.getText().toString()).apply()).setNegativeButton("Cancelar",null).show(); }
    private void showReport(){ long total=prefs.getInt("blocked",0)+prefs.getInt("browser_blocked",0); new AlertDialog.Builder(this).setTitle("Relatório BlockZone").setMessage("Anúncios bloqueados: "+fmt(total)+"\nRastreadores: "+fmt(prefs.getInt("trackers",0))+"\nNavegador protegido: GeckoView + filtros web").setPositiveButton("OK",null).show(); }
}
