package com.gothtime.optimizer;

import android.Manifest;
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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
import android.os.StatFs;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private WebView webView;
    private static final String PREFS = "gothtime_prefs";
    private static final String PREF_AUTO = "auto_maintenance";
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(android.graphics.Color.rgb(8, 7, 11));
        getWindow().setNavigationBarColor(android.graphics.Color.rgb(8, 7, 11));

        webView = new WebView(this);
        setContentView(webView);

        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setAllowFileAccess(true);
        ws.setAllowContentAccess(false);
        ws.setDatabaseEnabled(true);
        ws.setBuiltInZoomControls(false);
        ws.setDisplayZoomControls(false);
        ws.setTextZoom(100);

        webView.setBackgroundColor(android.graphics.Color.rgb(8, 7, 11));
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new GothTimeBridge(this), "GothTime");
        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    public static void scheduleMaintenance(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_AUTO, true).apply();

        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, MaintenanceReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                300,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        long first = System.currentTimeMillis() + DAY_MS;
        if (alarm != null) {
            alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, first, DAY_MS, pi);
        }
    }

    public static void cancelMaintenance(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(PREF_AUTO, false).apply();
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, MaintenanceReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                300,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        if (alarm != null) alarm.cancel(pi);
    }

    private static class GothTimeBridge {
        private final Activity activity;
        private final Context context;

        GothTimeBridge(Activity activity) {
            this.activity = activity;
            this.context = activity.getApplicationContext();
        }

        @JavascriptInterface
        public String getState() {
            JSONObject out = new JSONObject();
            try {
                Intent battery = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                int level = battery != null ? battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
                int scale = battery != null ? battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100) : 100;
                int pct = scale > 0 ? Math.round(level * 100f / scale) : level;
                int rawTemp = battery != null ? battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) : 0;
                float temp = rawTemp / 10f;
                int voltage = battery != null ? battery.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) : 0;
                int health = battery != null ? battery.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN) : BatteryManager.BATTERY_HEALTH_UNKNOWN;
                int status = battery != null ? battery.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN) : BatteryManager.BATTERY_STATUS_UNKNOWN;
                int plugged = battery != null ? battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) : 0;

                ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                if (am != null) am.getMemoryInfo(mi);
                long totalRam = mi.totalMem;
                long freeRam = mi.availMem;
                int ramPct = totalRam > 0 ? (int) Math.round((totalRam - freeRam) * 100d / totalRam) : 0;

                StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
                long totalStorage = stat.getTotalBytes();
                long freeStorage = stat.getAvailableBytes();
                int storagePct = totalStorage > 0 ? (int) Math.round((totalStorage - freeStorage) * 100d / totalStorage) : 0;

                boolean secure = hasSecurePermission();
                boolean usage = hasUsageAccess();
                boolean auto = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(PREF_AUTO, false);
                long lastMaintenance = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong("last_maintenance", 0L);

                int score = 100;
                if (ramPct >= 85) score -= 18; else if (ramPct >= 70) score -= 8;
                if (storagePct >= 92) score -= 22; else if (storagePct >= 85) score -= 10;
                if (temp >= 45f) score -= 24; else if (temp >= 40f) score -= 10;
                if (pct >= 0 && pct <= 15) score -= 8;
                if (score < 0) score = 0;

                out.put("model", Build.MODEL);
                out.put("manufacturer", Build.MANUFACTURER);
                out.put("android", Build.VERSION.RELEASE);
                out.put("sdk", Build.VERSION.SDK_INT);
                out.put("hardware", Build.HARDWARE);
                out.put("battery", pct);
                out.put("temperature", temp);
                out.put("voltage", voltage);
                out.put("batteryHealth", batteryHealthText(health));
                out.put("charging", status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL);
                out.put("chargingSource", pluggedText(plugged));
                out.put("ramPercent", ramPct);
                out.put("ramUsed", totalRam - freeRam);
                out.put("ramTotal", totalRam);
                out.put("storagePercent", storagePct);
                out.put("storageUsed", totalStorage - freeStorage);
                out.put("storageTotal", totalStorage);
                out.put("advanced", secure);
                out.put("usageAccess", usage);
                out.put("autoOptimize", auto);
                out.put("lastMaintenance", lastMaintenance);
                out.put("score", score);
                out.put("root", false);
                out.put("version", "3.0.0");
                out.put("mood", moodFor(pct, temp, ramPct, storagePct, score));
            } catch (Exception e) {
                try {
                    out.put("error", e.getClass().getSimpleName());
                } catch (JSONException ignored) {}
            }
            return out.toString();
        }

        @JavascriptInterface
        public String runAction(String action) {
            String a = action == null ? "" : action.toLowerCase(Locale.ROOT);
            try {
                switch (a) {
                    case "turbo":
                        if (!hasSecurePermission()) {
                            return result(false, "ADB Advanced necessário", "Conceda WRITE_SECURE_SETTINGS uma vez pelo ADB para o Turbo alterar as animações.");
                        }
                        setAnimations(0.5f);
                        return result(true, "Turbo Boost", "Animações do Android ajustadas para 0,5x.");
                    case "restore":
                        if (!hasSecurePermission()) {
                            return result(false, "Restauração bloqueada", "WRITE_SECURE_SETTINGS não está ativo. Sem essa permissão o GothTime não pode restaurar as escalas do Android.");
                        }
                        setAnimations(1.0f);
                        return result(true, "Padrões restaurados", "As três escalas de animação voltaram para 1x.");
                    case "clean":
                    case "clean_cache":
                        long before = directorySize(context.getCacheDir());
                        deleteChildren(context.getCacheDir());
                        return result(true, "Cache do GothTime limpo", "Foram liberados " + humanBytes(before) + ". Outros apps continuam protegidos pelo Android.");
                    case "battery_saver":
                    case "battery":
                        openIntent(new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS));
                        return result(true, "Economia de bateria", "Abri a tela oficial do Android. Ative ou ajuste o perfil de economia por lá.");
                    case "game":
                    case "game_mode":
                        if (hasSecurePermission()) setAnimations(0.5f);
                        boolean opened = openGameTurbo();
                        return result(true, "Game Mode", opened ? "Perfil visual 0,5x aplicado e Game Turbo aberto." : "Perfil visual 0,5x aplicado. O Game Turbo da MIUI não respondeu neste aparelho.");
                    case "deep_scan":
                        return deepScan();
                    case "usage":
                    case "usage_access":
                        requestUsageAccess();
                        return result(true, "Acesso de uso", "Abri a permissão de acesso ao uso dos aplicativos.");
                    case "developer":
                        openIntent(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
                        return result(true, "Opções do desenvolvedor", "Tela aberta.");
                    case "storage":
                        openIntent(new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS));
                        return result(true, "Armazenamento", "Tela do sistema aberta.");
                    case "app_settings":
                    case "configure":
                        openAppSettings();
                        return result(true, "Configurações do GothTime", "Tela oficial do aplicativo aberta.");
                    case "schedule_on":
                        MainActivity.scheduleMaintenance(context);
                        return result(true, "Otimização programada", "Manutenção leve diária ativada. O Android poderá flexibilizar o horário para economizar bateria.");
                    case "schedule_off":
                        MainActivity.cancelMaintenance(context);
                        return result(true, "Otimização programada", "Manutenção diária desativada.");
                    default:
                        return result(false, "Ação desconhecida", "Esta ação ainda não existe no GothTime 3.");
                }
            } catch (Exception e) {
                return result(false, "Não foi possível concluir", e.getClass().getSimpleName() + ": " + safeMessage(e));
            }
        }

        @JavascriptInterface
        public boolean getAutoOptimize() {
            return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(PREF_AUTO, false);
        }

        @JavascriptInterface
        public String setAutoOptimize(boolean enabled) {
            if (enabled) MainActivity.scheduleMaintenance(context); else MainActivity.cancelMaintenance(context);
            return result(true, "Otimização programada", enabled ? "Manutenção leve diária ativada." : "Manutenção diária desativada.");
        }

        @JavascriptInterface
        public void requestUsageAccess() {
            openIntent(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }

        @JavascriptInterface
        public String topApps() {
            JSONArray arr = new JSONArray();
            if (!hasUsageAccess()) return arr.toString();
            try {
                UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
                if (usm == null) return arr.toString();
                long end = System.currentTimeMillis();
                long start = end - DAY_MS;
                List<UsageStats> stats = new ArrayList<>(usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end));
                Collections.sort(stats, new Comparator<UsageStats>() {
                    @Override public int compare(UsageStats a, UsageStats b) {
                        return Long.compare(b.getTotalTimeInForeground(), a.getTotalTimeInForeground());
                    }
                });
                int count = 0;
                for (UsageStats s : stats) {
                    if (s.getTotalTimeInForeground() <= 0) continue;
                    JSONObject item = new JSONObject();
                    String pkg = s.getPackageName();
                    item.put("pkg", pkg);
                    item.put("label", appLabel(pkg));
                    item.put("ms", s.getTotalTimeInForeground());
                    arr.put(item);
                    count++;
                    if (count >= 12) break;
                }
            } catch (Exception ignored) {}
            return arr.toString();
        }

        @JavascriptInterface
        public String hibernatePackage(String pkg) {
            if (pkg == null || pkg.trim().isEmpty()) return result(false, "App inválido", "Pacote não informado.");
            try {
                Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + pkg));
                openIntent(i);
                return result(true, "Gerenciar aplicativo", "O Android não permite que um app comum force o encerramento de outro. Abri os controles oficiais deste aplicativo para você restringir bateria ou forçar parada.");
            } catch (Exception e) {
                return result(false, "Não foi possível abrir", safeMessage(e));
            }
        }

        @JavascriptInterface
        public String ask(String question) {
            String q = question == null ? "" : question.trim().toLowerCase(Locale.ROOT);
            JSONObject state;
            try { state = new JSONObject(getState()); } catch (Exception e) { state = new JSONObject(); }
            int battery = state.optInt("battery", -1);
            int ram = state.optInt("ramPercent", 0);
            int storage = state.optInt("storagePercent", 0);
            double temp = state.optDouble("temperature", 0);
            int score = state.optInt("score", 0);

            if (q.isEmpty()) return "Pode perguntar sobre bateria, temperatura, RAM, armazenamento, jogos, Turbo ou permissões.";
            if (containsAny(q, "oi", "olá", "ola", "hey", "eai", "e aí", "bom dia", "boa tarde", "boa noite")) {
                return "Oi. Estou monitorando o aparelho. Agora o score está em " + score + "/100. Pergunte por bateria, RAM, temperatura ou armazenamento.";
            }
            if (containsAny(q, "bateria", "carga", "carreg")) {
                return "Bateria em " + battery + "% e temperatura em " + oneDecimal(temp) + " °C. O GothTime pode abrir o Battery Saver e acompanhar temperatura, mas software não recupera desgaste químico da bateria.";
            }
            if (containsAny(q, "ram", "memória", "memoria")) {
                return "RAM em uso: " + ram + "%. Fechar apps à força o tempo todo pode piorar consumo. Use a lista de apps para identificar os que ficam mais tempo em primeiro plano.";
            }
            if (containsAny(q, "temperatura", "quente", "calor", "aquec")) {
                String extra = temp >= 45 ? " Está alta, então eu evitaria jogo e carregamento pesado até esfriar." : temp >= 40 ? " Está morna/alta, vale observar durante jogos." : " Está em uma faixa tranquila para uso normal.";
                return "Temperatura atual: " + oneDecimal(temp) + " °C." + extra;
            }
            if (containsAny(q, "armazenamento", "espaço", "espaco", "storage")) {
                return "Armazenamento usado: " + storage + "%. O Clean Cache limpa apenas os arquivos temporários do próprio GothTime; para outros apps o Android mantém proteção.");
            }
            if (containsAny(q, "jogo", "game", "fps")) {
                return "O Game Mode reduz animações para 0,5x quando o ADB Advanced está ativo e tenta abrir o Game Turbo da MIUI. Ele não faz overclock nem desativa proteção térmica.";
            }
            if (containsAny(q, "turbo", "anima", "rápido", "rapido", "lent")) {
                return state.optBoolean("advanced", false)
                        ? "ADB Advanced está ativo. O Turbo pode alterar as três escalas de animação para 0,5x, e Restaurar padrões volta todas para 1x."
                        : "O Turbo precisa do WRITE_SECURE_SETTINGS concedido uma vez por ADB para alterar as escalas de animação.";
            }
            if (containsAny(q, "root")) {
                return "Esta versão do GothTime não depende de root. Eu não vou afirmar que há root no aparelho sem uma detecção confiável.";
            }
            if (containsAny(q, "ajuda", "o que você faz", "o que voce faz")) {
                return "Eu leio os dados que o Android disponibiliza ao GothTime e explico bateria, temperatura, RAM, armazenamento, permissões e modos do aplicativo.";
            }
            return "Não entendi esse assunto ainda. Posso responder sobre bateria, temperatura, RAM, armazenamento, jogos, Turbo, ADB Advanced e manutenção programada.";
        }

        private String deepScan() {
            try {
                JSONObject s = new JSONObject(getState());
                int ram = s.optInt("ramPercent", 0);
                int storage = s.optInt("storagePercent", 0);
                double temp = s.optDouble("temperature", 0);
                int battery = s.optInt("battery", -1);
                JSONArray notes = new JSONArray();
                if (ram >= 80) notes.put("RAM acima de 80%: revise apps com maior tempo de uso.");
                if (storage >= 90) notes.put("Armazenamento acima de 90%: libere espaço para reduzir pressão no sistema.");
                if (temp >= 45) notes.put("Temperatura alta: evite carga pesada até o aparelho esfriar.");
                if (battery >= 0 && battery <= 15) notes.put("Bateria baixa: considere ativar economia de bateria.");
                if (notes.length() == 0) notes.put("Nenhum gargalo evidente foi detectado nos indicadores disponíveis ao GothTime.");
                JSONObject out = new JSONObject();
                out.put("ok", true);
                out.put("title", "Deep Scan concluído");
                out.put("message", notes.join("\n"));
                out.put("scan", true);
                out.put("score", s.optInt("score", 0));
                return out.toString();
            } catch (Exception e) {
                return result(false, "Deep Scan", safeMessage(e));
            }
        }

        private boolean hasSecurePermission() {
            return context.checkPermission(Manifest.permission.WRITE_SECURE_SETTINGS, Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_GRANTED;
        }

        private boolean hasUsageAccess() {
            try {
                AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
                if (appOps == null) return false;
                int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.getPackageName());
                return mode == AppOpsManager.MODE_ALLOWED;
            } catch (Exception e) {
                return false;
            }
        }

        private void setAnimations(float value) {
            Settings.Global.putFloat(context.getContentResolver(), Settings.Global.WINDOW_ANIMATION_SCALE, value);
            Settings.Global.putFloat(context.getContentResolver(), Settings.Global.TRANSITION_ANIMATION_SCALE, value);
            Settings.Global.putFloat(context.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, value);
        }

        private boolean openGameTurbo() {
            String[][] targets = new String[][] {
                    {"com.miui.securitycenter", "com.miui.gamebooster.ui.GameBoosterMainActivity"},
                    {"com.miui.securitycenter", "com.miui.gamebooster.ui.SelectGameActivity"}
            };
            for (String[] t : targets) {
                try {
                    Intent i = new Intent();
                    i.setClassName(t[0], t[1]);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(i);
                    return true;
                } catch (Exception ignored) {}
            }
            return false;
        }

        private void openAppSettings() {
            openIntent(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.getPackageName())));
        }

        private void openIntent(Intent i) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try { context.startActivity(i); } catch (Exception ignored) {}
        }

        private String appLabel(String pkg) {
            try {
                PackageManager pm = context.getPackageManager();
                return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
            } catch (Exception e) {
                return pkg;
            }
        }

        private static boolean containsAny(String text, String... terms) {
            for (String t : terms) if (text.contains(t)) return true;
            return false;
        }

        private static String result(boolean ok, String title, String message) {
            JSONObject o = new JSONObject();
            try {
                o.put("ok", ok);
                o.put("title", title);
                o.put("message", message);
            } catch (JSONException ignored) {}
            return o.toString();
        }

        private static String moodFor(int battery, float temp, int ram, int storage, int score) {
            if (temp >= 45f) return "alert";
            if (battery >= 0 && battery <= 15) return "worried";
            if (ram >= 85 || storage >= 92) return "focused";
            if (score >= 90) return "calm";
            return "neutral";
        }

        private static String batteryHealthText(int health) {
            switch (health) {
                case BatteryManager.BATTERY_HEALTH_GOOD: return "Boa";
                case BatteryManager.BATTERY_HEALTH_OVERHEAT: return "Superaquecida";
                case BatteryManager.BATTERY_HEALTH_DEAD: return "Ruim";
                case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE: return "Sobretensão";
                case BatteryManager.BATTERY_HEALTH_COLD: return "Fria";
                default: return "Desconhecida";
            }
        }

        private static String pluggedText(int plugged) {
            if (plugged == BatteryManager.BATTERY_PLUGGED_AC) return "Tomada";
            if (plugged == BatteryManager.BATTERY_PLUGGED_USB) return "USB";
            if (plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS) return "Sem fio";
            return "Bateria";
        }

        private static String oneDecimal(double n) {
            return String.format(Locale.US, "%.1f", n);
        }

        private static String safeMessage(Exception e) {
            return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        }

        private static long directorySize(File file) {
            if (file == null || !file.exists()) return 0;
            if (file.isFile()) return file.length();
            long total = 0;
            File[] files = file.listFiles();
            if (files != null) for (File f : files) total += directorySize(f);
            return total;
        }

        private static void deleteChildren(File dir) {
            if (dir == null || !dir.exists()) return;
            File[] files = dir.listFiles();
            if (files == null) return;
            for (File f : files) {
                if (f.isDirectory()) deleteChildren(f);
                try { f.delete(); } catch (Exception ignored) {}
            }
        }

        private static String humanBytes(long b) {
            if (b < 1024) return b + " B";
            double kb = b / 1024d;
            if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb);
            double mb = kb / 1024d;
            return String.format(Locale.US, "%.1f MB", mb);
        }
    }
}
