package com.netease.nis.alivedetecteddemo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_success.*

/**
 * Created by hzhuqi on 2019/10/14
 */
class SuccessActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_success)
        initView()
    }

    private fun initView() {
        btn_back_to_demo.setOnClickListener {
            finish()
        }
        img_btn_back.setOnClickListener {
            finish()
        }
    }
}