package com.drmacze.f16launcher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

@Composable
fun Phase45DependencyAwareShell(api: CommunityApi) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("f16_launcher", 0)
        val firstRunDone = prefs.getBoolean("first_run_done", false)
        val dataInstalled = prefs.getBoolean("dlavie_data_installed", false)
        val stage = prefs.getString("phase44_stage", "") ?: ""

        // Repair DATA is only shown after first setup. If first setup is already done,
        // OBB has already been usable by the game, so do not force users to re-download OBB.
        if (firstRunDone && !dataInstalled) {
            prefs.edit()
                .putBoolean("dlavie_obb_installed", true)
                .putString("phase44_stage", "data_after_first_run")
                .apply()
        }

        // Compatibility for users who already pressed the repair button on the previous build.
        if (stage == "data_after_first_run") {
            prefs.edit().putBoolean("dlavie_obb_installed", true).apply()
        }
    }
    Phase44DataAfterFirstRunShell(api)
}
