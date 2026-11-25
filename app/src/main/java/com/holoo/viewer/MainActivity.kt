package com.holoo.viewer

import android.app.Activity
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.content.Intent
import android.net.Uri
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var web: WebView
    private var incomingJson: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingJson = readIncoming(intent)
        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.addJavascriptInterface(Bridge(), "HolooAndroid")
        web.webViewClient = WebViewClient()
        web.webChromeClient = WebChromeClient()
        setContentView(web)
        web.loadUrl("file:///android_asset/index.html")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingJson = readIncoming(intent)
        web.reload()
    }

    private fun readIncoming(i: Intent?): String? {
        if (i?.action != Intent.ACTION_VIEW) return null
        val uri: Uri = i.data ?: return null
        return try {
            contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
        } catch (e: Exception) {
            Toast.makeText(this, "خواندن فایل JSON ناموفق بود", Toast.LENGTH_LONG).show()
            null
        }
    }

    inner class Bridge {
        @JavascriptInterface
        fun getJson(): String = incomingJson ?: ""
    }
}
