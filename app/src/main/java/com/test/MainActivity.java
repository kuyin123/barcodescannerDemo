package com.test;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.zxing.client.android.CaptureActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button bnt = (Button)findViewById(R.id.btn1);
        try{
            bnt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this,
                            CaptureActivity.class);
                    startActivity(intent);
                }
            });
           //
        }catch (Exception e){
            Exception a =e;
        }


    }
}
