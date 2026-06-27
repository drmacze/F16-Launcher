package com.drmacze.f16launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
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
import java.util.ArrayList;
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
    private static final int BG = Color.rgb(5, 7, 14), CARD = Color.rgb(17, 22, 36), CARD2 = Color.rgb(25, 32, 52);
    private static final int TEXT = Color.rgb(247, 249, 255), MUTED = Color.rgb(159, 171, 199), ACCENT = Color.rgb(108, 99, 255), CYAN = Color.rgb(0, 220, 255), GREEN = Color.rgb(31, 221, 144), YELLOW = Color.rgb(255, 198, 58);

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private SharedPreferences prefs;
    private EditText manifestUrl;
    private TextView status, log;
    private Button updateBtn, shizukuBadge, rootBadge;
    private JSONObject manifest;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (!prefs.contains("local_version_code")) prefs.edit().putInt("local_version_code", 1).apply();
        ui();
        refresh();
    }

    private void ui() {
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setBackgroundColor(BG);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(14), dp(18), dp(14));
        sv.addView(root);

        LinearLayout top = row();
        top.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(top, mp());
        LinearLayout title = col();
        top.addView(title, weight(1));
        title.addView(txt("DLavie", 30, TEXT, true));
        title.addView(txt("GameHub style launcher • FIFA 16 Mobile Mod Center", 13, MUTED, false));
        shizukuBadge = pill("Shizuku: cek");
        rootBadge = pill("Root: cek");
        top.addView(shizukuBadge); margin(shizukuBadge, dp(10),0,dp(8),0);
        top.addView(rootBadge);
        shizukuBadge.setOnClickListener(v -> askShizuku());

        LinearLayout hub = row();
        margin(hub, 0, dp(12), 0, 0);
        root.addView(hub, mp());

        LinearLayout hero = col();
        hero.setPadding(dp(20), dp(18), dp(20), dp(18));
        hero.setBackground(grad(28, Color.rgb(50, 58, 100), Color.rgb(9, 13, 25)));
        hub.addView(hero, weight(1.25f)); margin(hero,0,0,dp(14),0);
        TextView mini = txt("DLAVIE MOD HUB", 12, CYAN, true); mini.setLetterSpacing(0.12f); hero.addView(mini);
        TextView game = txt("FIFA 16 Mobile", 34, TEXT, true); margin(game,0,dp(8),0,0); hero.addView(game);
        hero.addView(txt("Landscape hub untuk update patch kecil, backup, restore, dan launch game tanpa ribet.", 14, MUTED, false));
        LinearLayout stats = row(); margin(stats,0,dp(16),0,0); hero.addView(stats, mp());
        stats.addView(tile("Version", "v" + prefs.getInt("local_version_code", 1)), weight(1)); stats.addView(space(dp(10),1));
        stats.addView(tile("Access", "Shizuku/Root"), weight(1)); stats.addView(space(dp(10),1));
        stats.addView(tile("Patch", "Delta only"), weight(1));
        Button launch = btn("▶  Launch FIFA 16", GREEN, Color.rgb(2,20,12)); launch.setTextSize(18); margin(launch,0,dp(18),0,0); hero.addView(launch); launch.setOnClickListener(v -> launchGame());
        Button checkHero = btn("Check Update", ACCENT, Color.WHITE); hero.addView(checkHero); checkHero.setOnClickListener(v -> checkUpdate());

        LinearLayout side = col(); hub.addView(side, weight(1));
        LinearLayout update = card(); side.addView(update, mp()); update.addView(section("UPDATE SOURCE"));
        manifestUrl = new EditText(this); manifestUrl.setText(prefs.getString("manifest_url", DEFAULT_MANIFEST)); manifestUrl.setSingleLine(false); manifestUrl.setMinLines(2); manifestUrl.setTextSize(12); manifestUrl.setTextColor(TEXT); manifestUrl.setHintTextColor(MUTED); manifestUrl.setPadding(dp(12), dp(8), dp(12), dp(8)); manifestUrl.setBackground(stroke(14, Color.rgb(10,14,25), Color.rgb(54,65,102))); margin(manifestUrl,0,dp(8),0,0); update.addView(manifestUrl, mp());
        LinearLayout upRow = row(); update.addView(upRow, mp());
        Button save = btn("Save URL", CARD2, TEXT); upRow.addView(save, weight(1)); margin(save,0,dp(10),dp(8),0); save.setOnClickListener(v -> { prefs.edit().putString("manifest_url", manifestUrl.getText().toString().trim()).apply(); say("URL update disimpan."); });
        Button check = btn("Check", ACCENT, Color.WHITE); upRow.addView(check, weight(1)); margin(check,0,dp(10),0,0); check.setOnClickListener(v -> checkUpdate());
        updateBtn = btn("Update Now", CYAN, Color.rgb(5,12,20)); updateBtn.setEnabled(false); updateBtn.setAlpha(.45f); update.addView(updateBtn); updateBtn.setOnClickListener(v -> applyUpdate());

        LinearLayout stat = card(); margin(stat,0,dp(12),0,0); side.addView(stat, mp()); stat.addView(section("SYSTEM STATUS")); status = txt("Memuat status...", 14, TEXT, false); margin(status,0,dp(8),0,0); stat.addView(status);
        LinearLayout tools = row(); margin(tools,0,dp(12),0,0); side.addView(tools, mp());
        Button sh = btn("Shizuku", CARD2, TEXT); tools.addView(sh, weight(1)); margin(sh,0,0,dp(8),0); sh.setOnClickListener(v -> askShizuku());
        Button restore = btn("Restore", CARD2, TEXT); tools.addView(restore, weight(1)); margin(restore,0,0,dp(8),0); restore.setOnClickListener(v -> restoreBackup());
        Button base = btn("Base v1", CARD2, TEXT); tools.addView(base, weight(1)); base.setOnClickListener(v -> { prefs.edit().putInt("local_version_code", 1).apply(); say("Versi lokal diset ke v1."); refresh(); });
        LinearLayout logs = card(); margin(logs,0,dp(12),0,0); side.addView(logs, mp()); logs.addView(section("ACTIVITY LOG")); log = txt("Siap digunakan.", 12, MUTED, false); margin(log,0,dp(8),0,0); logs.addView(log);
        setContentView(sv);
    }

    private TextView section(String s) { TextView t = txt(s, 11, MUTED, true); t.setLetterSpacing(.10f); return t; }
    private LinearLayout tile(String a, String b) { LinearLayout x = col(); x.setPadding(dp(12),dp(10),dp(12),dp(10)); x.setBackground(round(16, Color.rgb(18,24,42))); x.addView(txt(a,11,MUTED,false)); x.addView(txt(b,14,TEXT,true)); return x; }
    private LinearLayout row(){ LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); return l; }
    private LinearLayout col(){ LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout card(){ LinearLayout l=col(); l.setPadding(dp(14),dp(12),dp(14),dp(12)); l.setBackground(round(20,CARD)); return l; }
    private TextView txt(String s,int sp,int c,boolean b){ TextView v=new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(c); if(b)v.setTypeface(Typeface.DEFAULT_BOLD); return v; }
    private Button btn(String s,int bg,int fg){ Button b=new Button(this); b.setAllCaps(false); b.setText(s); b.setTextSize(14); b.setTextColor(fg); b.setTypeface(Typeface.DEFAULT_BOLD); b.setPadding(dp(10),dp(8),dp(10),dp(8)); b.setMinHeight(dp(42)); b.setBackground(round(16,bg)); margin(b,0,dp(10),0,0); return b; }
    private Button pill(String s){ Button b=btn(s,CARD2,MUTED); b.setTextSize(12); b.setMinHeight(dp(34)); b.setMinimumHeight(dp(34)); b.setBackground(round(999,CARD2)); return b; }
    private GradientDrawable round(int r,int c){ GradientDrawable d=new GradientDrawable(); d.setColor(c); d.setCornerRadius(dp(r)); return d; }
    private GradientDrawable stroke(int r,int c,int s){ GradientDrawable d=round(r,c); d.setStroke(dp(1),s); return d; }
    private GradientDrawable grad(int r,int a,int b){ GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b}); d.setCornerRadius(dp(r)); return d; }
    private LinearLayout.LayoutParams mp(){ return new LinearLayout.LayoutParams(-1,-2); }
    private LinearLayout.LayoutParams weight(float w){ return new LinearLayout.LayoutParams(0,-2,w); }
    private View space(int w,int h){ View v=new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(w,h)); return v; }
    private void margin(View v,int l,int t,int r,int b){ LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)v.getLayoutParams(); if(p==null)p=mp(); p.setMargins(l,t,r,b); v.setLayoutParams(p); }
    private int dp(int v){ return (int)(v*getResources().getDisplayMetrics().density+.5f); }

    private void refresh(){ io.execute(() -> { boolean sh=shizukuOk(), rt=rootOk(); int ver=prefs.getInt("local_version_code",1); main.post(() -> { status.setText("Mod Version: v"+ver+"\nAccess Mode: "+(sh?"Shizuku aktif":(rt?"Root aktif":"belum aktif"))+"\nTarget: "+GAME_PACKAGE); shizukuBadge.setText("Shizuku: "+(sh?"aktif":"off")); shizukuBadge.setTextColor(sh?GREEN:YELLOW); rootBadge.setText("Root: "+(rt?"aktif":"off")); rootBadge.setTextColor(rt?GREEN:MUTED); }); }); }
    private void askShizuku(){ try{ if(!Shizuku.pingBinder()){ say("Shizuku belum berjalan. Start Shizuku dulu."); return; } if(Shizuku.checkSelfPermission()==PackageManager.PERMISSION_GRANTED){ say("Shizuku aktif."); refresh(); return; } Shizuku.requestPermission(SHIZUKU_REQ); }catch(Throwable t){ say("Gagal cek Shizuku: "+t.getMessage()); } }
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){ super.onRequestPermissionsResult(r,p,g); if(r==SHIZUKU_REQ){ say("Permission Shizuku diperbarui."); refresh(); } }
    private boolean shizukuOk(){ try{return Shizuku.pingBinder()&&Shizuku.checkSelfPermission()==PackageManager.PERMISSION_GRANTED;}catch(Throwable t){return false;} }
    private boolean rootOk(){ try{ ShellResult r=normal(new String[]{"su","-c","id"}); return r.code==0&&r.out.contains("uid=0"); }catch(Throwable t){return false;} }
    private void checkUpdate(){ updateBtn.setEnabled(false); updateBtn.setAlpha(.45f); say("Mengecek update..."); io.execute(() -> { try{ manifest=new JSONObject(textUrl(manifestUrl.getText().toString().trim())); int local=prefs.getInt("local_version_code",1), latest=manifest.optInt("latestVersionCode",local); String name=manifest.optString("latestVersionName","v"+latest); main.post(() -> { if(latest>local){ say("Update tersedia: v"+local+" -> "+name); updateBtn.setEnabled(true); updateBtn.setAlpha(1f); } else say("Sudah versi terbaru: v"+local); refresh(); }); }catch(Throwable t){ say("Check update gagal: "+t.getMessage()); } }); }
    private void applyUpdate(){ updateBtn.setEnabled(false); updateBtn.setAlpha(.45f); say("Mulai update..."); io.execute(() -> { try{ if(manifest==null)manifest=new JSONObject(textUrl(manifestUrl.getText().toString().trim())); int latest=manifest.getInt("latestVersionCode"), local=prefs.getInt("local_version_code",1); while(local<latest){ JSONObject p=findPatch(manifest.getJSONArray("patches"),local); if(p==null)throw new IllegalStateException("Patch v"+local+" tidak ditemukan."); applyPatch(p); local=p.getInt("to"); prefs.edit().putInt("local_version_code",local).apply(); say("Patch selesai. Versi lokal v"+local); } say("Semua update selesai."); refresh(); }catch(Throwable t){ say("Update gagal: "+t.getMessage()); main.post(()->{updateBtn.setEnabled(true);updateBtn.setAlpha(1f);}); } }); }
    private JSONObject findPatch(JSONArray a,int from)throws Exception{ for(int i=0;i<a.length();i++){ JSONObject p=a.getJSONObject(i); if(p.optInt("from")==from)return p; } return null; }
    private void applyPatch(JSONObject p)throws Exception{ int to=p.getInt("to"); String target=p.optString("target","/sdcard/Android/data/com.ea.gp.fifaworld/"); File work=new File(getExternalFilesDir(null),"updates/v"+to), ex=new File(work,"extracted"); del(work); ex.mkdirs(); List<String> entries=new ArrayList<>(); if(p.has("files")){ JSONArray fs=p.getJSONArray("files"); String root=ex.getCanonicalPath()+File.separator; for(int i=0;i<fs.length();i++){ JSONObject f=fs.getJSONObject(i); String rel=f.getString("path").replace('\\','/'); if(rel.startsWith("../")||rel.contains("/../"))throw new IllegalStateException("Path tidak aman"); File out=new File(ex,rel); if(!out.getCanonicalPath().startsWith(root))throw new IllegalStateException("Path tidak aman"); File par=out.getParentFile(); if(par!=null)par.mkdirs(); try(FileOutputStream os=new FileOutputStream(out)){ os.write(f.optString("content","").getBytes("UTF-8")); } entries.add(rel); } say("Inline patch siap: "+entries.size()+" file."); } else { File zip=new File(work,"patch.zip"); say("Download "+p.optString("name","patch")+"..."); fileUrl(p.getString("url"),zip); String sha=p.optString("sha256","").trim(); if(!sha.isEmpty()&&!sha.equalsIgnoreCase(sha256(zip)))throw new IllegalStateException("SHA-256 tidak cocok"); unzip(zip,ex); entries.addAll(zipList(zip)); }
        String backup="/sdcard/F16Launcher/backups/v"+to+"/"+System.currentTimeMillis(); ShellResult r=priv(copyCmd(ex.getAbsolutePath(),target,backup,entries)); say(r.out.trim().isEmpty()?"Copy selesai.":r.out.trim()); if(r.code!=0)throw new IllegalStateException("Exit code "+r.code); prefs.edit().putString("last_backup_root",backup).putString("last_backup_target",target).apply(); }
    private String copyCmd(String src,String target,String backup,List<String> es){ if(!target.endsWith("/"))target+="/"; StringBuilder c=new StringBuilder("set -e; mkdir -p "+q(target)+"; mkdir -p "+q(backup)+"; "); for(String rel:es){ String s=src+"/"+rel,d=target+rel,bd=backup+"/"+rel; c.append("mkdir -p "+q(parent(d))+"; if [ -e "+q(d)+" ]; then mkdir -p "+q(parent(bd))+"; cp -af "+q(d)+" "+q(bd)+"; fi; cp -af "+q(s)+" "+q(d)+"; "); } return c.append("echo 'Applied "+es.size()+" file(s)';").toString(); }
    private void restoreBackup(){ say("Restore backup..."); io.execute(() -> { try{ String b=prefs.getString("last_backup_root",""), t=prefs.getString("last_backup_target","/sdcard/Android/data/com.ea.gp.fifaworld/"); if(b==null||b.trim().isEmpty())throw new IllegalStateException("Belum ada backup."); ShellResult r=priv("set -e; mkdir -p "+q(t)+"; cp -af "+q(b+"/.")+" "+q(t)+"; echo 'Restore backup selesai';"); say(r.out.trim()); if(r.code!=0)throw new IllegalStateException("Exit code "+r.code); }catch(Throwable e){ say("Restore gagal: "+e.getMessage()); } }); }
    private ShellResult priv(String cmd)throws Exception{ if(shizukuOk()){ say("Apply menggunakan Shizuku..."); java.lang.reflect.Method m=Shizuku.class.getDeclaredMethod("newProcess",String[].class,String[].class,String.class); m.setAccessible(true); return read((Process)m.invoke(null,new Object[]{new String[]{"sh","-c",cmd},null,null})); } if(rootOk()){ say("Apply menggunakan root/su..."); return normal(new String[]{"su","-c",cmd}); } throw new IllegalStateException("Shizuku/root belum aktif."); }
    private String textUrl(String u)throws Exception{ HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection(); c.setConnectTimeout(20000); c.setReadTimeout(30000); try(BufferedReader br=new BufferedReader(new InputStreamReader(c.getInputStream()))){ StringBuilder s=new StringBuilder(); String l; while((l=br.readLine())!=null)s.append(l).append('\n'); return s.toString(); } finally{ c.disconnect(); } }
    private void fileUrl(String u,File out)throws Exception{ HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection(); c.setConnectTimeout(20000); c.setReadTimeout(60000); try(InputStream in=new BufferedInputStream(c.getInputStream()); FileOutputStream os=new FileOutputStream(out)){ byte[] b=new byte[131072]; int n; while((n=in.read(b))>0)os.write(b,0,n); } finally{ c.disconnect(); } }
    private void unzip(File z,File d)throws Exception{ d.mkdirs(); String root=d.getCanonicalPath()+File.separator; try(ZipInputStream in=new ZipInputStream(new FileInputStream(z))){ ZipEntry e; byte[] b=new byte[131072]; while((e=in.getNextEntry())!=null){ File o=new File(d,e.getName()); if(!o.getCanonicalPath().startsWith(root))throw new IllegalStateException("Zip path tidak aman"); if(e.isDirectory())o.mkdirs(); else{ File p=o.getParentFile(); if(p!=null)p.mkdirs(); try(FileOutputStream os=new FileOutputStream(o)){ int n; while((n=in.read(b))>0)os.write(b,0,n); } } } } }
    private List<String> zipList(File z)throws Exception{ List<String> l=new ArrayList<>(); try(ZipFile f=new ZipFile(z)){ Enumeration<? extends ZipEntry> e=f.entries(); while(e.hasMoreElements()){ ZipEntry x=e.nextElement(); if(!x.isDirectory())l.add(x.getName().replace('\\','/')); } } return l; }
    private String sha256(File f)throws Exception{ MessageDigest md=MessageDigest.getInstance("SHA-256"); try(InputStream in=new FileInputStream(f)){ byte[] b=new byte[131072]; int n; while((n=in.read(b))>0)md.update(b,0,n); } StringBuilder s=new StringBuilder(); for(byte x:md.digest())s.append(String.format(Locale.US,"%02x",x)); return s.toString(); }
    private ShellResult normal(String[] cmd)throws Exception{ return read(new ProcessBuilder(cmd).redirectErrorStream(true).start()); }
    private ShellResult read(Process p)throws Exception{ StringBuilder o=new StringBuilder(); try(BufferedReader br=new BufferedReader(new InputStreamReader(p.getInputStream()))){ String l; while((l=br.readLine())!=null)o.append(l).append('\n'); } return new ShellResult(p.waitFor(),o.toString()); }
    private void launchGame(){ Intent i=getPackageManager().getLaunchIntentForPackage(GAME_PACKAGE); if(i!=null)startActivity(i); else{ say("FIFA 16 tidak ditemukan."); try{startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="+GAME_PACKAGE)));}catch(Throwable ignored){} } }
    private static String q(String s){ return "'"+s.replace("'","'\\''")+"'"; } private static String parent(String p){ int i=p.lastIndexOf('/'); return i<=0?"/":p.substring(0,i); }
    private void del(File f){ if(f==null||!f.exists())return; if(f.isDirectory()){ File[] a=f.listFiles(); if(a!=null)for(File x:a)del(x); } f.delete(); }
    private void say(String s){ main.post(()-> log.append("\n"+s)); }
    private static class ShellResult{ final int code; final String out; ShellResult(int c,String o){code=c;out=o;} }
}
