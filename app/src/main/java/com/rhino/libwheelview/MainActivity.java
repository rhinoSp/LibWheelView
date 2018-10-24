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

        mWheelView1 = findViewById(R.id.WheelView1);
        String[] displayedValues = new String[] {
              "晴", "阴","多云","小雨","中雨","大雨","暴雨","冻雨"
        };
        mWheelView1.setMinValue(0);
        mWheelView1.setMaxValue(displayedValues.length-1);
        mWheelView1.setDisplayedValues(displayedValues);
        mWheelView1.setOnValueChangedListener(new WheelView.OnValueChangeListener() {
            @Override
            public void onValueChange(WheelView picker, int oldVal, int newVal) {
                Log.d("RHINO", "1111 oldVal = " + oldVal + ", newVal = " + newVal);
            }
        });
        mWheelView2 = findViewById(R.id.WheelView2);
        mWheelView1.setOnValueChangedListener(new WheelView.OnValueChangeListener() {
            @Override
            public void onValueChange(WheelView picker, int oldVal, int newVal) {
                Log.d("RHINO", "2222 oldVal = " + oldVal + ", newVal = " + newVal);
            }
        });

    }
}
