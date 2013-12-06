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

    private int numWidth;
    private int rateWidth;

    KeywordResult[] values;

    private TextPaint textPaint;
    private Paint paint;

    private Rect r;

    int graphSingleHeight;

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

        this.numWidth = getResources().getDimensionPixelSize(
                R.dimen.graph_num_width);
        this.rateWidth = getResources().getDimensionPixelSize(
                R.dimen.graph_rate_width);

        this.graphSingleHeight = getResources().getDimensionPixelSize(
                R.dimen.graph_single_height);

    }

    public void setValues(KeywordResult[] kws) {
        this.values = kws;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        Drawable background = getBackground();
        if (background != null) {
            background.getPadding(r);
        } else {
            r.setEmpty();
        }
        int padding = r.top + r.bottom;
        final int count = values == null ? 0 : values.length;
        int height = padding + graphSingleHeight * count;
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.values == null || this.values.length == 0) {
            return;
        }

        Drawable background = getBackground();
        if (background != null) {
            background.getPadding(r);
        } else {
            r.setEmpty();
        }

        final int count = this.values.length;
        int height = (getHeight() - r.top - r.bottom) / count;
        textPaint.setTextSize(height * 0.5f);

        final float maxValue = this.values[0].getPer();
        for (int i = 0; i < count; i++) {
            final float rate = this.values[i].getPer() / maxValue;
            drawSingle(canvas, i, rate, r.left, r.top + i * height, getWidth()
                    - r.right, r.top + (i + 1) * height);
        }
    }

    private void drawSingle(Canvas canvas, int i, float rate, int left,
            int top, int right, int bottom) {
        final KeywordResult value = this.values[i];
        final int height = bottom - top;
        final int centerHorizontal = (right - left) / 2;

        this.textPaint.setTextAlign(Align.LEFT);

        final String title = getEllipsizedText(
                (i + 1) + ". " + value.getKeyword(), centerHorizontal
                        - this.numWidth);
        canvas.drawText(title, left,
                (top + bottom + this.textPaint.getTextSize()) / 2, textPaint);

        this.textPaint.setTextAlign(Align.RIGHT);
        canvas.drawText(String.valueOf(value.getNum()), centerHorizontal, (top
                + bottom + this.textPaint.getTextSize()) / 2, textPaint);

        final int graphLeft = left + centerHorizontal;
        canvas.drawRect(graphLeft, bottom - height * 0.85f, graphLeft
                + ((right - this.rateWidth) - graphLeft) * rate, top + height
                * 0.85f, paint);

        this.textPaint.setTextAlign(Align.RIGHT);
        canvas.drawText(value.getPer() + "%", right,
                (top + bottom + this.textPaint.getTextSize()) / 2, textPaint);
    }

    private String getEllipsizedText(String text, int width) {
        return TextUtils.ellipsize(text, this.textPaint, width, TruncateAt.END)
                .toString();
    }
}
