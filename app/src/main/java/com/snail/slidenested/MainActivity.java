package com.snail.slidenested;

import android.graphics.Color;
import android.support.design.widget.TabLayout;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final FrameLayout mFrameLayout = findViewById(R.id.frameLayout);
        TabLayout mTabLayout = findViewById(R.id.tabLayout);
        mTabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        mTabLayout.addTab(mTabLayout.newTab().setText("费用说明"));
        mTabLayout.addTab(mTabLayout.newTab().setText("预定须知"));
        mTabLayout.addTab(mTabLayout.newTab().setText("退款政策"));
        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        mFrameLayout.setBackgroundColor(Color.parseColor("#ff0000"));
                        break;

                    case 1:
                        mFrameLayout.setBackgroundColor(Color.parseColor("#0000ff"));
                        break;

                    case 2:
                        mFrameLayout.setBackgroundColor(Color.parseColor("#00ff00"));
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        NestedScrollView mScrollView = findViewById(R.id.nestedScrollView);
        mScrollView.animate().translationY(-150).alpha(1.0f).setDuration(500);

        SlideNestedPanelLayout mPanelLayout = findViewById(R.id.slideNestedPanelLayout);
        mPanelLayout.setStateCallback(new StateCallback() {
            @Override
            public void onExpandedState() {
                Log.i("-->","onExpandedState");
            }

            @Override
            public void onCollapsedState() {
                Log.i("-->","onCollapsedState");
            }
        });
    }
}
