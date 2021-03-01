package com.netease.nis.alivedetecteddemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * Created by hzhuqi on 2020/2/24
 */
public class Util {
    public static void showToast(final Activity activity, final String tip) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity.getApplicationContext(), tip, Toast.LENGTH_LONG).show();
            }
        });
    }

    public static void showDialog(final Activity activity, final String title, final String message,
                                  final String positiveText, final String negativeText,
                                  final DialogInterface.OnClickListener positiveListener,
                                  final DialogInterface.OnClickListener negativeListener) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(title)
                        .setMessage(message)
                        .setPositiveButton(positiveText, positiveListener)
                        .setNegativeButton(negativeText, negativeListener)
                        .show();
            }
        });

    }

    public static boolean isNetWorkAvailable(Context context) {
        boolean isAvailable = false;
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isAvailable()) {
            isAvailable = true;
        }
        return isAvailable;
    }

    /**
     * 设置当前窗口亮度
     *
     * @param brightness
     */
    public static void setWindowBrightness(Activity context, float brightness) {
        Window window = context.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.screenBrightness = brightness;
        window.setAttributes(lp);
    }

}
