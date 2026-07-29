from pathlib import Path
import re

GUIDED = Path("app/src/main/java/com/drmacze/f16launcher/DLavieGuidedActivity.kt")
SPLASH = Path("app/src/main/java/com/drmacze/f16launcher/ShinySplashActivity.kt")

for path in (GUIDED, SPLASH):
    if not path.exists():
        raise SystemExit(f"Required source not found: {path}")

# ── ShinySplashActivity: dlavie://connect never carries credentials ─────────────
splash = SPLASH.read_text(encoding="utf-8")
connect_start = "        // ── DLavie Portal Connect: check for deep link ──"
connect_end = "        // ── v7.9.95: Game sharing deep link"
if connect_start not in splash or connect_end not in splash:
    raise SystemExit("Portal connect markers not found in ShinySplashActivity")

start = splash.index(connect_start)
end = splash.index(connect_end, start)
secure_connect = '''        // ── v325 SECURITY: Portal links never carry session credentials ──
        val portalData = intent?.data
        if (portalData != null && portalData.scheme == "dlavie" && portalData.host == "connect") {
            val legacySecrets = PortalAuthSecurity.containsLegacyPortalSecrets(portalData)
            if (legacySecrets) {
                android.util.Log.w("DLaviePortal", "Rejected legacy connect link containing credentials")
                android.widget.Toast.makeText(
                    this,
                    "Tautan login lama ditolak demi keamanan. Silakan login langsung di launcher.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }

            val target = Intent(this, DLavieGuidedActivity::class.java).apply {
                putExtra("portal_login_requested", true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(target)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
            return
        }

'''
splash = splash[:start] + secure_connect + splash[end:]

route_start = "                            val target = if (!token.isNullOrBlank()) {"
route_end = "                            target.addFlags("
if route_start not in splash or route_end not in splash:
    raise SystemExit("Auth routing markers not found in ShinySplashActivity")

start = splash.index(route_start)
end = splash.index(route_end, start)
secure_route = '''                            val target = if (!token.isNullOrBlank() && PortalAuthSecurity.isJwtUsable(token)) {
                                // Sync only a locally valid Supabase session to community state.
                                val refresh = prefs.getString("refresh_token", "") ?: ""
                                val userId = try {
                                    val payload = token.split(".").getOrNull(1) ?: ""
                                    val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
                                    val decoded = android.util.Base64.decode(padded, android.util.Base64.URL_SAFE)
                                    org.json.JSONObject(String(decoded)).optString("sub", "")
                                } catch (_: Exception) { "" }
                                communityPrefs.edit()
                                    .putString("access_token", token)
                                    .putString("refresh_token", refresh)
                                    .putString("user_id", userId)
                                    .apply()
                                Intent(this, ModernLauncherActivity::class.java)
                            } else {
                                if (!token.isNullOrBlank()) PortalAuthSecurity.clearSession(this)
                                if (isGuest) {
                                    Intent(this, ModernLauncherActivity::class.java)
                                } else {
                                    Intent(this, DLavieGuidedActivity::class.java)
                                }
                            }
'''
splash = splash[:start] + secure_route + splash[end:]

if "webToken" in splash or "getQueryParameter(\"refresh\")" in splash:
    raise SystemExit("Legacy token bridge remains in ShinySplashActivity")
SPLASH.write_text(splash, encoding="utf-8")

# ── DLavieGuidedActivity: verify callbacks and remove paste-token flow ──────────
guided = GUIDED.read_text(encoding="utf-8")
if "import androidx.lifecycle.lifecycleScope" not in guided:
    guided = guided.replace(
        "import androidx.activity.compose.setContent\n",
        "import androidx.activity.compose.setContent\nimport androidx.lifecycle.lifecycleScope\n",
        1,
    )

class_methods_start = "    override fun onCreate(savedInstanceState: Bundle?) {"
class_methods_end = "\n}\n\n// ─── Maintenance config"
if class_methods_start not in guided or class_methods_end not in guided:
    raise SystemExit("DLavieGuidedActivity lifecycle markers not found")

