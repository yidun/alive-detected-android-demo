package com.netease.nis.alivedetecteddemo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.KeyEvent
import android.webkit.PermissionRequest
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.just.agentweb.AgentWeb
import com.just.agentweb.AgentWebUIControllerImplBase
import com.just.agentweb.WebChromeClient
import kotlinx.android.synthetic.main.activity_webview.*

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

    private var mAgentWeb: AgentWeb? = null
    private var permissionRequest: PermissionRequest? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

//        btn_type.setOnClickListener {
//            val intent = Intent(
//                this,
//                WelcomeActivity::class.java
//            )
//            startActivity(intent)
//        }
        loadWeb()
    }

    private fun loadWeb() {
        mAgentWeb = AgentWeb.with(this)
            .setAgentWebParent(rl_wv_container, FrameLayout.LayoutParams(-1, -1))
            .useDefaultIndicator()
            .setAgentWebUIController(object : AgentWebUIControllerImplBase() {
                override fun onSelectItemsPrompt(
                    view: WebView,
                    url: String,
                    ways: Array<String>,
                    callback: Handler.Callback
                ) {
                    super.onSelectItemsPrompt(
                        view,
                        url,
                        arrayOf(getString(R.string.agentweb_camera)),
                        callback
                    )
                }
            })
            .setWebChromeClient(object : WebChromeClient() {
                override fun onPermissionRequest(request: PermissionRequest?) {
                    Log.i("WebViewActivity", "h5权限回调")
                    if (ContextCompat.checkSelfPermission(
                            this@WebViewActivity,
                            Manifest.permission.CAMERA
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        Log.i("WebViewActivity", "权限申请")
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
            })
            .setMainFrameErrorView(R.layout.agentweb_error_page, -1)
            .setSecurityType(AgentWeb.SecurityType.STRICT_CHECK)
            .interceptUnkownUrl() //拦截找不到相关页面的Scheme
            .createAgentWeb()
            .ready()
            .go(ALIVE_URL)
    }

    override fun onResume() {
        mAgentWeb?.webLifeCycle?.onResume()
        super.onResume()
    }

    override fun onPause() {
        mAgentWeb?.webLifeCycle?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mAgentWeb?.webLifeCycle?.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return if (mAgentWeb?.handleKeyEvent(keyCode, event) == true) {
            true
        } else super.onKeyDown(keyCode, event)
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