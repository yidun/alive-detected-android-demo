package com.netease.nis.alivedetecteddemo

import android.app.Application
import android.util.Log
import com.tencent.smtt.sdk.QbSdk

/**
 * @author liuxiaoshuai
 * @date 2022/7/11
 * @desc
 * @email liulingfeng@mistong.com
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()

        QbSdk.initX5Environment(this, object : QbSdk.PreInitCallback {
            override fun onCoreInitFinished() {
            }

            override fun onViewInitFinished(isX5: Boolean) {
                Log.d("App", "x5预初始化结束$isX5")
            }

        })
        QbSdk.setDownloadWithoutWifi(true)
    }
}