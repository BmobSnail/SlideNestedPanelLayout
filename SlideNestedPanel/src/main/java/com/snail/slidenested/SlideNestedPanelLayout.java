package com.snail.slidenested;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import com.snail.slidenestedpanel.R;

/**
 * author：created by Snail.江
 * time: 2018/12/20 11:13
 * email：409962004@qq.com
 * TODO: 仿美团订单，嵌套滑动拖拽控件
 */
public class SlideNestedPanelLayout extends ViewGroup {


    //默认面板状态
    private static final PanelState DEFAULT_STATE = PanelState.COLLAPSED;
    //最少滑动速度
    private static final int DEFAULT_FLING_VELOCITY = 400;
    //默认渐变色
    private static final int DEFAULT_FADE_COLOR = 0x99000000;
    //默认覆盖标识
    private static final boolean DEFAULT_OVERLAY_FLAG = false;
    //默认裁剪标识
    private static final boolean DEFAULT_CLIP_FLAG = true;
    //默认基准点
    private static final float DEFAULT_ANCHOR_POINT = 1.0f;
    //默认面板高度
    private static final int DEFAULT_PANEL_HEIGHT = 68;
    //默认视觉差比例
    private static final int DEFAULT_PARALLAX_OFFSET = 0;


    //面板高度
    private int mPanelHeight;


    //视觉差比例
    private int mParallaxOffset;


    //最少滑动速度因子
    private int mFlingVelocity;


    //覆盖渐变颜色
    private int mFadeColor;


    //嵌套滑动view resId
    private int mScrollViewResId;


    //覆盖内容标识
    private boolean mOverlayFlag;


    //裁剪标识
    private boolean mClipPanelFlag;


    //面板停止基准点
    private float mAnchorPoint;


    //状态
    private PanelState mPanelState = DEFAULT_STATE;


    //拖拽助手helper
    private ViewDragHelper mDragHelper = null;


    //是否依附到窗口
    private boolean isAttachedToWindow = true;


    //锁定面板，不可滑动标识
    private boolean isUnableToDrag;


    //主View
    private View mMainView;


    //面板内拖拽view
    private View mDragView;


    //面板内Scroll View
    private View mScrollView;


    //滑动辅助
    private ScrollableViewHelper mScrollableViewHelper = new ScrollableViewHelper();


    //面板距离展开的位置，范围0-anchorPoint(0为折叠，anchorPoint为展开基准点)
    private float mSlideOffset;


    //记录下面板已滑动的范围
    private int mSlideRange;


    //是否自己处理事件
    private boolean isMyHandleTouch = false;
    
    
    //是否到顶
    private boolean isOnTopFlag = false;


    //绘制区域
    private final Rect mTmpRect = new Rect();


    //当Main滑动时用来画渐变的画笔
    private final Paint mCoveredFadePaint = new Paint();


    //标记触摸位置
    private float mPrevMotionX;
    private float mPrevMotionY;
    private float mInitialMotionX;
    private float mInitialMotionY;


    public SlideNestedPanelLayout(Context context) {
        this(context, null);
    }

