package ir.holoo.viewer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private lateinit var webView: WebView
    @Volatile private var pageReady = false
    @Volatile private var pendingJson: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                pageReady = true
                deliverPendingJson()
            }
        }
        setContentView(webView)
        webView.loadUrl("file:///android_asset/index.html")
        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(incoming: Intent?) {
        if (incoming == null) return
        val uri: Uri? = when (incoming.action) {
            Intent.ACTION_VIEW -> incoming.data
            Intent.ACTION_SEND -> {
                @Suppress("DEPRECATION")
                (incoming.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri)
            }
            else -> null
        }
        if (uri == null) return
        thread {
            try {
                val text = contentResolver.openInputStream(uri)?.use { readUtf8(it) }
                    ?: throw IllegalStateException("فایل قابل خواندن نیست")
                pendingJson = text
                runOnUiThread { deliverPendingJson() }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "خواندن فایل ناموفق بود: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun readUtf8(input: InputStream): String {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            if (total > 200 * 1024 * 1024) throw IllegalArgumentException("حجم فایل بیش از ۲۰۰ مگابایت است")
            out.write(buffer, 0, count)
        }
        return String(out.toByteArray(), StandardCharsets.UTF_8)
    }

    private fun deliverPendingJson() {
        val text = pendingJson ?: return
        if (!pageReady) return
        pendingJson = null
        val quoted = JSONObject.quote(text)
        webView.evaluateJavascript("window.loadHolooJsonText($quoted);", null)
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
