from pathlib import Path

SOURCE = Path('app/src/main/java/com/drmacze/f16launcher/MaintenanceSystem.kt')
text = SOURCE.read_text(encoding='utf-8')

# Modifier.weight is a RowScope member extension in this Compose version.
# Importing layout.weight resolves to an internal symbol and breaks compilation.
text = text.replace('import androidx.compose.foundation.layout.weight\n', '')

SOURCE.write_text(text, encoding='utf-8')
print('MaintenanceSystem imports normalized.')
