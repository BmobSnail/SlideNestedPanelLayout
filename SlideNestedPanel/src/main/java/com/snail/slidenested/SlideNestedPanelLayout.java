package com.snail.slidenested;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

import com.snail.slidenestedpanel.R;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * author：created by Snail.江
 * time: 2018/12/20 11:13
 * email：409962004@qq.com
 * TODO: 仿美团订单，嵌套滑动拖拽控件
 */
public class SlideNestedPanelLayout extends ViewGroup {


    //默认的属性
    private static final int[] DEFAULT_ATTRS = {android.R.attr.gravity};
    //默认面板状态
    private static final PanelState DEFAULT_STATE = PanelState.COLLAPSED;
    //最少滑动因子
    private static final int DEFAULT_FLING_VELOCITY = 400;
    //默认渐变色
    private static final int DEFAULT_FADE_COLOR = 0x99000000;
    //默认覆盖标识
    private static final boolean DEFAULT_OVERLAY_FLAG = false;
    //默认裁剪标识
    private static final boolean DEFAULT_CLIP_FLAG = true;
    //默认锚点
    private static final float DEFAULT_ANCHOR_POINT = 1.0f;
    //默认面板高度
    private static final int DEFAULT_PANEL_HEIGHT = 68;
    //默认阴影高度
    private static final int DEFAULT_SHADOW_HEIGHT = 4;
    //默认视觉差比例
    private static final int DEFAULT_PARALLAX_OFFSET = 0;


    //面板高度
    private int mPanelHeight;


    //阴影高度
    private int mShadowHeight;


    //视觉差比例
    private int mParallaxOffset;


    //最少滑动速度因子
    private int mFlingVelocity;


    //覆盖渐变颜色
    private int mFadeColor;


    //拖拽view resId
    private int mDragViewResId;


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


    //阴影
    private Drawable mShadowDrawable = null;


    //拖拽助手helper
    private ViewDragHelper mDragHelper = null;


    //折叠面板时是否可以上滑
    private boolean isSlidingUp = false;


    //标记最后一次操作的布局状态？？？
    private boolean isFirstLayout = true;

    //锁定面板，不可滑动标识
    private boolean isUnableToDrag;


    //是否可触摸
    private boolean isTouchEnable = false;


    //面板内拖拽view
    private View mDragView;


    //面板内Scroll View
    private View mScrollView;


    //可以滑动的子view
    private View mSlideChildView;


    //主View
    private View mMainView;


    //面板回调集合
    private final List<PanelSlideListener> mPanelSlideListeners = new CopyOnWriteArrayList<>();


    //滑动辅助
    private ScrollableViewHelper mScrollableViewHelper = new ScrollableViewHelper();


    //渐变部分的监听
    private OnClickListener mFadeOnClickListener;


    //面板距离展开的位置，范围0-anchorPoint(0为折叠，anchorPoint为展开基准点)
    private float mSlideOffset;


    //记录下面板已滑动的范围
    private int mSlideRange;


    //是否自己处理事件
    private boolean isHandlingTouch = false;
    
    
    //是否到顶
    private boolean mArrivedTopFlag = false;


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

        //能否编辑
        if (isInEditMode()) {
            mShadowDrawable = null;
            mDragHelper = null;
            return;
        }

