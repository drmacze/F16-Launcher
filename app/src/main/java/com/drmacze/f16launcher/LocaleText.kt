package com.drmacze.f16launcher

import android.content.Context

/**
 * Key-based catalog for user-facing text added after the legacy typed Strings
 * model. It provides per-key English fallback, interpolation, and one locale
 * contract shared with the website. Existing Strings remains supported while
 * screens are migrated incrementally.
 */
object LocaleText {
    private val english = mapOf(
        "startup.product" to "MOBILE LAUNCHER",
        "startup.check_session" to "Checking session",
        "startup.prepare_services" to "Preparing services",
        "startup.prepare_interface" to "Preparing interface",
        "startup.ready" to "Ready",
        "language.title" to "Language",
        "language.current" to "Current language",
        "language.mode" to "Mode",
        "language.auto_mode" to "Auto (follow device)",
        "language.manual_mode" to "Manual",
        "language.select" to "Choose language",
        "language.use_device" to "Follow device language",
        "language.applied" to "Language changes are applied immediately.",
        "language.auto_description" to "DLavie will follow your device language when it is supported.",
        "portal.heading" to "Connect DLavie Portal",
        "portal.subtitle" to "Use the same verified account on Portal and Launcher.",
        "portal.step_login" to "Sign in to Portal",
        "portal.step_approve" to "Approve connection",
        "portal.step_return" to "Return automatically",
        "portal.start" to "Start secure connection",
        "portal.waiting" to "Waiting for Portal approval",
        "portal.verifying" to "Verifying secure connection",
        "portal.preparing" to "Preparing your account",
        "portal.connected" to "Account connected",
        "portal.reopen" to "Open Portal again",
        "portal.retry" to "Try again",
        "portal.expired" to "This connection request has expired. Start again from the launcher.",
        "portal.failed" to "The connection could not be completed. No account data was saved.",
        "maintenance.label" to "Service maintenance",
        "maintenance.retry" to "Try again",
        "maintenance.offline" to "Play offline",
        "maintenance.staff" to "Staff access",
        "generic.loading" to "Loading…",
        "generic.close" to "Close",
        "generic.cancel" to "Cancel",
        "generic.continue" to "Continue",
        "generic.error" to "Something went wrong",
    )

    private val indonesian = english + mapOf(
        "startup.product" to "MOBILE LAUNCHER",
        "startup.check_session" to "Memeriksa sesi",
        "startup.prepare_services" to "Menyiapkan layanan",
        "startup.prepare_interface" to "Menyiapkan antarmuka",
        "startup.ready" to "Siap",
        "language.title" to "Bahasa",
        "language.current" to "Bahasa saat ini",
        "language.mode" to "Mode",
        "language.auto_mode" to "Auto (mengikuti perangkat)",
        "language.manual_mode" to "Manual",
        "language.select" to "Pilih bahasa",
        "language.use_device" to "Ikuti bahasa perangkat",
        "language.applied" to "Perubahan bahasa diterapkan langsung.",
        "language.auto_description" to "DLavie mengikuti bahasa perangkat bila bahasa tersebut didukung.",
        "portal.heading" to "Hubungkan DLavie Portal",
        "portal.subtitle" to "Gunakan akun terverifikasi yang sama di Portal dan Launcher.",
        "portal.step_login" to "Masuk ke Portal",
        "portal.step_approve" to "Setujui koneksi",
        "portal.step_return" to "Kembali otomatis",
        "portal.start" to "Mulai koneksi aman",
        "portal.waiting" to "Menunggu persetujuan Portal",
        "portal.verifying" to "Memverifikasi koneksi aman",
        "portal.preparing" to "Menyiapkan akun Anda",
        "portal.connected" to "Akun berhasil terhubung",
        "portal.reopen" to "Buka Portal lagi",
        "portal.retry" to "Coba lagi",
        "portal.expired" to "Permintaan koneksi sudah kedaluwarsa. Mulai lagi dari launcher.",
        "portal.failed" to "Koneksi belum dapat diselesaikan. Tidak ada data akun yang disimpan.",
        "maintenance.label" to "Pemeliharaan layanan",
        "maintenance.retry" to "Coba lagi",
        "maintenance.offline" to "Main offline",
        "maintenance.staff" to "Akses staff",
        "generic.loading" to "Memuat…",
        "generic.close" to "Tutup",
        "generic.cancel" to "Batal",
        "generic.continue" to "Lanjutkan",
        "generic.error" to "Terjadi kesalahan",
    )

