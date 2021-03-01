package com.netease.nis.alivedetecteddemo;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by hzhuqi on 2019/10/14
 */
public class FailureActivity extends Activity {
    private ImageButton imgBtnBack;
    private Button btnBackToDemo;
    private TextView tvToken, tvCopy;
    private ClipboardManager mClipboardManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_failure);
        mClipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        String token = getIntent().getStringExtra("token");
        initView(token);
    }


    private void initView(String token) {
        tvToken = findViewById(R.id.tv_token);
        if (!TextUtils.isEmpty(token)) {
            tvToken.setText(token);
        }
        btnBackToDemo = findViewById(R.id.btn_back_to_demo);
        btnBackToDemo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(FailureActivity.this, WelcomeActivity.class);
                startActivity(intent);
            }
        });
        imgBtnBack = findViewById(R.id.img_btn_back);
        imgBtnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(FailureActivity.this, WelcomeActivity.class);
                startActivity(intent);
            }
        });

        tvCopy = findViewById(R.id.tv_copy);
        tvCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String token = tvToken.getText().toString();
                ClipData clipData = ClipData.newPlainText("token", token);
                mClipboardManager.setPrimaryClip(clipData);
                Util.showToast(FailureActivity.this, "Token复制成功");
            }
        });
    }
}
