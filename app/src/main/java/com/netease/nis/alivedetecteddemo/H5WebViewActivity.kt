package com.netease.nis.alivedetecteddemo

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.tencent.smtt.export.external.TbsCoreSettings
import com.tencent.smtt.export.external.interfaces.PermissionRequest
import com.tencent.smtt.sdk.QbSdk
import com.tencent.smtt.sdk.WebChromeClient
import kotlinx.android.synthetic.main.activity_webview_h5.*


/**
 * @author liuxiaoshuai
 * @date 2022/7/11
 * @desc 腾讯h5 webView示例
 * @email liulingfeng@mistong.com
 */
class H5WebViewActivity : AppCompatActivity() {
    companion object {
        private const val ALIVE_URL =
            "https://verify.dun.163.com/prod/index.html"
    }

    private var permissionRequest: PermissionRequest? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview_h5)

        // 在调用TBS初始化、创建WebView之前进行如下配置
        // 在调用TBS初始化、创建WebView之前进行如下配置
        val map = HashMap<String, Any>()
        map[TbsCoreSettings.TBS_SETTINGS_USE_SPEEDY_CLASSLOADER] = true
        map[TbsCoreSettings.TBS_SETTINGS_USE_DEXLOADER_SERVICE] = true
        QbSdk.initTbsSettings(map)

        // 刘海屏适配
        webView?.settingsExtension?.setDisplayCutoutEnable(true)
        webView?.settings?.savePassword = false
        webView?.settings?.javaScriptEnabled = true
        webView?.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {
                Log.i("H5WebViewActivity", "h5权限回调")
                if (ContextCompat.checkSelfPermission(
                        this@H5WebViewActivity,
                        Manifest.permission.CAMERA
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.i("H5WebViewActivity", "权限申请")
                    this@H5WebViewActivity.permissionRequest = request
                    ActivityCompat.requestPermissions(
                        this@H5WebViewActivity,
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
        try {
            if (webView != null) {
                webView.stopLoading()
                webView.removeAllViewsInLayout()
                webView.removeAllViews()
                webView.webViewClient = null
                webView.destroy()
            }
        } catch (throwable: Throwable) {
            throwable.printStackTrace();
        } finally {
            super.onDestroy();
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.i("WebViewActivity", "权限申请通过")
            permissionRequest?.grant(permissionRequest?.resources)
        } else {
            Log.i("WebViewActivity", "权限申请拒绝")
            permissionRequest?.deny()
        }
    }
}