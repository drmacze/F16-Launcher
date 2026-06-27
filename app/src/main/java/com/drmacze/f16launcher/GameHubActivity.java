package com.drmacze.f16launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import rikka.shizuku.Shizuku;

public class GameHubActivity extends Activity {
    private static final String GAME_PACKAGE = "com.ea.gp.fifaworld";
    private static final String DEFAULT_MANIFEST = "https://raw.githubusercontent.com/drmacze/F16/main/updates/latest.json";
    private static final String PREFS = "f16_launcher";
    private static final int SHIZUKU_REQ = 1601;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final StringBuilder logBuffer = new StringBuilder("Siap digunakan.");
    private final List<Button> navButtons = new ArrayList<>();

    private SharedPreferences prefs;
    private LinearLayout shell;
    private LinearLayout nav;
    private LinearLayout content;
    private EditText manifestUrl;
    private TextView statusView;
    private TextView latestView;
    private TextView logView;
    private Button updateBtn;
    private JSONObject manifest;
    private String currentPage = "home";

    private boolean dark;
    private int bg, glass, glass2, glass3, text, muted, border, primary, candy, green, yellow, danger;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (!prefs.contains("local_version_code")) prefs.edit().putInt("local_version_code", 1).apply();
        applyThemeVars();
        immersive();
        buildShell();
        showHome();
        refresh();
        if (prefs.getBoolean("auto_check", false)) main.postDelayed(() -> checkUpdate(), 700);
    }

    private void immersive() {
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    private void applyThemeVars() {
        dark = prefs.getBoolean("theme_dark", true);
        primary = Color.rgb(0, 198, 255);
        candy = Color.rgb(72, 116, 255);
        green = Color.rgb(31, 221, 144);
        yellow = Color.rgb(255, 198, 58);
        danger = Color.rgb(255, 82, 105);
        if (dark) {
            bg = Color.rgb(4, 7, 13);
            glass = Color.argb(218, 16, 22, 36);
            glass2 = Color.argb(225, 24, 32, 52);
            glass3 = Color.argb(235, 35, 47, 78);
            text = Color.rgb(247, 250, 255);
            muted = Color.rgb(158, 173, 204);
            border = Color.argb(135, 98, 143, 255);
        } else {
            bg = Color.rgb(230, 241, 250);
            glass = Color.argb(220, 255, 255, 255);
            glass2 = Color.argb(230, 240, 249, 255);
            glass3 = Color.argb(240, 223, 241, 255);
            text = Color.rgb(17, 25, 40);
            muted = Color.rgb(89, 105, 132);
            border = Color.argb(165, 76, 139, 255);
        }
    }

    private void buildShell() {
        navButtons.clear();
        shell = row();
        shell.setBackgroundColor(bg);
        shell.setPadding(dp(14), dp(12), dp(14), dp(12));

        nav = col();
        nav.setPadding(dp(12), dp(14), dp(12), dp(14));
        nav.setBackground(glassRound(28, dark ? Color.argb(175, 14, 20, 34) : Color.argb(190, 255, 255, 255)));
        shell.addView(nav, new LinearLayout.LayoutParams(dp(190), LinearLayout.LayoutParams.MATCH_PARENT));

        TextView logo = txt("DL", 26, primary, true);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(strokeRound(18, dark ? Color.argb(145, 0, 198, 255) : Color.argb(100, 0, 198, 255), border));
        nav.addView(logo, new LinearLayout.LayoutParams(dp(58), dp(58)));
        margin(logo, 0, 0, 0, dp(10));
        nav.addView(txt("DLavie Hub", 17, text, true));
        nav.addView(txt("Carbon + Candy Blue", 11, muted, false));
        nav.addView(space(1, dp(18)));
        nav.addView(navButton("⌂  Home", "home"));
        nav.addView(navButton("◉  Community", "community"));
        nav.addView(navButton("◇  Upgrade Plan", "plan"));
        nav.addView(navButton("☻  Profile", "profile"));
        nav.addView(space(1, dp(12)), new LinearLayout.LayoutParams(1, 0, 1));
        TextView mode = txt(dark ? "Dark liquid glass" : "Light liquid glass", 11, muted, false);
        nav.addView(mode);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = col();
        content.setPadding(dp(16), 0, 0, 0);
        scroll.addView(content);
        shell.addView(scroll, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1));
        setContentView(shell);
    }

    private Button navButton(String label, String key) {
        Button b = btn(label, glass2, text);
        b.setGravity(Gravity.CENTER_VERTICAL);
        b.setTag(key);
        b.setOnClickListener(v -> {
            String k = String.valueOf(v.getTag());
            if ("home".equals(k)) showHome();
            else if ("community".equals(k)) showCommunity();
            else if ("plan".equals(k)) showPlan();
            else showProfile();
        });
        navButtons.add(b);
        return b;
    }

    private void select(String page) {
        currentPage = page;
        for (Button b : navButtons) {
            boolean on = page.equals(String.valueOf(b.getTag()));
            b.setTextColor(on ? Color.WHITE : text);
            b.setBackground(on ? gradRound(18, candy, primary) : glassRound(18, glass2));
        }
    }

    private void clear(String page) {
        content.removeAllViews();
        statusView = null;
        latestView = null;
        logView = null;
        updateBtn = null;
        select(page);
    }

    private void showHome() {
        clear("home");
        LinearLayout top = row();
        content.addView(top, mp());

        LinearLayout left = col();
        left.setPadding(dp(20), dp(18), dp(20), dp(18));
        left.setBackground(gradRound(30, dark ? Color.rgb(24, 35, 61) : Color.rgb(220, 242, 255), dark ? Color.rgb(8, 12, 24) : Color.rgb(245, 252, 255)));
        top.addView(left, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.25f));
        margin(left, 0, 0, dp(14), 0);

        TextView tag = txt("DLAVIE MOD CENTER", 12, primary, true);
        tag.setLetterSpacing(0.12f);
        left.addView(tag);
        TextView title = txt("FIFA 16 Mobile", 36, text, true);
        margin(title, 0, dp(8), 0, 0);
        left.addView(title);
        TextView desc = txt("Landscape game hub untuk update patch kecil, backup aman, restore cepat, dan launch game.", 14, muted, false);
        margin(desc, 0, dp(5), 0, dp(14));
        left.addView(desc);

        LinearLayout stats = row();
        left.addView(stats, mp());
        stats.addView(infoTile("MOD VERSION", "v" + prefs.getInt("local_version_code", 1)), weight(1));
        stats.addView(space(dp(10), 1));
        stats.addView(infoTile("PATCH ENGINE", "Delta only"), weight(1));
        stats.addView(space(dp(10), 1));
        stats.addView(infoTile("BACKUP", "Auto-safe"), weight(1));

        Button launch = btn("▶  Launch FIFA 16", green, Color.rgb(1, 20, 12));
        launch.setTextSize(18);
        margin(launch, 0, dp(18), 0, 0);
        launch.setOnClickListener(v -> launchGame());
        left.addView(launch, mp());
        Button checkHero = btn("Check Update", candy, Color.WHITE);
        checkHero.setOnClickListener(v -> checkUpdate());
        left.addView(checkHero, mp());

        LinearLayout right = col();
        top.addView(right, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        LinearLayout update = glassCard();
        right.addView(update, mp());
        update.addView(section("UPDATE SOURCE"));
        manifestUrl = new EditText(this);
        manifestUrl.setText(prefs.getString("manifest_url", DEFAULT_MANIFEST));
        manifestUrl.setSingleLine(false);
        manifestUrl.setMinLines(2);
        manifestUrl.setTextSize(12);
        manifestUrl.setTextColor(text);
        manifestUrl.setHintTextColor(muted);
        manifestUrl.setPadding(dp(12), dp(8), dp(12), dp(8));
        manifestUrl.setBackground(strokeRound(16, dark ? Color.argb(135, 8, 13, 25) : Color.argb(150, 255, 255, 255), border));
        margin(manifestUrl, 0, dp(8), 0, 0);
        update.addView(manifestUrl, mp());
        LinearLayout row = row();
        update.addView(row, mp());
        Button save = btn("Save URL", glass2, text);
        save.setOnClickListener(v -> { prefs.edit().putString("manifest_url", manifestUrl.getText().toString().trim()).apply(); say("URL update disimpan."); });
        row.addView(save, weight(1)); margin(save, 0, dp(10), dp(8), 0);
        Button check = btn("Check", candy, Color.WHITE);
        check.setOnClickListener(v -> checkUpdate());
        row.addView(check, weight(1)); margin(check, 0, dp(10), 0, 0);
        updateBtn = btn("Update Now", primary, Color.rgb(2, 18, 31));
        updateBtn.setEnabled(false);
        updateBtn.setAlpha(0.45f);
        updateBtn.setOnClickListener(v -> applyUpdate());
        update.addView(updateBtn, mp());
        latestView = txt("Latest: belum dicek", 12, muted, false);
        margin(latestView, 0, dp(8), 0, 0);
        update.addView(latestView);

        LinearLayout statusCard = glassCard();
        margin(statusCard, 0, dp(12), 0, 0);
        right.addView(statusCard, mp());
        statusCard.addView(section("SYSTEM STATUS"));
        statusView = txt("Memuat status...", 14, text, false);
        margin(statusView, 0, dp(8), 0, 0);
        statusCard.addView(statusView);

        LinearLayout tools = row();
        margin(tools, 0, dp(12), 0, 0);
        right.addView(tools, mp());
        Button sh = btn("Shizuku", glass2, text); sh.setOnClickListener(v -> askShizuku()); tools.addView(sh, weight(1)); margin(sh,0,0,dp(8),0);
        Button restore = btn("Restore", glass2, text); restore.setOnClickListener(v -> restoreBackup()); tools.addView(restore, weight(1)); margin(restore,0,0,dp(8),0);
        Button base = btn("Base v1", glass2, text); base.setOnClickListener(v -> { prefs.edit().putInt("local_version_code", 1).apply(); say("Versi lokal diset ke v1."); refresh(); }); tools.addView(base, weight(1));

        LinearLayout logCard = glassCard();
        margin(logCard, 0, dp(12), 0, 0);
        right.addView(logCard, mp());
        logCard.addView(section("ACTIVITY LOG"));
        logView = txt(logBuffer.toString(), 12, muted, false);
        margin(logView, 0, dp(8), 0, 0);
        logCard.addView(logView);
        refresh();
    }

    private void showCommunity() {
        clear("community");
        content.addView(pageHeader("Community", "Link nyata untuk source, report issue, dan update mod. Item tanpa data tidak ditampilkan."));
        LinearLayout grid = row(); content.addView(grid, mp());
        LinearLayout left = glassCard(); grid.addView(left, weight(1)); margin(left,0,0,dp(12),0);
        left.addView(section("OFFICIAL LINKS"));
        left.addView(linkButton("Open Launcher Repo", "https://github.com/drmacze/F16-Launcher"));
        left.addView(linkButton("Open Mod Data Repo", "https://github.com/drmacze/F16"));
        left.addView(linkButton("Report Issue / Request Feature", "https://github.com/drmacze/F16-Launcher/issues"));
        LinearLayout right = glassCard(); grid.addView(right, weight(1));
        right.addView(section("COMMUNITY STATUS"));
        right.addView(detail("GitHub Launcher", "Aktif"));
        right.addView(detail("GitHub Mod Data", "Aktif"));
        right.addView(detail("Telegram / Discord / WhatsApp", "Belum diset. Section disembunyikan sampai ada link asli."));
    }

    private void showPlan() {
        clear("plan");
        content.addView(pageHeader("Upgrade Plan", "Subscription sengaja dikosongkan dulu sesuai arahan kamu."));
        LinearLayout card = glassCard(); content.addView(card, mp());
        TextView big = txt("Coming Soon", 34, primary, true); card.addView(big);
        TextView p = txt("Sistem subscription belum aktif. Tidak ada checkout palsu, benefit palsu, atau tombol pembayaran dummy. Halaman ini akan diisi nanti saat model plan DLavie sudah final.", 15, muted, false);
        margin(p,0,dp(10),0,0); card.addView(p);
        card.addView(detail("Current Access", "Developer / Local Build"));
        card.addView(detail("Subscription Engine", "Belum aktif"));
        card.addView(detail("Premium Content", "Belum dirilis"));
    }

    private void showProfile() {
        clear("profile");
        content.addView(pageHeader("Profile", "Detail developer, app, device, dan settings yang benar-benar berfungsi."));
        LinearLayout grid = row(); content.addView(grid, mp());
        LinearLayout left = glassCard(); grid.addView(left, weight(1)); margin(left,0,0,dp(12),0);
        left.addView(section("PROFILE"));
        TextView avatar = txt("DLavie Developer", 26, text, true); left.addView(avatar);
        left.addView(detail("Role", "FIFA 16 Mobile Mod Developer"));
        left.addView(detail("Device", Build.MANUFACTURER + " " + Build.MODEL));
        left.addView(detail("Android", Build.VERSION.RELEASE + " / SDK " + Build.VERSION.SDK_INT));
        left.addView(detail("Target Game", GAME_PACKAGE));
        left.addView(detail("Local Mod Version", "v" + prefs.getInt("local_version_code", 1)));
        left.addView(detail("Last Backup", emptyDash(prefs.getString("last_backup_root", ""))));

        LinearLayout right = col(); grid.addView(right, weight(1));
        LinearLayout settings = glassCard(); right.addView(settings, mp()); settings.addView(section("SETTINGS"));
        settings.addView(settingButton("Theme: " + (dark ? "Dark" : "Light"), () -> { prefs.edit().putBoolean("theme_dark", !dark).apply(); applyThemeVars(); immersive(); buildShell(); showProfile(); refresh(); }));
        settings.addView(settingButton("Auto Check Update: " + onOff(prefs.getBoolean("auto_check", false)), () -> { toggle("auto_check"); showProfile(); }));
        settings.addView(settingButton("Auto Launch After Patch: " + onOff(prefs.getBoolean("auto_launch", false)), () -> { toggle("auto_launch"); showProfile(); }));
        settings.addView(detail("Force Landscape", "Aktif permanen untuk GameHub layout"));
        settings.addView(detail("Backup Before Patch", "Aktif permanen untuk keamanan mod"));
        LinearLayout actions = row(); settings.addView(actions, mp());
        Button reset = btn("Reset v1", glass2, text); reset.setOnClickListener(v -> { prefs.edit().putInt("local_version_code", 1).apply(); say("Versi lokal diset ke v1."); showProfile(); }); actions.addView(reset, weight(1)); margin(reset,0,dp(10),dp(8),0);
        Button clear = btn("Clear Log", glass2, text); clear.setOnClickListener(v -> { logBuffer.setLength(0); logBuffer.append("Log dibersihkan."); showProfile(); }); actions.addView(clear, weight(1)); margin(clear,0,dp(10),0,0);

        LinearLayout app = glassCard(); margin(app,0,dp(12),0,0); right.addView(app, mp()); app.addView(section("APP DETAILS"));
        app.addView(detail("Launcher Package", getPackageName()));
        app.addView(detail("APK Version", appVersion()));
        app.addView(detail("Installed", appInstallInfo(true)));
        app.addView(detail("Updated", appInstallInfo(false)));
        app.addView(detail("Manifest URL", prefs.getString("manifest_url", DEFAULT_MANIFEST)));
    }

    private LinearLayout pageHeader(String title, String sub) {
        LinearLayout h = glassCard(); margin(h,0,0,0,dp(12));
        h.addView(txt(title, 30, text, true));
        h.addView(txt(sub, 13, muted, false));
        return h;
    }

    private Button linkButton(String title, String url) { Button b = btn(title, glass2, text); b.setOnClickListener(v -> openUrl(url)); return b; }
    private Button settingButton(String title, final Runnable r) { Button b = btn(title, glass2, text); b.setOnClickListener(v -> r.run()); return b; }
    private void toggle(String key) { prefs.edit().putBoolean(key, !prefs.getBoolean(key, false)).apply(); }
    private String onOff(boolean b) { return b ? "On" : "Off"; }
    private String emptyDash(String s) { return (s == null || s.trim().isEmpty()) ? "—" : s; }

    private LinearLayout detail(String label, String value) {
        LinearLayout d = col(); d.setPadding(0, dp(8), 0, 0);
        d.addView(txt(label, 11, muted, true));
        d.addView(txt(value, 13, text, false));
        return d;
    }

    private LinearLayout infoTile(String label, String value) {
        LinearLayout box = col(); box.setPadding(dp(12), dp(10), dp(12), dp(10)); box.setBackground(glassRound(18, glass2));
        box.addView(txt(label, 10, muted, true)); box.addView(txt(value, 15, text, true)); return box;
    }

    private void refresh() {
        io.execute(() -> {
            boolean sh = shizukuOk(), rt = rootOk(); int ver = prefs.getInt("local_version_code", 1);
            String s = "Mod Version: v" + ver + "\nAccess Mode: " + (sh ? "Shizuku aktif" : (rt ? "Root aktif" : "belum aktif")) + "\nTarget: " + GAME_PACKAGE;
            main.post(() -> { if (statusView != null) statusView.setText(s); });
        });
    }

    private void askShizuku() {
        try {
            if (!Shizuku.pingBinder()) { say("Shizuku belum berjalan. Start Shizuku dulu."); return; }
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) { say("Shizuku aktif dan permission sudah diberikan."); refresh(); return; }
            Shizuku.requestPermission(SHIZUKU_REQ);
        } catch (Throwable t) { say("Gagal cek Shizuku: " + t.getMessage()); }
    }
    @Override public void onRequestPermissionsResult(int r, String[] p, int[] g) { super.onRequestPermissionsResult(r,p,g); if (r == SHIZUKU_REQ) { say("Permission Shizuku diperbarui."); refresh(); } }
    private boolean shizukuOk() { try { return Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED; } catch (Throwable t) { return false; } }
    private boolean rootOk() { try { ShellResult r = normal(new String[]{"su","-c","id"}); return r.code == 0 && r.out.contains("uid=0"); } catch (Throwable t) { return false; } }

    private void checkUpdate() {
        if (manifestUrl != null) prefs.edit().putString("manifest_url", manifestUrl.getText().toString().trim()).apply();
        if (updateBtn != null) { updateBtn.setEnabled(false); updateBtn.setAlpha(.45f); }
        say("Mengecek update...");
        io.execute(() -> { try {
            manifest = new JSONObject(textUrl(prefs.getString("manifest_url", DEFAULT_MANIFEST)));
            int local = prefs.getInt("local_version_code", 1), latest = manifest.optInt("latestVersionCode", local); String name = manifest.optString("latestVersionName", "v" + latest);
            main.post(() -> { if (latest > local) { say("Update tersedia: v" + local + " -> " + name); if (updateBtn != null) { updateBtn.setEnabled(true); updateBtn.setAlpha(1f); } } else say("Sudah versi terbaru: v" + local); if (latestView != null) latestView.setText("Latest: " + name); refresh(); });
        } catch (Throwable t) { say("Check update gagal: " + t.getMessage()); } });
    }

    private void applyUpdate() {
        if (updateBtn != null) { updateBtn.setEnabled(false); updateBtn.setAlpha(.45f); }
        say("Mulai update...");
        io.execute(() -> { try {
            if (manifest == null) manifest = new JSONObject(textUrl(prefs.getString("manifest_url", DEFAULT_MANIFEST)));
            int latest = manifest.getInt("latestVersionCode"), local = prefs.getInt("local_version_code", 1);
            while (local < latest) { JSONObject p = findPatch(manifest.getJSONArray("patches"), local); if (p == null) throw new IllegalStateException("Patch v" + local + " tidak ditemukan."); applyPatch(p); local = p.getInt("to"); prefs.edit().putInt("local_version_code", local).apply(); say("Patch selesai. Versi lokal v" + local); }
            say("Semua update selesai."); refresh(); if (prefs.getBoolean("auto_launch", false)) main.postDelayed(() -> launchGame(), 600);
        } catch (Throwable t) { say("Update gagal: " + t.getMessage()); main.post(() -> { if (updateBtn != null) { updateBtn.setEnabled(true); updateBtn.setAlpha(1f); } }); } });
    }
    private JSONObject findPatch(JSONArray a, int from) throws Exception { for (int i=0;i<a.length();i++) { JSONObject p=a.getJSONObject(i); if (p.optInt("from") == from) return p; } return null; }

    private void applyPatch(JSONObject p) throws Exception {
        int to = p.getInt("to"); String target = p.optString("target", "/sdcard/Android/data/com.ea.gp.fifaworld/");
        File work = new File(getExternalFilesDir(null), "updates/v" + to), ex = new File(work, "extracted"); del(work); ex.mkdirs(); List<String> entries = new ArrayList<>();
        if (p.has("files")) { JSONArray fs = p.getJSONArray("files"); String root = ex.getCanonicalPath() + File.separator; for (int i=0;i<fs.length();i++) { JSONObject f = fs.getJSONObject(i); String rel = f.getString("path").replace('\\','/'); if (rel.startsWith("../") || rel.contains("/../")) throw new IllegalStateException("Path tidak aman"); File out = new File(ex, rel); if (!out.getCanonicalPath().startsWith(root)) throw new IllegalStateException("Path tidak aman"); File par = out.getParentFile(); if (par != null) par.mkdirs(); try (FileOutputStream os = new FileOutputStream(out)) { os.write(f.optString("content", "").getBytes("UTF-8")); } entries.add(rel); } say("Inline patch siap: " + entries.size() + " file."); }
        else { File zip = new File(work, "patch.zip"); say("Download " + p.optString("name", "patch") + "..."); fileUrl(p.getString("url"), zip); String sha = p.optString("sha256", "").trim(); if (!sha.isEmpty() && !sha.equalsIgnoreCase(sha256(zip))) throw new IllegalStateException("SHA-256 tidak cocok"); unzip(zip, ex); entries.addAll(zipList(zip)); }
        String backup = "/sdcard/F16Launcher/backups/v" + to + "/" + System.currentTimeMillis(); ShellResult r = priv(copyCmd(ex.getAbsolutePath(), target, backup, entries)); say(r.out.trim().isEmpty() ? "Copy selesai." : r.out.trim()); if (r.code != 0) throw new IllegalStateException("Exit code " + r.code); prefs.edit().putString("last_backup_root", backup).putString("last_backup_target", target).apply();
    }
    private String copyCmd(String src, String target, String backup, List<String> es) { if (!target.endsWith("/")) target += "/"; StringBuilder c = new StringBuilder("set -e; mkdir -p " + q(target) + "; mkdir -p " + q(backup) + "; "); for (String rel: es) { String s=src+"/"+rel, d=target+rel, bd=backup+"/"+rel; c.append("mkdir -p ").append(q(parent(d))).append("; if [ -e ").append(q(d)).append(" ]; then mkdir -p ").append(q(parent(bd))).append("; cp -af ").append(q(d)).append(" ").append(q(bd)).append("; fi; cp -af ").append(q(s)).append(" ").append(q(d)).append("; "); } return c.append("echo 'Applied ").append(es.size()).append(" file(s)';").toString(); }
    private void restoreBackup() { say("Restore backup..."); io.execute(() -> { try { String b = prefs.getString("last_backup_root", ""), t = prefs.getString("last_backup_target", "/sdcard/Android/data/com.ea.gp.fifaworld/"); if (b == null || b.trim().isEmpty()) throw new IllegalStateException("Belum ada backup."); ShellResult r = priv("set -e; mkdir -p " + q(t) + "; cp -af " + q(b + "/.") + " " + q(t) + "; echo 'Restore backup selesai';"); say(r.out.trim()); if (r.code != 0) throw new IllegalStateException("Exit code " + r.code); } catch (Throwable e) { say("Restore gagal: " + e.getMessage()); } }); }

    private ShellResult priv(String cmd) throws Exception { if (shizukuOk()) { say("Apply menggunakan Shizuku..."); java.lang.reflect.Method m = Shizuku.class.getDeclaredMethod("newProcess", String[].class, String[].class, String.class); m.setAccessible(true); return read((Process)m.invoke(null, new Object[]{new String[]{"sh","-c",cmd}, null, null})); } if (rootOk()) { say("Apply menggunakan root/su..."); return normal(new String[]{"su","-c",cmd}); } throw new IllegalStateException("Shizuku/root belum aktif."); }

    private String appVersion() { try { PackageInfo p = getPackageManager().getPackageInfo(getPackageName(), 0); long code = Build.VERSION.SDK_INT >= 28 ? p.getLongVersionCode() : p.versionCode; return p.versionName + " (" + code + ")"; } catch (Throwable t) { return "Unknown"; } }
    private String appInstallInfo(boolean first) { try { PackageInfo p = getPackageManager().getPackageInfo(getPackageName(), 0); long time = first ? p.firstInstallTime : p.lastUpdateTime; return DateFormat.getDateTimeInstance().format(new Date(time)); } catch (Throwable t) { return "Unknown"; } }
    private void openUrl(String url) { try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Throwable t) { say("Gagal membuka link: " + t.getMessage()); } }
    private void launchGame() { Intent i = getPackageManager().getLaunchIntentForPackage(GAME_PACKAGE); if (i != null) startActivity(i); else { say("FIFA 16 tidak ditemukan."); try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + GAME_PACKAGE))); } catch (Throwable ignored) {} } }

    private String textUrl(String u) throws Exception { HttpURLConnection c = (HttpURLConnection)new URL(u).openConnection(); c.setConnectTimeout(20000); c.setReadTimeout(30000); try (BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()))) { StringBuilder s = new StringBuilder(); String l; while ((l = br.readLine()) != null) s.append(l).append('\n'); return s.toString(); } finally { c.disconnect(); } }
    private void fileUrl(String u, File out) throws Exception { HttpURLConnection c = (HttpURLConnection)new URL(u).openConnection(); c.setConnectTimeout(20000); c.setReadTimeout(60000); try (InputStream in = new BufferedInputStream(c.getInputStream()); FileOutputStream os = new FileOutputStream(out)) { byte[] b = new byte[131072]; int n; while ((n = in.read(b)) > 0) os.write(b,0,n); } finally { c.disconnect(); } }
    private void unzip(File z, File d) throws Exception { d.mkdirs(); String root = d.getCanonicalPath() + File.separator; try (ZipInputStream in = new ZipInputStream(new FileInputStream(z))) { ZipEntry e; byte[] b = new byte[131072]; while ((e = in.getNextEntry()) != null) { File o = new File(d, e.getName()); if (!o.getCanonicalPath().startsWith(root)) throw new IllegalStateException("Zip path tidak aman"); if (e.isDirectory()) o.mkdirs(); else { File p = o.getParentFile(); if (p != null) p.mkdirs(); try (FileOutputStream os = new FileOutputStream(o)) { int n; while ((n = in.read(b)) > 0) os.write(b,0,n); } } } } }
    private List<String> zipList(File z) throws Exception { List<String> l = new ArrayList<>(); try (ZipFile f = new ZipFile(z)) { Enumeration<? extends ZipEntry> e = f.entries(); while (e.hasMoreElements()) { ZipEntry x = e.nextElement(); if (!x.isDirectory()) l.add(x.getName().replace('\\','/')); } } return l; }
    private String sha256(File f) throws Exception { MessageDigest md = MessageDigest.getInstance("SHA-256"); try (InputStream in = new FileInputStream(f)) { byte[] b = new byte[131072]; int n; while ((n = in.read(b)) > 0) md.update(b,0,n); } StringBuilder s = new StringBuilder(); for (byte x: md.digest()) s.append(String.format(Locale.US, "%02x", x)); return s.toString(); }
    private ShellResult normal(String[] cmd) throws Exception { return read(new ProcessBuilder(cmd).redirectErrorStream(true).start()); }
    private ShellResult read(Process p) throws Exception { StringBuilder o = new StringBuilder(); try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) { String l; while ((l = br.readLine()) != null) o.append(l).append('\n'); } return new ShellResult(p.waitFor(), o.toString()); }
    private void del(File f) { if (f == null || !f.exists()) return; if (f.isDirectory()) { File[] a = f.listFiles(); if (a != null) for (File x: a) del(x); } f.delete(); }
    private void say(String s) { main.post(() -> { logBuffer.append('\n').append(s); if (logView != null) logView.setText(logBuffer.toString()); }); }

    private LinearLayout row() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); return l; }
    private LinearLayout col() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout glassCard() { LinearLayout l = col(); l.setPadding(dp(16), dp(14), dp(16), dp(14)); l.setBackground(strokeRound(24, glass, border)); return l; }
    private TextView section(String s) { TextView t = txt(s, 11, muted, true); t.setLetterSpacing(.12f); return t; }
    private TextView txt(String s, int sp, int c, boolean b) { TextView v = new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(c); if (b) v.setTypeface(Typeface.DEFAULT_BOLD); return v; }
    private Button btn(String s, int bgc, int fg) { Button b = new Button(this); b.setAllCaps(false); b.setText(s); b.setTextSize(14); b.setTextColor(fg); b.setTypeface(Typeface.DEFAULT_BOLD); b.setPadding(dp(10), dp(8), dp(10), dp(8)); b.setMinHeight(dp(42)); b.setBackground(glassRound(18, bgc)); margin(b,0,dp(10),0,0); return b; }
    private GradientDrawable glassRound(int r, int c) { GradientDrawable d = new GradientDrawable(); d.setColor(c); d.setCornerRadius(dp(r)); return d; }
    private GradientDrawable strokeRound(int r, int c, int s) { GradientDrawable d = glassRound(r,c); d.setStroke(dp(1), s); return d; }
    private GradientDrawable gradRound(int r, int a, int b) { GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{a,b}); d.setCornerRadius(dp(r)); return d; }
    private LinearLayout.LayoutParams mp() { return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); }
    private LinearLayout.LayoutParams weight(float w) { return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, w); }
    private View space(int w, int h) { View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(w,h)); return v; }
    private void margin(View v, int l, int t, int r, int b) { LinearLayout.LayoutParams p = (LinearLayout.LayoutParams)v.getLayoutParams(); if (p == null) p = mp(); p.setMargins(l,t,r,b); v.setLayoutParams(p); }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
    private static String q(String s) { return "'" + s.replace("'", "'\\''") + "'"; }
    private static String parent(String p) { int i = p.lastIndexOf('/'); return i <= 0 ? "/" : p.substring(0, i); }
    private static class ShellResult { final int code; final String out; ShellResult(int c, String o) { code = c; out = o; } }
}
