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
if "BuildConfig.PORTAL_SSO_ENDPOINT" not in portal:
    raise SystemExit("Portal SSO endpoint was not moved to BuildConfig")
PORTAL.write_text(portal, encoding="utf-8")

print("v326 auth interoperability patch applied")
