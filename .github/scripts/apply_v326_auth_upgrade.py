from pathlib import Path

GUIDED = Path("app/src/main/java/com/drmacze/f16launcher/DLavieGuidedActivity.kt")
PORTAL = Path("app/src/main/java/com/drmacze/f16launcher/PortalSsoActivity.kt")

for path in (GUIDED, PORTAL):
    if not path.exists():
        raise SystemExit(f"required source missing: {path}")

guided = GUIDED.read_text(encoding="utf-8")
if "&code_challenge_method=s256" in guided:
    guided = guided.replace("&code_challenge_method=s256", "&code_challenge_method=S256")
if "&code_challenge_method=S256" not in guided:
    raise SystemExit("Google OAuth PKCE method not found")
if "&prompt=select_account" not in guided:
    guided = guided.replace(
        '"&code_challenge_method=S256"',
        '"&code_challenge_method=S256" +\n            "&prompt=select_account"',
        1,
    )
GUIDED.write_text(guided, encoding="utf-8")

portal = PORTAL.read_text(encoding="utf-8")
portal = portal.replace(
    '        private const val SSO_ENDPOINT = "https://lvmucsxbmadtsgrxuwmo.supabase.co/functions/v1/launcher-sso"\n',
    "",
)
portal = portal.replace("URL(SSO_ENDPOINT)", "URL(BuildConfig.PORTAL_SSO_ENDPOINT)")
portal = portal.replace(
    '        val capability = uri.getQueryParameter("cap")\n',
    '        val capability = uri.getQueryParameter("cap").orEmpty()\n',
)
portal = portal.replace(
    '''        val authCode = uri.getQueryParameter("code")
        val returnedState = uri.getQueryParameter("state")
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val verifier = prefs.getString(KEY_VERIFIER, null)
        val expectedState = prefs.getString(KEY_STATE, null)
        val startedAt = prefs.getLong(KEY_STARTED_AT, 0L)

        if (!isBase64Url(authCode, 32, 128) ||
            !isBase64Url(returnedState, 32, 128) ||
            verifier.isNullOrBlank() || expectedState.isNullOrBlank() ||
            System.currentTimeMillis() - startedAt !in 0..MAX_FLOW_AGE_MS ||
            !constantTimeEquals(expectedState, returnedState)
        ) {''',
    '''        val authCode = uri.getQueryParameter("code").orEmpty()
        val returnedState = uri.getQueryParameter("state").orEmpty()
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val verifier = prefs.getString(KEY_VERIFIER, null).orEmpty()
        val expectedState = prefs.getString(KEY_STATE, null).orEmpty()
        val startedAt = prefs.getLong(KEY_STARTED_AT, 0L)

        if (!isBase64Url(authCode, 32, 128) ||
            !isBase64Url(returnedState, 32, 128) ||
            verifier.isBlank() || expectedState.isBlank() ||
            System.currentTimeMillis() - startedAt !in 0..MAX_FLOW_AGE_MS ||
            !constantTimeEquals(expectedState, returnedState)
        ) {''',
)
if "BuildConfig.PORTAL_SSO_ENDPOINT" not in portal:
    raise SystemExit("Portal SSO endpoint was not moved to BuildConfig")
if 'val capability = uri.getQueryParameter("cap").orEmpty()' not in portal:
    raise SystemExit("capability parsing was not hardened")
if 'val authCode = uri.getQueryParameter("code").orEmpty()' not in portal:
    raise SystemExit("auth code parsing was not hardened")
PORTAL.write_text(portal, encoding="utf-8")

print("v326 auth interoperability patch applied")
