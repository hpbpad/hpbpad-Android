package com.justsystems.hpb.pad;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.justsystems.hpb.pad.util.Debug;

/**
 * 上から開くSlidingDrawerのようなView。<br>
 * 2.3以下ではViewのorientation属性に対応していないため、独自に実装した。
 * 
 */
public class SlidingView extends LinearLayout implements AnimationListener,
        OnTouchListener, OnGestureListener {

    public static interface OnStateChengeListener {

        public void onOpen();

        public void onClose();
    }

    /** アニメーションの最大時間 */
    private static final int MAX_ANIMATION_DURATION = 700;
    /** 通常のアニメーションの時間 */
    private static final int DEFAULT_ANIMATION_DURATION = 300;
    /** アニメーションの最短時間 */
    private static final int MIN_ANIMATION_DURATION = 200;

    private OnStateChengeListener stateListener;

    private ImageView handle;
    private GestureDetector detector;
    /** ハンドルの押下時アイコン */
    private Drawable presed;
    /** ハンドルの非押下時アイコン */
    private Drawable notPresed;

    private boolean isOpen;

    public SlidingView(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.presed = context.getResources().getDrawable(
                R.drawable.ic_top_arrow_up_pressed);
        this.notPresed = context.getResources().getDrawable(
                R.drawable.ic_top_arrow_up_not_pressed);

        this.detector = new GestureDetector(context, this);
        setOnTouchListener(this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.handle = (ImageView) findViewById(R.id.handle);
        this.handle.setOnTouchListener(this.touchListener);
    }

    public void toggle() {
        if (isOpen) {
            close();
        } else {
            open();
        }
    }

    public void openWitoutAnimation() {
        offsetTopAndBottom(-getTop());
        isOpen = true;
    }

    public void open() {
        open(DEFAULT_ANIMATION_DURATION, true);
    }

    public void open(int duration) {
        open(duration, false);
    }

    private void open(int duration, boolean calcOffset) {
        if (isOpen) {
            return;
        }
        Debug.logd("open duration" + duration + "calcOffset" + calcOffset);
        final int top = getTop();
        final int offset = top;
        Debug.logd("offset" + offset);
        offsetTopAndBottom(-top);
        TranslateAnimation anim = new TranslateAnimation(0, 0, offset, 0);
        anim.setAnimationListener(this);
        duration = normalizeDuration(duration, Math.abs(offset), calcOffset);
        Debug.logd("open duration" + duration);
        anim.setDuration(duration);
        startAnimation(anim);
    }

    public void closeWitoutAnimation() {
        offsetTopAndBottom(-getTop() - getHeight());
        isOpen = false;
    }

    public void close() {
        close(DEFAULT_ANIMATION_DURATION, true);
    }

    public void close(int duration) {
        close(duration, false);
    }

    public void close(int duration, boolean calcOffset) {
        if (!isOpen) {
            return;
        }
        Debug.logd("close duration" + duration + "calcOffset" + calcOffset);
        final int offset = getTop();
        Debug.logd("offset" + offset);
        offsetTopAndBottom(-offset);
        TranslateAnimation anim = new TranslateAnimation(0, 0, offset,
                -getHeight());
        anim.setAnimationListener(this);
        duration = normalizeDuration(duration, Math.abs(getHeight() - offset),
                calcOffset);
        Debug.logd("close duration" + duration);
        anim.setDuration(duration);
        startAnimation(anim);
    }

    private int normalizeDuration(int duration, int distance, boolean calcOffset) {
        if (calcOffset) {
            return (int) (Math.max(MIN_ANIMATION_DURATION,
                    Math.min(MAX_ANIMATION_DURATION, duration))
                    * distance / (float) getHeight());
        } else {
            final float v = distance / (float) duration;
            if (v < getHeight() / (float) MAX_ANIMATION_DURATION) {
                return (int) (MAX_ANIMATION_DURATION * distance / (float) getHeight());
            } else if (v > getHeight() / (float) MIN_ANIMATION_DURATION) {
                return (int) (MIN_ANIMATION_DURATION * distance / (float) getHeight());
            } else {
                return Math.max(MIN_ANIMATION_DURATION,
                        Math.min(MAX_ANIMATION_DURATION, duration));
            }
        }
    }

    public boolean isOpen() {
        return this.isOpen;
    }

    public void setOnStateChangeListener(OnStateChengeListener listener) {
        this.stateListener = listener;
    }

    @Override
    public void onAnimationStart(Animation animation) {
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        isOpen = !isOpen;
        if (!isOpen) {
            offsetTopAndBottom(-getTop() - getHeight());
            if (this.stateListener != null) {
                this.stateListener.onClose();
            }
        } else {
            offsetTopAndBottom(-getTop());
            if (this.stateListener != null) {
                this.stateListener.onOpen();
            }
        }
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return true;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        close();
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
            float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
            float velocityY) {
        final float difY = e2.getRawY() - e1.getRawY();
        final float difX = e2.getX() - e1.getX();
        if (difY < 0 && Math.abs(difY) > Math.abs(difX)) {
            final float distY = Math.abs(difY);
            final long time = e2.getEventTime() - e1.getEventTime();
            final float restY = getHeight() - distY;
            Debug.logd("restY" + restY + " time " + time + " distY" + distY);
            final int animationDuration = (int) (time * restY / distY);
            Debug.logd("animationDuration" + animationDuration);

            close(animationDuration);
            return true;
        }
        return false;
    }

    private OnTouchListener touchListener = new OnTouchListener() {
        private int downY;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            final int action = event.getAction();
            switch (action) {
            case MotionEvent.ACTION_DOWN:
                handle.setImageDrawable(presed);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handle.setImageDrawable(notPresed);
                break;
            default:
                break;
            }

            if (detector.onTouchEvent(event)) {
                return true;
            }

            final int y = (int) event.getY();
            final int offset = y - downY;
            switch (action) {
            case MotionEvent.ACTION_DOWN:
                this.downY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                if (getBottom() + offset > getHeight()) {
                    offsetTopAndBottom(-getTop());
                    invalidate(0, -getTop(), getWidth(), getHeight() - getTop());
                } else if (0 < getBottom() + offset) {
                    offsetTopAndBottom(offset);
                    invalidate(0, -getTop(), getWidth(), getHeight() - getTop());
                }
                break;
            case MotionEvent.ACTION_UP:
                if (getBottom() > getHeight() / 2) {
                    offsetTopAndBottom(-getTop());
                    invalidate();
                } else {
                    close();
                }
                this.downY = 0;
                break;
            default:
                break;
            }
            return true;
        }
    };

}
