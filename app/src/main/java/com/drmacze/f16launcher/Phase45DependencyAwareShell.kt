package com.drmacze.f16launcher

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun Phase45DependencyAwareShell(api: CommunityApi) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("f16_launcher", 0)
    val firstRunDone = prefs.getBoolean("first_run_done", false)
    val dataInstalled = prefs.getBoolean("dlavie_data_installed", false)
    val stage = prefs.getString("phase44_stage", "") ?: ""

    // Apply this before Phase44 renders. LaunchedEffect was too late: Phase44 had already
    // read old prefs and showed Download OBB. In repair mode, first setup proves OBB is usable,
    // so the only required step is final DATA install.
    if ((firstRunDone && !dataInstalled) || stage == "data_after_first_run") {
        prefs.edit()
            .putBoolean("dlavie_obb_installed", true)
            .putBoolean("first_run_done", true)
            .putBoolean("dlavie_data_installed", false)
            .putString("phase44_stage", "data_after_first_run")
            .apply()
    }

    Phase44DataAfterFirstRunShell(api)
}