        //定义拖拽的插值器
        Interpolator scrollerInterpolator = null;
        //获取自定义属性
        if (attrs != null) {
            TypedArray defAttrs = context.obtainStyledAttributes(defStyleAttr, DEFAULT_ATTRS);
            if (defAttrs != null) {
                int gravity = defAttrs.getInt(0, Gravity.BOTTOM);
                setGravity(gravity);
                defAttrs.recycle();
            }

            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SlideNestedPanelLayout);
            if (ta != null) {
                mPanelHeight = ta.getDimensionPixelSize(R.styleable.SlideNestedPanelLayout_panelHeight, -1);
                mShadowHeight = ta.getDimensionPixelSize(R.styleable.SlideNestedPanelLayout_shadowHeight, -1);
                mParallaxOffset = ta.getDimensionPixelSize(R.styleable.SlideNestedPanelLayout_parallaxOffset, -1);
                mFlingVelocity = ta.getInt(R.styleable.SlideNestedPanelLayout_flingVelocity, DEFAULT_FLING_VELOCITY);
                mFadeColor = ta.getColor(R.styleable.SlideNestedPanelLayout_fadeColor, DEFAULT_FADE_COLOR);
                mDragViewResId = ta.getResourceId(R.styleable.SlideNestedPanelLayout_drawView, -1);
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

        if (mShadowHeight == -1)
            mShadowHeight = (int) (DEFAULT_SHADOW_HEIGHT * density + 0.5f);

        if (mParallaxOffset == -1)
            mParallaxOffset = (int) (DEFAULT_PARALLAX_OFFSET * density);

        //如果阴影为0则不显示
        if (mShadowHeight > 0) {
            if (isSlidingUp) {
                mShadowDrawable = getResources().getDrawable(R.drawable.above_shadow);
            } else {
                mShadowDrawable = getResources().getDrawable(R.drawable.below_shadow);
            }
        } else {
            mShadowDrawable = null;
        }

        //不要执行onDraw
        setWillNotDraw(false);

        mDragHelper = ViewDragHelper.create(this, 1.0f, scrollerInterpolator, new DragHelperCallback());
        mDragHelper.setMinVelocity(mFlingVelocity * density);

        isTouchEnable = true;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        isFirstLayout = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        isFirstLayout = false;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        if (mDragViewResId != -1)
            setDragView(findViewById(mDragViewResId));

        if (mScrollViewResId != -1)
            setScrollView(findViewById(mScrollViewResId));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int paddingLeft = getPaddingLeft();
        final int paddingTop = getPaddingTop();
        final int childCount = getChildCount();

        if (isFirstLayout) {
            switch (mPanelState) {
                case EXPANDED:
                    mSlideOffset = 1.0f;
                    break;

                case ANCHORED:
                    mSlideOffset = mAnchorPoint;
                    break;

                case HIDDEN:
                    int newTop = computePanelToPosition(0.0f) + (isSlidingUp ? +mPanelHeight : -mPanelHeight);
                    mSlideOffset = computePanelToPosition(newTop);
                    break;

                default:
                    mSlideOffset = 0.f;
                    break;
            }
        }

        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final PanelLayoutParams params = (PanelLayoutParams) child.getLayoutParams();

            if (child.getVisibility() == GONE && (i == 0 || isFirstLayout))
                continue;

            final int childHeight = child.getMeasuredHeight();
            int childTop = paddingTop;

            if (child == mSlideChildView)
                childTop = computePanelToPosition(mSlideOffset);

            if (!isSlidingUp)
                if (child == mMainView && !mOverlayFlag)
                    childTop = computePanelToPosition(mSlideOffset) + mSlideChildView.getMeasuredHeight();

            final int childBottom = childTop + childHeight;
            final int childLeft = paddingLeft + params.leftMargin;
            final int childRight = childLeft + child.getMeasuredWidth();
            child.layout(childLeft,childTop,childRight,childBottom);
        }

        if (isFirstLayout)
            updateObscuredViewVisibility();
        applyParallaxForCurrentSlideOffset();
        isFirstLayout = false;
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
        mSlideChildView = getChildAt(1);

        if (mDragView == null)
            setDragView(mSlideChildView);

        //如果触摸面板层不可见，就设置隐藏状态
        if (mSlideChildView.getVisibility() != VISIBLE)
            mPanelState = PanelState.HIDDEN;

        int layoutWidht = widthSize - getPaddingLeft() - getPaddingRight();
        int layoutHeight = heightSize - getPaddingTop() - getPaddingBottom();

        //第一步：首先测量子view的宽高
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final PanelLayoutParams params = (PanelLayoutParams) child.getLayoutParams();

            if (child.getVisibility() == GONE && i == 0)
                continue;

            int width = layoutWidht;
            int height = layoutHeight;