    private val overrides = mapOf(
        "ms" to mapOf(
            "startup.check_session" to "Menyemak sesi", "startup.prepare_services" to "Menyediakan perkhidmatan", "startup.prepare_interface" to "Menyediakan antara muka", "startup.ready" to "Sedia",
            "language.title" to "Bahasa", "language.current" to "Bahasa semasa", "language.select" to "Pilih bahasa", "language.use_device" to "Ikut bahasa peranti", "portal.start" to "Mulakan sambungan selamat", "generic.continue" to "Teruskan"
        ),
        "pt" to mapOf(
            "startup.check_session" to "Verificando sessão", "startup.prepare_services" to "Preparando serviços", "startup.prepare_interface" to "Preparando interface", "startup.ready" to "Pronto",
            "language.title" to "Idioma", "language.current" to "Idioma atual", "language.select" to "Escolher idioma", "language.use_device" to "Seguir idioma do dispositivo", "portal.start" to "Iniciar conexão segura", "generic.continue" to "Continuar"
        ),
        "es" to mapOf(
            "startup.check_session" to "Comprobando sesión", "startup.prepare_services" to "Preparando servicios", "startup.prepare_interface" to "Preparando interfaz", "startup.ready" to "Listo",
            "language.title" to "Idioma", "language.current" to "Idioma actual", "language.select" to "Elegir idioma", "language.use_device" to "Usar idioma del dispositivo", "portal.start" to "Iniciar conexión segura", "generic.continue" to "Continuar"
        ),
        "de" to mapOf(
            "startup.check_session" to "Sitzung wird geprüft", "startup.prepare_services" to "Dienste werden vorbereitet", "startup.prepare_interface" to "Oberfläche wird vorbereitet", "startup.ready" to "Bereit",
            "language.title" to "Sprache", "language.current" to "Aktuelle Sprache", "language.select" to "Sprache wählen", "language.use_device" to "Gerätesprache verwenden", "portal.start" to "Sichere Verbindung starten", "generic.continue" to "Weiter"
        ),
        "fr" to mapOf(
            "startup.check_session" to "Vérification de la session", "startup.prepare_services" to "Préparation des services", "startup.prepare_interface" to "Préparation de l’interface", "startup.ready" to "Prêt",
            "language.title" to "Langue", "language.current" to "Langue actuelle", "language.select" to "Choisir la langue", "language.use_device" to "Suivre la langue de l’appareil", "portal.start" to "Démarrer la connexion sécurisée", "generic.continue" to "Continuer"
        ),
        "ja" to mapOf(
            "startup.check_session" to "セッションを確認中", "startup.prepare_services" to "サービスを準備中", "startup.prepare_interface" to "画面を準備中", "startup.ready" to "準備完了",
            "language.title" to "言語", "language.current" to "現在の言語", "language.select" to "言語を選択", "language.use_device" to "端末の言語に従う", "portal.start" to "安全な接続を開始", "generic.continue" to "続行"
        ),
        "zh" to mapOf(
            "startup.check_session" to "正在检查会话", "startup.prepare_services" to "正在准备服务", "startup.prepare_interface" to "正在准备界面", "startup.ready" to "就绪",
            "language.title" to "语言", "language.current" to "当前语言", "language.select" to "选择语言", "language.use_device" to "跟随设备语言", "portal.start" to "开始安全连接", "generic.continue" to "继续"
        ),
        "ar" to mapOf(
            "startup.check_session" to "جارٍ التحقق من الجلسة", "startup.prepare_services" to "جارٍ تجهيز الخدمات", "startup.prepare_interface" to "جارٍ تجهيز الواجهة", "startup.ready" to "جاهز",
            "language.title" to "اللغة", "language.current" to "اللغة الحالية", "language.select" to "اختر اللغة", "language.use_device" to "اتباع لغة الجهاز", "portal.start" to "بدء اتصال آمن", "generic.continue" to "متابعة"
        ),
    )

    private fun catalog(languageCode: String): Map<String, String> = when (languageCode) {
        "id" -> indonesian
        "en" -> english
        else -> english + (overrides[languageCode] ?: emptyMap())
    }

    fun get(context: Context, key: String, vararg values: Pair<String, Any?>): String {
        return get(LanguageManager.getCurrentLanguage(context), key, *values)
    }

    fun get(languageCode: String, key: String, vararg values: Pair<String, Any?>): String {
        var text = catalog(languageCode)[key] ?: english[key] ?: key
        values.forEach { (name, value) -> text = text.replace("{$name}", value?.toString().orEmpty()) }
        return text
    }
}
