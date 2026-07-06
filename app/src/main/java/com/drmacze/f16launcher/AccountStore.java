package com.drmacze.f16launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * v7.9.28: Multi-account manager — store list of accounts in SharedPreferences.
 *
 * Accounts disimpan sebagai JSON array di prefs "dlavie_community" key "accounts".
 * Active account ditrack via key "active_user_id".
 *
 * Max 5 accounts per device.
 */
public class AccountStore {
    private static final String TAG = "AccountStore";
    private static final String PREFS = "dlavie_community";
    private static final String KEY_ACCOUNTS = "accounts";
    private static final String KEY_ACTIVE = "active_user_id";
    private static final int MAX_ACCOUNTS = 5;

    public static class Account {
        public String userId;
        public String email;
        public String username;
        public String displayName;
        public String avatarUrl;
        public String role;
        public String country;
        public String accessToken;
        public String refreshToken;

        public JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("user_id", userId);
                o.put("email", email == null ? "" : email);
                o.put("username", username == null ? "" : username);
                o.put("display_name", displayName == null ? "" : displayName);
                o.put("avatar_url", avatarUrl == null ? "" : avatarUrl);
                o.put("role", role == null ? "member" : role);
                o.put("country", country == null ? "" : country);
                o.put("access_token", accessToken == null ? "" : accessToken);
                o.put("refresh_token", refreshToken == null ? "" : refreshToken);
            } catch (Exception e) {
                Log.e(TAG, "toJson failed", e);
            }
            return o;
        }

        public static Account fromJson(JSONObject o) {
            Account a = new Account();
            a.userId = o.optString("user_id", "");
            a.email = o.optString("email", "");
            a.username = o.optString("username", "");
            a.displayName = o.optString("display_name", "");
            a.avatarUrl = o.optString("avatar_url", "");
            a.role = o.optString("role", "member");
            a.country = o.optString("country", "");
            a.accessToken = o.optString("access_token", "");
            a.refreshToken = o.optString("refresh_token", "");
            return a;
        }
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Get all saved accounts. */
    public static java.util.List<Account> listAccounts(Context ctx) {
        java.util.List<Account> result = new java.util.ArrayList<>();
        try {
            String json = prefs(ctx).getString(KEY_ACCOUNTS, "[]");
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                result.add(Account.fromJson(arr.getJSONObject(i)));
            }
        } catch (Exception e) {
            Log.e(TAG, "listAccounts failed", e);
        }
        return result;
    }

    /** Save/update account in list. Called after login. */
    public static void saveAccount(Context ctx, Account account) {
        if (account.userId == null || account.userId.isEmpty()) return;
        try {
            java.util.List<Account> accounts = listAccounts(ctx);
            // Remove existing with same userId
            accounts.removeIf(a -> a.userId.equals(account.userId));
            // Add new
            accounts.add(account);
            // Cap at MAX_ACCOUNTS (remove oldest = first)
            while (accounts.size() > MAX_ACCOUNTS) {
                accounts.remove(0);
            }
            // Save
            JSONArray arr = new JSONArray();
            for (Account a : accounts) {
                arr.put(a.toJson());
            }
            prefs(ctx).edit().putString(KEY_ACCOUNTS, arr.toString()).putString(KEY_ACTIVE, account.userId).apply();
        } catch (Exception e) {
            Log.e(TAG, "saveAccount failed", e);
        }
    }

    /** Switch to a different account by userId. Returns true if successful. */
    public static boolean switchAccount(Context ctx, String userId) {
        try {
            java.util.List<Account> accounts = listAccounts(ctx);
            for (Account a : accounts) {
                if (a.userId.equals(userId)) {
                    SharedPreferences.Editor e = prefs(ctx).edit();
                    e.putString("access_token", a.accessToken);
                    e.putString("refresh_token", a.refreshToken);
                    e.putString("user_id", a.userId);
                    e.putString("username", a.username);
                    e.putString("display_name", a.displayName);
                    e.putString("avatar_url", a.avatarUrl);
                    e.putString("role", a.role);
                    if (!a.country.isEmpty()) e.putString("country", a.country);
                    e.putBoolean("is_guest", false);
                    e.putString(KEY_ACTIVE, userId);
                    e.apply();
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "switchAccount failed", e);
        }
        return false;
    }

    /** Remove account from list. Returns true if it was the active account. */
    public static boolean removeAccount(Context ctx, String userId) {
        try {
            java.util.List<Account> accounts = listAccounts(ctx);
            String activeId = prefs(ctx).getString(KEY_ACTIVE, "");
            accounts.removeIf(a -> a.userId.equals(userId));

            JSONArray arr = new JSONArray();
            for (Account a : accounts) {
                arr.put(a.toJson());
            }
            SharedPreferences.Editor e = prefs(ctx).edit();
            e.putString(KEY_ACCOUNTS, arr.toString());

            boolean wasActive = userId.equals(activeId);
            if (wasActive) {
                if (accounts.isEmpty()) {
                    // No accounts left — clear active
                    e.remove(KEY_ACTIVE);
                } else {
                    // Switch to first remaining account
                    Account next = accounts.get(0);
                    e.putString("access_token", next.accessToken);
                    e.putString("refresh_token", next.refreshToken);
                    e.putString("user_id", next.userId);
                    e.putString("username", next.username);
                    e.putString("display_name", next.displayName);
                    e.putString("avatar_url", next.avatarUrl);
                    e.putString("role", next.role);
                    e.putString(KEY_ACTIVE, next.userId);
                }
            }
            e.apply();
            return wasActive;
        } catch (Exception e) {
            Log.e(TAG, "removeAccount failed", e);
            return false;
        }
    }

    /** Get active user ID. */
    public static String getActiveUserId(Context ctx) {
        return prefs(ctx).getString(KEY_ACTIVE, "");
    }

    /** Get active account. */
    public static Account getActiveAccount(Context ctx) {
        String activeId = getActiveUserId(ctx);
        if (activeId.isEmpty()) return null;
        for (Account a : listAccounts(ctx)) {
            if (a.userId.equals(activeId)) return a;
        }
        return null;
    }
}
