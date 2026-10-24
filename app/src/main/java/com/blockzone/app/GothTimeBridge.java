package com.blockzone.app;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.Process;
import android.os.StatFs;
import android.provider.Settings;
import android.util.Base64;
import android.webkit.JavascriptInterface;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

final class GothTimeBridge {
    private final Activity activity;
    private final Context context;

    GothTimeBridge(Activity activity) {
        this.activity = activity;
        this.context = activity.getApplicationContext();
    }

    @JavascriptInterface public String getMascot() {
        try (InputStream in = context.getResources().openRawResource(R.drawable.blockzone_hero);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
        } catch (Throwable e) {
            return "";
        }
    }

    @JavascriptInterface public String getState() {
        JSONObject o = new JSONObject();
        try {
            Intent i = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int level = 0;
            double temp = 0;
            boolean charging = false;
            if (i != null) {
                int l = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                level = scale > 0 ? Math.round(l * 100f / scale) : l;
                temp = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0;
                int status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
            }
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            StatFs sf = new StatFs(Environment.getDataDirectory().getAbsolutePath());
            long total = sf.getTotalBytes();
            long avail = sf.getAvailableBytes();
            o.put("model", Build.MODEL);
            o.put("android", Build.VERSION.RELEASE);
            o.put("battery", level);
            o.put("temp", String.format(Locale.US, "%.1f", temp));
            o.put("charging", charging);
            o.put("ramTotal", mi.totalMem);
            o.put("ramUsed", Math.max(0, mi.totalMem - mi.availMem));
            o.put("storageTotal", total);
            o.put("storageUsed", Math.max(0, total - avail));
            o.put("advanced", hasSecureSettings());
            o.put("root", hasRoot());
            o.put("usage", hasUsageAccess());
        } catch (Throwable e) {
            try { o.put("error", e.getClass().getSimpleName()); } catch (Throwable ignored) {}
        }
        return o.toString();
    }

    @JavascriptInterface public boolean getAutoOptimize() {
        return context.getSharedPreferences("gothtime", Context.MODE_PRIVATE).getBoolean("auto", false);
    }

    @JavascriptInterface public String setAutoOptimize(boolean on) {
        context.getSharedPreferences("gothtime", Context.MODE_PRIVATE).edit().putBoolean("auto", on).apply();
        scheduleMaintenance(on);
        return result(true, "Otimização programada", on ? "Manutenção leve diária ativada." : "Manutenção automática desativada.", "");
    }

