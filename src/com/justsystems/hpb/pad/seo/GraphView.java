package com.justsystems.hpb.pad.seo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.view.View;

import com.justsystems.hpb.pad.R;

public class GraphView extends View {

    private int textWidth;
    private int rateWidth;

    private int count;

    private String[] titles;
    private float[] values;

    private TextPaint textPaint;
    private Paint paint;

    private Rect r;

    public GraphView(Context context) {
        super(context);
        init();
    }

    public GraphView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GraphView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        this.textPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
        this.textPaint.setColor(Color.BLACK);
        this.paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.paint.setColor(0x800074A2);
        this.r = new Rect();

        this.textWidth = getResources().getDimensionPixelSize(
                R.dimen.graph_text_width);
        this.rateWidth = getResources().getDimensionPixelSize(
                R.dimen.graph_rate_width);

    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setTitle(String[] title) {
        this.titles = title;
    }

    public void setValue(float[] value) {
        this.values = value;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.count == 0) {
            return;
        }

        Drawable background = getBackground();
        if (background != null) {
            background.getPadding(r);
        } else {
            r.setEmpty();
        }

        int height = (getHeight() - r.top - r.bottom) / this.count;
        textPaint.setTextSize(height * 0.5f);

        for (int i = 0; i < this.count; i++) {
            final float rate = values[i] / values[0];
            drawSingle(canvas, i, rate, r.left, r.top + i * height, getWidth()
                    - r.right, r.top + (i + 1) * height);
        }
    }

    private void drawSingle(Canvas canvas, int i, float rate, int left,
            int top, int right, int bottom) {
        final int height = bottom - top;

        this.textPaint.setTextAlign(Align.LEFT);
        final String title = getEllipsizedText((i + 1) + ". " + this.titles[i]);

        canvas.drawText(title, left,
                (top + bottom + this.textPaint.getTextSize()) / 2, textPaint);

        final int graphLeft = left + this.textWidth;
        canvas.drawRect(graphLeft, bottom - height * 0.85f, graphLeft
                + ((right - this.rateWidth) - graphLeft) * rate, top + height
                * 0.85f, paint);

        this.textPaint.setTextAlign(Align.RIGHT);
        canvas.drawText(values[i] + "%", right,
                (top + bottom + this.textPaint.getTextSize()) / 2, textPaint);
    }

    private String getEllipsizedText(String text) {
        return TextUtils.ellipsize(text, this.textPaint, this.textWidth,
                TruncateAt.END).toString();
    }
}
