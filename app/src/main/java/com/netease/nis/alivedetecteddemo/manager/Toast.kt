package com.netease.nis.alivedetecteddemo.manager

import android.content.Context
import android.widget.Toast

/**
 * @author liuxiaoshuai
 * @date 2022/3/16
 * @desc
 * @email liulingfeng@mistong.com
 */

fun String.showToast(context: Context, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(context, this, duration).show()
}

fun Int.showToast(context: Context, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(context, this, duration).show()
}