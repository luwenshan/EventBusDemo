package com.lws.eventbusdemo;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.lws.myeventbus.MyEventBus;
import com.lws.myeventbus.Subscribe;
import com.lws.myeventbus.ThreadMode;

public class MyEventBusDemoActivity extends AppCompatActivity {
    private Button mBtn;
    private TextView mTv;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_event_bus_demo);

        mBtn = findViewById(R.id.btn);
        mTv = findViewById(R.id.tv);
        mBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyEventBus.getDefault().post("Hi!!!!!");
            }
        });
        Button btn2 = findViewById(R.id.btn2);
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        MyEventBus.getDefault().post("hahahhahahah");
                    }
                }).start();
            }
        });
        MyEventBus.getDefault().register(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateUI(String event) {
        mTv.setText(event);
    }
}