    @JavascriptInterface public void requestUsageAccess() {
        activity.runOnUiThread(() -> {
            try { activity.startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)); }
            catch (Throwable e) { activity.startActivity(new Intent(Settings.ACTION_SETTINGS)); }
        });
    }

    @JavascriptInterface public String topApps() {
        JSONArray out = new JSONArray();
        if (!hasUsageAccess()) return out.toString();
        try {
            UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
            long end = System.currentTimeMillis();
            List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, end - 86400000L, end);
            if (stats == null) return out.toString();
            stats = new ArrayList<>(stats);
            stats.sort(Comparator.comparingLong(UsageStats::getTotalTimeInForeground).reversed());
            PackageManager pm = context.getPackageManager();
            int count = 0;
            for (UsageStats u : stats) {
                if (u.getTotalTimeInForeground() < 120000L || u.getPackageName().equals(context.getPackageName())) continue;
                JSONObject j = new JSONObject();
                String name = u.getPackageName();
                try {
                    ApplicationInfo ai = pm.getApplicationInfo(u.getPackageName(), 0);
                    name = pm.getApplicationLabel(ai).toString();
                } catch (Throwable ignored) {}
                j.put("name", name);
                j.put("pkg", u.getPackageName());
                j.put("min", Math.max(1, TimeUnit.MILLISECONDS.toMinutes(u.getTotalTimeInForeground())));
                out.put(j);
                if (++count >= 10) break;
            }
        } catch (Throwable ignored) {}
        return out.toString();
    }

    @JavascriptInterface public String hibernatePackage(String pkg) {
        String cleanPkg = pkg == null ? "" : pkg.replaceAll("[^a-zA-Z0-9._-]", "");
        if (cleanPkg.isEmpty()) return result(false, "Hibernação", "Pacote inválido.", "");
        if (hasRoot()) {
            boolean ok = root("am force-stop " + cleanPkg);
            return result(ok, "Hibernação", ok ? "Aplicativo encerrado com acesso root." : "O comando root foi recusado.", cleanPkg);
        }
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            am.killBackgroundProcesses(cleanPkg);
            return result(true, "Hibernação", "Solicitei ao Android o encerramento do processo em segundo plano.", cleanPkg);
        } catch (Throwable e) {
            return result(false, "Hibernação", "O Android bloqueou essa ação neste modo.", cleanPkg);
        }
    }

    @JavascriptInterface public String runAction(String action) {
        if ("turbo".equals(action)) return turbo();
        if ("balanced".equals(action) || "restore".equals(action)) return restore();
        if ("clean".equals(action)) return cleanCache();
        if ("battery_on".equals(action)) return batterySaver();
        if ("game".equals(action)) return gameMode();
        if ("scan".equals(action)) return scan();
        if ("root".equals(action)) return accessStatus();
        return result(false, "GothTime", "Ação não reconhecida.", String.valueOf(action));
    }

    @JavascriptInterface public String ask(String q) {
        try {
            JSONObject s = new JSONObject(getState());
            double t = Double.parseDouble(s.optString("temp", "0"));
            int ram = pct(s.optLong("ramUsed"), s.optLong("ramTotal"));
            int storage = pct(s.optLong("storageUsed"), s.optLong("storageTotal"));
            String z = q == null ? "" : q.toLowerCase(Locale.ROOT);
            if (z.contains("bateria")) return "Use Battery Saver e reduza apps em segundo plano. Software não recupera desgaste químico da bateria.";
            if (z.contains("jogo") || z.contains("fps")) return t >= 42 ? "Eu evitaria Game Mode agora: " + t + " °C." : "Game Mode reduz animações e, com root, limpa a sessão sem desativar proteção térmica.";
            if (z.contains("ram")) return "RAM usada agora: " + ram + "%.";
            if (z.contains("armazen")) return "Armazenamento usado: " + storage + "%. Tente manter ao menos 15% livre.";
            if (z.contains("root")) return hasRoot() ? "Root confirmado." : "Root não foi confirmado. ADB Advanced ainda libera os ajustes suportados.";
            return t >= 42 ? "Primeiro cuide da temperatura." : "Rode o Deep Scan para cruzar RAM, armazenamento e temperatura.";
        } catch (Throwable e) { return "Não consegui ler todos os sensores agora."; }
    }

    private String turbo() {
        boolean adv = hasSecureSettings();
        boolean rooted = hasRoot();
        boolean ok = false;
        if (adv) ok |= setAnimations(.5f);
        if (rooted) ok |= root("am kill-all");
        if (!adv && !rooted) return result(false, "Turbo Boost precisa de acesso", "Sem ADB Advanced ou root, o Android bloqueia os ajustes profundos.", "ADB: pm grant com.gothtime.optimizer android.permission.WRITE_SECURE_SETTINGS");
        return result(ok, "Turbo Boost", ok ? "Perfil Turbo aplicado." : "Parte dos ajustes foi recusada.", rooted ? "Animações 0,5x · sessão limpa com root" : "Animações 0,5x");
    }

    private String restore() {
        boolean ok = false;
        if (hasSecureSettings()) ok |= setAnimations(1f);
        if (hasRoot()) ok |= root("settings put global low_power 0");
        return result(ok, "Padrões restaurados", ok ? "Ajustes revertidos." : "ADB Advanced ou root é necessário.", "");
    }

    private String cleanCache() {
        deleteChildren(context.getCacheDir());
        deleteChildren(context.getExternalCacheDir());
        boolean deep = hasRoot() && root("pm trim-caches 999G");
        return result(true, "Clean Cache", deep ? "Cache local e caches elegíveis do sistema foram solicitados para limpeza." : "Cache do GothTime foi limpo. Outros apps continuam protegidos pelo Android.", deep ? "Root ativo" : "Modo seguro");
    }

    private String batterySaver() {
        boolean ok = false;
        if (hasRoot()) ok = root("settings put global low_power 1");
        if (!ok && hasSecureSettings()) {
            try { Settings.Global.putInt(context.getContentResolver(), "low_power", 1); ok = true; } catch (Throwable ignored) {}
        }
        return result(ok, "Battery Saver", ok ? "Perfil de economia solicitado ao Android." : "Conceda ADB Advanced ou root para alteração direta.", "");
    }

    private String gameMode() {
        boolean adv = hasSecureSettings();
        boolean rooted = hasRoot();
        boolean ok = false;
        if (adv) ok |= setAnimations(.5f);
        if (rooted) {
            ok |= root("settings put global low_power 0");
            ok |= root("am kill-all");
        }
        if (!adv && !rooted) return result(false, "Game Mode precisa de acesso", "Sem ADB Advanced ou root eu não vou fingir que alterei o sistema.", "A proteção térmica permanece ativa.");
        return result(ok, "Game Mode", ok ? "Perfil de jogo aplicado." : "Parte do perfil foi recusada.", rooted ? "Root ativo" : "ADB Advanced");
    }

    private String scan() {
        try {
            JSONObject s = new JSONObject(getState());
            int ram = pct(s.optLong("ramUsed"), s.optLong("ramTotal"));
            int storage = pct(s.optLong("storageUsed"), s.optLong("storageTotal"));
            double temp = Double.parseDouble(s.optString("temp", "0"));
            int score = 100;
            if (ram > 75) score -= Math.min(25, ram - 75);
            if (storage > 80) score -= Math.min(25, (storage - 80) * 2);
            if (temp > 39) score -= Math.min(30, (int)Math.round((temp - 39) * 5));
            score = Math.max(20, score);
            String msg = score >= 85 ? "Sistema em boa forma." : score >= 65 ? "Há margem clara para aliviar o aparelho." : "O aparelho está sob pressão agora.";
            return result(true, "Deep Scan", msg, "Nota " + score + "/100 · RAM " + ram + "% · armazenamento " + storage + "% · " + temp + " °C");
        } catch (Throwable e) {
            return result(false, "Deep Scan", "Falha ao ler sensores.", "");
        }
    }

    private String accessStatus() {
        if (hasRoot()) return result(true, "Root confirmado", "O GothTime recebeu acesso root.", "Modo profundo disponível nas ações implementadas.");
        if (hasSecureSettings()) return result(true, "ADB Advanced ativo", "WRITE_SECURE_SETTINGS já foi concedido.", "Turbo, Game Mode e Battery Saver têm acesso ampliado.");
        return result(false, "Modo normal", "Root e ADB Advanced ainda não foram confirmados.", "Use ADB para liberar WRITE_SECURE_SETTINGS sem precisar de root.");
    }

    private boolean setAnimations(float value) {
        try {
            Settings.Global.putFloat(context.getContentResolver(), Settings.Global.WINDOW_ANIMATION_SCALE, value);
            Settings.Global.putFloat(context.getContentResolver(), Settings.Global.TRANSITION_ANIMATION_SCALE, value);
            Settings.Global.putFloat(context.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, value);
            return true;
        } catch (Throwable e) { return false; }
    }

    private boolean hasSecureSettings() {
        return context.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasUsageAccess() {
        try {
            AppOpsManager op = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            return op.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.getPackageName()) == AppOpsManager.MODE_ALLOWED;
        } catch (Throwable e) { return false; }
    }

    private boolean hasRoot() {
        try {
            java.lang.Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "id"});
            boolean done = p.waitFor(1200, TimeUnit.MILLISECONDS);
            if (!done) { p.destroy(); return false; }
            return p.exitValue() == 0;
        } catch (Throwable e) { return false; }
    }

    private boolean root(String cmd) {
        try {
            java.lang.Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            boolean done = p.waitFor(3500, TimeUnit.MILLISECONDS);
            if (!done) { p.destroy(); return false; }
            return p.exitValue() == 0;
        } catch (Throwable e) { return false; }
    }

    private void scheduleMaintenance(boolean on) {
        try {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            PendingIntent pi = PendingIntent.getBroadcast(context, 2107, new Intent(context, MaintenanceReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            am.cancel(pi);
            if (on) am.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 43200000L, AlarmManager.INTERVAL_DAY, pi);
        } catch (Throwable ignored) {}
    }

    private static int pct(long used, long total) {
        return total <= 0 ? 0 : (int)Math.max(0, Math.min(100, Math.round(used * 100.0 / total)));
    }

    private static void deleteChildren(File f) {
        if (f == null || !f.exists()) return;
        File[] files = f.listFiles();
        if (files == null) return;
        for (File x : files) deleteRec(x);
    }

    private static void deleteRec(File f) {
        if (f == null) return;
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null) for (File x : files) deleteRec(x);
        }
        try { f.delete(); } catch (Throwable ignored) {}
    }

    private static String result(boolean ok, String title, String message, String detail) {
        try {
            JSONObject o = new JSONObject();
            o.put("ok", ok);
            o.put("title", title);
            o.put("message", message);
            o.put("detail", detail);
            return o.toString();
        } catch (Throwable e) {
            return "{\"ok\":false,\"title\":\"GothTime\",\"message\":\"Erro interno\",\"detail\":\"\"}";
        }
    }
}
