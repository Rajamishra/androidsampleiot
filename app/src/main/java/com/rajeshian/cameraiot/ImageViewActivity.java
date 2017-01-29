package com.rajeshian.cameraiot;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import java.io.File;

public class ImageViewActivity extends AppCompatActivity {

    ImageView mImgView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);
        mImgView = (ImageView) findViewById(R.id.imageView);

        File imgFile = new File(Environment.getExternalStorageDirectory().toString() + "/SmartCCTV/intruder.jpg");

        if (imgFile.exists()) {

            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

            mImgView.setImageBitmap(myBitmap);

        }
    }
}
