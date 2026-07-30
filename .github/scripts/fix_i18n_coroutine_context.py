from pathlib import Path

path = Path('app/src/main/java/com/drmacze/f16launcher/PortalSsoActivity.kt')
text = path.read_text(encoding='utf-8')
for key in (
    'portal.sync_profile',
    'portal.sync_message',
    'portal.connected',
    'portal.same_account',
    'portal.opening_home',
):
    text = text.replace(
        f'LocaleText.get(this, "{key}")',
        f'LocaleText.get(this@PortalSsoActivity, "{key}")',
    )
path.write_text(text, encoding='utf-8')
