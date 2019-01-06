package com.snail.slidenested;

import android.view.View;

/**
 * author：created by Snail.江
 * time: 2018/12/21 11:55
 * email：409962004@qq.com
 * TODO: 面板滑动回调
 */
public interface PanelSlideListener {

    //当面板滑动位置发送变化
    void onPanelSlide(View panel,float slideOffset);


    //当面板状态发送变化
    void onPanelStateChanged(View panel,PanelState preState,PanelState newState);
}
