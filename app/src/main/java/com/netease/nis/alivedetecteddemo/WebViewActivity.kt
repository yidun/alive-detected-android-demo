package com.netease.nis.alivedetecteddemo

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.ViewParent
import android.webkit.JsPromptResult
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * @author liu
 * @date 2021/10/12
 * @desc H5视频活体
 * @email liulingfeng@mistong.com
 */
class WebViewActivity : AppCompatActivity() {
    companion object {
        private const val ALIVE_URL =
            "https://verify.dun.163.com/prod/index.html"
    }

    private var permissionRequest: PermissionRequest? = null
    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        initWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        webView = findViewById(R.id.webView)
        webView?.settings?.javaScriptEnabled = true
        webView?.settings?.allowFileAccess = false
        webView?.settings?.setGeolocationEnabled(false)
        webView?.settings?.allowContentAccess = false
        // 缩放按钮
        webView?.settings?.builtInZoomControls = false
        // 自适应屏幕
        webView?.settings?.useWideViewPort = true
        // 默认大视野模式
        webView?.settings?.loadWithOverviewMode = true
        webView?.settings?.setSupportMultipleWindows(true)
        webView?.settings?.domStorageEnabled = true
        webView?.settings?.databaseEnabled = true
        webView?.settings?.defaultTextEncodingName = "UTF-8"
        webView?.isHorizontalScrollBarEnabled = false
        webView?.isVerticalScrollBarEnabled = false

        webView?.webViewClient = object : WebViewClient() {
            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                return this.shouldOverrideUrlLoading(view, request?.url.toString())
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                Log.i("WebViewActivity", "shouldOverrideUrlLoading${url}")
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                Log.i("WebViewActivity", "onPageStarted${url}")
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                Log.i("WebViewActivity", "onPageFinished${url}")
                super.onPageFinished(view, url)
            }

        }
        webView?.webChromeClient = object : WebChromeClient() {
            override fun onJsPrompt(
                view: WebView?,
                url: String?,
                message: String?,
                defaultValue: String?,
                result: JsPromptResult?
            ): Boolean {
                Log.i("WebViewActivity", "onJsPrompt$message")
                return super.onJsPrompt(view, url, message, defaultValue, result)
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                Log.i("WebViewActivity", "h5权限回调")
                if (ContextCompat.checkSelfPermission(
                        this@WebViewActivity,
                        Manifest.permission.CAMERA
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.i("H5WebViewActivity", "权限申请")
                    this@WebViewActivity.permissionRequest = request
                    ActivityCompat.requestPermissions(
                        this@WebViewActivity,
                        arrayOf(Manifest.permission.CAMERA),
                        1001
                    )
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        request?.grant(request.resources)
                    }
                }
            }
        }
        webView?.loadUrl(ALIVE_URL)
    }

    override fun onDestroy() {
        webView?.let {
            val parent: ViewParent = it.parent
            (parent as ViewGroup).removeView(webView)
            it.stopLoading()
            // 退出时调用此方法，移除绑定的服务，否则某些特定系统会报错
            it.settings.javaScriptEnabled = false
            it.clearHistory()
            it.removeAllViews()
            it.destroy()
        }
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if ((keyCode == KeyEvent.KEYCODE_BACK && webView != null && webView?.canGoBack() == true)) {
            webView?.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.i("WebViewActivity", "权限申请通过")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                permissionRequest?.grant(permissionRequest?.resources)
            }

        } else {
            Log.i("WebViewActivity", "权限申请拒绝")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                permissionRequest?.deny()
            }
        }
    }
}