start = guided.index(class_methods_start)
end = guided.index(class_methods_end, start)
secure_methods = r'''    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // OAuth callbacks must be processed before an existing session can redirect away.
        if (intent?.action == Intent.ACTION_VIEW && PortalAuthSecurity.isTrustedAuthCallback(intent?.data)) {
            handleDeepLink(intent)
            return
        }

        val existing = loadSession(this)
        if (existing != null) {
            syncToCommunityPrefs(this, existing)
            startActivity(Intent(this, ModernLauncherActivity::class.java))
            finish()
            return
        }

        setContent { DLavieGuidedApp() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_VIEW && PortalAuthSecurity.isTrustedAuthCallback(intent.data)) {
            handleDeepLink(intent)
        }
    }

    /**
     * Accepts OAuth only after the launcher initiated the flow, then verifies the
     * returned access token with Supabase Auth before storing any session data.
     */
    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data
        if (intent?.action != Intent.ACTION_VIEW || !PortalAuthSecurity.isTrustedAuthCallback(data)) {
            return
        }

        if (!PortalAuthSecurity.consumeOAuthAttempt(this)) {
            PortalAuthSecurity.clearSession(this)
            deepLinkResult = "Error: Permintaan login tidak dikenali atau sudah kedaluwarsa. Mulai login lagi dari launcher."
            setContent { DLavieGuidedApp(deepLinkResult = deepLinkResult) }
            return
        }

        val fragment = data?.fragment.orEmpty()
        val query = data?.query.orEmpty()
        val params = mutableMapOf<String, String>()
        (fragment + "&" + query)
            .split("&")
            .filter { it.contains("=") }
            .forEach { pair ->
                val index = pair.indexOf('=')
                if (index > 0) {
                    val key = pair.substring(0, index)
                    val value = java.net.URLDecoder.decode(pair.substring(index + 1), "UTF-8")
                    params[key] = value
                }
            }

        val accessToken = params["access_token"]
        val refreshToken = params["refresh_token"]
        val error = params["error"]

        if (!error.isNullOrBlank()) {
            PortalAuthSecurity.clearSession(this)
            val description = params["error_description"] ?: error
            deepLinkResult = "Error: Google login gagal — $description"
            setContent { DLavieGuidedApp(deepLinkResult = deepLinkResult) }
            return
        }

        if (accessToken.isNullOrBlank() || refreshToken.isNullOrBlank()) {
            PortalAuthSecurity.clearSession(this)
            deepLinkResult = "Error: Sesi login tidak lengkap. Mulai login lagi dari launcher."
            setContent { DLavieGuidedApp(deepLinkResult = deepLinkResult) }
            return
        }

        deepLinkResult = "Memverifikasi sesi login…"
        setContent { DLavieGuidedApp(deepLinkResult = deepLinkResult) }

        lifecycleScope.launch {
            val verified = withContext(Dispatchers.IO) {
                PortalAuthSecurity.verifySession(accessToken)
            }

            if (verified == null) {
                PortalAuthSecurity.clearSession(this@DLavieGuidedActivity)
                deepLinkResult = "Error: Sesi tidak dapat diverifikasi. Silakan login kembali."
                setContent { DLavieGuidedApp(deepLinkResult = deepLinkResult) }
                return@launch
            }

            val session = AuthSession(accessToken, refreshToken, verified.email)
            saveSession(this@DLavieGuidedActivity, session)
            syncToCommunityPrefs(this@DLavieGuidedActivity, session)
            CommunityApi(this@DLavieGuidedActivity).clearGuest()

            withContext(Dispatchers.IO) {
                runCatching { CommunityApi(this@DLavieGuidedActivity).loadMyProfile() }
            }
            runCatching {
                Telemetry.track(
                    this@DLavieGuidedActivity,
                    Telemetry.EVT_LOGIN,
                    mapOf("method" to "google_oauth_verified")
                )
            }

            deepLinkResult = "OK: Login Google terverifikasi. Memuat launcher…"
            setContent { DLavieGuidedApp(deepLinkResult = deepLinkResult) }
            delay(900)
            startActivity(
                Intent(this@DLavieGuidedActivity, ModernLauncherActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            finish()
        }
    }
}'''
guided = guided[:start] + secure_methods + guided[end + 2:]

# Remove the legacy UI that instructed users to paste credential-bearing URLs.
manual_start = "                // ── v7.9.54: Connect Manual"
manual_end = "            // ── Mode: LOGIN / REGISTER / FORGOT"
if manual_start not in guided or manual_end not in guided:
    raise SystemExit("Manual connect block markers not found")
start = guided.index(manual_start)
end = guided.index(manual_end, start)
security_note = '''                Spacer(Modifier.height(18.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White.copy(alpha = 0.035f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = GuideGreen, modifier = Modifier.size(17.dp))
                        Text(
                            "Login dilakukan langsung di launcher. DLavie tidak pernah meminta Anda menyalin token atau URL sesi.",
                            color = GuideMuted,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

'''
guided = guided[:start] + security_note + guided[end:]

