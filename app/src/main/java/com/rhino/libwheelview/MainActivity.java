package com.rhino.libwheelview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.rhino.wheel.WheelView;

public class MainActivity extends AppCompatActivity {

    private WheelView mWheelView1;
    private WheelView mWheelView2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String[] displayedValues1 = new String[] {
                "晴", "阴","多云","小雨","中雨","大雨","暴雨","冻雨"
        };
        mWheelView1 = findViewById(R.id.WheelView1);
        mWheelView1.setMinValue(0);
        mWheelView1.setMaxValue(displayedValues1.length-1);
        mWheelView1.setDisplayedValues(displayedValues1);
        mWheelView1.setOnValueChangedListener(new WheelView.OnValueChangeListener() {
            @Override
            public void onValueChange(WheelView picker, int oldVal, int newVal) {
                Log.d("RHINO", "1111 oldVal = " + oldVal + ", newVal = " + newVal);
            }
        });

        String[] displayedValues2 = new String[] {
                "晴晴晴晴晴晴晴晴晴晴", "阴","多云","小雨","中雨","大雨","暴雨","冻雨"
        };
        mWheelView2 = findViewById(R.id.WheelView2);
        mWheelView2.setMinValue(0);
        mWheelView2.setMaxValue(displayedValues2.length-1);
        mWheelView2.setDisplayedValues(displayedValues2);
        mWheelView2.setOnValueChangedListener(new WheelView.OnValueChangeListener() {
            @Override
            public void onValueChange(WheelView picker, int oldVal, int newVal) {
                Log.d("RHINO", "2222 oldVal = " + oldVal + ", newVal = " + newVal);
            }
        });

    }
}
