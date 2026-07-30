from pathlib import Path
import re

MODERN = Path('app/src/main/java/com/drmacze/f16launcher/ModernLauncherActivity.kt')
GUIDED = Path('app/src/main/java/com/drmacze/f16launcher/DLavieGuidedActivity.kt')
for source in (MODERN, GUIDED):
    if not source.exists():
        raise SystemExit(f'{source} not found')

modern = MODERN.read_text(encoding='utf-8')
guided = GUIDED.read_text(encoding='utf-8')

# MaintenanceInfo now lives in MaintenanceSystem.kt so all launcher surfaces share one model.
model_start = modern.find('/**\n * Maintenance mode info — dibaca dari Supabase app_config key="maintenance".')
model_end_marker = '/**\n * Latest notification campaign'
if model_start >= 0:
    model_end = modern.find(model_end_marker, model_start)
    if model_end < 0:
        raise SystemExit('MaintenanceInfo end marker not found')
    modern = modern[:model_start] + '// MaintenanceInfo is defined in MaintenanceSystem.kt.\n\n' + modern[model_end:]
elif 'data class MaintenanceInfo(' in modern:
    raise SystemExit('Unexpected MaintenanceInfo definition remains')

old_state = '''    // ── Maintenance state (Bug 1-3: full/partial scope + staff bypass) ──
    var maintenanceState by remember { mutableStateOf<MaintenanceInfo?>(null) }
    var maintenanceChecked by remember { mutableStateOf(false) }
    var partialBypassed by remember { mutableStateOf(false) } // user tekan "Masuk Launcher" saat scope=partial
'''
new_state = '''    // ── Unified maintenance state from DLavie Dev Hub ──
    var maintenanceState by remember { mutableStateOf<MaintenanceInfo?>(null) }
    var maintenanceChecked by remember { mutableStateOf(false) }
    var maintenanceRefreshing by remember { mutableStateOf(false) }
    var partialBypassed by remember { mutableStateOf(false) } // staff bypass for full maintenance
'''
if old_state in modern:
    modern = modern.replace(old_state, new_state, 1)
elif new_state not in modern:
    raise SystemExit('Modern maintenance state marker not found')

fetch_start = modern.find('    // ── Fetch maintenance untuk SEMUA user (termasuk staff) ──')
fetch_end = modern.find('    // ── Cek app update saat app dibuka ──', fetch_start)
if fetch_start >= 0 and fetch_end >= 0:
    modern = modern[:fetch_start] + '''    // Fetch the same live state managed by the Portal Dev Hub.
    LaunchedEffect(Unit) {
        maintenanceState = MaintenanceRepository.fetch(context)
        maintenanceChecked = true
    }

''' + modern[fetch_end:]
elif 'MaintenanceRepository.fetch(context)' not in modern:
    raise SystemExit('Modern initial maintenance fetch marker not found')

route_start_marker = '                    // ── Staff bypass: skip maintenance entirely (Bug 3) ──'
route_end_marker = '                    // ── PIN lock (non-staff, post-maintenance-check) ──'
route_start = modern.find(route_start_marker)
route_end = modern.find(route_end_marker, route_start)
if route_start >= 0 and route_end >= 0:
    replacement = '''                    // Full maintenance blocks normal users. Staff can bypass explicitly.
                    maintenanceChecked && maintenanceState?.isFull == true
                            && !partialBypassed -> {
                        FullScreenMaintenance(
                            maintenance = maintenanceState!!,
                            isStaff = isStaff,
                            refreshing = maintenanceRefreshing,
                            onRetry = {
                                if (!maintenanceRefreshing) {
                                    maintenanceRefreshing = true
                                    updateScope.launch {
                                        MaintenanceRepository.clearMemoryCache()
                                        maintenanceState = MaintenanceRepository.fetch(context, forceRefresh = true)
                                        maintenanceRefreshing = false
                                    }
                                }
                            },
                            onEnter = { partialBypassed = true },
                        )
                    }

'''
    modern = modern[:route_start] + replacement + modern[route_end:]
elif 'maintenanceState?.isFull == true' not in modern:
    raise SystemExit('Modern maintenance route markers not found')

