package com.justsystems.hpb.pad;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * テンプレートを表示するView。<br>
 * 画像とViewを同じ高さにしています。
 */
public final class TemplateView extends ImageView {

    public TemplateView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TemplateView(Context context, AttributeSet attrs, int style) {
        super(context, attrs, style);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Drawable d = getDrawable();
        if (d == null) {
            return;
        }
        final int viewWidth = getMeasuredWidth();
        final int imageWidth = d.getIntrinsicWidth();
        final int drawableDrawWidth = viewWidth - getPaddingLeft()
                - getPaddingRight();
        final float drawableRate = drawableDrawWidth / (float) imageWidth;
        if (drawableRate > 1) {
            return;
        }

        final int drawbleHeight = d.getIntrinsicHeight();
        final int newDrawbleHeight = (int) (drawbleHeight * drawableRate);
        final int newHeight = getPaddingTop() + newDrawbleHeight
                + getPaddingBottom();

        setMeasuredDimension(viewWidth, newHeight);
    }
}
