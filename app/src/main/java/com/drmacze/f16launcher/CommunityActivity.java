package com.drmacze.f16launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommunityActivity extends Activity {
    private static final int BG = Color.rgb(5, 7, 14);
    private static final int GLASS = Color.argb(220, 16, 22, 36);
    private static final int GLASS2 = Color.argb(235, 24, 32, 52);
    private static final int TEXT = Color.rgb(247, 250, 255);
    private static final int MUTED = Color.rgb(158, 173, 204);
    private static final int BLUE = Color.rgb(0, 198, 255);
    private static final int CANDY = Color.rgb(72, 116, 255);
    private static final int GREEN = Color.rgb(31, 221, 144);
    private static final int BORDER = Color.argb(135, 98, 143, 255);

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private CommunityApi api;
    private LinearLayout root;
    private LinearLayout categoriesBox;
    private LinearLayout topicsBox;
    private LinearLayout threadBox;
    private TextView status;
    private String selectedCategoryId = "";
    private String selectedTopicId = "";
    private String selectedTopicTitle = "";

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        api = new CommunityApi(this);
        if (api.loggedIn()) showForum(); else showAuth();
    }

    private void base() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        root = col();
        root.setPadding(dp(16), dp(14), dp(16), dp(14));
        scroll.addView(root);
        setContentView(scroll);
    }

    private void showAuth() {
        base();
        LinearLayout shell = row();
        root.addView(shell, mp());
        LinearLayout left = card();
        shell.addView(left, weight(1.1f));
        margin(left,0,0,dp(14),0);
        TextView title = txt("DLavie Community", 34, TEXT, true);
        left.addView(title);
        left.addView(txt("Login/Register untuk membuat topic, reply, mention @username, dan nanti upload file.", 14, MUTED, false));
        left.addView(info("Username", "Wajib, 3-24 karakter: huruf, angka, underscore."));
        left.addView(info("Display Name", "Wajib, 2-40 karakter. Nama tampil di community."));
        left.addView(info("Avatar", "Opsional. Bisa diisi URL avatar atau dikosongkan."));
        left.addView(info("Upload File", "Backend metadata siap. Storage bucket akan diaktifkan setelah Supabase Storage tersedia."));
        Button back = btn("← Back to Launcher", GLASS2, TEXT);
        back.setOnClickListener(v -> finish());
        left.addView(back);

        LinearLayout form = card();
        shell.addView(form, weight(1));
        form.addView(section("ACCOUNT"));
        EditText email = input("Email");
        EditText pass = input("Password");
        EditText username = input("Username wajib untuk register");
        EditText display = input("Display name wajib untuk register");
        EditText avatar = input("Avatar URL opsional");
        form.addView(email); form.addView(pass); form.addView(username); form.addView(display); form.addView(avatar);
        status = txt("Belum login.", 12, MUTED, false);
        margin(status,0,dp(8),0,0);
        form.addView(status);
        Button login = btn("Login", CANDY, Color.WHITE);
        Button register = btn("Register", BLUE, Color.rgb(2,18,31));
        form.addView(login); form.addView(register);
        login.setOnClickListener(v -> doLogin(email.getText().toString(), pass.getText().toString()));
        register.setOnClickListener(v -> doRegister(email.getText().toString(), pass.getText().toString(), username.getText().toString(), display.getText().toString(), avatar.getText().toString()));
    }

    private void doLogin(String email, String pass) {
        setStatus("Login...");
        io.execute(() -> {
            try { api.login(email, pass); main.post(this::showForum); }
            catch (Throwable t) { setStatus("Login gagal: " + t.getMessage()); }
        });
    }

    private void doRegister(String email, String pass, String username, String display, String avatar) {
        if (!username.matches("[a-zA-Z0-9_]{3,24}")) { setStatus("Username wajib 3-24 karakter: huruf, angka, underscore."); return; }
        if (display.trim().length() < 2) { setStatus("Display name wajib minimal 2 karakter."); return; }
        setStatus("Register...");
        io.execute(() -> {
            try { api.register(email, pass, username, display, avatar); if (api.loggedIn()) main.post(this::showForum); else setStatus("Register berhasil. Jika email confirmation aktif, cek email lalu login."); }
            catch (Throwable t) { setStatus("Register gagal: " + t.getMessage()); }
        });
    }

    private void showForum() {
        base();
        LinearLayout top = row();
        top.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(top, mp());
        LinearLayout title = col();
        top.addView(title, weight(1));
        title.addView(txt("DLavie Community", 28, TEXT, true));
        title.addView(txt("@" + safe(api.username()) + " • " + safe(api.displayName()), 13, MUTED, false));
        Button profile = btn("Profile", GLASS2, TEXT);
        Button logout = btn("Logout", GLASS2, TEXT);
        Button back = btn("Launcher", GLASS2, TEXT);
        top.addView(profile); margin(profile,dp(8),0,dp(8),0);
        top.addView(logout); margin(logout,0,0,dp(8),0);
        top.addView(back);
        profile.setOnClickListener(v -> loadProfileCard());
        logout.setOnClickListener(v -> { api.logout(); showAuth(); });
        back.setOnClickListener(v -> finish());

        LinearLayout hub = row();
        margin(hub,0,dp(12),0,0);
        root.addView(hub, mp());
        categoriesBox = card(); topicsBox = card(); threadBox = card();
        hub.addView(categoriesBox, new LinearLayout.LayoutParams(dp(190), LinearLayout.LayoutParams.WRAP_CONTENT));
        margin(categoriesBox,0,0,dp(12),0);
        hub.addView(topicsBox, weight(1)); margin(topicsBox,0,0,dp(12),0);
        hub.addView(threadBox, weight(1.25f));
        loadCategories();
        loadTopics();
        emptyThread("Pilih topic untuk membaca thread dan membalas.");
    }

    private void loadCategories() {
        categoriesBox.removeAllViews();
        categoriesBox.addView(section("CHANNELS"));
        Button all = btn("All Topics", CANDY, Color.WHITE);
        categoriesBox.addView(all);
        all.setOnClickListener(v -> { selectedCategoryId = ""; loadTopics(); });
        io.execute(() -> {
            try {
                JSONArray arr = api.categories();
                main.post(() -> {
                    for (int i=0;i<arr.length();i++) try {
                        JSONObject c = arr.getJSONObject(i);
                        Button b = btn(c.optString("name"), GLASS2, TEXT);
                        b.setOnClickListener(v -> { selectedCategoryId = c.optString("id"); loadTopics(); });
                        categoriesBox.addView(b);
                    } catch (Throwable ignored) { }
                });
            } catch (Throwable t) { main.post(() -> categoriesBox.addView(txt("Gagal load kategori: " + t.getMessage(), 12, MUTED, false))); }
        });
    }

    private void loadTopics() {
        topicsBox.removeAllViews();
        topicsBox.addView(section("TOPICS"));
        EditText newTitle = input("Judul topic baru");
        EditText newBody = input("Isi topic baru. Bisa tag @username");
        topicsBox.addView(newTitle); topicsBox.addView(newBody);
        Button create = btn("+ New Topic", BLUE, Color.rgb(2,18,31));
        topicsBox.addView(create);
        create.setOnClickListener(v -> createTopic(newTitle.getText().toString(), newBody.getText().toString()));
        TextView loading = txt("Loading topics...", 12, MUTED, false);
        topicsBox.addView(loading);
        io.execute(() -> {
            try {
                JSONArray arr = api.topics(selectedCategoryId);
                main.post(() -> {
                    topicsBox.removeView(loading);
                    if (arr.length() == 0) topicsBox.addView(txt("Belum ada topic.", 12, MUTED, false));
                    for (int i=0;i<arr.length();i++) try {
                        JSONObject t = arr.getJSONObject(i);
                        Button b = btn(t.optString("title") + "  •  " + t.optInt("reply_count") + " replies", GLASS2, TEXT);
                        b.setGravity(Gravity.CENTER_VERTICAL);
                        b.setOnClickListener(v -> { selectedTopicId = t.optString("id"); selectedTopicTitle = t.optString("title"); loadThread(t); });
                        topicsBox.addView(b);
                    } catch (Throwable ignored) { }
                });
            } catch (Throwable e) { main.post(() -> { topicsBox.removeView(loading); topicsBox.addView(txt("Gagal load topic: " + e.getMessage(), 12, MUTED, false)); }); }
        });
    }

    private void createTopic(String title, String body) {
        if (selectedCategoryId.isEmpty()) { toast("Pilih channel dulu sebelum membuat topic."); return; }
        if (title.trim().length() < 4 || body.trim().isEmpty()) { toast("Judul minimal 4 karakter dan isi wajib diisi."); return; }
        toast("Membuat topic...");
        io.execute(() -> { try { JSONObject t = api.createTopic(selectedCategoryId, title, body); selectedTopicId = t.optString("id"); selectedTopicTitle = t.optString("title"); main.post(() -> { loadTopics(); loadThread(t); }); } catch (Throwable e) { toast("Gagal membuat topic: " + e.getMessage()); } });
    }

    private void loadThread(JSONObject topic) {
        threadBox.removeAllViews();
        threadBox.addView(section("THREAD"));
        threadBox.addView(txt(topic.optString("title"), 22, TEXT, true));
        threadBox.addView(txt(topic.optString("body"), 13, MUTED, false));
        Button attach = btn("Upload File / Screenshot", GLASS2, TEXT);
        attach.setOnClickListener(v -> toast("Upload file butuh Storage bucket aktif. Metadata attachments sudah siap di backend."));
        threadBox.addView(attach);
        TextView loading = txt("Loading replies...", 12, MUTED, false);
        threadBox.addView(loading);
        io.execute(() -> {
            try {
                JSONArray posts = api.posts(topic.optString("id"));
                main.post(() -> {
                    threadBox.removeView(loading);
                    for (int i=0;i<posts.length();i++) try { addPost(posts.getJSONObject(i)); } catch (Throwable ignored) { }
                    replyComposer();
                });
            } catch (Throwable e) { main.post(() -> { threadBox.removeView(loading); threadBox.addView(txt("Gagal load reply: " + e.getMessage(), 12, MUTED, false)); replyComposer(); }); }
        });
    }

    private void addPost(JSONObject p) {
        LinearLayout box = col();
        box.setPadding(dp(10), dp(8), dp(10), dp(8));
        box.setBackground(glass(GLASS2, 16));
        margin(box,0,dp(8),0,0);
        box.addView(txt(shortId(p.optString("author_id")) + " • " + p.optString("created_at"), 10, MUTED, false));
        box.addView(txt(p.optString("body"), 13, TEXT, false));
        Button reply = btn("Reply to this", GLASS, TEXT);
        reply.setOnClickListener(v -> toast("Reply target disiapkan. Tulis balasan di box bawah dengan mention @username."));
        box.addView(reply);
        threadBox.addView(box);
    }

    private void replyComposer() {
        EditText reply = input("Tulis reply. Bisa tag @username");
        threadBox.addView(reply);
        Button send = btn("Send Reply", GREEN, Color.rgb(2,20,12));
        threadBox.addView(send);
        send.setOnClickListener(v -> {
            String text = reply.getText().toString();
            if (text.trim().isEmpty()) { toast("Reply tidak boleh kosong."); return; }
            toast("Mengirim reply...");
            io.execute(() -> {
                try {
                    api.createPost(selectedTopicId, "", text);
                    JSONObject t = new JSONObject();
                    t.put("id", selectedTopicId);
                    t.put("title", selectedTopicTitle);
                    t.put("body", "");
                    main.post(() -> {
                        reply.setText("");
                        loadTopics();
                        loadThread(t);
                    });
                } catch (Throwable e) {
                    toast("Gagal reply: " + e.getMessage());
                }
            });
        });
    }

    private void emptyThread(String s) {
        threadBox.removeAllViews();
        threadBox.addView(section("THREAD"));
        threadBox.addView(txt(s, 14, MUTED, false));
    }

    private void loadProfileCard() {
        threadBox.removeAllViews();
        threadBox.addView(section("PROFILE"));
        threadBox.addView(txt(safe(api.displayName()), 24, TEXT, true));
        threadBox.addView(info("Username", "@" + safe(api.username())));
        threadBox.addView(info("Avatar", "Opsional. Bisa ditambahkan nanti dari URL avatar di profile."));
        io.execute(() -> { try { JSONObject p = api.loadMyProfile(); main.post(() -> { threadBox.addView(info("Role", p.optString("role", "member"))); threadBox.addView(info("Bio", p.optString("bio", "Belum diisi"))); }); } catch (Throwable e) { toast("Gagal load profile: " + e.getMessage()); } });
    }

    private LinearLayout info(String a, String b) { LinearLayout x = col(); margin(x,0,dp(10),0,0); x.addView(txt(a, 11, MUTED, true)); x.addView(txt(b, 13, TEXT, false)); return x; }
    private EditText input(String hint) { EditText e = new EditText(this); e.setHint(hint); e.setHintTextColor(MUTED); e.setTextColor(TEXT); e.setTextSize(13); e.setSingleLine(false); e.setMinLines(1); e.setPadding(dp(12), dp(8), dp(12), dp(8)); e.setBackground(stroke(GLASS2, 15)); margin(e,0,dp(10),0,0); return e; }
    private TextView section(String s) { TextView t = txt(s, 11, MUTED, true); t.setLetterSpacing(.12f); return t; }
    private TextView txt(String s, int sp, int color, boolean bold) { TextView t = new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(color); if (bold) t.setTypeface(Typeface.DEFAULT_BOLD); return t; }
    private Button btn(String s, int bg, int fg) { Button b = new Button(this); b.setAllCaps(false); b.setText(s); b.setTextColor(fg); b.setTextSize(13); b.setTypeface(Typeface.DEFAULT_BOLD); b.setMinHeight(dp(40)); b.setBackground(glass(bg, 16)); margin(b,0,dp(8),0,0); return b; }
    private LinearLayout card() { LinearLayout c = col(); c.setPadding(dp(14), dp(12), dp(14), dp(12)); c.setBackground(stroke(GLASS, 24)); return c; }
    private GradientDrawable glass(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private GradientDrawable stroke(int color, int radius) { GradientDrawable d = glass(color, radius); d.setStroke(dp(1), BORDER); return d; }
    private LinearLayout row() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); return l; }
    private LinearLayout col() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout.LayoutParams mp() { return new LinearLayout.LayoutParams(-1, -2); }
    private LinearLayout.LayoutParams weight(float w) { return new LinearLayout.LayoutParams(0, -2, w); }
    private void margin(View v, int l, int t, int r, int b) { LinearLayout.LayoutParams p = (LinearLayout.LayoutParams) v.getLayoutParams(); if (p == null) p = mp(); p.setMargins(l,t,r,b); v.setLayoutParams(p); }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
    private void setStatus(String s) { main.post(() -> { if (status != null) status.setText(s); }); }
    private void toast(String s) { main.post(() -> { if (threadBox != null) threadBox.addView(txt(s, 12, BLUE, false)); else if (status != null) status.setText(s); }); }
    private String shortId(String s) { return s == null || s.length() < 8 ? "user" : "user:" + s.substring(0, 8); }
    private String safe(String s) { return s == null || s.isEmpty() ? "unknown" : s; }
}
