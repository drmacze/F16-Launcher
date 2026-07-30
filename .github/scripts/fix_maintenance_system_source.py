from pathlib import Path

SOURCE = Path('app/src/main/java/com/drmacze/f16launcher/MaintenanceSystem.kt')
text = SOURCE.read_text(encoding='utf-8')

if 'import androidx.compose.foundation.layout.weight' not in text:
    marker = 'import androidx.compose.foundation.layout.widthIn\n'
    if marker not in text:
        raise SystemExit('widthIn import marker not found')
    text = text.replace(marker, marker + 'import androidx.compose.foundation.layout.weight\n', 1)

SOURCE.write_text(text, encoding='utf-8')
print('MaintenanceSystem imports normalized.')
