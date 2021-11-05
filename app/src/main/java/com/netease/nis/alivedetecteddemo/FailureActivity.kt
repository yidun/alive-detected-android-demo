package com.netease.nis.alivedetecteddemo

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.TextView
import com.netease.nis.alivedetecteddemo.Util.showToast
import kotlinx.android.synthetic.main.activity_failure.*

/**
 * Created by hzhuqi on 2019/10/14
 */
class FailureActivity : Activity() {
    private var tvToken: TextView? = null
    private var mClipboardManager: ClipboardManager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_failure)
        mClipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val token = intent.getStringExtra("token")
        initView(token)
    }

    private fun initView(token: String?) {
        tvToken = findViewById(R.id.tv_token)
        if (!TextUtils.isEmpty(token)) {
            tvToken?.text = token
        }
        btn_back_to_demo.setOnClickListener {
            finish()
        }
        img_btn_back.setOnClickListener {
            finish()
        }
        tv_copy.setOnClickListener {
            val clipData = ClipData.newPlainText("token", tvToken?.text.toString())
            mClipboardManager?.setPrimaryClip(clipData)
            showToast(this@FailureActivity, "Token复制成功")
        }
    }
}