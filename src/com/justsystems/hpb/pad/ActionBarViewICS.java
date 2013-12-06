package com.justsystems.hpb.pad;

import android.content.Context;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

/**
 * 
 * HoneyComb,ICS専用のActionBarクラス。
 * <p>
 * HoneyComb,ICSではAnimation後の {@link #setScaleType(ScaleType)}がうまく動作しない。<br>
 * 回避のため、 {@link android.view.animation.Animation#setFillAfter(boolean)}で対応している。
 * <br>
 * 更に、ICSではアニメーション後に
 * {@link #setBackgroundDrawable(android.graphics.drawable.Drawable)}
 * が効かない不具合がある。<br>
 * 回避策が見つからないため、今回はロゴを動かさない。
 * <p>
 * ロゴを動かす場合には {@link #MOVE_LOGO}をtrueにすれば良い。<br>
 * 
 */

public class ActionBarViewICS extends ActionBarView {

    private static final boolean MOVE_LOGO = false;

    public ActionBarViewICS(Context context) {
        super(context);
        if (!MOVE_LOGO) {
            setScaleType(ScaleType.FIT_CENTER);
        }
    }

    public void moveLogoCenter() {
        if (MOVE_LOGO) {
            setBackgroundDrawable(null);
            this.moveCenterAnimation = new TranslateAnimation(0,
                    -getMoveLength(), 0, 0);
            this.moveCenterAnimation.setDuration(ANIMATION_DURATION);
            this.moveCenterAnimation.setFillAfter(true);
            this.moveCenterAnimation.setAnimationListener(this);
            startAnimation(this.moveCenterAnimation);
        } else {
            setImageResource(R.drawable.ic_actionbar_logo);
        }
    }

    public void moveLogoRight() {

        if (MOVE_LOGO) {
            this.moveRightAnimation = new TranslateAnimation(0,
                    getMoveLength(), 0, 0);
            this.moveRightAnimation.setDuration(ANIMATION_DURATION);
            this.moveRightAnimation.setFillAfter(true);
            this.moveRightAnimation.setAnimationListener(this);
            startAnimation(this.moveRightAnimation);
        } else {
            setImageResource(R.drawable.ic_actionbar_logo_closed);
        }
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        if (animation == this.moveCenterAnimation) {
            setScaleType(ScaleType.FIT_CENTER);
        } else if (animation == this.moveRightAnimation) {
            setScaleType(ScaleType.FIT_END);
            setBackgroundDrawable(getResources().getDrawable(
                    R.drawable.ic_top_arrow_down_ics));
        }
    }

    @Override
    public void onConfigraionChanged() {
        setAnimation(null);
        requestLayout();
        invalidate();
    }
}
