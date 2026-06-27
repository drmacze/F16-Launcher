package com.drmacze.f16launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

public class MainActivity extends Activity {
    private static final String PREFS = "f16_launcher";
    private static final String KEY_VERSION = "local_version_code";
    private static final String KEY_URL = "manifest_url";
    private static final int SHIZUKU_REQ = 1601;
    private static final String GAME_PACKAGE = "com.ea.gp.fifaworld";
    private static final String DEFAULT_MANIFEST = "https://raw.githubusercontent.com/drmacze/F16/main/updates/latest.json";

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private SharedPreferences prefs;
    private TextView statusView;
    private TextView logView;
    private EditText manifestUrl;
    private Button updateButton;

    private JSONObject lastManifest;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (!prefs.contains(KEY_VERSION)) prefs.edit().putInt(KEY_VERSION, 1).apply();
        buildUi();
        refreshStatus();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("F16 Launcher");
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        statusView = new TextView(this);
        statusView.setTextSize(14);
        statusView.setPadding(0, dp(10), 0, dp(10));
        root.addView(statusView);

        manifestUrl = new EditText(this);
        manifestUrl.setSingleLine(false);
        manifestUrl.setText(prefs.getString(KEY_URL, DEFAULT_MANIFEST));
        manifestUrl.setHint("Manifest update URL");
        root.addView(manifestUrl);

        Button saveUrl = button("Simpan URL Update");
        saveUrl.setOnClickListener(v -> {
            prefs.edit().putString(KEY_URL, manifestUrl.getText().toString().trim()).apply();
            appendLog("URL update disimpan.");
        });
        root.addView(saveUrl);

        Button shizuku = button("Aktifkan / Cek Shizuku");
        shizuku.setOnClickListener(v -> requestShizuku());
        root.addView(shizuku);

        Button check = button("Check Update");
        check.setOnClickListener(v -> checkUpdate());
        root.addView(check);

        updateButton = button("Update Now");
        updateButton.setEnabled(false);
        updateButton.setOnClickListener(v -> applyUpdates());
        root.addView(updateButton);

        Button markBase = button("Tandai Data Sekarang = Base v1");
        markBase.setOnClickListener(v -> {
            prefs.edit().putInt(KEY_VERSION, 1).apply();
            appendLog("Versi lokal diset ke v1. Update berikutnya akan mulai dari patch v1 -> v2.");
            refreshStatus();
        });
        root.addView(markBase);

        Button launch = button("Launch FIFA 16");
        launch.setOnClickListener(v -> launchGame());
        root.addView(launch);

        logView = new TextView(this);
        logView.setTextSize(13);
        logView.setPadding(0, dp(14), 0, 0);
        root.addView(logView);

        setContentView(scroll);
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(text);
        return b;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void refreshStatus() {
        io.execute(() -> {
            boolean root = isRootAvailable();
            boolean shizuku = isShizukuReady();
            int version = prefs.getInt(KEY_VERSION, 1);
            String s = "Versi lokal: v" + version + "\n"
                    + "Shizuku: " + (shizuku ? "aktif" : "belum aktif") + "\n"
                    + "Root: " + (root ? "aktif" : "tidak terdeteksi") + "\n"
                    + "Target game: " + GAME_PACKAGE;
            main.post(() -> statusView.setText(s));
        });
    }

    private void requestShizuku() {
        try {
            if (!Shizuku.pingBinder()) {
                appendLog("Shizuku belum berjalan. Buka app Shizuku lalu Start via Wireless debugging/root.");
                return;
            }
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                appendLog("Shizuku sudah aktif dan permission sudah diberikan.");
                refreshStatus();
                return;
            }
            Shizuku.requestPermission(SHIZUKU_REQ);
        } catch (Throwable t) {
            appendLog("Gagal cek Shizuku: " + t.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SHIZUKU_REQ) {
            appendLog("Permission Shizuku diperbarui.");
            refreshStatus();
        }
    }