            //如果是主view，要记录需要overlay的高度
            if (child == mMainView) {
                if (!mOverlayFlag && mPanelState != PanelState.HIDDEN)
                    height -= mPanelHeight;
                width -= params.leftMargin + params.rightMargin;
            } else if (child == mSlideChildView) {
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

            if (child == mSlideChildView)
                mSlideRange = mSlideChildView.getMeasuredHeight() - mPanelHeight;
        }

        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (h != oldh)
            isFirstLayout = true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled() || !isTouchEnable())
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
        //若展开，点击外部折叠起来
        if (mPanelState == PanelState.ANCHORED &&
                !isViewUnder(mSlideChildView, (int) ev.getX(), (int) ev.getY())) {
            smoothSlideTo(0);
            return true;
        }

        //1 向上滚动,true能滚动,false滚到底部
        //-1 向下滚动,true能滚动,false滚到顶部
        if (!isEnabled() || !isTouchEnable() ||
                (isUnableToDrag && action != MotionEvent.ACTION_DOWN) || mArrivedTopFlag) {
            if (!mDragView.canScrollVertically(-1)) {
                mDragView.setScrollY(1);
                mArrivedTopFlag = false;
            }
            mDragHelper.abort();
            return super.dispatchTouchEvent(ev);
        }

        final float x = ev.getX();
        final float y = ev.getY();

        if (action == MotionEvent.ACTION_DOWN) {
            isHandlingTouch = false;
            mPrevMotionX = x;
            mPrevMotionY = y;
        } else if (action == MotionEvent.ACTION_MOVE) {
            float dx = x - mPrevMotionX;
            float dy = y - mPrevMotionY;
            mPrevMotionX = x;
            mPrevMotionY = y;

            if (Math.abs(dx) > Math.abs(dy)) {
                // Scrolling horizontally, so ignore
                return super.dispatchTouchEvent(ev);
            }

            // If the scroll view isn't under the touch, pass the
            // event along to the dragView.
            if (!isViewUnder(mScrollView, (int) mInitialMotionX, (int) mInitialMotionY) ) {
                return super.dispatchTouchEvent(ev);
            }

            // Which direction (up or down) is the drag moving?
            if (dy * (isSlidingUp ? 1 : -1) > 0) { // Collapsing
                // Is the child less than fully scrolled?
                // Then let the child handle it.
                if (mScrollableViewHelper.getScrollableViewScrollPosition(mScrollView, isSlidingUp) > 0) {
                    isHandlingTouch = true;
                    return super.dispatchTouchEvent(ev);
                }

                // Was the child handling the touch previously?
                // Then we need to rejigger things so that the
                // drag panel gets a proper down event.
                if (isHandlingTouch) {
                    // Send an 'UP' event to the child.
                    MotionEvent up = MotionEvent.obtain(ev);
                    up.setAction(MotionEvent.ACTION_CANCEL);
                    super.dispatchTouchEvent(up);
                    up.recycle();

                    // Send a 'DOWN' event to the panel. (We'll cheat
                    // and hijack this one)
                    ev.setAction(MotionEvent.ACTION_DOWN);
                }

                isHandlingTouch = false;
                return this.onTouchEvent(ev);
            } else if (dy * (isSlidingUp ? 1 : -1) < 0) { // Expanding
                // Is the panel less than fully expanded?
                // Then we'll handle the drag here.
                if (mSlideOffset < 1.0f) {
                    isHandlingTouch = false;
                    return this.onTouchEvent(ev);
                }

                // Was the panel handling the touch previously?
                // Then we need to rejigger things so that the
                // child gets a proper down event.
                if (!isHandlingTouch && mDragHelper.isDragging()) {
                    mDragHelper.cancel();
                    ev.setAction(MotionEvent.ACTION_DOWN);
                }

                isHandlingTouch = true;
                return super.dispatchTouchEvent(ev);
            }
        } else if (action == MotionEvent.ACTION_UP) {
            // If the scrollable view was handling the touch and we receive an up
            // we want to clear any previous dragging state so we don't intercept a touch stream accidentally
            if (isHandlingTouch) {
                mDragHelper.setDragState(ViewDragHelper.STATE_IDLE);
            }
        }

        // In all other cases, just let the default behavior take over.
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        //如果scrollView处理事件，则不要拦截
        if (isHandlingTouch && !isTouchEnable()) {
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
                if (!isViewUnder(mDragView, (int) x, (int) y)) {
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
                //点击渐变部分，如果有监听就关了
                if (ady <= dragSlop
                        && adx <= dragSlop
                        && mSlideOffset > 0 && !isViewUnder(mSlideChildView, (int) mInitialMotionX, (int) mInitialMotionY) && mFadeOnClickListener != null) {
                    playSoundEffect(android.view.SoundEffectConstants.CLICK);
                    mFadeOnClickListener.onClick(this);
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

        if (mSlideChildView != null && mSlideChildView != child) { // if main view
            // Clip against the slider; no sense drawing what will immediately be covered,
            // Unless the panel is set to overlay content
            canvas.getClipBounds(mTmpRect);
            if (!mOverlayFlag) {
                if (isSlidingUp) {
                    mTmpRect.bottom = Math.min(mTmpRect.bottom, mSlideChildView.getTop());
                } else {
                    mTmpRect.top = Math.max(mTmpRect.top, mSlideChildView.getBottom());
                }
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

    @Override
    public void draw(Canvas c) {
        super.draw(c);

        // draw the shadow
        if (mShadowDrawable != null && mSlideChildView != null) {
            final int right = mSlideChildView.getRight();
            final int top;
            final int bottom;
            if (isSlidingUp) {
                top = mSlideChildView.getTop() - mShadowHeight;
                bottom = mSlideChildView.getTop();
            } else {
                top = mSlideChildView.getBottom();
                bottom = mSlideChildView.getBottom() + mShadowHeight;
            }
            final int left = mSlideChildView.getLeft();
            mShadowDrawable.setBounds(left, top, right, bottom);
            mShadowDrawable.draw(c);
        }
    }

    /**坐标是否落在target view*/
    private boolean isViewUnder(View view,int x,int y){
        if (view == null)
            return false;
        int[] viewLocation = new int[2];
        view.getLocationOnScreen(viewLocation);
        int[] parentLocation = new int[2];
        this.getLocationOnScreen(parentLocation);
        int screenX = parentLocation[0] + x;
        int screenY = parentLocation[1] + y;
        return screenX >= viewLocation[0] && screenX < viewLocation[0] + view.getWidth() &&
                screenY >= viewLocation[1] && screenY < viewLocation[1] + view.getHeight();
    }


    /**
     * 判断是否可触摸
     */
    private boolean isTouchEnable() {
        return isTouchEnable && mSlideChildView != null && mPanelState != PanelState.HIDDEN;
    }


    /**
     * 设置面板状态
     */
    private void setPanelState(PanelState state) {
        //放弃运行中的动画，允许改变面板状态
        if (mDragHelper.getViewDragState() == ViewDragHelper.STATE_SETTLING)
            mDragHelper.abort();

        if (state == null || state == PanelState.DRAGGING)
            throw new IllegalArgumentException("面板状态不能为null或者拖拽中");

        if (!isEnabled()
                || (!isFirstLayout && mSlideChildView == null)
                || state == mPanelState
                || mPanelState == PanelState.DRAGGING)
            return;

        if (isFirstLayout) {
            setPanelStateInternal(state);
        } else {
            if (mPanelState == PanelState.HIDDEN) {
                mSlideChildView.setVisibility(View.VISIBLE);
                requestLayout();
            }
            switch (state) {
                case ANCHORED:
                    break;

                case COLLAPSED:
                    break;

                case EXPANDED:
                    break;

                case HIDDEN:
                    break;
            }
        }
    }


    /**
     * 面板内部状态的同步
     */
    private void setPanelStateInternal(PanelState state) {
        if (mPanelState == state)
            return;
        PanelState oldState = mPanelState;
        mPanelState = state;
        dispatchOnPanelStateChanged(this, oldState, state);
    }


    /**
     * 分发出去面板同步的状态
     */
    private void dispatchOnPanelStateChanged(View panel, PanelState preState, PanelState newState) {
        synchronized (mPanelSlideListeners) {
            for (PanelSlideListener p : mPanelSlideListeners) {
                p.onPanelStateChanged(panel, preState, newState);
            }
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }


    /**
     * 设置默认布局方式
     */
    public void setGravity(int gravity) {
        if (gravity != Gravity.TOP && gravity != Gravity.BOTTOM)
            throw new IllegalArgumentException("只能设置top或者bottom");
        isSlidingUp = gravity == Gravity.BOTTOM;
        if (!isFirstLayout)
            requestLayout();
    }


    /**
     * 设置面板内的拖拽view
     */
    private void setDragView(View view) {
        if (view == null)
            return;
        if (mDragView != null)
            mDragView.setOnClickListener(null);

        mDragView = view;
        mDragView.setClickable(true);
        mDragView.setFocusable(false);
        mDragView.setFocusableInTouchMode(false);
        mDragView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isEnabled() || !isTouchEnable())
                    return;
                //不在展开和触摸状态时，根据锚点设置面板状态
                if (mPanelState != PanelState.EXPANDED && mPanelState != PanelState.ANCHORED)
                    if (mAnchorPoint < 1.0f)
                        setPanelState(PanelState.ANCHORED);
                    else
                        setPanelState(PanelState.EXPANDED);
                else
                    setPanelState(PanelState.COLLAPSED);

            }
        });
    }


    /**
     * 设置面板内滚动view
     */
    private void setScrollView(View view) {
        mScrollView = view;
    }


    /**
     * 平滑到目标位置
     */
    private boolean smoothSlideTo(float slideOffset) {
        if (!isEnabled() || mSlideChildView == null)
            return false;

        int panelTop = computePanelToPosition(slideOffset);

        if (mDragHelper.smoothSlideViewTo(mSlideChildView, mSlideChildView.getLeft(), panelTop)) {
            setAllChildrenVisible();
            ViewCompat.postInvalidateOnAnimation(this);
            return true;
        }
        return false;
    }


    /**
     * 计算滑动的偏移量
     */
    private int computePanelToPosition(float slideOffset) {
        int slideViewHeight = mSlideChildView != null ? mSlideChildView.getMeasuredHeight() : 0;
        int slidePixelOffset = (int) (slideOffset * mSlideRange);
        return isSlidingUp
                ? getMeasuredHeight() - getPaddingBottom() - mPanelHeight - slidePixelOffset
                : getPaddingTop() - slideViewHeight + mPanelHeight + slidePixelOffset;
    }


    /**
     * 设置所有view的可见状态
     */
    private void setAllChildrenVisible() {
        for (int i = 0, childCount = getChildCount(); i < childCount; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == View.INVISIBLE)
                child.setVisibility(View.VISIBLE);
        }
    }


    /**获取当前偏移*/
    private int getParallaxOffset(){
        int offset = (int)(mParallaxOffset * Math.max(mSlideOffset,0));
        return isSlidingUp ? -offset : offset;
    }


    /**更新视觉差位置*/
    private void applyParallaxForCurrentSlideOffset(){
        if (mParallaxOffset > 0) {
            int mainViewOffset = getParallaxOffset();
            ViewCompat.setTranslationY(mMainView,mainViewOffset);
        }
    }


    /**
     * 是否有不透明的背景
     */
    private boolean hasOpaqueBackground(View view) {
        final Drawable bg = view.getBackground();
        return bg != null && bg.getOpacity() == PixelFormat.OPAQUE;
    }


    /**
     * 唤醒view的可见状态*/
    void updateObscuredViewVisibility() {
        if (getChildCount() == 0) {
            return;
        }
        final int leftBound = getPaddingLeft();
        final int rightBound = getWidth() - getPaddingRight();
        final int topBound = getPaddingTop();
        final int bottomBound = getHeight() - getPaddingBottom();
        final int left;
        final int right;
        final int top;
        final int bottom;
        if (mSlideChildView != null && hasOpaqueBackground(mSlideChildView)) {
            left = mSlideChildView.getLeft();
            right = mSlideChildView.getRight();
            top = mSlideChildView.getTop();
            bottom = mSlideChildView.getBottom();
        } else {
            left = right = top = bottom = 0;
        }
        View child = getChildAt(0);
        final int clampedChildLeft = Math.max(leftBound, child.getLeft());
        final int clampedChildTop = Math.max(topBound, child.getTop());
        final int clampedChildRight = Math.min(rightBound, child.getRight());
        final int clampedChildBottom = Math.min(bottomBound, child.getBottom());
        final int vis;
        if (clampedChildLeft >= left && clampedChildTop >= top &&
                clampedChildRight <= right && clampedChildBottom <= bottom) {
            vis = INVISIBLE;
        } else {
            vis = VISIBLE;
        }
        child.setVisibility(vis);
    }


    /**
     * 把滑动监听分发出去
     * */
    void dispatchOnPanelSlide(View panel) {
        synchronized (mPanelSlideListeners) {
            for (PanelSlideListener l : mPanelSlideListeners) {
                l.onPanelSlide(panel, mSlideOffset);
            }
        }
    }


    /**
     * 面板拖拽
     * */
    private void onPanelDragged(int newTop) {
        setPanelStateInternal(PanelState.DRAGGING);
        // Recompute the slide offset based on the new top position
        mSlideOffset = computeSlideOffset(newTop);
        applyParallaxForCurrentSlideOffset();
        // Dispatch the slide event
        dispatchOnPanelSlide(mSlideChildView);
        // If the slide offset is negative, and overlay is not on, we need to increase the
        // height of the main content
        LayoutParams lp = mMainView.getLayoutParams();
        int defaultHeight = getHeight() - getPaddingBottom() - getPaddingTop() - mPanelHeight;

        if (mSlideOffset <= 0 && !mOverlayFlag) {
            // expand the main view
            lp.height = isSlidingUp ? (newTop - getPaddingBottom()) : (getHeight() - getPaddingBottom() - mSlideChildView.getMeasuredHeight() - newTop);
            if (lp.height == defaultHeight) {
                lp.height = LayoutParams.MATCH_PARENT;
            }
            mMainView.requestLayout();
        } else if (lp.height != LayoutParams.MATCH_PARENT && !mOverlayFlag) {
            lp.height = LayoutParams.MATCH_PARENT;
            mMainView.requestLayout();
        }
    }


    /**
     * 计算滑动偏移量*/
    private float computeSlideOffset(int topPosition) {
        // Compute the panel top position if the panel is collapsed (offset 0)
        final int topBoundCollapsed = computePanelTopPosition(0);

        // Determine the new slide offset based on the collapsed top position and the new required
        // top position
        return (isSlidingUp
                ? (float) (topBoundCollapsed - topPosition) / mSlideRange
                : (float) (topPosition - topBoundCollapsed) / mSlideRange);
    }


    /**
     * 计算画板到顶的偏移*/
    private int computePanelTopPosition(float slideOffset) {
        int slidingViewHeight = mSlideChildView != null ? mSlideChildView.getMeasuredHeight() : 0;
        int slidePixelOffset = (int) (slideOffset * mSlideRange);
        return isSlidingUp
                ? getMeasuredHeight() - getPaddingBottom() - mPanelHeight - slidePixelOffset
                : getPaddingTop() - slidingViewHeight + mPanelHeight + slidePixelOffset;
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

    /**
     * 点击渐变层的回调*/
    public void setOnFadeClickListener(OnClickListener listener) {
        this.mFadeOnClickListener = listener;
    }

    /**
     * 拖拽回调
     */
    private class DragHelperCallback extends ViewDragHelper.Callback {
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return !isUnableToDrag && child == mSlideChildView;

        }

        @Override
        public void onViewDragStateChanged(int state) {
            if (mDragHelper != null && mDragHelper.getViewDragState() == ViewDragHelper.STATE_IDLE) {
                mSlideOffset = computeSlideOffset(mSlideChildView.getTop());
                applyParallaxForCurrentSlideOffset();

                if (mSlideOffset == 1) {
                    updateObscuredViewVisibility();
                    setPanelStateInternal(PanelState.EXPANDED);
                } else if (mSlideOffset == 0) {
                    setPanelStateInternal(PanelState.COLLAPSED);
                } else if (mSlideOffset < 0) {
                    setPanelStateInternal(PanelState.HIDDEN);
                    mSlideChildView.setVisibility(View.INVISIBLE);
                } else {
                    updateObscuredViewVisibility();
                    setPanelStateInternal(PanelState.ANCHORED);
                }
            }
        }

        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {
            setAllChildrenVisible();
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            onPanelDragged(top);
            invalidate();
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            int target;
//            float direction = isSlidingUp ? -yvel : yvel;
//            if (direction > 0 && mSlideOffset <= mAnchorPoint) {//上滑，展开并停止到基准点
//                mArrivedTopFlag = true;
//                target = computePanelTopPosition(mAnchorPoint);
//            } else if (direction > 0 && mSlideOffset > mAnchorPoint) {//上滑超过基准点，完全展开
//                mArrivedTopFlag = true;
//                target = computePanelTopPosition(1.0f);
//            } else if (direction < 0 && mSlideOffset >= mAnchorPoint) {//下滑，收缩并停在基准点
//                mArrivedTopFlag = true;
//                target = computePanelTopPosition(mAnchorPoint);
//            } else if (direction < 0 && mSlideOffset < mAnchorPoint) {//下滑超过基准点，完全收缩
//                mArrivedTopFlag = false;
//                target = computePanelTopPosition(0.0f);
//            } else
            if (mSlideOffset >= (1.f + mAnchorPoint) / 2) {//超过基准点足够远，完全展开
                mArrivedTopFlag = true;
                target = computePanelTopPosition(1.0f);
            } else if (mSlideOffset >= mAnchorPoint / 2) {//超过基准点1/2，展开并停止到基准点
                mArrivedTopFlag = true;
                target = computePanelTopPosition(mAnchorPoint);
            } else {//否则都不满足，收缩
                mArrivedTopFlag = false;
                target = computePanelTopPosition(0.0f);
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
            final int collapsedTop = computePanelTopPosition(0.f);
            final int expandedTop = computePanelTopPosition(mAnchorPoint);
            if (isSlidingUp) {
                return Math.min(Math.max(top, expandedTop), collapsedTop);
            } else {
                return Math.min(Math.max(top, collapsedTop), expandedTop);
            }
        }
    }
}
