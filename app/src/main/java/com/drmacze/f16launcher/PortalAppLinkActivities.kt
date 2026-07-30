package com.drmacze.f16launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.browser.customtabs.CustomTabsIntent
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Minimal exported entry point for dlavie://connect.
 *
 * It creates the PKCE verifier and state inside the official launcher process, stores
 * them in private app storage, and asks the Portal to return through the verified HTTPS
 * callback declared in BuildConfig.PORTAL_SSO_CALLBACK_URL.
 */
class PortalConnectActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent?.data
        if (intent?.action != Intent.ACTION_VIEW || !isTrustedConnectUri(uri)) {
            fail("Permintaan Connect tidak valid.")
            return
        }

        val capability = uri?.getQueryParameter("cap").orEmpty()
        if (!isBase64UrlLinkValue(capability, 32, 128)) {
            fail("Capability Portal tidak tersedia atau sudah kedaluwarsa.")
            return
        }

        val verifier = randomLinkSecret(64)
        val state = randomLinkSecret(32)
        val challenge = sha256Base64UrlLinkValue(verifier)

        getSharedPreferences(PORTAL_SSO_PREFS, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .putString(PORTAL_KEY_CAPABILITY, capability)
            .putString(PORTAL_KEY_VERIFIER, verifier)
            .putString(PORTAL_KEY_STATE, state)
            .putLong(PORTAL_KEY_STARTED_AT, System.currentTimeMillis())
            .apply()

        val authorizationUrl = Uri.parse(PORTAL_URL).buildUpon()
            .appendQueryParameter("launcher_sso", "1")
            .appendQueryParameter("cap", capability)
            .appendQueryParameter("challenge", challenge)
            .appendQueryParameter("state", state)
            .appendQueryParameter("callback_uri", BuildConfig.PORTAL_SSO_CALLBACK_URL)
            .build()
            .toString() + "#/portal"

        val target = Uri.parse(authorizationUrl)
        runCatching {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
                .launchUrl(this, target)
        }.recoverCatching {
            startActivity(Intent(Intent.ACTION_VIEW, target))
        }.onFailure {
            clearPortalPending()
            fail("Browser tidak dapat dibuka untuk menyelesaikan Secure Connect.")
            return
        }

        finish()
    }

    private fun clearPortalPending() {
        getSharedPreferences(PORTAL_SSO_PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun fail(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }
}

/**
 * Verified HTTPS callback trampoline.
 *
 * Android verifies drmacze.github.io against /.well-known/assetlinks.json before
 * routing this link automatically. The activity still validates every URI component,
 * then converts the callback into an explicit in-package intent for PortalSsoActivity.
 */
class VerifiedPortalCallbackActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        forwardVerifiedCallback(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        forwardVerifiedCallback(intent)
    }

    private fun forwardVerifiedCallback(source: Intent?) {
        val uri = source?.data
        if (source?.action != Intent.ACTION_VIEW || !isTrustedVerifiedCallback(uri)) {
            fail("Callback Portal tidak terverifikasi.")
            return
        }

        val authCode = uri?.getQueryParameter("code").orEmpty()
        val state = uri?.getQueryParameter("state").orEmpty()
        if (!isBase64UrlLinkValue(authCode, 32, 128) ||
            !isBase64UrlLinkValue(state, 32, 128)
        ) {
            fail("Kode Secure Connect tidak lengkap atau tidak valid.")
            return
        }

        val internalUri = Uri.Builder()
            .scheme("dlavie")
            .authority("portal-complete")
            .appendQueryParameter("code", authCode)
            .appendQueryParameter("state", state)
            .build()

        startActivity(
            Intent(this, PortalSsoActivity::class.java)
                .setAction(Intent.ACTION_VIEW)
                .setData(internalUri)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
    }

    private fun fail(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }
}

private const val PORTAL_URL = "https://drmacze.github.io/dlavie-web/"
private const val PORTAL_SSO_PREFS = "dlavie_portal_sso_pending"
private const val PORTAL_KEY_CAPABILITY = "capability"
private const val PORTAL_KEY_VERIFIER = "verifier"
private const val PORTAL_KEY_STATE = "state"
private const val PORTAL_KEY_STARTED_AT = "started_at"

private fun isTrustedConnectUri(uri: Uri?): Boolean {
    if (uri == null) return false
    return uri.scheme.equals("dlavie", ignoreCase = true) &&
        uri.host.equals("connect", ignoreCase = true) &&
        uri.userInfo == null &&
        uri.fragment == null &&
        (uri.path.isNullOrEmpty() || uri.path == "/") &&
        uri.queryParameterNames == setOf("cap")
}

private fun isTrustedVerifiedCallback(uri: Uri?): Boolean {
    if (uri == null) return false
    val expected = Uri.parse(BuildConfig.PORTAL_SSO_CALLBACK_URL)
    val expectedPort = expected.port
    val actualPort = uri.port
    val portMatches = actualPort == expectedPort ||
        (expectedPort == -1 && actualPort == 443)

    return uri.scheme.equals(expected.scheme, ignoreCase = true) &&
        uri.host.equals(expected.host, ignoreCase = true) &&
        portMatches &&
        uri.userInfo == null &&
        uri.fragment == null &&
        uri.encodedPath == expected.encodedPath &&
        uri.queryParameterNames == setOf("code", "state")
}

private fun isBase64UrlLinkValue(value: String?, min: Int, max: Int): Boolean =
    value != null && value.length in min..max && value.all {
        it.isLetterOrDigit() || it == '-' || it == '_'
    }

private fun randomLinkSecret(size: Int): String {
    val bytes = ByteArray(size).also { SecureRandom().nextBytes(it) }
    return android.util.Base64.encodeToString(
        bytes,
        android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING,
    )
}

private fun sha256Base64UrlLinkValue(value: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.US_ASCII))
    return android.util.Base64.encodeToString(
        digest,
        android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING,
    )
}
