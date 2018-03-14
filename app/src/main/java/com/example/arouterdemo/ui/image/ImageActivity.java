package com.example.arouterdemo.ui.image;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.example.arouterdemo.ui.ModuleUtil;
import com.example.arouterdemo.ui.R;

@Route(path = ModuleUtil.PATH_APP_IMAGE)
public class ImageActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view);
    }
}