    private boolean isShizukuReady() {
        try {
            return Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean isRootAvailable() {
        try {
            ShellResult r = runNormal(new String[]{"su", "-c", "id"});
            return r.code == 0 && r.output.contains("uid=0");
        } catch (Throwable t) {
            return false;
        }
    }

    private void checkUpdate() {
        updateButton.setEnabled(false);
        appendLog("Mengecek update...");
        io.execute(() -> {
            try {
                String url = manifestUrl.getText().toString().trim();
                String json = downloadText(url);
                JSONObject obj = new JSONObject(json);
                lastManifest = obj;
                int local = prefs.getInt(KEY_VERSION, 1);
                int latest = obj.optInt("latestVersionCode", local);
                String name = obj.optString("latestVersionName", "v" + latest);
                main.post(() -> {
                    if (latest > local) {
                        appendLog("Update tersedia: lokal v" + local + " -> " + name);
                        updateButton.setEnabled(true);
                    } else {
                        appendLog("Sudah versi terbaru: v" + local);
                    }
                    refreshStatus();
                });
            } catch (Throwable t) {
                appendLog("Check update gagal: " + t.getMessage());
            }
        });
    }

    private void applyUpdates() {
        updateButton.setEnabled(false);
        appendLog("Mulai update...");
        io.execute(() -> {
            try {
                if (lastManifest == null) {
                    String json = downloadText(manifestUrl.getText().toString().trim());
                    lastManifest = new JSONObject(json);
                }
                int latest = lastManifest.getInt("latestVersionCode");
                int local = prefs.getInt(KEY_VERSION, 1);
                while (local < latest) {
                    JSONObject patch = findPatch(lastManifest.getJSONArray("patches"), local);
                    if (patch == null) {
                        throw new IllegalStateException("Patch dari v" + local + " tidak ditemukan di manifest.");
                    }
                    applyOnePatch(patch);
                    local = patch.getInt("to");
                    prefs.edit().putInt(KEY_VERSION, local).apply();
                    appendLog("Patch selesai. Versi lokal sekarang v" + local);
                }
                appendLog("Semua update selesai.");
                refreshStatus();
            } catch (Throwable t) {
                appendLog("Update gagal: " + t.getMessage());
                updateButton.setEnabled(true);
            }
        });
    }

    private JSONObject findPatch(JSONArray patches, int from) throws Exception {
        for (int i = 0; i < patches.length(); i++) {
            JSONObject p = patches.getJSONObject(i);
            if (p.optInt("from") == from) return p;
        }
        return null;
    }

    private void applyOnePatch(JSONObject patch) throws Exception {
        int to = patch.getInt("to");
        String name = patch.optString("name", "patch-v" + to);
        String url = patch.getString("url");
        String sha = patch.optString("sha256", "").trim();
        String target = patch.optString("target", "/sdcard/Android/data/com.ea.gp.fifaworld/");

        appendLog("Download " + name + "...");
        File work = new File(getExternalFilesDir(null), "updates/v" + to);
        deleteRecursive(work);
        if (!work.mkdirs() && !work.exists()) throw new IllegalStateException("Tidak bisa membuat folder kerja.");
        File zip = new File(work, "patch.zip");
        downloadFile(url, zip);

        if (!sha.isEmpty()) {
            String got = sha256(zip);
            if (!got.equalsIgnoreCase(sha)) throw new IllegalStateException("SHA-256 tidak cocok. File update dibatalkan.");
            appendLog("SHA-256 valid.");
        } else {
            appendLog("SHA-256 kosong, verifikasi dilewati.");
        }

        File extract = new File(work, "extracted");
        unzip(zip, extract);
        appendLog("Patch diekstrak.");

        List<String> entries = listZipFiles(zip);
        String backup = "/sdcard/F16Launcher/backups/v" + to + "/" + System.currentTimeMillis();
        String cmd = buildCopyCommand(extract.getAbsolutePath(), target, backup, entries);
        ShellResult r = runPrivileged(cmd);
        appendLog(r.output.trim().isEmpty() ? "Copy selesai." : r.output.trim());
        if (r.code != 0) throw new IllegalStateException("Command gagal, exit code " + r.code);
    }

    private String buildCopyCommand(String srcRoot, String targetRoot, String backupRoot, List<String> entries) {
        if (!targetRoot.endsWith("/")) targetRoot += "/";
        StringBuilder c = new StringBuilder();
        c.append("set -e; ");
        c.append("mkdir -p ").append(q(targetRoot)).append("; ");
        c.append("mkdir -p ").append(q(backupRoot)).append("; ");
        for (String rel : entries) {
            String src = srcRoot + "/" + rel;
            String dst = targetRoot + rel;
            String dstDir = parentOf(dst);
            String backupDst = backupRoot + "/" + rel;
            String backupDir = parentOf(backupDst);
            c.append("mkdir -p ").append(q(dstDir)).append("; ");
            c.append("if [ -e ").append(q(dst)).append(" ]; then mkdir -p ").append(q(backupDir)).append("; cp -af ").append(q(dst)).append(" ").append(q(backupDst)).append("; fi; ");
            c.append("cp -af ").append(q(src)).append(" ").append(q(dst)).append("; ");
        }
        return c.toString();
    }

    private ShellResult runPrivileged(String command) throws Exception {
        if (isShizukuReady()) {
            appendLog("Apply menggunakan Shizuku...");
            Process p = startShizukuShell(new String[]{"sh", "-c", command});
            return readProcess(p);
        }
        if (isRootAvailable()) {
            appendLog("Apply menggunakan root/su...");
            return runNormal(new String[]{"su", "-c", command});
        }
        throw new IllegalStateException("Shizuku/root belum aktif. Aktifkan Shizuku atau root dulu.");
    }

    private Process startShizukuShell(String[] cmd) throws Exception {
        java.lang.reflect.Method method = Shizuku.class.getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
        method.setAccessible(true);
        return (Process) method.invoke(null, new Object[]{cmd, null, null});
    }

    private String downloadText(String urlString) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(urlString).openConnection();
        con.setConnectTimeout(20000);
        con.setReadTimeout(30000);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            return sb.toString();
        } finally {
            con.disconnect();
        }
    }

