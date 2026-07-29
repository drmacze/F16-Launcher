from pathlib import Path
import re

source = Path("app/src/main/java/com/drmacze/f16launcher/DLavieGuidedActivity.kt")
if not source.exists():
    raise SystemExit("DLavieGuidedActivity.kt not found")

text = source.read_text(encoding="utf-8")

method_start = "    private fun handleDeepLink(intent: Intent?) {"
method_end_marker = "\n    }\n}\n\n// ─── Maintenance config"
if method_start not in text or method_end_marker not in text:
    raise SystemExit("Secure OAuth callback markers not found")

start = text.index(method_start)
end = text.index(method_end_marker, start) + len("\n    }")

pkce_method = r'''    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data
        if (intent?.action != Intent.ACTION_VIEW || !PortalAuthSecurity.isTrustedAuthCallback(data)) {
            return
        }

        if (PortalAuthSecurity.containsLegacyPortalSecrets(data)) {
            PortalAuthSecurity.clearSession(this)
            deepLinkResult = "Error: Callback lama yang membawa token ditolak. Mulai login lagi dari launcher."
            setContent { DLavieGuidedApp(deepLinkResult = deepLinkResult) }
            return
        }

        val error = data?.getQueryParameter("error")
        if (!error.isNullOrBlank()) {
            PortalAuthSecurity.consumeOAuthVerifier(this)
            PortalAuthSecurity.clearSession(this)
            val description = data.getQueryParameter("error_description") ?: error
            deepLinkResult = "Error: Google login gagal — $description"
            setContent { DLavieGuidedApp(deepLinkResult = deepLinkResult) }
            return
        }

        val authCode = data?.getQueryParameter("code")
        val verifier = PortalAuthSecurity.consumeOAuthVerifier(this)
        if (authCode.isNullOrBlank() || verifier.isNullOrBlank()) {
            PortalAuthSecurity.clearSession(this)
            deepLinkResult = "Error: Kode login tidak lengkap atau sudah kedaluwarsa. Mulai login lagi dari launcher."
            setContent { DLavieGuidedApp(deepLinkResult = deepLinkResult) }
            return
        }

        deepLinkResult = "Memverifikasi kode login…"
        setContent { DLavieGuidedApp(deepLinkResult = deepLinkResult) }

        lifecycleScope.launch {
            val verifiedSession = withContext(Dispatchers.IO) {
                PortalAuthSecurity.exchangePkceCode(authCode, verifier)
            }

            if (verifiedSession == null) {
                PortalAuthSecurity.clearSession(this@DLavieGuidedActivity)
                deepLinkResult = "Error: Kode login tidak dapat diverifikasi. Silakan login kembali."
                setContent { DLavieGuidedApp(deepLinkResult = deepLinkResult) }
                return@launch
            }

            val session = AuthSession(
                verifiedSession.accessToken,
                verifiedSession.refreshToken,
                verifiedSession.user.email
            )
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
                    mapOf("method" to "google_oauth_pkce")
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
    }'''

text = text[:start] + pkce_method + text[end:]

old_oauth = re.compile(
    r'\s*PortalAuthSecurity\.markOAuthAttempt\(context\)\n'
    r'\s*val redirect = "dlavie://auth-callback"\n'
    r'\s*val url = "\$\{SUPABASE_URL\}/auth/v1/authorize\?provider=google&redirect_to=\$\{java\.net\.URLEncoder\.encode\(redirect, "UTF-8"\)\}"'
)
new_oauth = '''
        val codeChallenge = PortalAuthSecurity.beginOAuthAttempt(context)
        val redirect = "dlavie://auth-callback"
        val url = "${SUPABASE_URL}/auth/v1/authorize" +
            "?provider=google" +
            "&redirect_to=${java.net.URLEncoder.encode(redirect, "UTF-8")}" +
            "&code_challenge=${java.net.URLEncoder.encode(codeChallenge, "UTF-8")}" +
            "&code_challenge_method=s256"'''
text, count = old_oauth.subn(new_oauth, text, count=1)
if count != 1:
    raise SystemExit("Google OAuth launcher block could not be upgraded to PKCE")

for forbidden in (
    'params["access_token"]',
    'params["refresh_token"]',
    'PortalAuthSecurity.markOAuthAttempt',
    'PortalAuthSecurity.consumeOAuthAttempt',
):
    if forbidden in text:
        raise SystemExit(f"Implicit OAuth fragment remains: {forbidden}")

required = (
    'PortalAuthSecurity.beginOAuthAttempt(context)',
    'PortalAuthSecurity.exchangePkceCode(authCode, verifier)',
    'getQueryParameter("code")',
)
for marker in required:
    if marker not in text:
        raise SystemExit(f"PKCE marker missing: {marker}")

source.write_text(text, encoding="utf-8")
print("Launcher OAuth upgraded to Authorization Code + PKCE")
