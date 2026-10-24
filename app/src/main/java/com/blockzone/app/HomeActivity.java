package com.blockzone.app;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class HomeActivity extends Activity {
    private LinearLayout root;
    private TextView battery, temp, ram, storage, assistant;
    private static final int BG = Color.rgb(6,7,11);
    private static final int PANEL = Color.rgb(17,19,26);
    private static final int PINK = Color.rgb(255,85,187);
    private static final int TEXT = Color.rgb(246,242,247);
    private static final int MUTED = Color.rgb(158,154,170);
    private static final String MASCOT = "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";

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        buildUi(); refresh();
    }

    @Override protected void onResume() { super.onResume(); refresh(); }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this); scroll.setBackgroundColor(BG); scroll.setFillViewport(true);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(16),dp(12),dp(16),dp(30));
        scroll.addView(root); setContentView(scroll);

        LinearLayout hero = card(); hero.setPadding(dp(18),dp(18),dp(10),dp(12)); hero.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout copy = new LinearLayout(this); copy.setOrientation(LinearLayout.VERTICAL); copy.setPadding(0,0,dp(8),0);
        TextView brand = title("GothTime", 32); brand.setTextColor(Color.rgb(255,212,237)); brand.setTypeface(Typeface.SERIF, Typeface.BOLD);
        TextView kicker = small("POCO F1 OPTIMIZER"); kicker.setTextColor(Color.rgb(255,139,212)); kicker.setLetterSpacing(.18f);
        TextView h = title("Seu Poco ainda tem fôlego.", 20); h.setPadding(0,dp(16),0,dp(6));
        TextView p = body("Eu cuido do peso morto. Você fica com a velocidade.");
        assistant = body("Analisando o aparelho… ♡"); assistant.setTextColor(Color.rgb(249,216,235)); assistant.setPadding(0,dp(12),0,0);
        copy.addView(brand); copy.addView(kicker); copy.addView(h); copy.addView(p); copy.addView(assistant);
        hero.addView(copy, new LinearLayout.LayoutParams(0,-2,1));
        ImageView iv = new ImageView(this); try { byte[] raw=Base64.decode(MASCOT,Base64.DEFAULT); iv.setImageBitmap(BitmapFactory.decodeByteArray(raw,0,raw.length)); } catch(Exception ignored){}
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP); hero.addView(iv,new LinearLayout.LayoutParams(dp(118),dp(190)));
        root.addView(hero); gap(14);

        section("Estado do aparelho");
        LinearLayout metrics = new LinearLayout(this); metrics.setOrientation(LinearLayout.HORIZONTAL);
        battery = metric(metrics,"--%","Bateria"); temp = metric(metrics,"--°C","Temperatura"); ram = metric(metrics,"--%","RAM usada");
        root.addView(metrics); gap(8);
        LinearLayout metrics2 = new LinearLayout(this); metrics2.setOrientation(LinearLayout.HORIZONTAL);
        storage = metric(metrics2,"--%","Armazenamento"); metric(metrics2,"Poco F1","Dispositivo");
        root.addView(metrics2); gap(16);

        section("Ações rápidas");
        addAction("🚀  Turbo Boost", "Reduz animações para 0,5x. Sem overclock.", v -> turbo());
        addAction("🧹  Limpeza segura", "Abre o gerenciador de armazenamento do Android.", v -> open(Settings.ACTION_INTERNAL_STORAGE_SETTINGS));
        addAction("🍃  Battery Saver", "Abre a economia de bateria nativa.", v -> open(Settings.ACTION_BATTERY_SAVER_SETTINGS));
        addAction("❄️  Apps em segundo plano", "Gerencie restrições e consumo por aplicativo.", v -> open(Settings.ACTION_APPLICATION_SETTINGS));
        addAction("🎮  Game Mode", "Abre a central de Segurança/Game Turbo da MIUI quando disponível.", v -> gameMode());
        addAction("🔎  Deep Scan", "Reavalia memória, espaço, bateria e temperatura.", v -> {refresh(); Toast.makeText(this,"Deep Scan concluído.",Toast.LENGTH_SHORT).show();});
        addAction("🛠️  Opções do desenvolvedor", "Ative Depuração USB para o modo Advanced.", v -> open(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
        addAction("↩️  Restaurar animações", "Retorna as escalas do Android para 1x.", v -> restoreAnimations());

        gap(14); section("Assistente GothTime");
        LinearLayout tip=card(); tip.setPadding(dp(16),dp(14),dp(16),dp(14));
        TextView t=body("Dica da GothTime: mantenha pelo menos 15% do armazenamento livre e evite jogar enquanto a bateria estiver muito quente. Menos drama térmico, mais FPS. ♡");
        tip.addView(t); root.addView(tip);
    }

    private void refresh() {
        try {
            Intent i=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED)); int level=0; double c=0;
            if(i!=null){ int l=i.getIntExtra(BatteryManager.EXTRA_LEVEL,-1), s=i.getIntExtra(BatteryManager.EXTRA_SCALE,100); level=s>0?Math.round(l*100f/s):l; c=i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0)/10.0; }
            ActivityManager am=(ActivityManager)getSystemService(ACTIVITY_SERVICE); ActivityManager.MemoryInfo mi=new ActivityManager.MemoryInfo(); am.getMemoryInfo(mi);
            int ramPct=(int)Math.round((1.0-(double)mi.availMem/mi.totalMem)*100);
            StatFs sf=new StatFs(Environment.getDataDirectory().getPath()); long total=sf.getTotalBytes(), avail=sf.getAvailableBytes(); int stor=(int)Math.round((1.0-(double)avail/total)*100);
            battery.setText(level+"%"); temp.setText(String.format(java.util.Locale.US,"%.1f°C",c)); ram.setText(ramPct+"%"); storage.setText(stor+"%");
            if(c>=43) assistant.setText("Seu aparelho está quente. Dá um descanso para ele agora. ♡");
            else if(stor>=85) assistant.setText("Seu armazenamento está apertado. Eu começaria limpando arquivos grandes. ♡");
            else assistant.setText("Tudo sob controle. Seu Poco ainda sabe correr. ♡");
        } catch(Exception e){ assistant.setText("Não consegui ler um dos sensores, mas continuo funcionando. ♡"); }
    }

    private void turbo() {
        try {
            if(checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS")!=android.content.pm.PackageManager.PERMISSION_GRANTED){
                String cmd="adb shell pm grant com.gothtime.optimizer android.permission.WRITE_SECURE_SETTINGS";
                ClipboardManager cb=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE); cb.setPrimaryClip(ClipData.newPlainText("GothTime Advanced",cmd));
                Toast.makeText(this,"Modo Advanced necessário. Comando ADB copiado.",Toast.LENGTH_LONG).show(); return;
            }
            Settings.Global.putFloat(getContentResolver(),Settings.Global.WINDOW_ANIMATION_SCALE,.5f);
            Settings.Global.putFloat(getContentResolver(),Settings.Global.TRANSITION_ANIMATION_SCALE,.5f);
            Settings.Global.putFloat(getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,.5f);
            Toast.makeText(this,"Turbo Boost ativo: animações 0,5x.",Toast.LENGTH_SHORT).show();
        } catch(Exception e){Toast.makeText(this,"Não foi possível ativar o Turbo.",Toast.LENGTH_LONG).show();}
    }

    private void restoreAnimations() {
        try {
            Settings.Global.putFloat(getContentResolver(),Settings.Global.WINDOW_ANIMATION_SCALE,1f);
            Settings.Global.putFloat(getContentResolver(),Settings.Global.TRANSITION_ANIMATION_SCALE,1f);
            Settings.Global.putFloat(getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1f);
            Toast.makeText(this,"Animações restauradas para 1x.",Toast.LENGTH_SHORT).show();
        } catch(Exception e){Toast.makeText(this,"Modo Advanced necessário.",Toast.LENGTH_LONG).show();}
    }

    private void gameMode() {
        try { Intent x=getPackageManager().getLaunchIntentForPackage("com.miui.securitycenter"); if(x!=null) startActivity(x); else throw new Exception(); }
        catch(Exception e){Toast.makeText(this,"Game Turbo não foi localizado nesta ROM.",Toast.LENGTH_LONG).show();}
    }

    private void open(String action) { try{startActivity(new Intent(action));}catch(Exception e){startActivity(new Intent(Settings.ACTION_SETTINGS));} }
    private LinearLayout card() { LinearLayout l=new LinearLayout(this); l.setBackground(round(PANEL,20,Color.rgb(43,45,58))); return l; }
    private void section(String s) { TextView t=title(s,17); t.setPadding(dp(2),dp(4),0,dp(9)); root.addView(t); }
    private TextView title(String s,int sp) { TextView t=new TextView(this); t.setText(s); t.setTextColor(TEXT); t.setTextSize(sp); t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t; }
    private TextView body(String s) { TextView t=new TextView(this); t.setText(s); t.setTextColor(Color.rgb(200,195,206)); t.setTextSize(13); t.setLineSpacing(0,1.15f); return t; }
    private TextView small(String s) { TextView t=new TextView(this); t.setText(s); t.setTextColor(MUTED); t.setTextSize(10); return t; }
    private TextView metric(LinearLayout parent,String val,String label) { LinearLayout c=card(); c.setOrientation(LinearLayout.VERTICAL); c.setGravity(Gravity.CENTER); c.setPadding(dp(8),dp(13),dp(8),dp(13)); TextView v=title(val,17); v.setTextColor(Color.rgb(255,213,237)); TextView l=small(label); l.setPadding(0,dp(4),0,0); c.addView(v); c.addView(l); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1); lp.setMargins(dp(3),0,dp(3),0); parent.addView(c,lp); return v; }
    private void addAction(String name,String desc,View.OnClickListener click) { Button b=new Button(this); b.setAllCaps(false); b.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL); b.setText(name+"\n"+desc); b.setTextColor(TEXT); b.setTextSize(13); b.setPadding(dp(16),dp(10),dp(16),dp(10)); b.setBackground(round(PANEL,17,Color.rgb(50,43,57))); b.setOnClickListener(click); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(82)); lp.setMargins(0,0,0,dp(9)); root.addView(b,lp); }
    private GradientDrawable round(int fill,int radius,int stroke) { GradientDrawable g=new GradientDrawable(); g.setColor(fill); g.setCornerRadius(dp(radius)); g.setStroke(dp(1),stroke); return g; }
    private void gap(int d) { View v=new View(this); root.addView(v,new LinearLayout.LayoutParams(1,dp(d))); }
    private int dp(int x) { return (int)(x*getResources().getDisplayMetrics().density+.5f); }
}
