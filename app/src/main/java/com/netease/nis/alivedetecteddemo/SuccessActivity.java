package com.netease.nis.alivedetecteddemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

/**
 * Created by hzhuqi on 2019/10/14
 */
public class SuccessActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_success);
        initView();
    }


    private void initView() {
        Button btnBackToDemo = findViewById(R.id.btn_back_to_demo);
        btnBackToDemo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SuccessActivity.this, WelcomeActivity.class);
                startActivity(intent);
            }
        });
        ImageButton imgBtnBack = findViewById(R.id.img_btn_back);
        imgBtnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SuccessActivity.this, WelcomeActivity.class);
                startActivity(intent);
            }
        });
    }
}