# Portal is informational; authentication happens directly in the launcher.
guided = guided.replace(
    '"Sign in or register your DLavie account via the portal website."',
    '"Login aman dilakukan langsung di launcher. Portal web tidak mengirim token ke aplikasi."',
)
guided = guided.replace('label = "Connect to Portal"', 'label = "Buka Portal DLavie"')
guided = guided.replace(
    '"Already connected? The launcher will auto-login if your token is still valid."',
    '"Gunakan email/password atau Google di launcher. Jangan pernah membagikan token login."',
)

# Require a recent launcher-initiated OAuth attempt.
guided = guided.replace(
    '        val redirect = "dlavie://auth-callback"\n',
    '        PortalAuthSecurity.markOAuthAttempt(context)\n        val redirect = "dlavie://auth-callback"\n',
    1,
)

# Existing persisted sessions must at least pass issuer/audience/expiry checks.
load_session_pattern = re.compile(
    r'private fun loadSession\(context: Context\): AuthSession\? \{ val p = context\.getSharedPreferences\(PREF_AUTH, Context\.MODE_PRIVATE\); val token = p\.getString\(PREF_TOKEN, null\) \?: return null; val refresh = p\.getString\(PREF_REFRESH, ""\) \?: ""; val email = p\.getString\(PREF_EMAIL, ""\) \?: ""; return AuthSession\(token, refresh, email\) \}'
)
secure_load_session = '''private fun loadSession(context: Context): AuthSession? {
    val prefs = context.getSharedPreferences(PREF_AUTH, Context.MODE_PRIVATE)
    val token = prefs.getString(PREF_TOKEN, null) ?: return null
    if (!PortalAuthSecurity.isJwtUsable(token)) {
        PortalAuthSecurity.clearSession(context)
        return null
    }
    val refresh = prefs.getString(PREF_REFRESH, "") ?: ""
    val email = prefs.getString(PREF_EMAIL, "") ?: ""
    return AuthSession(token, refresh, email)
}'''
guided, replacements = load_session_pattern.subn(secure_load_session, guided, count=1)
if replacements != 1:
    raise SystemExit("loadSession implementation could not be hardened")

# Verify password/signup sessions with /auth/v1/user before saving them.
register_identity = '''    val userObj = json.optJSONObject("user")
    val userEmail = userObj?.optString("email", email) ?: email
    val userId = userObj?.optString("id", "") ?: ""'''
register_verified = '''    val userObj = json.optJSONObject("user")
    val verifiedUser = if (token.isNotBlank()) PortalAuthSecurity.verifySession(token) else null
    if (token.isNotBlank() && verifiedUser == null) {
        throw IllegalStateException("Sesi pendaftaran tidak dapat diverifikasi. Silakan login kembali.")
    }
    val userEmail = verifiedUser?.email?.ifBlank { userObj?.optString("email", email) ?: email }
        ?: (userObj?.optString("email", email) ?: email)
    val userId = verifiedUser?.id ?: (userObj?.optString("id", "") ?: "")'''
if register_identity not in guided:
    raise SystemExit("Register identity block not found")
guided = guided.replace(register_identity, register_verified, 1)

auth_identity = '''    val userEmail = json.optJSONObject("user")?.optString("email", email) ?: email
    val userId = json.optJSONObject("user")?.optString("id", "") ?: ""'''
auth_verified = '''    val verifiedUser = if (token.isNotBlank()) PortalAuthSecurity.verifySession(token) else null
    if (token.isNotBlank() && verifiedUser == null) {
        throw IllegalStateException("Sesi login tidak dapat diverifikasi. Silakan coba lagi.")
    }
    val userEmail = verifiedUser?.email?.ifBlank { json.optJSONObject("user")?.optString("email", email) ?: email }
        ?: (json.optJSONObject("user")?.optString("email", email) ?: email)
    val userId = verifiedUser?.id ?: (json.optJSONObject("user")?.optString("id", "") ?: "")'''
if auth_identity not in guided:
    raise SystemExit("Password auth identity block not found")
guided = guided.replace(auth_identity, auth_verified, 1)

# Security assertions: fail the build if legacy credential transfer returns.
for forbidden in (
    'placeholder = { Text("dlavie://connect?token=',
    'Text("Connect Manual"',
    'putString("access_token", token)\n                                    .putString("refresh_token", refresh)\n                                    .putString("user_id", uid)',
):
    if forbidden in guided:
        raise SystemExit(f"Legacy portal auth fragment remains: {forbidden}")
if "PortalAuthSecurity.verifySession(accessToken)" not in guided:
    raise SystemExit("OAuth callback verification was not installed")

GUIDED.write_text(guided, encoding="utf-8")
print("Portal auth hardening applied: token bridge removed, callbacks verified")
