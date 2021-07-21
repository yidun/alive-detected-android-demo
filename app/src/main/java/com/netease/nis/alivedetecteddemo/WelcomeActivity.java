package com.netease.nis.alivedetecteddemo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created by hzhuqi on 2019/3/21
 */
public class WelcomeActivity extends Activity {
    private final int RC_ALL_PERM = 10000;
    private final String[] PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    Button btnJumpToMainActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
        btnJumpToMainActivity = findViewById(R.id.btn_jump_to_main_act);
        btnJumpToMainActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!EasyPermissions.hasPermissions(WelcomeActivity.this, Manifest.permission.CAMERA)) {
                    Toast.makeText(getApplicationContext(), "您未授予相机权限，请到设置中开启权限", Toast.LENGTH_LONG).show();
                } else if (!EasyPermissions.hasPermissions(WelcomeActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(getApplicationContext(), "您未授予文件存储权限，请到设置中开启权限", Toast.LENGTH_LONG).show();
                } else if (!Util.isNetWorkAvailable(getApplicationContext())) {
                    Toast.makeText(getApplicationContext(), "网络异常，请检查网络连接", Toast.LENGTH_LONG).show();
                } else {
                    Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
                    startActivity(intent);
                }
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        requestPermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // EasyPermissions handles the request result.
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @AfterPermissionGranted(RC_ALL_PERM)
    public void requestPermissions() {
        if (!EasyPermissions.hasPermissions(this, PERMISSIONS)) {
            EasyPermissions.requestPermissions(this, getString(R.string.permission_tip),
                    RC_ALL_PERM, PERMISSIONS);
        }
    }
}