old_default = 'MainShell(api, maintenanceInfo = maintenanceState, onLogout = logoutAction, initialPostId = initialPostId, onTriggerUpdate = { info -> updateInfo = info; showUpdatePopup = true })'
new_default = 'MainShell(api, maintenanceInfo = maintenanceState?.copy(staffBypass = isStaff), onLogout = logoutAction, initialPostId = initialPostId, onTriggerUpdate = { info -> updateInfo = info; showUpdatePopup = true })'
if old_default in modern:
    modern = modern.replace(old_default, new_default, 1)
elif new_default not in modern:
    raise SystemExit('Modern MainShell maintenance marker not found')

# Replace the animated/shiny full-screen screen with the focused professional screen.
full_start = modern.find('// ─── Full-screen maintenance (Bug 1: scope = "full" | "partial")')
full_end = modern.find('// ─── Shiny Text (gradient sweep animation on title)', full_start)
if full_start >= 0 and full_end >= 0:
    modern = modern[:full_start] + '''// ─── Professional full-screen maintenance ───────────────────────────────────
@Composable
fun FullScreenMaintenance(
    maintenance: MaintenanceInfo,
    isStaff: Boolean = false,
    allowStaffLogin: Boolean = false,
    refreshing: Boolean = false,
    onRetry: () -> Unit = {},
    onEnter: () -> Unit,
) {
    ProfessionalMaintenanceScreen(
        maintenance = maintenance,
        refreshing = refreshing,
        isStaff = isStaff,
        allowStaffLogin = allowStaffLogin,
        onRetry = onRetry,
        onStaffEnter = onEnter,
    )
}

''' + modern[full_end:]
elif 'ProfessionalMaintenanceScreen(' not in modern:
    raise SystemExit('FullScreenMaintenance markers not found')

# Normalize both action-blocking expressions exactly once. This keeps the script idempotent.
blocked_pattern = re.compile(r'(?m)^(\s*)val maintenanceBlocked = .*$')
blocked_matches = list(blocked_pattern.finditer(modern))
if len(blocked_matches) < 2:
    raise SystemExit('Expected Home and Update maintenanceBlocked expressions')
modern = blocked_pattern.sub(
    lambda match: (
        f'{match.group(1)}val maintenanceBlocked = maintenanceInfo?.enabled == true '
        '&& maintenanceInfo?.scope == "partial" && !maintenanceInfo.staffBypass'
    ),
    modern,
)

# Refresh the banner from the unified repository rather than a second legacy API path.
modern = modern.replace(
    'runCatching { maintenanceState = fetchMaintenanceInfo(api) }',
    'runCatching { maintenanceState = MaintenanceRepository.fetch(context, forceRefresh = true) }',
)

banner_start = modern.find('        // ── Maintenance banner (cek app_config.maintenance via Supabase) ──')
banner_end = modern.find('        // ── Notification banner (dari notification_campaigns, latest sent) ──', banner_start)
if banner_start >= 0 and banner_end >= 0:
    modern = modern[:banner_start] + '''        // Compact maintenance status from the same Dev Hub state.
        maintenanceState?.takeIf { it.enabled }?.let { maintenance ->
            MaintenanceStatusBanner(
                maintenance = maintenance.copy(staffBypass = maintenanceInfo?.staffBypass == true),
            )
        }

''' + modern[banner_end:]
elif 'MaintenanceStatusBanner(' not in modern:
    raise SystemExit('Home maintenance banner markers not found')

legacy_fetch_start = modern.find('/**\n * Fetch maintenance info dari Supabase app_config (key="maintenance").')
legacy_fetch_end = modern.find('/**\n * Fetch latest sent notification campaign', legacy_fetch_start)
if legacy_fetch_start >= 0 and legacy_fetch_end >= 0:
    modern = modern[:legacy_fetch_start] + modern[legacy_fetch_end:]
elif 'fun fetchMaintenanceInfo(' in modern:
    raise SystemExit('Legacy Modern maintenance fetch remains')

# Guided login: full maintenance gets the same clean screen; partial maintenance keeps login available.
guided_model_start = guided.find('// ─── Maintenance config (fetched at startup)')
guided_model_end = guided.find('// ─── Country picker list', guided_model_start)
if guided_model_start >= 0 and guided_model_end >= 0:
    guided = guided[:guided_model_start] + '// MaintenanceInfo is shared from MaintenanceSystem.kt.\n' + guided[guided_model_end:]
