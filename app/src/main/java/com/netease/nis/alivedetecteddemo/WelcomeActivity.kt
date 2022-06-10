package com.netease.nis.alivedetecteddemo

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_welcome.*
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions

/**
 * @author liu
 * @date 2021/10/12
 * @desc 欢迎页
 * @email liulingfeng@mistong.com
 */
class WelcomeActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {
    companion object {
        private const val TAG = "WelcomeActivity"
        private const val RC_ALL_PERM = 10000
    }

    private val PERMISSIONS =
        arrayOf(Manifest.permission.CAMERA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        btn_jump_to_main_act.setOnClickListener {
            checkPermissionAndJump(MainActivity::class.java)
        }

        btn_type.setOnClickListener {
            checkPermissionAndJump(WebViewActivity::class.java)
        }
    }

    private fun checkPermissionAndJump(clazz: Class<*>) {
        if (!EasyPermissions.hasPermissions(this, Manifest.permission.CAMERA)) {
            Toast.makeText(applicationContext, "您未授予相机权限，请到设置中开启权限", Toast.LENGTH_LONG).show()
        } else {
            val intent = Intent(this, clazz)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        requestPermissions()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        Log.i(TAG, "权限申请成功")
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        Log.i(TAG, "权限申请被拒绝")
    }

    @AfterPermissionGranted(RC_ALL_PERM)
    fun requestPermissions() {
        if (!EasyPermissions.hasPermissions(
                this,
                *PERMISSIONS
            )
        ) {
            EasyPermissions.requestPermissions(
                this, getString(R.string.permission_tip),
                RC_ALL_PERM, *PERMISSIONS
            )
        }
    }
}