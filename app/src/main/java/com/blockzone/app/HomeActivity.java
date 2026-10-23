package com.blockzone.app;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.VpnService;
import android.os.*;
import android.util.Base64;
import android.view.*;
import android.widget.*;

public class HomeActivity extends Activity {
    private static final int VPN_REQ=2001;
    private SharedPreferences prefs;
    private FrameLayout root;
    private static final String ART="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";

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs=getSharedPreferences("blockzone",MODE_PRIVATE);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        if(Build.VERSION.SDK_INT>=30) getWindow().setDecorFitsSystemWindows(false);
        else getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        build();
    }

    private void build() {
        root=new FrameLayout(this); root.setBackgroundColor(Color.BLACK);
        byte[] raw=Base64.decode(ART,Base64.DEFAULT);
        Bitmap bm=BitmapFactory.decodeByteArray(raw,0,raw.length);
        ImageView art=new ImageView(this); art.setImageBitmap(bm); art.setScaleType(ImageView.ScaleType.FIT_XY);
        root.addView(art,new FrameLayout.LayoutParams(-1,-1));
        addRegion(.055f,.322f,.945f,.441f,v->toggleVpn(),"Ativar ou desativar proteção");
        addRegion(.035f,.642f,.965f,.707f,v->showWhitelist(),"Lista branca");
        addRegion(.035f,.823f,.965f,.895f,v->startActivity(new Intent(this,YouTubeActivity.class)),"YouTube protegido");
        addRegion(.78f,.025f,.97f,.085f,v->showWhitelist(),"Configurações");
        addRegion(.19f,.918f,.43f,.995f,v->startActivity(new Intent(this,MainActivity.class)),"Apps");
        addRegion(.43f,.918f,.71f,.995f,v->Toast.makeText(this,"Relatórios disponíveis na tela principal do BlockZone",Toast.LENGTH_SHORT).show(),"Relatórios");
        addRegion(.71f,.918f,.995f,.995f,v->showWhitelist(),"Configurações");
        setContentView(root);
    }

    private void addRegion(float x1,float y1,float x2,float y2,View.OnClickListener click,String desc) {
        View v=new View(this); v.setContentDescription(desc); v.setBackgroundColor(Color.TRANSPARENT); v.setOnClickListener(click);
        root.addView(v,new FrameLayout.LayoutParams(1,1));
        root.post(()->{
            FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)v.getLayoutParams();
            lp.width=(int)(root.getWidth()*(x2-x1)); lp.height=(int)(root.getHeight()*(y2-y1));
            lp.leftMargin=(int)(root.getWidth()*x1); lp.topMargin=(int)(root.getHeight()*y1); v.setLayoutParams(lp);
        });
    }

    private boolean active() { return prefs.getBoolean("active",false); }
    private void toggleVpn() {
        if(active()) { stopService(new Intent(this,BlockZoneVpnService.class)); prefs.edit().putBoolean("active",false).apply(); Toast.makeText(this,"Proteção desativada",Toast.LENGTH_SHORT).show(); return; }
        Intent prep=VpnService.prepare(this); if(prep!=null) startActivityForResult(prep,VPN_REQ); else startProtection();
    }
    private void startProtection() { startForegroundService(new Intent(this,BlockZoneVpnService.class)); prefs.edit().putBoolean("active",true).apply(); Toast.makeText(this,"Proteção BlockZone ativa",Toast.LENGTH_SHORT).show(); }
    @Override protected void onActivityResult(int req,int res,Intent data) { super.onActivityResult(req,res,data); if(req==VPN_REQ&&res==RESULT_OK)startProtection(); }
    private void showWhitelist() {
        final EditText e=new EditText(this); e.setHint("exemplo.com"); e.setText(prefs.getString("whitelist",""));
        new AlertDialog.Builder(this).setTitle("Lista branca").setMessage("Domínios separados por vírgula.").setView(e)
          .setPositiveButton("Salvar",(d,w)->prefs.edit().putString("whitelist",e.getText().toString()).apply()).setNegativeButton("Cancelar",null).show();
    }
}
