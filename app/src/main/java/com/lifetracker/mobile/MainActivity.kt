package com.lifetracker.mobile

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

/**
 * Life Tracker ships as a single self-contained web app (assets/www/index.html)
 * running inside a plain WebView. All data lives in the WebView's local
 * storage on this device; nothing is ever sent over the network, so this
 * app requests no permissions at all.
 *
 * Two small native bridges make the in-page "Backup & data" screen behave
 * like a real Android app instead of a browser tab:
 *  - saveBackup(): the page calls this to hand the app a JSON backup string,
 *    which is written to wherever the user picks via Android's own "Save
 *    file" dialog (Storage Access Framework).
 *  - onShowFileChooser(): backs the page's existing "Choose a file" restore
 *    button with Android's document picker, so the browser file input
 *    just works without any other change to the page.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var filePickerCallback: ValueCallback<Array<Uri>>? = null
    private var pendingBackupJson: String? = null
    private var pendingBackupName: String = "life-tracker-backup.json"

    private val openDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            val callback = filePickerCallback
            filePickerCallback = null
            callback?.onReceiveValue(if (uri != null) arrayOf(uri) else null)
        }

    private val createDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
            val json = pendingBackupJson
            pendingBackupJson = null
            if (uri == null || json == null) return@registerForActivityResult
            try {
                contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(this, "Backup saved", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Couldn't save the file", Toast.LENGTH_SHORT).show()
            }
        }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        webView.addJavascriptInterface(BackupBridge(), "Android")
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView?,
                callback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                filePickerCallback?.onReceiveValue(null)
                filePickerCallback = callback
                return try {
                    openDocumentLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    true
                } catch (e: Exception) {
                    filePickerCallback = null
                    false
                }
            }
        }

        webView.loadUrl("file:///android_asset/www/index.html")

        // Let the hardware/gesture back action close an open sheet or the
        // drawer first, matching what back already does on claude.ai and
        // only exiting the app once nothing is open.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                webView.evaluateJavascript(
                    "(function(){" +
                        "var s=document.getElementById('sheetHost');" +
                        "if(s&&s.firstChild)return 'sheet';" +
                        "var d=document.getElementById('drawer');" +
                        "if(d&&d.classList.contains('open'))return 'drawer';" +
                        "return 'none';" +
                        "})()"
                ) { result ->
                    when (result?.trim('"')) {
                        "sheet" -> webView.evaluateJavascript("closeSheet()", null)
                        "drawer" -> webView.evaluateJavascript("closeDrawer()", null)
                        else -> {
                            isEnabled = false
                            onBackPressedDispatcher.onBackPressed()
                        }
                    }
                }
            }
        })
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    private inner class BackupBridge {
        /** Called from the page's "Save backup file" button. */
        @JavascriptInterface
        fun saveBackup(json: String, filename: String) {
            pendingBackupJson = json
            if (filename.isNotBlank()) pendingBackupName = filename
            runOnUiThread {
                try {
                    createDocumentLauncher.launch(pendingBackupName)
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Couldn't open the save dialog", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
