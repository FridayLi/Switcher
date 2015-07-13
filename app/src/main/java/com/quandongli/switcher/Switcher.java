package com.quandongli.switcher;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

public class Switcher extends View {

    public static final int CLOSE = 0;

    public static final int OPEN = 1;

    private static final int DURATION = 200;

    private static final float THRESHOLD = 0.5f;

    private int mOpenBackgroundColor = Color.RED;

    private int mCloseBackgroundColor = Color.GRAY;

    private int mGap;

    private Paint mBackgroundPaint;

    private Paint mThumbPaint;

    private int mThumbLeft;

    private int mState = CLOSE;

    private Scroller mScroller;

    private float mTouchDownX;

    private float mThumbX;

    private boolean mHasMoved;

    private OnStateChangeListener mStateChangeListener = new OnStateChangeListener() {
        @Override
        public void onStateChanged(int state) {

        }
    };

    public interface OnStateChangeListener {
        void onStateChanged(int state);
    }


    public Switcher(Context context) {
        super(context);
        init(null, 0);
    }

    public Switcher(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public Switcher(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.Switcher, defStyle, 0);


        mOpenBackgroundColor = a.getColor(
                R.styleable.Switcher_openBackgroundColor,
                mOpenBackgroundColor);
        mCloseBackgroundColor = a.getColor(
                R.styleable.Switcher_closeBackgroundColor,
                mCloseBackgroundColor);
        mGap = a.getDimensionPixelSize(R.styleable.Switcher_thumbGap, 5);

        a.recycle();

        mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(mOpenBackgroundColor);
        mBackgroundPaint.setAlpha(0);
        mBackgroundPaint.setStyle(Paint.Style.FILL);

        mThumbPaint = new Paint();
        mThumbPaint.setColor(Color.WHITE);
        mThumbPaint.setStyle(Paint.Style.FILL);

        mThumbLeft = mGap;

        mScroller = new Scroller(getContext(), new DecelerateInterpolator());

        setBackgroundColor(mCloseBackgroundColor);

    }

    public void setStateChangeListener(OnStateChangeListener listener) {
        if (listener == null)
            return;
        mStateChangeListener = listener;
    }

    public void setState(int state) {
        if (state == mState)
            return;
        if (state == OPEN) {
            open();
        } else if (state == CLOSE) {
            close();
        }
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {

            mThumbLeft = mScroller.getCurrX();
            int alpha = (int)(255*(float)(mThumbLeft - mGap)/(float)getThumbWidth());
            if (alpha < 0) {
                alpha = 0;
            } else if (alpha > 255) {
                alpha = 255;
            }
            mBackgroundPaint.setAlpha(alpha);
            postInvalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawBackGround(canvas);
        drawThumb(canvas);
    }

    private void drawThumb(Canvas canvas) {
        int thumbWidth = getThumbWidth();
        int thumbHeight = getThumbHeight();
        canvas.drawRect(mThumbLeft, mGap, mThumbLeft + thumbWidth, thumbHeight + mGap, mThumbPaint);
//        Log.i("test","drawThumb left = " + mThumbLeft + ",right = " + (mThumbLeft + thumbWidth));
    }


    private void drawBackGround(Canvas canvas) {
        canvas.drawRect(0, 0, getWidth(), getHeight(), mBackgroundPaint);
    }

    private int getThumbWidth() {
        return getWidth()/2 - mGap;
    }

    private int getThumbHeight() {
        return getHeight() - 2*mGap;
    }

    private float getOpenThreshold() {
        return (getWidth()/2)*(1 + THRESHOLD);

    }

    private float getCloseThreshold() {
        return getWidth()*THRESHOLD/2;
    }

    private void open() {
        if (mState == OPEN)
            return;
        mState = OPEN;

        mScroller.startScroll(mThumbLeft, 0, getThumbWidth(), 0, DURATION);
        invalidate();
        mStateChangeListener.onStateChanged(mState);
    }

    private void close() {
        if (mState == CLOSE)
            return;
        mState = CLOSE;

        mScroller.startScroll(mThumbLeft, 0, 0 - getThumbWidth(), 0, DURATION);
        invalidate();
        mStateChangeListener.onStateChanged(mState);
    }

    private void moveThumb(float rate) {
        mThumbLeft = (int)(mThumbX + getThumbWidth()*rate);
        int alpha = (int)(255*rate);
        mBackgroundPaint.setAlpha(alpha);
        if (mThumbLeft < mGap) {
            mThumbLeft = mGap;
            return;
        } else if (mThumbLeft > getWidth()/2) {
            mThumbLeft = getWidth()/2;
            return;
        }
        invalidate();
    }

    private void autoScroll() {
        if (mState == OPEN) {
            if (mThumbLeft < getOpenThreshold()) {
                scrollToState(CLOSE);
            } else {
                scrollToState(OPEN);
            }
        } else {
            if (mThumbLeft < getCloseThreshold()) {
                scrollToState(CLOSE);
            } else {
                scrollToState(OPEN);
            }
        }
    }

    private void scrollToState(int state) {
        if (mThumbLeft <= mGap || mThumbLeft >= getThumbWidth())
            return;
        int duration = (mThumbLeft - mGap)*DURATION/getThumbWidth();
        int finalPosition;
        if (state == OPEN) {
            finalPosition = getWidth()/2;
        } else {
            finalPosition = mGap;
        }
        mScroller.startScroll(mThumbLeft, 0, finalPosition - mThumbLeft, 0, duration);
        invalidate();
        if (state != mState) {
            mState = state;
            mStateChangeListener.onStateChanged(mState);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mTouchDownX = event.getX();
            mThumbX = mThumbLeft;
            mHasMoved = false;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            if (mHasMoved) {
                autoScroll();
            } else {
                if (event.getX() > getWidth()/2) {
                    open();
                } else {
                    close();
                }
            }

        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            mHasMoved = true;
            float dx = event.getX() - mTouchDownX;
            float rate = dx / (float)getThumbWidth();
            moveThumb(rate);

        }
        return true;
    }
}
