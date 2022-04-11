package com.netease.nis.alivedetecteddemo.manager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author liuxiaoshuai
 * @date 2022/3/16
 * @desc
 * @email liulingfeng@mistong.com
 */
object BroadcastDispatcher {
    private var canInit = AtomicBoolean(true)
    private var isScreenOff = AtomicBoolean(false)
    private var screenOffReceiver: ScreenOffReceiver = ScreenOffReceiver()
    private lateinit var screenOffFilter: IntentFilter
    private var listener: ScreenStatusChangedListener? = null

    init {
        if (canInit.get()) {
            val intentFilter = IntentFilter("android.intent.action.SCREEN_OFF")
            screenOffFilter = intentFilter
            intentFilter.addAction("android.intent.action.SCREEN_ON")
            screenOffFilter.addAction("android.intent.action.USER_PRESENT")
            canInit.set(false)
        }

    }

    @Synchronized
    fun registerScreenOff(context: Context) {
        if (!isScreenOff.get()) {
            try {
                context.registerReceiver(screenOffReceiver, screenOffFilter)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isScreenOff.set(true)
        }
    }

    @Synchronized
    fun unRegisterScreenOff(context: Context) {
        if (isScreenOff.get()) {
            try {
                context.unregisterReceiver(screenOffReceiver)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            listener = null
            isScreenOff.set(false)
        }
    }

    @Synchronized
    fun addScreenStatusChangedListener(listener: ScreenStatusChangedListener) {
        this.listener = listener
    }

    private class ScreenOffReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action: String? = intent?.action
            action?.let {
                when (it) {
                    "android.intent.action.SCREEN_OFF" -> {
                        listener?.onBackground()
                    }
                    // USER_PRESENT可能比SCREEN_OFF接收快
                    "android.intent.action.USER_PRESENT" -> {
                        listener?.onForeground()
                    }
                    else -> {

                    }
                }
            }
        }
    }

    interface ScreenStatusChangedListener {
        fun onForeground()
        fun onBackground()
    }
}