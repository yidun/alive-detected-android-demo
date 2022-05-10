package com.netease.nis.alivedetecteddemo.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.net.ConnectivityManager
import android.widget.Toast

/**
 * Created by hzhuqi on 2020/2/24
 */
object Util {
    @JvmStatic
    fun showDialog(
        activity: Activity, title: String?, message: String?,
        positiveText: String?, negativeText: String?,
        positiveListener: DialogInterface.OnClickListener?,
        negativeListener: DialogInterface.OnClickListener?
    ) {
        if (!activity.isFinishing) {
            activity.runOnUiThread {
                val builder = AlertDialog.Builder(activity)
                builder.setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(positiveText, positiveListener)
                    .setNegativeButton(negativeText, negativeListener)
                    .show()
            }
        }
    }

    fun isNetWorkAvailable(context: Context): Boolean {
        var isAvailable = false
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = cm.activeNetworkInfo
        if (networkInfo != null && networkInfo.isAvailable) {
            isAvailable = true
        }
        return isAvailable
    }

    /**
     * 设置当前窗口亮度
     *
     * @param brightness
     */
    @JvmStatic
    fun setWindowBrightness(context: Activity, brightness: Float) {
        val window = context.window
        val lp = window.attributes
        lp.screenBrightness = brightness
        window.attributes = lp
    }

    fun dip2px(context: Context, dipValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dipValue * scale + 0.5f).toInt()
    }
}