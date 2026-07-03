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
    private final StringBuilder logs = new StringBuilder("Siap digunakan.");
    private SharedPreferences prefs;
    private LinearLayout content;
    private EditText manifestUrl;
    private TextView statusView, logView, latestView;
    private Button updateBtn;
    private JSONObject manifest;
    private boolean dark;
    private int bg, glass, glass2, text, muted, border, blue, candy, green, yellow;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (!prefs.contains("local_version_code")) prefs.edit().putInt("local_version_code", 1).apply();
        theme();
        immersive();
        shell();
        home();
        refresh();
        if (prefs.getBoolean("auto_check", false)) main.postDelayed(() -> checkUpdate(), 700);
    }

    private void theme() {
        dark = prefs.getBoolean("theme_dark", true);
        blue = Color.rgb(0, 198, 255);
        candy = Color.rgb(72, 116, 255);
        green = Color.rgb(31, 221, 144);
        yellow = Color.rgb(255, 198, 58);
        if (dark) {
            bg = Color.rgb(4, 7, 13);
            glass = Color.argb(218, 16, 22, 36);
            glass2 = Color.argb(230, 24, 32, 52);
            text = Color.rgb(247, 250, 255);
            muted = Color.rgb(158, 173, 204);
            border = Color.argb(135, 98, 143, 255);
        } else {
            bg = Color.rgb(230, 241, 250);
            glass = Color.argb(225, 255, 255, 255);
            glass2 = Color.argb(235, 239, 248, 255);
            text = Color.rgb(17, 25, 40);
            muted = Color.rgb(89, 105, 132);
            border = Color.argb(165, 76, 139, 255);
        }
    }

    private void immersive() {
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    private void shell() {
        LinearLayout root = row();
        root.setBackgroundColor(bg);
        root.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout nav = col();
        nav.setPadding(dp(12), dp(14), dp(12), dp(14));
        nav.setBackground(stroke(glass, 28));
        root.addView(nav, new LinearLayout.LayoutParams(dp(205), LinearLayout.LayoutParams.MATCH_PARENT));
        TextView logo = txt("DL", 26, blue, true);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(stroke(Color.argb(120, 0, 198, 255), 18));
        nav.addView(logo, new LinearLayout.LayoutParams(dp(58), dp(58)));
        nav.addView(txt("DLavie Hub", 17, text, true));
        nav.addView(txt("Carbon + Candy Blue", 11, muted, false));
        nav.addView(space(1, dp(16)));
        nav.addView(navBtn("⌂  Home", () -> home()));
        nav.addView(navBtn("◉  Community", () -> startActivity(new Intent(this, CommunityActivity.class))));
        nav.addView(navBtn("◇  Upgrade Plan", () -> plan()));
        nav.addView(navBtn("◆  Profile", () -> profile()));
        nav.addView(space(1, dp(12)), new LinearLayout.LayoutParams(1, 0, 1));
        nav.addView(txt(dark ? "Dark liquid glass" : "Light liquid glass", 11, muted, false));
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = col();
        content.setPadding(dp(16), 0, 0, 0);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1));
        setContentView(root);
    }

    private Button navBtn(String s, final Runnable r) {
        Button b = btn(s, glass2, text);
        b.setGravity(Gravity.CENTER_VERTICAL);
        b.setOnClickListener(v -> r.run());
        return b;
    }

    private void clear() {
        content.removeAllViews();
        statusView = null; logView = null; latestView = null; updateBtn = null;
    }

    private void home() {
        clear();
        LinearLayout hub = row();
        content.addView(hub, mp());
        LinearLayout hero = col();
        hero.setPadding(dp(20), dp(18), dp(20), dp(18));
        hero.setBackground(grad(30, dark ? Color.rgb(24,35,61) : Color.rgb(220,242,255), dark ? Color.rgb(8,12,24) : Color.rgb(245,252,255)));
        hub.addView(hero, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.25f));
        margin(hero,0,0,dp(14),0);
        TextView tag = txt("DLAVIE MOD CENTER", 12, blue, true); tag.setLetterSpacing(.12f); hero.addView(tag);
        hero.addView(txt("FIFA 16 Mobile", 36, text, true));
        hero.addView(txt("GameHub launcher untuk update patch kecil, community, backup, restore, dan launch game.", 14, muted, false));
        LinearLayout stats = row(); margin(stats,0,dp(16),0,0); hero.addView(stats, mp());
        stats.addView(tile("MOD VERSION", "v" + prefs.getInt("local_version_code", 1)), weight(1)); stats.addView(space(dp(10),1));
        stats.addView(tile("COMMUNITY", "Supabase"), weight(1)); stats.addView(space(dp(10),1));
        stats.addView(tile("PATCH", "Delta only"), weight(1));
        Button launch = btn("▶  Launch FIFA 16", green, Color.rgb(1,20,12)); launch.setTextSize(18); launch.setOnClickListener(v -> launchGame()); hero.addView(launch, mp());
        Button community = btn("Open DLavie Community", candy, Color.WHITE); community.setOnClickListener(v -> startActivity(new Intent(this, CommunityActivity.class))); hero.addView(community, mp());
        Button checkHero = btn("Check Update", candy, Color.WHITE); checkHero.setOnClickListener(v -> checkUpdate()); hero.addView(checkHero, mp());

        LinearLayout side = col(); hub.addView(side, weight(1));
        LinearLayout update = card(); side.addView(update, mp()); update.addView(section("UPDATE SOURCE"));
        manifestUrl = new EditText(this); manifestUrl.setText(prefs.getString("manifest_url", DEFAULT_MANIFEST)); manifestUrl.setSingleLine(false); manifestUrl.setMinLines(2); manifestUrl.setTextSize(12); manifestUrl.setTextColor(text); manifestUrl.setHintTextColor(muted); manifestUrl.setPadding(dp(12),dp(8),dp(12),dp(8)); manifestUrl.setBackground(stroke(glass2, 16)); margin(manifestUrl,0,dp(8),0,0); update.addView(manifestUrl, mp());
        LinearLayout row = row(); update.addView(row, mp());
        Button save = btn("Save URL", glass2, text); save.setOnClickListener(v -> { prefs.edit().putString("manifest_url", manifestUrl.getText().toString().trim()).apply(); say("URL update disimpan."); }); row.addView(save, weight(1)); margin(save,0,dp(10),dp(8),0);
        Button check = btn("Check", candy, Color.WHITE); check.setOnClickListener(v -> checkUpdate()); row.addView(check, weight(1)); margin(check,0,dp(10),0,0);
        updateBtn = btn("Update Now", blue, Color.rgb(2,18,31)); updateBtn.setEnabled(false); updateBtn.setAlpha(.45f); updateBtn.setOnClickListener(v -> applyUpdate()); update.addView(updateBtn, mp());
        latestView = txt("Latest: belum dicek", 12, muted, false); margin(latestView,0,dp(8),0,0); update.addView(latestView);
        LinearLayout status = card(); margin(status,0,dp(12),0,0); side.addView(status, mp()); status.addView(section("SYSTEM STATUS")); statusView = txt("Memuat status...", 14, text, false); margin(statusView,0,dp(8),0,0); status.addView(statusView);
        LinearLayout tools = row(); margin(tools,0,dp(12),0,0); side.addView(tools, mp());
        Button sh = btn("Shizuku", glass2, text); sh.setOnClickListener(v -> askShizuku()); tools.addView(sh, weight(1)); margin(sh,0,0,dp(8),0);
        Button restore = btn("Restore", glass2, text); restore.setOnClickListener(v -> restoreBackup()); tools.addView(restore, weight(1)); margin(restore,0,0,dp(8),0);
        Button base = btn("Base v1", glass2, text); base.setOnClickListener(v -> { prefs.edit().putInt("local_version_code", 1).apply(); say("Versi lokal diset ke v1."); refresh(); }); tools.addView(base, weight(1));
        LinearLayout log = card(); margin(log,0,dp(12),0,0); side.addView(log, mp()); log.addView(section("ACTIVITY LOG")); logView = txt(logs.toString(), 12, muted, false); margin(logView,0,dp(8),0,0); log.addView(logView);
        refresh();
    }

    private void plan() {
        clear();
        LinearLayout card = card(); content.addView(card, mp());
        card.addView(txt("Upgrade Plan", 32, text, true));
        card.addView(txt("Coming Soon. Subscription dikosongkan dulu, tanpa checkout palsu dan tanpa benefit dummy.", 15, muted, false));
        card.addView(detail("Current Access", "Developer / Local Build"));
        card.addView(detail("Subscription Engine", "Belum aktif"));
    }

    private void profile() {
        clear();
        LinearLayout grid = row(); content.addView(grid, mp());
        LinearLayout left = card(); grid.addView(left, weight(1)); margin(left,0,0,dp(12),0);
        left.addView(txt("Profile", 32, text, true));
        left.addView(detail("Developer", "DLavie Developer"));
        left.addView(detail("Device", Build.MANUFACTURER + " " + Build.MODEL));
        left.addView(detail("Android", Build.VERSION.RELEASE + " / SDK " + Build.VERSION.SDK_INT));
        left.addView(detail("Target Game", GAME_PACKAGE));
        left.addView(detail("Local Mod Version", "v" + prefs.getInt("local_version_code", 1)));
        left.addView(detail("Last Backup", empty(prefs.getString("last_backup_root", ""))));
        LinearLayout right = card(); grid.addView(right, weight(1));
        right.addView(section("SETTINGS"));
        right.addView(setting("Theme: " + (dark ? "Dark" : "Light"), () -> { prefs.edit().putBoolean("theme_dark", !dark).apply(); theme(); immersive(); shell(); profile(); }));
        right.addView(setting("Auto Check Update: " + onOff(prefs.getBoolean("auto_check", false)), () -> { toggle("auto_check"); profile(); }));
        right.addView(setting("Auto Launch After Patch: " + onOff(prefs.getBoolean("auto_launch", false)), () -> { toggle("auto_launch"); profile(); }));
        right.addView(detail("APK Version", appVersion()));
        right.addView(detail("Installed", appInstall(true)));
        right.addView(detail("Updated", appInstall(false)));
        right.addView(detail("Community Backend", CommunityApi.SUPABASE_URL));
    }

    private LinearLayout tile(String a, String b) { LinearLayout x = col(); x.setPadding(dp(12),dp(10),dp(12),dp(10)); x.setBackground(stroke(glass2,18)); x.addView(txt(a,10,muted,true)); x.addView(txt(b,15,text,true)); return x; }
    private LinearLayout detail(String a, String b) { LinearLayout x = col(); margin(x,0,dp(10),0,0); x.addView(txt(a,11,muted,true)); x.addView(txt(b,13,text,false)); return x; }
    private Button setting(String s, final Runnable r) { Button b = btn(s, glass2, text); b.setOnClickListener(v -> r.run()); return b; }
    private void toggle(String k) { prefs.edit().putBoolean(k, !prefs.getBoolean(k, false)).apply(); }
    private String onOff(boolean b) { return b ? "On" : "Off"; }
    private String empty(String s) { return s == null || s.trim().isEmpty() ? "—" : s; }

    private void refresh() { io.execute(() -> { boolean sh = shizukuOk(), rt = rootOk(); int ver = prefs.getInt("local_version_code", 1); String s = "Mod Version: v" + ver + "\nAccess Mode: " + (sh ? "Shizuku aktif" : (rt ? "Root aktif" : "belum aktif")) + "\nTarget: " + GAME_PACKAGE; main.post(() -> { if (statusView != null) statusView.setText(s); }); }); }
    private void askShizuku() { try { if (!Shizuku.pingBinder()) { say("Shizuku belum berjalan. Start Shizuku dulu."); return; } if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) { say("Shizuku aktif."); refresh(); return; } Shizuku.requestPermission(SHIZUKU_REQ); } catch (Throwable t) { say("Gagal cek Shizuku: " + t.getMessage()); } }
    @Override public void onRequestPermissionsResult(int r, String[] p, int[] g) { super.onRequestPermissionsResult(r,p,g); if (r == SHIZUKU_REQ) { say("Permission Shizuku diperbarui."); refresh(); } }
    private boolean shizukuOk() { try { return Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED; } catch (Throwable t) { return false; } }
    private boolean rootOk() { try { ShellResult r = normal(new String[]{"su","-c","id"}); return r.code == 0 && r.out.contains("uid=0"); } catch (Throwable t) { return false; } }

    private void checkUpdate() { if (manifestUrl != null) prefs.edit().putString("manifest_url", manifestUrl.getText().toString().trim()).apply(); if (updateBtn != null) { updateBtn.setEnabled(false); updateBtn.setAlpha(.45f); } say("Mengecek update..."); io.execute(() -> { try { manifest = new JSONObject(textUrl(prefs.getString("manifest_url", DEFAULT_MANIFEST))); int local = prefs.getInt("local_version_code",1), latest = manifest.optInt("latestVersionCode", local); String name = manifest.optString("latestVersionName", "v" + latest); main.post(() -> { if (latest > local) { say("Update tersedia: v" + local + " -> " + name); if (updateBtn != null) { updateBtn.setEnabled(true); updateBtn.setAlpha(1f); } } else say("Sudah versi terbaru: v" + local); if (latestView != null) latestView.setText("Latest: " + name); refresh(); }); } catch (Throwable t) { say("Check update gagal: " + t.getMessage()); } }); }
    private void applyUpdate() { if (updateBtn != null) { updateBtn.setEnabled(false); updateBtn.setAlpha(.45f); } say("Mulai update..."); io.execute(() -> { try { if (manifest == null) manifest = new JSONObject(textUrl(prefs.getString("manifest_url", DEFAULT_MANIFEST))); int latest = manifest.getInt("latestVersionCode"), local = prefs.getInt("local_version_code",1); while (local < latest) { JSONObject p = findPatch(manifest.getJSONArray("patches"), local); if (p == null) throw new IllegalStateException("Patch v" + local + " tidak ditemukan."); applyPatch(p); local = p.getInt("to"); prefs.edit().putInt("local_version_code", local).apply(); say("Patch selesai. Versi lokal v" + local); } say("Semua update selesai."); refresh(); if (prefs.getBoolean("auto_launch", false)) main.postDelayed(() -> launchGame(), 600); } catch (Throwable t) { say("Update gagal: " + t.getMessage()); main.post(() -> { if (updateBtn != null) { updateBtn.setEnabled(true); updateBtn.setAlpha(1f); } }); } }); }
    private JSONObject findPatch(JSONArray a, int from) throws Exception { for (int i=0;i<a.length();i++) { JSONObject p=a.getJSONObject(i); if (p.optInt("from") == from) return p; } return null; }
    private void applyPatch(JSONObject p) throws Exception { int to=p.getInt("to"); String target=p.optString("target","/sdcard/Android/data/com.ea.gp.fifaworld/"); File work=new File(getExternalFilesDir(null),"updates/v"+to), ex=new File(work,"extracted"); del(work); ex.mkdirs(); List<String> entries=new ArrayList<>(); if (p.has("files")) { JSONArray fs=p.getJSONArray("files"); String root=ex.getCanonicalPath()+File.separator; for (int i=0;i<fs.length();i++) { JSONObject f=fs.getJSONObject(i); String rel=f.getString("path").replace('\\','/'); if (rel.startsWith("../")||rel.contains("/../")) throw new IllegalStateException("Path tidak aman"); File out=new File(ex,rel); if (!out.getCanonicalPath().startsWith(root)) throw new IllegalStateException("Path tidak aman"); File par=out.getParentFile(); if (par!=null) par.mkdirs(); try(FileOutputStream os=new FileOutputStream(out)) { os.write(f.optString("content","").getBytes("UTF-8")); } entries.add(rel); } say("Inline patch siap: " + entries.size() + " file."); } else { File zip=new File(work,"patch.zip"); say("Download " + p.optString("name","patch") + "..."); fileUrl(p.getString("url"), zip); String sha=p.optString("sha256","").trim(); if (!sha.isEmpty() && !sha.equalsIgnoreCase(sha256(zip))) throw new IllegalStateException("SHA-256 tidak cocok"); unzip(zip, ex); entries.addAll(zipList(zip)); } String backup="/sdcard/F16Launcher/backups/v"+to+"/"+System.currentTimeMillis(); ShellResult r=priv(copyCmd(ex.getAbsolutePath(), target, backup, entries)); say(r.out.trim().isEmpty()?"Copy selesai.":r.out.trim()); if (r.code != 0) throw new IllegalStateException("Exit code " + r.code); prefs.edit().putString("last_backup_root", backup).putString("last_backup_target", target).apply(); }
    private String copyCmd(String src, String target, String backup, List<String> es) { if (!target.endsWith("/")) target += "/"; StringBuilder c = new StringBuilder("set -e; mkdir -p " + q(target) + "; mkdir -p " + q(backup) + "; "); for (String rel: es) { String s=src+"/"+rel, d=target+rel, bd=backup+"/"+rel; c.append("mkdir -p ").append(q(parent(d))).append("; if [ -e ").append(q(d)).append(" ]; then mkdir -p ").append(q(parent(bd))).append("; cp -af ").append(q(d)).append(" ").append(q(bd)).append("; fi; cp -af ").append(q(s)).append(" ").append(q(d)).append("; "); } return c.append("echo 'Applied ").append(es.size()).append(" file(s)';").toString(); }
    private void restoreBackup() { say("Restore backup..."); io.execute(() -> { try { String b = prefs.getString("last_backup_root", ""), t = prefs.getString("last_backup_target", "/sdcard/Android/data/com.ea.gp.fifaworld/"); if (b == null || b.trim().isEmpty()) throw new IllegalStateException("Belum ada backup."); ShellResult r = priv("set -e; mkdir -p " + q(t) + "; cp -af " + q(b + "/.") + " " + q(t) + "; echo 'Restore backup selesai';"); say(r.out.trim()); if (r.code != 0) throw new IllegalStateException("Exit code " + r.code); } catch (Throwable e) { say("Restore gagal: " + e.getMessage()); } }); }
    private ShellResult priv(String cmd) throws Exception { if (shizukuOk()) { say("Apply menggunakan Shizuku..."); java.lang.reflect.Method m = Shizuku.class.getDeclaredMethod("newProcess", String[].class, String[].class, String.class); m.setAccessible(true); return read((Process)m.invoke(null, new Object[]{new String[]{"sh","-c",cmd}, null, null})); } if (rootOk()) { say("Apply menggunakan root/su..."); return normal(new String[]{"su","-c",cmd}); } throw new IllegalStateException("Shizuku/root belum aktif."); }

    private void launchGame() { Intent i = getPackageManager().getLaunchIntentForPackage(GAME_PACKAGE); if (i != null) startActivity(i); else { say("FIFA 16 tidak ditemukan."); try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + GAME_PACKAGE))); } catch (Throwable ignored) {} } }
    private String appVersion() { try { PackageInfo p = getPackageManager().getPackageInfo(getPackageName(), 0); long code = Build.VERSION.SDK_INT >= 28 ? p.getLongVersionCode() : p.versionCode; return p.versionName + " (" + code + ")"; } catch (Throwable t) { return "Unknown"; } }
    private String appInstall(boolean first) { try { PackageInfo p = getPackageManager().getPackageInfo(getPackageName(), 0); return DateFormat.getDateTimeInstance().format(new Date(first ? p.firstInstallTime : p.lastUpdateTime)); } catch (Throwable t) { return "Unknown"; } }
    private String textUrl(String u) throws Exception { HttpURLConnection c = (HttpURLConnection)new URL(u).openConnection(); c.setConnectTimeout(20000); c.setReadTimeout(30000); try(BufferedReader br=new BufferedReader(new InputStreamReader(c.getInputStream()))) { StringBuilder s=new StringBuilder(); String l; while((l=br.readLine())!=null) s.append(l).append('\n'); return s.toString(); } finally { c.disconnect(); } }
    private void fileUrl(String u, File out) throws Exception { HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection(); c.setConnectTimeout(20000); c.setReadTimeout(60000); try(InputStream in=new BufferedInputStream(c.getInputStream()); FileOutputStream os=new FileOutputStream(out)) { byte[] b=new byte[131072]; int n; while((n=in.read(b))>0) os.write(b,0,n); } finally { c.disconnect(); } }
    private void unzip(File z, File d) throws Exception { d.mkdirs(); String root=d.getCanonicalPath()+File.separator; try(ZipInputStream in=new ZipInputStream(new FileInputStream(z))) { ZipEntry e; byte[] b=new byte[131072]; while((e=in.getNextEntry())!=null) { File o=new File(d,e.getName()); if(!o.getCanonicalPath().startsWith(root)) throw new IllegalStateException("Zip path tidak aman"); if(e.isDirectory()) o.mkdirs(); else { File p=o.getParentFile(); if(p!=null) p.mkdirs(); try(FileOutputStream os=new FileOutputStream(o)) { int n; while((n=in.read(b))>0) os.write(b,0,n); } } } } }
    private List<String> zipList(File z) throws Exception { List<String> l=new ArrayList<>(); try(ZipFile f=new ZipFile(z)) { Enumeration<? extends ZipEntry> e=f.entries(); while(e.hasMoreElements()) { ZipEntry x=e.nextElement(); if(!x.isDirectory()) l.add(x.getName().replace('\\','/')); } } return l; }
    private String sha256(File f) throws Exception { MessageDigest md=MessageDigest.getInstance("SHA-256"); try(InputStream in=new FileInputStream(f)) { byte[] b=new byte[131072]; int n; while((n=in.read(b))>0) md.update(b,0,n); } StringBuilder s=new StringBuilder(); for(byte x: md.digest()) s.append(String.format(Locale.US,"%02x",x)); return s.toString(); }
    private ShellResult normal(String[] cmd) throws Exception { return read(new ProcessBuilder(cmd).redirectErrorStream(true).start()); }
    private ShellResult read(Process p) throws Exception { StringBuilder o=new StringBuilder(); try(BufferedReader br=new BufferedReader(new InputStreamReader(p.getInputStream()))) { String l; while((l=br.readLine())!=null) o.append(l).append('\n'); } return new ShellResult(p.waitFor(), o.toString()); }
    private void del(File f) { if (f == null || !f.exists()) return; if (f.isDirectory()) { File[] a=f.listFiles(); if (a!=null) for(File x:a) del(x); } f.delete(); }
    private void say(String s) { main.post(() -> { logs.append('\n').append(s); if (logView != null) logView.setText(logs.toString()); }); }

    private LinearLayout row() { LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); return l; }
    private LinearLayout col() { LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout card() { LinearLayout l=col(); l.setPadding(dp(16),dp(14),dp(16),dp(14)); l.setBackground(stroke(glass,24)); return l; }
    private TextView section(String s) { TextView t=txt(s,11,muted,true); t.setLetterSpacing(.12f); return t; }
    private TextView txt(String s, int sp, int c, boolean b) { TextView v=new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(c); if(b) v.setTypeface(Typeface.DEFAULT_BOLD); return v; }
    private Button btn(String s, int bgc, int fg) { Button b=new Button(this); b.setAllCaps(false); b.setText(s); b.setTextSize(14); b.setTextColor(fg); b.setTypeface(Typeface.DEFAULT_BOLD); b.setPadding(dp(10),dp(8),dp(10),dp(8)); b.setMinHeight(dp(42)); b.setBackground(round(bgc,18)); margin(b,0,dp(10),0,0); return b; }
    private GradientDrawable round(int c, int r) { GradientDrawable d=new GradientDrawable(); d.setColor(c); d.setCornerRadius(dp(r)); return d; }
    private GradientDrawable stroke(int c, int r) { GradientDrawable d=round(c,r); d.setStroke(dp(1), border); return d; }
    private GradientDrawable grad(int r, int a, int b) { GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{a,b}); d.setCornerRadius(dp(r)); return d; }
    private LinearLayout.LayoutParams mp() { return new LinearLayout.LayoutParams(-1,-2); }
    private LinearLayout.LayoutParams weight(float w) { return new LinearLayout.LayoutParams(0,-2,w); }
    private View space(int w, int h) { View v=new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(w,h)); return v; }
    private void margin(View v, int l, int t, int r, int b) { LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)v.getLayoutParams(); if(p==null) p=mp(); p.setMargins(l,t,r,b); v.setLayoutParams(p); }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
    private static String q(String s) { return "'" + s.replace("'", "'\\''") + "'"; }
    private static String parent(String p) { int i=p.lastIndexOf('/'); return i <= 0 ? "/" : p.substring(0,i); }
    private static class ShellResult { final int code; final String out; ShellResult(int c, String o) { code=c; out=o; } }
}
