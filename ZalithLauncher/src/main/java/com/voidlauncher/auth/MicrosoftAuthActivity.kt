package com.voidlauncher.auth

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity

/**
 * Dedicated, one-purpose WebView for Microsoft login only. The main VOID interface is
 * entirely native Compose. The redirect URL is intercepted; the code is returned to the
 * caller and the WebView is destroyed immediately.
 */
class MicrosoftAuthActivity : ComponentActivity() {

    @Suppress("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Microsoft sign in"

        val webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: android.webkit.WebResourceRequest,
                ): Boolean {
                    val url = request.url.toString()
                    if (url.startsWith(REDIRECT_URL)) {
                        val code = Uri.parse(url).getQueryParameter("code")
                        val error = Uri.parse(url).getQueryParameter("error")
                        val result = Intent().apply {
                            putExtra(EXTRA_CODE, code)
                            putExtra(EXTRA_ERROR, error)
                        }
                        setResult(if (code != null) Activity.RESULT_OK else Activity.RESULT_CANCELED, result)
                        finish()
                        return true
                    }
                    return false
                }
            }
        }
        setContentView(webView)
        webView.loadUrl(AUTH_URL)
    }

    override fun onDestroy() {
        super.onDestroy()
        // No token or cookie is retained by VOID: the session WebView is discarded.
    }

    companion object {
        private const val CLIENT_ID = "00000000402b5328"
        const val REDIRECT_URL = "https://login.live.com/oauth20_desktop.srf"
        const val EXTRA_CODE = "void_ms_code"
        const val EXTRA_ERROR = "void_ms_error"

        // Public client ID used by open-source Android launchers; PKCE is not supported
        // by this legacy endpoint, so no client secret is involved.
        val AUTH_URL: String = buildString {
            append("https://login.live.com/oauth20_authorize.srf?")
            append("client_id=$CLIENT_ID")
            append("&response_type=code")
            append("&redirect_uri=").append(java.net.URLEncoder.encode(REDIRECT_URL, "UTF-8"))
            append("&scope=").append(java.net.URLEncoder.encode("service::user.auth.xboxlive.com::MBI_SSL", "UTF-8"))
            append("&prompt=select_account")
        }
    }
}