elif 'private data class MaintenanceState(' in guided:
    raise SystemExit('Guided MaintenanceState remains unexpectedly')

old_guided_state = '''    // Maintenance state fetched at app startup BEFORE the login screen is shown.
    var maintenance by remember { mutableStateOf(MaintenanceState()) }
    var showLogin  by remember { mutableStateOf(false) }
    // v6.8.4: Deep link result dari Google OAuth callback
    var deepLinkMsg by remember { mutableStateOf(deepLinkResult ?: "") }

    LaunchedEffect(Unit) {
        maintenance = withContext(Dispatchers.IO) { fetchMaintenanceConfig() }
        if (!maintenance.enabled) showLogin = true
    }
'''
new_guided_state = '''    val maintenanceScope = rememberCoroutineScope()
    var maintenance by remember { mutableStateOf<MaintenanceInfo?>(null) }
    var maintenanceLoaded by remember { mutableStateOf(false) }
    var maintenanceRefreshing by remember { mutableStateOf(false) }
    var showLogin by remember { mutableStateOf(false) }
    // v6.8.4: Deep link result dari Google OAuth callback
    var deepLinkMsg by remember { mutableStateOf(deepLinkResult ?: "") }

    LaunchedEffect(Unit) {
        maintenance = MaintenanceRepository.fetch(context)
        maintenanceLoaded = true
        showLogin = maintenance?.isFull != true
    }
'''
if old_guided_state in guided:
    guided = guided.replace(old_guided_state, new_guided_state, 1)
elif new_guided_state not in guided:
    raise SystemExit('Guided maintenance state marker not found')

guided_screen_start = guided.find('                if (maintenance.enabled && !showLogin) {')
guided_screen_end = guided.find('                } else if (showLogin || maintenance.loaded) {', guided_screen_start)
if guided_screen_start >= 0 and guided_screen_end >= 0:
    new_screen = '''                if (maintenanceLoaded && maintenance?.isFull == true && !showLogin) {
                    FullScreenMaintenance(
                        maintenance = maintenance!!,
                        allowStaffLogin = true,
                        refreshing = maintenanceRefreshing,
                        onRetry = {
                            if (!maintenanceRefreshing) {
                                maintenanceRefreshing = true
                                maintenanceScope.launch {
                                    MaintenanceRepository.clearMemoryCache()
                                    maintenance = MaintenanceRepository.fetch(context, forceRefresh = true)
                                    maintenanceRefreshing = false
                                    if (maintenance?.isFull != true) showLogin = true
                                }
                            }
                        },
                        onEnter = { showLogin = true },
                    )
                } else if (showLogin || maintenanceLoaded) {'''
    guided = guided[:guided_screen_start] + new_screen + guided[guided_screen_end + len('                } else if (showLogin || maintenance.loaded) {'):]
elif 'allowStaffLogin = true' not in guided:
    raise SystemExit('Guided maintenance screen markers not found')

legacy_guided_start = guided.find('/**\n * Fetch maintenance config from app_config (key = \'maintenance\').')
legacy_guided_end = guided.find('private fun jsonArrayObjectsToTitles', legacy_guided_start)
if legacy_guided_start >= 0 and legacy_guided_end >= 0:
    guided = guided[:legacy_guided_start] + guided[legacy_guided_end:]
elif 'private fun fetchMaintenanceConfig()' in guided:
    raise SystemExit('Legacy Guided maintenance fetch remains')

required_modern = [
    'MaintenanceRepository.fetch(context)',
    'maintenanceState?.isFull == true',
    'ProfessionalMaintenanceScreen(',
    'MaintenanceStatusBanner(',
    'staffBypass = isStaff',
]
required_guided = [
    'MaintenanceRepository.fetch(context)',
    'allowStaffLogin = true',
    'maintenanceLoaded',
]
for item in required_modern:
    if item not in modern:
        raise SystemExit(f'Modern maintenance patch missing: {item}')
for item in required_guided:
    if item not in guided:
        raise SystemExit(f'Guided maintenance patch missing: {item}')
if 'fetchMaintenanceInfo(api)' in modern or 'fetchMaintenanceConfig()' in guided:
    raise SystemExit('Legacy maintenance fetch call remains')

MODERN.write_text(modern, encoding='utf-8')
GUIDED.write_text(guided, encoding='utf-8')
print('Unified maintenance redesign materialized.')