    private void downloadFile(String urlString, File out) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(urlString).openConnection();
        con.setConnectTimeout(20000);
        con.setReadTimeout(60000);
        int total = con.getContentLength();
        try (InputStream in = new BufferedInputStream(con.getInputStream()); FileOutputStream fos = new FileOutputStream(out)) {
            byte[] buf = new byte[1024 * 128];
            int n;
            long done = 0;
            long lastLog = 0;
            while ((n = in.read(buf)) > 0) {
                fos.write(buf, 0, n);
                done += n;
                if (System.currentTimeMillis() - lastLog > 1200) {
                    lastLog = System.currentTimeMillis();
                    if (total > 0) appendLog("Download " + (done * 100 / total) + "%");
                }
            }
        } finally {
            con.disconnect();
        }
    }

    private void unzip(File zip, File dest) throws Exception {
        if (!dest.mkdirs() && !dest.exists()) throw new IllegalStateException("Tidak bisa membuat folder extract.");
        String destPath = dest.getCanonicalPath() + File.separator;
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zip))) {
            ZipEntry e;
            byte[] buf = new byte[1024 * 128];
            while ((e = zis.getNextEntry()) != null) {
                File out = new File(dest, e.getName());
                if (!out.getCanonicalPath().startsWith(destPath)) throw new IllegalStateException("Zip path tidak aman: " + e.getName());
                if (e.isDirectory()) {
                    out.mkdirs();
                } else {
                    File parent = out.getParentFile();
                    if (parent != null) parent.mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(out)) {
                        int n;
                        while ((n = zis.read(buf)) > 0) fos.write(buf, 0, n);
                    }
                }
            }
        }
    }

    private List<String> listZipFiles(File zip) throws Exception {
        List<String> list = new ArrayList<>();
        try (ZipFile zf = new ZipFile(zip)) {
            Enumeration<? extends ZipEntry> en = zf.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                if (!e.isDirectory()) {
                    String n = e.getName().replace('\\', '/');
                    if (!n.startsWith("../") && !n.contains("/../")) list.add(n);
                }
            }
        }
        return list;
    }

    private String sha256(File f) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new FileInputStream(f)) {
            byte[] b = new byte[1024 * 128];
            int n;
            while ((n = in.read(b)) > 0) md.update(b, 0, n);
        }
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte x : digest) sb.append(String.format(Locale.US, "%02x", x));
        return sb.toString();
    }

    private ShellResult runNormal(String[] cmd) throws Exception {
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        return readProcess(p);
    }

    private ShellResult readProcess(Process p) throws Exception {
        StringBuilder out = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) out.append(line).append('\n');
        }
        int code = p.waitFor();
        return new ShellResult(code, out.toString());
    }

    private void launchGame() {
        Intent i = getPackageManager().getLaunchIntentForPackage(GAME_PACKAGE);
        if (i != null) {
            startActivity(i);
        } else {
            appendLog("FIFA 16 tidak ditemukan: " + GAME_PACKAGE);
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + GAME_PACKAGE)));
            } catch (Throwable ignored) { }
        }
    }

    private static String q(String s) {
        return "'" + s.replace("'", "'\\''") + "'";
    }

    private static String parentOf(String p) {
        int i = p.lastIndexOf('/');
        return i <= 0 ? "/" : p.substring(0, i);
    }

    private void deleteRecursive(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null) for (File x : files) deleteRecursive(x);
        }
        f.delete();
    }

    private void appendLog(String s) {
        main.post(() -> logView.append("\n" + s));
    }

    private static class ShellResult {
        final int code;
        final String output;
        ShellResult(int c, String o) { code = c; output = o; }
    }
}