    public SlideNestedPanelLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlideNestedPanelLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        //定义拖拽的插值器
        //获取自定义属性
        Interpolator scrollerInterpolator = null;
        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SlideNestedPanelLayout);
            if (ta != null) {
                mPanelHeight = ta.getDimensionPixelSize(R.styleable.SlideNestedPanelLayout_panelHeight, -1);
                mParallaxOffset = ta.getDimensionPixelSize(R.styleable.SlideNestedPanelLayout_parallaxOffset, -1);
                mFlingVelocity = ta.getInt(R.styleable.SlideNestedPanelLayout_flingVelocity, DEFAULT_FLING_VELOCITY);
                mFadeColor = ta.getColor(R.styleable.SlideNestedPanelLayout_fadeColor, DEFAULT_FADE_COLOR);
                mScrollViewResId = ta.getResourceId(R.styleable.SlideNestedPanelLayout_scrollView, -1);
                mOverlayFlag = ta.getBoolean(R.styleable.SlideNestedPanelLayout_overlay, DEFAULT_OVERLAY_FLAG);
                mClipPanelFlag = ta.getBoolean(R.styleable.SlideNestedPanelLayout_clipPanel, DEFAULT_CLIP_FLAG);
                mAnchorPoint = ta.getFloat(R.styleable.SlideNestedPanelLayout_anchorPoint, DEFAULT_ANCHOR_POINT);
                mPanelState = PanelState.values()[ta.getInt(R.styleable.SlideNestedPanelLayout_initialState, DEFAULT_STATE.ordinal())];

                int interpolatorResId = ta.getResourceId(R.styleable.SlideNestedPanelLayout_interpolator, -1);
                if (interpolatorResId != -1) {
                    scrollerInterpolator = AnimationUtils.loadInterpolator(context, interpolatorResId);
                }
                ta.recycle();
            }
        }

        //根据密度算默值
        final float density = context.getResources().getDisplayMetrics().density;
        if (mPanelHeight == -1)
            mPanelHeight = (int) (DEFAULT_PANEL_HEIGHT * density + 0.5f);

        if (mParallaxOffset == -1)
            mParallaxOffset = (int) (DEFAULT_PARALLAX_OFFSET * density);

        //不要执行onDraw
        setWillNotDraw(false);

        mDragHelper = ViewDragHelper.create(this, 1.0f, scrollerInterpolator, new DragHelperCallback());
        mDragHelper.setMinVelocity(mFlingVelocity * density);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        isAttachedToWindow = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        isAttachedToWindow = false;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new PanelLayoutParams();
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof MarginLayoutParams
                ? new PanelLayoutParams((MarginLayoutParams) p)
                : new PanelLayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof PanelLayoutParams && super.checkLayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new PanelLayoutParams(getContext(), attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (mScrollViewResId != -1)
            mScrollView = findViewById(mScrollViewResId);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int paddingLeft = getPaddingLeft();
        final int paddingTop = getPaddingTop();
        final int childCount = getChildCount();

        if (isAttachedToWindow) {
            switch (mPanelState) {
                case EXPANDED:
                    mSlideOffset = mAnchorPoint;
                    break;

                case ANCHORED:
                    mSlideOffset = mAnchorPoint;
                    break;

                default:
                    mSlideOffset = 0.f;
                    break;
            }
        }

        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final PanelLayoutParams params = (PanelLayoutParams) child.getLayoutParams();

            if (child.getVisibility() == GONE && (i == 0 || isAttachedToWindow))
                continue;

            final int childHeight = child.getMeasuredHeight();
            int childTop = paddingTop;

            if (child == mDragView)
                childTop = computePanelToPosition(mSlideOffset);

            final int childBottom = childTop + childHeight;
            final int childLeft = paddingLeft + params.leftMargin;
            final int childRight = childLeft + child.getMeasuredWidth();
            child.layout(childLeft,childTop,childRight,childBottom);
        }

        applyParallaxForCurrentSlideOffset();
        isAttachedToWindow = false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        //宽高必须占满
        if (widthMode != MeasureSpec.EXACTLY && widthMode != MeasureSpec.AT_MOST)
            throw new IllegalArgumentException("Width 必须填满或者指定值");
        else if (heightMode != MeasureSpec.EXACTLY && heightMode != MeasureSpec.AT_MOST)
            throw new IllegalArgumentException("Height 必须填或者指定值");

        final int childCount = getChildCount();

        if (childCount != 2)
            throw new IllegalArgumentException("SlideNestedPanelLayout必须有2给子view");

        mMainView = getChildAt(0);
        mDragView = getChildAt(1);

        int layoutWidth = widthSize - getPaddingLeft() - getPaddingRight();
        int layoutHeight = heightSize - getPaddingTop() - getPaddingBottom();

        //第一步：首先测量子view的宽高
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final PanelLayoutParams params = (PanelLayoutParams) child.getLayoutParams();

            if (child.getVisibility() == GONE && i == 0)
                continue;

            int width = layoutWidth;
            int height = layoutHeight;


            //如果是主view，要记录需要overlay的高度
            if (child == mMainView) {
                if (!mOverlayFlag)
                    height -= mPanelHeight;
                width -= params.leftMargin + params.rightMargin;
            } else if (child == mDragView) {
                height -= params.topMargin;
            }

            //判断width应该使用的Mode
            int childWidthSpec;
            if (params.width == LayoutParams.WRAP_CONTENT) {
                childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST);
            } else if (params.width == LayoutParams.MATCH_PARENT) {
                childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
            } else {
                childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
            }

            //判断height应该使用的Mode
            int childHeightSpec;
            if (params.height == LayoutParams.WRAP_CONTENT) {
                childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST);
            } else {
                //根据权重修正高度
                if (params.weight > 0 && params.weight < 1)
                    height = (int) (height * params.weight);
                else if (params.height != LayoutParams.MATCH_PARENT)
                    height = params.height;
                childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
            }

            child.measure(childWidthSpec, childHeightSpec);

            if (child == mDragView)
                mSlideRange = mDragView.getMeasuredHeight() - mPanelHeight;
        }

        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled())
            return super.onTouchEvent(event);
        try {
            mDragHelper.processTouchEvent(event);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);
        //点击外部渐变层折叠收缩
        //松开，非折叠状态，且不在拖拽，点击落在dragView
        if (ev.getAction() == MotionEvent.ACTION_UP
                && !mDragHelper.isDragging()
                && mPanelState != PanelState.COLLAPSED
                && isViewUnder(mDragView, (int) ev.getX(), (int) ev.getY())) {
            mScrollView.scrollTo(0,0);
            mDragHelper.smoothSlideViewTo(mDragView,0,computePanelToPosition(0f));
            ViewCompat.postInvalidateOnAnimation(this);

            //点击渐变收缩后，把拖拽标识恢复
            setEnabled(true);
            return true;
        }

        final float x = ev.getX();
        final float y = ev.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:{
                mPrevMotionX = x;
                mPrevMotionY = y;
            }
            break;

            case MotionEvent.ACTION_MOVE:{
                float dx = x - mPrevMotionX;
                float dy = y - mPrevMotionY;
                mPrevMotionX = x;
                mPrevMotionY = y;

                //横向滑动就不分发了
                if (Math.abs(dx) > Math.abs(dy)) {
                    return true;
                }

                //滑动向上、向下
                if (dy > 0) { //收缩

                    if (mScrollableViewHelper.getScrollableViewScrollPosition(mScrollView, true) > 0) {
                        isMyHandleTouch = true;
                        return super.dispatchTouchEvent(ev);
                    }

                    //之前子view处理了事件
                    //我们就需要重新组合一下让面板得到一个合理的点击事件
                    if (isMyHandleTouch) {
                        MotionEvent up = MotionEvent.obtain(ev);
                        up.setAction(MotionEvent.ACTION_CANCEL);
                        super.dispatchTouchEvent(up);
                        up.recycle();
                        ev.setAction(MotionEvent.ACTION_DOWN);
                    }

                    isMyHandleTouch = false;
                    return this.onTouchEvent(ev);
                } else { //展开

                    //scrollY=0表示没滑动过，canScroll(1)表示可scroll up
                    //逻辑或的意义：拖拽到顶后，要不要禁用外部拖拽
                    if (isOnTopFlag) {
                        int offset = mDragView.getScrollY();
                        boolean scroll = mScrollableViewHelper.getScrollableViewScrollPosition(mScrollView, true) > 0;
                        setEnabled(offset == 0 || scroll);
                        mDragHelper.abort();
                        return super.dispatchTouchEvent(ev);
                    }

                    //面板是否全部展开
                    if (mSlideOffset < mAnchorPoint) {
                        isMyHandleTouch = false;
                        return this.onTouchEvent(ev);
                    }

                    if (!isMyHandleTouch && mDragHelper.isDragging()) {
                        mDragHelper.cancel();
                        ev.setAction(MotionEvent.ACTION_DOWN);
                    }

                    isMyHandleTouch = true;
                    return super.dispatchTouchEvent(ev);
                }
            }

            case MotionEvent.ACTION_UP:{
                //如果内嵌视图正在处理触摸，会接收到一个up事件、
                //我们想要清楚之前所有的拖拽状态，这样我们就不会意外拦截一个触摸事件
                if (isMyHandleTouch) {
                    mDragHelper.setDragState(ViewDragHelper.STATE_IDLE);
                }
            }
            break;
        }

        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        //如果scrollView处理事件，则不要拦截
        if (isMyHandleTouch) {
            mDragHelper.abort();
            return false;
        }

        int action = MotionEventCompat.getActionMasked(ev);
        float x = ev.getX();
        float y = ev.getY();
        float adx = Math.abs(x - mInitialMotionX);
        float ady = Math.abs(y - mInitialMotionY);
        int dragSlop = mDragHelper.getTouchSlop();

        switch (action) {
            case MotionEvent.ACTION_DOWN:{
                isUnableToDrag = false;
                mInitialMotionX = x;
                mInitialMotionY = y;
                if (isViewUnder(mDragView, (int) x, (int) y)) {
                    mDragHelper.cancel();
                    isUnableToDrag = true;
                    return false;
                }
            }
            break;

            case MotionEvent.ACTION_MOVE:{
                if (ady > dragSlop && adx > ady) {
                    mDragHelper.cancel();
                    isUnableToDrag = true;
                    return false;
                }
            }
            break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:{
                //如果抬手释放后drawView还在拖拽状态，需要调用processTouchEvent
                if (mDragHelper.isDragging()) {
                    mDragHelper.processTouchEvent(ev);
                    return true;
                }
            }
            break;
        }
        return mDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        boolean result;
        final int save = canvas.save();
        //mainView的遮盖渐变层
        if (mDragView != null && mDragView != child) {
            canvas.getClipBounds(mTmpRect);
            if (!mOverlayFlag) {
                mTmpRect.bottom = Math.min(mTmpRect.bottom, mDragView.getTop());
            }
            if (mClipPanelFlag) {
                canvas.clipRect(mTmpRect);
            }
            result = super.drawChild(canvas, child, drawingTime);
            if (mFadeColor != 0 && mSlideOffset > 0) {
                final int baseAlpha = (mFadeColor & 0xff000000) >>> 24;
                final int imag = (int) (baseAlpha * mSlideOffset);
                final int color = imag << 24 | (mFadeColor & 0xffffff);
                mCoveredFadePaint.setColor(color);
                canvas.drawRect(mTmpRect, mCoveredFadePaint);
            }
        } else {
            result = super.drawChild(canvas, child, drawingTime);
        }

        //没有合适的回调方法，只能另辟蹊径了
        //在这里判断dragView有没有到顶，然后把事件给内嵌view
        final int targetY = computePanelToPosition(mAnchorPoint);
        final int originalY = computePanelToPosition(0f);
        if (mDragView.getTop() == targetY) {
            isOnTopFlag = true;
        }else if (mDragView.getTop() == originalY){
            isOnTopFlag = false;
        }else {
            isOnTopFlag = false;
        }

        canvas.restoreToCount(save);
        return result;
    }

    @Override
    public void computeScroll() {
        if (mDragHelper != null && mDragHelper.continueSettling(true)) {
            if (!isEnabled()) {
                mDragHelper.abort();
                return;
            }
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }


    /**坐标是否落在target view*/
    private boolean isViewUnder(View view,int x,int y){
        if (view == null)
            return true;
        int[] viewLocation = new int[2];
        view.getLocationOnScreen(viewLocation);
        int[] parentLocation = new int[2];
        this.getLocationOnScreen(parentLocation);
        int screenX = parentLocation[0] + x;
        int screenY = parentLocation[1] + y;
        return screenX < viewLocation[0] || screenX >= viewLocation[0] + view.getWidth() ||
                screenY < viewLocation[1] || screenY >= viewLocation[1] + view.getHeight();
    }


    /**
     * 面板状态
     */
    private void setPanelStateInternal(PanelState state) {
        if (mPanelState == state)
            return;
        mPanelState = state;
    }


    /**
     * 计算滑动的偏移量
     */
    private int computePanelToPosition(float slideOffset) {
        int slidePixelOffset = (int) (slideOffset * mSlideRange);
        return getMeasuredHeight() - getPaddingBottom() - mPanelHeight - slidePixelOffset;
    }


    /**更新视觉差位置*/
    private void applyParallaxForCurrentSlideOffset(){
        if (mParallaxOffset > 0) {
            int offset = -(int)(mParallaxOffset * Math.max(mSlideOffset,0));
            mMainView.setTranslationY(offset);
        }
    }


    /**
     * 拖拽状态更新以及位置的更新
     * */
    private void onPanelDragged(int newTop) {
        setPanelStateInternal(PanelState.DRAGGING);
        //重新计算距离顶部偏移
        mSlideOffset = computeSlideOffset(newTop);
        //更新视觉差效果和分发事件
        applyParallaxForCurrentSlideOffset();
        //如果偏移是向上，覆盖则无效，需要增加main的高度
        LayoutParams lp = mMainView.getLayoutParams();
        int defaultHeight = getHeight() - getPaddingBottom() - getPaddingTop() - mPanelHeight;
        if (mSlideOffset <= 0 && !mOverlayFlag) {
            lp.height = (newTop - getPaddingBottom());
            if (lp.height == defaultHeight) {
                lp.height = LayoutParams.MATCH_PARENT;
            }
        } else if (lp.height != LayoutParams.MATCH_PARENT && !mOverlayFlag) {
            lp.height = LayoutParams.MATCH_PARENT;
        }
        mMainView.requestLayout();
    }


    /**
     * 计算滑动偏移量*/
    private float computeSlideOffset(int topPosition) {
        final int topBoundCollapsed = computePanelToPosition(0f);
        return (float) (topBoundCollapsed - topPosition) / mSlideRange;
    }


    /**
     * 拖拽回调
     */
    private class DragHelperCallback extends ViewDragHelper.Callback {

        private boolean slideUp = false;

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return !isUnableToDrag && child == mDragView;
        }

        @Override
        public void onViewDragStateChanged(int state) {
            if (mDragHelper != null && mDragHelper.getViewDragState() == ViewDragHelper.STATE_IDLE) {
                mSlideOffset = computeSlideOffset(mDragView.getTop());
                applyParallaxForCurrentSlideOffset();

                if (mSlideOffset == 1) {
                    setPanelStateInternal(PanelState.EXPANDED);
                } else if (mSlideOffset == 0) {
                    setPanelStateInternal(PanelState.COLLAPSED);
                } else {
                    setPanelStateInternal(PanelState.ANCHORED);
                }
            }
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            slideUp = dy > 0;//正为收缩，负为展开
            onPanelDragged(top);
            invalidate();
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            int target;
            if (!slideUp) {
                if (mSlideOffset >= mAnchorPoint / 6) {
                    target = computePanelToPosition(mAnchorPoint);
                } else {
                    target = computePanelToPosition(0f);
                }
            }else {
                if (mSlideOffset >= mAnchorPoint / 3) {
                    target = computePanelToPosition(0f);
                } else {
                    target = computePanelToPosition(mAnchorPoint);
                }
            }

            if (mDragHelper != null) {
                mDragHelper.settleCapturedViewAt(releasedChild.getLeft(), target);
            }

            invalidate();
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return mSlideRange;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            final int original = computePanelToPosition(0f);
            final int anchor = computePanelToPosition(mAnchorPoint);
            return Math.min(Math.max(top, anchor), original);
        }
    }
}
