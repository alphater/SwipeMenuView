package com.alphater.swipe;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;

public class SwipeMenuView extends ViewGroup {

    private int mScaleTouchSlop;
    private int mMaxVelocity;
    private int mPointerId;
    private int mHeight;
    private int mRightMenuWidths;
    private int mLimit;

    private View mContentView;
    static SwipeMenuView mViewCache;

    private PointF mLastP = new PointF();
    private PointF mFirstP = new PointF();

    private VelocityTracker mVelocityTracker;

    private boolean isUnMoved = true;
    private boolean isUserSwiped;
    private boolean isTouching;
    private boolean isSwipeEnable;
    private boolean isAntiRentPlug;
    private boolean isInterceptFlag;
    private boolean isLeftSwipe;
    private boolean isOpen;

    private ValueAnimator mOpenAnim, mCloseAnim;

    public SwipeMenuView(Context context) {
        this(context, null);
    }

    public SwipeMenuView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwipeMenuView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mScaleTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mMaxVelocity = ViewConfiguration.get(context).getScaledMaximumFlingVelocity();

        isSwipeEnable = true;
        isAntiRentPlug = true;
        isLeftSwipe = true;
        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SwipeMenuView, defStyleAttr, 0);
        int count = ta.getIndexCount();
        for (int i = 0; i < count; i++) {
            int attr = ta.getIndex(i);
            if (attr == R.styleable.SwipeMenuView_swipe_enable) {
                isSwipeEnable = ta.getBoolean(attr, true);
            } else if (attr == R.styleable.SwipeMenuView_anti_rent_plug) {
                isAntiRentPlug = ta.getBoolean(attr, true);
            } else if (attr == R.styleable.SwipeMenuView_left_swipe) {
                isLeftSwipe = ta.getBoolean(attr, true);
            }
        }
        ta.recycle();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childCount = getChildCount();
        int left = getPaddingLeft();
        int right = getPaddingLeft();
        for (int i = 0; i < childCount; i++) {
            View childView = getChildAt(i);
            if (childView.getVisibility() != GONE) {
                if (i == 0) {
                    childView.layout(left, getPaddingTop(), left + childView.getMeasuredWidth(), getPaddingTop() + childView.getMeasuredHeight());
                    left = left + childView.getMeasuredWidth();
                } else {
                    if (isLeftSwipe) {
                        childView.layout(left, getPaddingTop(), left + childView.getMeasuredWidth(), getPaddingTop() + childView.getMeasuredHeight());
                        left = left + childView.getMeasuredWidth();
                    } else {
                        childView.layout(right - childView.getMeasuredWidth(), getPaddingTop(), right, getPaddingTop() + childView.getMeasuredHeight());
                        right = right - childView.getMeasuredWidth();
                    }
                }
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setClickable(true);

        mRightMenuWidths = 0;
        int contentWidth = 0;
        int childCount = getChildCount();

        final boolean measureMatchParentChildren = MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY;
        boolean isNeedMeasureChildHeight = false;

        for (int i = 0; i < childCount; i++) {
            View childView = getChildAt(i);
            childView.setClickable(true);
            if (childView.getVisibility() != GONE) {
                measureChild(childView, widthMeasureSpec, heightMeasureSpec);
                final MarginLayoutParams lp = (MarginLayoutParams) childView.getLayoutParams();
                mHeight = Math.max(mHeight, childView.getMeasuredHeight());
                if (measureMatchParentChildren && lp.height == LayoutParams.MATCH_PARENT) {
                    isNeedMeasureChildHeight = true;
                }
                if (i > 0) {
                    mRightMenuWidths += childView.getMeasuredWidth();
                } else {
                    mContentView = childView;
                    contentWidth = childView.getMeasuredWidth();
                }
            }
        }
        setMeasuredDimension(getPaddingLeft() + getPaddingRight() + contentWidth, mHeight + getPaddingTop() + getPaddingBottom());
        mLimit = mRightMenuWidths * 4 / 10;
        if (isNeedMeasureChildHeight) {
            forceUniformHeight(childCount, widthMeasureSpec);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (isSwipeEnable) {
            acquireVelocityTracker(ev);
            final VelocityTracker verTracker = mVelocityTracker;
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isUserSwiped = false;
                    isUnMoved = true;
                    isInterceptFlag = false;
                    if (isTouching) {
                        return false;
                    } else {
                        isTouching = true;
                    }
                    mLastP.set(ev.getRawX(), ev.getRawY());
                    mFirstP.set(ev.getRawX(), ev.getRawY());
                    if (mViewCache != null) {
                        if (mViewCache != this) {
                            mViewCache.smoothClose();
                            isInterceptFlag = isAntiRentPlug;
                        }
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    mPointerId = ev.getPointerId(0);
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (isInterceptFlag) {
                        break;
                    }
                    float gap = mLastP.x - ev.getRawX();
                    if (Math.abs(gap) > 10 || Math.abs(getScrollX()) > 10) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    if (Math.abs(gap) > mScaleTouchSlop) {
                        isUnMoved = false;
                    }
                    scrollBy((int) (gap), 0);
                    if (isLeftSwipe) {
                        if (getScrollX() < 0) {
                            scrollTo(0, 0);
                        }
                        if (getScrollX() > mRightMenuWidths) {
                            scrollTo(mRightMenuWidths, 0);
                        }
                    } else {
                        if (getScrollX() < -mRightMenuWidths) {
                            scrollTo(-mRightMenuWidths, 0);
                        }
                        if (getScrollX() > 0) {
                            scrollTo(0, 0);
                        }
                    }
                    mLastP.set(ev.getRawX(), ev.getRawY());
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (Math.abs(ev.getRawX() - mFirstP.x) > mScaleTouchSlop) {
                        isUserSwiped = true;
                    }
                    if (!isInterceptFlag) {
                        verTracker.computeCurrentVelocity(1000, mMaxVelocity);
                        final float velocityX = verTracker.getXVelocity(mPointerId);
                        if (Math.abs(velocityX) > 1000) {
                            if (velocityX < -1000) {
                                if (isLeftSwipe) {
                                    smoothOpen();
                                } else {
                                    smoothClose();
                                }
                            } else {
                                if (isLeftSwipe) {
                                    smoothClose();
                                } else {
                                    smoothOpen();
                                }
                            }
                        } else {
                            if (Math.abs(getScrollX()) > mLimit) {
                                smoothOpen();
                            } else {
                                smoothClose();
                            }
                        }
                    }
                    recycleVelocityTracker();
                    isTouching = false;
                    break;
                default:
                    break;
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(ev.getRawX() - mFirstP.x) > mScaleTouchSlop) return true;
                break;
            case MotionEvent.ACTION_UP:
                if (isLeftSwipe) {
                    if (getScrollX() > mScaleTouchSlop) {
                        if (ev.getX() < getWidth() - getScrollX()) {
                            if (isUnMoved) {
                                smoothClose();
                            }
                            return true;
                        }
                    }
                } else {
                    if (-getScrollX() > mScaleTouchSlop) {
                        if (ev.getX() > -getScrollX()) {
                            if (isUnMoved) {
                                smoothClose();
                            }
                            return true;
                        }
                    }
                }
                if (isUserSwiped) {
                    return true;
                }
                break;
        }
        return isInterceptFlag || super.onInterceptTouchEvent(ev);
    }

    public void smoothOpen() {
        mViewCache = SwipeMenuView.this;
        if (null != mContentView) {
            mContentView.setLongClickable(false);
        }

        if (mCloseAnim != null && mCloseAnim.isRunning()) {
            mCloseAnim.cancel();
        }
        mOpenAnim = ValueAnimator.ofInt(getScrollX(), isLeftSwipe ? mRightMenuWidths : -mRightMenuWidths);
        mOpenAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                scrollTo((Integer) animation.getAnimatedValue(), 0);
            }
        });
        mOpenAnim.setInterpolator(new OvershootInterpolator());
        mOpenAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isOpen = true;
            }
        });
        mOpenAnim.setDuration(300).start();
    }

    /**
     * Smooth off
     */
    public void smoothClose() {
        mViewCache = null;
        if (null != mContentView) {
            mContentView.setLongClickable(true);
        }

        if (mOpenAnim != null && mOpenAnim.isRunning()) {
            mOpenAnim.cancel();
        }
        mCloseAnim = ValueAnimator.ofInt(getScrollX(), 0);
        mCloseAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                scrollTo((Integer) animation.getAnimatedValue(), 0);
            }
        });
        mCloseAnim.setInterpolator(new AccelerateInterpolator());
        mCloseAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isOpen = false;
            }
        });
        mCloseAnim.setDuration(300).start();
    }

    private void acquireVelocityTracker(final MotionEvent event) {
        if (null == mVelocityTracker) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
    }

    private void recycleVelocityTracker() {
        if (null != mVelocityTracker) {
            mVelocityTracker.clear();
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (this == mViewCache) {
            mViewCache.smoothClose();
            mViewCache = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    public boolean performLongClick() {
        return Math.abs(getScrollX()) <= mScaleTouchSlop && super.performLongClick();
    }

    /**
     * Quick close the swipe menu
     * <p>
     * ListView must be called
     * RecyclerView, if you call notifyItemRemoved () you don't have to call
     */
    public void quickClose() {
        if (this == mViewCache) {
            if (null != mOpenAnim && mOpenAnim.isRunning()) {
                mOpenAnim.cancel();
            }
            mViewCache.scrollTo(0, 0);
            mViewCache = null;
        }
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    private void forceUniformHeight(int count, int widthMeasureSpec) {
        // Pretend that the linear layout has an exact size. This is the measured height of
        // ourselves. The measured height should be the max height of the children, changed
        // to accommodate the heightMeasureSpec from the parent
        int uniformMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY);
        for (int i = 0; i < count; ++i) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
                if (lp.height == LayoutParams.MATCH_PARENT) {
                    // Temporarily force children to reuse their old measured width
                    // FIXME: this may not be right for something like wrapping text?
                    int oldWidth = lp.width;
                    lp.width = child.getMeasuredWidth();
                    // Remeasure with new dimensions
                    measureChildWithMargins(child, widthMeasureSpec, 0, uniformMeasureSpec, 0);
                    lp.width = oldWidth;
                }
            }
        }
    }

    public boolean isSwipeEnable() {
        return isSwipeEnable;
    }

    public void setSwipeEnable(boolean swipeEnable) {
        isSwipeEnable = swipeEnable;
    }

    public boolean isAntiRentPlug() {
        return isAntiRentPlug;
    }

    /**
     * Whether to open the blocking effects
     */
    public SwipeMenuView setAntiRentPlug(boolean antiRentPlug) {
        isAntiRentPlug = antiRentPlug;
        return this;
    }

    public boolean isLeftSwipe() {
        return isLeftSwipe;
    }

    /**
     * Whether to open the left slip, otherwise the right slip
     */
    public SwipeMenuView setLeftSwipe(boolean leftSwipe) {
        isLeftSwipe = leftSwipe;
        return this;
    }

    public static SwipeMenuView getViewCache() {
        return mViewCache;
    }

}
