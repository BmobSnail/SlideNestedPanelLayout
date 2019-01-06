package com.snail.slidenested;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.ViewGroup;

/**
 * author：created by Snail.江
 * time: 2018/12/21 14:26
 * email：409962004@qq.com
 * TODO: 自定义布局属性
 */
public class PanelLayoutParams extends ViewGroup.MarginLayoutParams {

    private static final int[] ATTRS = {android.R.attr.layout_weight};

    public float weight = 0;

    public PanelLayoutParams() {
        super(MATCH_PARENT, MATCH_PARENT);
    }

    public PanelLayoutParams(int width, int height) {
        super(width, height);
    }

    public PanelLayoutParams(int width, int height, int weight) {
        this(width, height);
        this.weight = weight;
    }

    public PanelLayoutParams(ViewGroup.LayoutParams source) {
        super(source);
    }

    public PanelLayoutParams(ViewGroup.MarginLayoutParams source) {
        super(source);
    }

    public PanelLayoutParams(PanelLayoutParams source) {
        super(source);
    }


    public PanelLayoutParams(Context c, AttributeSet attrs) {
        super(c, attrs);

        final TypedArray ta = c.obtainStyledAttributes(attrs, ATTRS);
        if (ta != null) {
            this.weight = ta.getFloat(0, 0);
            ta.recycle();
        }
    }
}
