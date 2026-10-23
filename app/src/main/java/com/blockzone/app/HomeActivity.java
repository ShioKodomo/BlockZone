package com.blockzone.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
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
    private static final int BG=Color.rgb(8,10,16), CARD=Color.rgb(17,22,33), PINK=Color.rgb(255,83,119), MUTED=Color.rgb(170,174,188);
    private SharedPreferences prefs;
    private TextView status, blocked, trackers, appsCount;
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
        LinearLayout shell=new LinearLayout(this); shell.setOrientation(LinearLayout.VERTICAL); shell.setBackgroundColor(BG);
        ScrollView scroll=new ScrollView(this); shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(12),dp(14),dp(12),dp(16)); scroll.addView(root);

        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);root.addView(head);
        ImageView icon=new ImageView(this);icon.setImageResource(R.mipmap.ic_launcher);icon.setScaleType(ImageView.ScaleType.CENTER_CROP);icon.setBackground(box(Color.rgb(255,80,108),18));
        head.addView(icon,new LinearLayout.LayoutParams(dp(72),dp(72)));
        LinearLayout hc=new LinearLayout(this);hc.setOrientation(LinearLayout.VERTICAL);hc.setPadding(dp(11),0,0,0);head.addView(hc,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout brand=new LinearLayout(this);brand.setGravity(Gravity.CENTER_VERTICAL);hc.addView(brand);
        brand.addView(tx("Block",31,Color.WHITE,true));brand.addView(tx("Zone",31,PINK,true));
        TextView shield=tx("◆",19,PINK,true);shield.setPadding(dp(3),0,0,0);brand.addView(shield);
        hc.addView(tx("Navegue sem anúncios,\ncom mais liberdade.",13,MUTED,false));
        TextView gear=tx("⚙",26,Color.rgb(218,222,232),false);gear.setGravity(Gravity.CENTER);gear.setOnClickListener(v->showWhitelist());head.addView(gear,new LinearLayout.LayoutParams(dp(44),dp(44)));
        gap(root,10);

        FrameLayout hero=new FrameLayout(this);hero.setBackground(box(Color.rgb(35,16,24),24));root.addView(hero,new LinearLayout.LayoutParams(-1,dp(274)));
        ImageView girl=new ImageView(this);girl.setImageResource(R.mipmap.ic_launcher);girl.setScaleType(ImageView.ScaleType.CENTER_CROP);
        FrameLayout.LayoutParams gp=new FrameLayout.LayoutParams(dp(205),dp(205),Gravity.TOP|Gravity.RIGHT);gp.topMargin=dp(4);gp.rightMargin=dp(2);hero.addView(girl,gp);

        LinearLayout copy=new LinearLayout(this);copy.setOrientation(LinearLayout.VERTICAL);copy.setPadding(dp(16),dp(17),0,0);
        FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(dp(175),dp(190),Gravity.TOP|Gravity.LEFT);hero.addView(copy,cp);
        TextView shieldBig=tx("⬡",50,PINK,true);copy.addView(shieldBig,new LinearLayout.LayoutParams(dp(72),dp(72)));
        status=tx("Proteção desativada",22,Color.WHITE,true);copy.addView(status);
        TextView sub=tx("Ative o BlockZone para navegar com menos anúncios e mais privacidade.",12,Color.rgb(215,205,211),false);sub.setPadding(0,dp(5),0,0);copy.addView(sub);

        vpnButton=new Button(this);vpnButton.setTextColor(Color.WHITE);vpnButton.setTextSize(17);vpnButton.setTypeface(Typeface.DEFAULT,Typeface.BOLD);vpnButton.setAllCaps(false);vpnButton.setBackground(box(PINK,28));
        FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-1,dp(58),Gravity.BOTTOM);bp.setMargins(dp(14),0,dp(14),dp(13));hero.addView(vpnButton,bp);vpnButton.setOnClickListener(v->toggleVpn());
        gap(root,8);

        LinearLayout metrics=new LinearLayout(this);root.addView(metrics,new LinearLayout.LayoutParams(-1,dp(105)));
        blocked=metric(metrics,"Anúncios\nbloqueados hoje","0",Color.rgb(255,113,137),Color.rgb(42,18,26));
        trackers=metric(metrics,"Rastreadores\nbloqueados","0",Color.rgb(178,126,255),Color.rgb(24,22,42));
        appsCount=metric(metrics,"Apps protegidos","12",Color.rgb(105,176,255),Color.rgb(15,29,43));

        TextView cfg=tx("Configurações",22,Color.WHITE,true);cfg.setPadding(0,dp(12),0,0);root.addView(cfg);
        root.addView(tx("Personalize sua proteção do seu jeito.",12,MUTED,false));gap(root,7);

        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setBackground(box(CARD,19));panel.setPadding(dp(6),dp(2),dp(6),dp(2));root.addView(panel);
        setting(panel,"▣","Bloquear anúncios","Impede anúncios em apps e sites.","block_ads",true);
        divider(panel);
        action(panel,"☷","Lista branca","Permita sites e apps de sua confiança.",v->showWhitelist());
        divider(panel);
        setting(panel,"⏻","Iniciar com o celular","Ativa a proteção automaticamente.","boot",false);

        LinearLayout appsTitle=new LinearLayout(this);appsTitle.setGravity(Gravity.CENTER_VERTICAL);appsTitle.setPadding(0,dp(12),0,dp(5));root.addView(appsTitle);
        appsTitle.addView(tx("Apps protegidos",18,Color.WHITE,true),new LinearLayout.LayoutParams(0,-2,1));
        TextView see=tx("Ver todos ›",12,PINK,false);see.setPadding(dp(8),dp(6),0,dp(6));see.setOnClickListener(v->startActivity(new Intent(this,MainActivity.class)));appsTitle.addView(see);

        LinearLayout yt=new LinearLayout(this);yt.setGravity(Gravity.CENTER_VERTICAL);yt.setPadding(dp(10),dp(9),dp(10),dp(9));yt.setBackground(box(CARD,18));root.addView(yt);
        TextView ytIcon=tx("▶",22,Color.WHITE,true);ytIcon.setGravity(Gravity.CENTER);ytIcon.setBackground(box(Color.rgb(255,0,48),12));yt.addView(ytIcon,new LinearLayout.LayoutParams(dp(48),dp(48)));
        LinearLayout yc=new LinearLayout(this);yc.setOrientation(LinearLayout.VERTICAL);yc.setPadding(dp(10),0,0,0);yt.addView(yc,new LinearLayout.LayoutParams(0,-2,1));
        yc.addView(tx("YouTube",15,Color.WHITE,true));yc.addView(tx("Sem anúncios e mais foco.",11,MUTED,false));
        TextView badge=tx("✓ Protegido",10,Color.rgb(110,231,173),true);badge.setPadding(dp(9),dp(6),dp(9),dp(6));badge.setBackground(box(Color.rgb(16,55,40),14));yt.addView(badge);
        TextView arrow=tx("›",24,MUTED,false);arrow.setPadding(dp(7),0,0,0);yt.addView(arrow);
        yt.setOnClickListener(v->startActivity(new Intent(this,YouTubeActivity.class)));

        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);nav.setBackgroundColor(Color.rgb(12,15,22));nav.setPadding(dp(4),dp(4),dp(4),dp(5));shell.addView(nav,new LinearLayout.LayoutParams(-1,dp(70)));
        navItem(nav,"⌂","Início",true,v->{});
        navItem(nav,"▦","Apps",false,v->startActivity(new Intent(this,MainActivity.class)));
        navItem(nav,"▥","Relatórios",false,v->Toast.makeText(this,"Relatórios do BlockZone",Toast.LENGTH_SHORT).show());
        navItem(nav,"⚙","Configurações",false,v->showWhitelist());

        setContentView(shell);refresh();
    }

    private TextView metric(LinearLayout p,String label,String value,int accent,int bg){
        LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setPadding(dp(10),dp(10),dp(8),dp(8));x.setBackground(box(bg,16));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);lp.setMargins(dp(2),0,dp(2),0);p.addView(x,lp);
        TextView l=tx(label,10,Color.rgb(212,216,225),false);x.addView(l);TextView v=tx(value,20,Color.WHITE,true);v.setPadding(0,dp(8),0,0);x.addView(v);return v;
    }

    private void setting(LinearLayout root,String symbol,String title,String sub,String key,boolean def){
        LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);r.setPadding(dp(7),dp(8),dp(4),dp(8));root.addView(r,new LinearLayout.LayoutParams(-1,-2));
        TextView ic=tx(symbol,20,Color.rgb(255,126,151),true);ic.setGravity(Gravity.CENTER);ic.setBackground(box(Color.rgb(56,28,39),12));r.addView(ic,new LinearLayout.LayoutParams(dp(44),dp(44)));
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(10),0,0,0);r.addView(c,new LinearLayout.LayoutParams(0,-2,1));c.addView(tx(title,15,Color.WHITE,true));c.addView(tx(sub,11,MUTED,false));
        Switch sw=new Switch(this);sw.setChecked(key.equals("block_ads")?active():prefs.getBoolean(key,def));sw.setEnabled(!key.equals("block_ads"));sw.setOnCheckedChangeListener((b,v)->prefs.edit().putBoolean(key,v).apply());r.addView(sw);
    }

    private void action(LinearLayout root,String symbol,String title,String sub,View.OnClickListener click){
        LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);r.setPadding(dp(7),dp(8),dp(4),dp(8));r.setOnClickListener(click);root.addView(r,new LinearLayout.LayoutParams(-1,-2));
        TextView ic=tx(symbol,20,Color.rgb(255,126,151),true);ic.setGravity(Gravity.CENTER);ic.setBackground(box(Color.rgb(56,28,39),12));r.addView(ic,new LinearLayout.LayoutParams(dp(44),dp(44)));
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(10),0,0,0);r.addView(c,new LinearLayout.LayoutParams(0,-2,1));c.addView(tx(title,15,Color.WHITE,true));c.addView(tx(sub,11,MUTED,false));
        r.addView(tx("›",24,MUTED,false));
    }

    private void divider(LinearLayout p){View d=new View(this);d.setBackgroundColor(Color.rgb(35,40,51));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(1));lp.leftMargin=dp(60);p.addView(d,lp);}

    private void navItem(LinearLayout nav,String symbol,String label,boolean selected,View.OnClickListener click){
        LinearLayout item=new LinearLayout(this);item.setOrientation(LinearLayout.VERTICAL);item.setGravity(Gravity.CENTER);item.setOnClickListener(click);nav.addView(item,new LinearLayout.LayoutParams(0,-1,1));
        item.addView(tx(symbol,21,selected?PINK:Color.rgb(185,190,202),true));item.addView(tx(label,9,selected?PINK:Color.rgb(185,190,202),false));
    }

    private boolean active(){return prefs.getBoolean("active",false);}    
    private void refresh(){
        boolean a=active();status.setText(a?"Proteção ativa!":"Proteção desativada");status.setTextColor(a?Color.rgb(255,112,134):Color.WHITE);vpnButton.setText(a?"⏻  Desativar proteção":"⏻  Ativar proteção");
        blocked.setText(NumberFormat.getIntegerInstance(new Locale("pt","BR")).format(prefs.getInt("blocked",0)));
        trackers.setText(NumberFormat.getIntegerInstance(new Locale("pt","BR")).format(prefs.getInt("trackers",0)));
        appsCount.setText("12");
    }
    private void toggleVpn(){if(active()){stopService(new Intent(this,BlockZoneVpnService.class));prefs.edit().putBoolean("active",false).apply();refresh();return;}Intent prep=VpnService.prepare(this);if(prep!=null)startActivityForResult(prep,VPN_REQ);else startProtection();}
    private void startProtection(){startForegroundService(new Intent(this,BlockZoneVpnService.class));prefs.edit().putBoolean("active",true).apply();refresh();}
    @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(req==VPN_REQ&&res==RESULT_OK)startProtection();}
    @Override protected void onResume(){super.onResume();if(status!=null)refresh();}
    private void showWhitelist(){final EditText e=new EditText(this);e.setHint("exemplo.com");e.setText(prefs.getString("whitelist",""));new AlertDialog.Builder(this).setTitle("Lista branca").setMessage("Digite domínios separados por vírgula.").setView(e).setPositiveButton("Salvar",(d,w)->prefs.edit().putString("whitelist",e.getText().toString()).apply()).setNegativeButton("Cancelar",null).show();}
}
