package com.drmacze.f16launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import rikka.shizuku.Shizuku

object ShizukuSetup {
    private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    private const val REQUEST_CODE = 1616

    fun isInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
        true
    } catch (_: Throwable) {
        false
    }

    fun isRunning(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Throwable) {
        false
    }

    fun hasPermission(): Boolean = try {
        Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) {
        false
    }

    fun status(context: Context): String {
        return when {
            hasPermission() -> "Ready"
            isRunning() -> "Need Permission"
            isInstalled(context) -> "Need Start"
            else -> "Not Installed"
        }
    }

    fun shortHint(context: Context): String {
        return when (status(context)) {
            "Ready" -> "Shizuku sudah aktif dan izin sudah diberikan."
            "Need Permission" -> "Shizuku sudah berjalan. Tekan Grant Permission lalu izinkan DLavie."
            "Need Start" -> "Shizuku terinstall, tapi service belum berjalan. Buka Shizuku lalu Start via Wireless debugging/ADB/root."
            else -> "Shizuku belum terinstall. Install Shizuku dulu dari Play Store/GitHub."
        }
    }

    fun requestPermission(): Boolean {
        return try {
            if (!Shizuku.pingBinder()) return false
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) return true
            Shizuku.requestPermission(REQUEST_CODE)
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun openApp(context: Context) {
        val launch = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
        if (launch != null) {
            context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } else {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$SHIZUKU_PACKAGE")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
