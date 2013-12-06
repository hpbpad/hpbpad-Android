package com.justsystems.hpb.pad;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

import org.wordpress.android.util.DeviceUtils;

public class ActionBarView extends ImageView implements AnimationListener {

    protected static final int ANIMATION_DURATION = 500;

    protected Animation moveCenterAnimation;
    protected Animation moveRightAnimation;

    public ActionBarView(Context context) {
        super(context);
        setScaleType(ScaleType.FIT_END);
    }

    /**
     * ロゴを右→中央に動かす。 ロゴが中央にある状態でこの関数を呼んだ場合の動作は保証しない。
     */
    public void moveLogoCenter() {
        setBackgroundDrawable(null);
        this.moveCenterAnimation = new TranslateAnimation(0, -getMoveLength(),
                0, 0);
        this.moveCenterAnimation.setDuration(ANIMATION_DURATION);
        this.moveCenterAnimation.setAnimationListener(this);
        startAnimation(this.moveCenterAnimation);
    }

    /**
     * ロゴを中央→右に動かす。 ロゴが右にある状態でこの関数を呼んだ場合の動作は保証しない。
     */
    public void moveLogoRight() {
        this.moveRightAnimation = new TranslateAnimation(0, getMoveLength(), 0,
                0);
        this.moveRightAnimation.setDuration(ANIMATION_DURATION);
        this.moveRightAnimation.setAnimationListener(this);
        startAnimation(this.moveRightAnimation);
    }

    /**
     * ロゴを動かす長さを取得する。
     * 
     * @return ロゴを動かす長さ
     */
    protected final int getMoveLength() {
        Drawable d = getDrawable();
        final int dHeight = d.getIntrinsicHeight();
        final int dWidth = d.getIntrinsicWidth();
        final int viewHeight = getHeight();
        final int viewWidth = getWidth() - getPaddingRight() - getPaddingLeft();

        final float rate = viewHeight / (float) dHeight;
        return (viewWidth - (int) (dWidth * rate)) / 2;

    }

    public void onConfigraionChanged() {
    }

    @Override
    public void onAnimationStart(Animation animation) {
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        if (animation == this.moveCenterAnimation) {
            setAnimation(null);
            setScaleType(ScaleType.FIT_CENTER);
        } else if (animation == this.moveRightAnimation) {
            setAnimation(null);
            setScaleType(ScaleType.FIT_END);
            if (DeviceUtils.isOverEqualThanHoneycomb()) {
                setBackgroundDrawable(getResources().getDrawable(
                        R.drawable.ic_top_arrow_down));
                invalidate();
            } else {
                //Android 2.xではアニメーション直後に背景を設定しても反映されないことがあるため
                //短時間の遅延を設けてから設定する。
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setBackgroundDrawable(getResources().getDrawable(
                                R.drawable.ic_top_arrow_down));
                    }
                }, 100);
            }
        }
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
    }
}
