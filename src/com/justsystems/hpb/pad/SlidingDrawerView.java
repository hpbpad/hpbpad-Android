package com.justsystems.hpb.pad;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.SlidingDrawer;
import android.widget.SlidingDrawer.OnDrawerCloseListener;
import android.widget.SlidingDrawer.OnDrawerOpenListener;

public class SlidingDrawerView extends SlidingDrawer implements AbsSlidingView,
        OnTouchListener, OnDrawerOpenListener, OnDrawerCloseListener {

    public SlidingDrawerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        View content = getContent();
        content.setOnTouchListener(this);
    }

    private OnStateChengeListener stateListener;

    public void setOnStateChangeListener(OnStateChengeListener listener) {
        this.stateListener = listener;
        setOnDrawerOpenListener(this);
        setOnDrawerCloseListener(this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return true;
    }

    @Override
    public void onDrawerClosed() {
        this.stateListener.onClose();
    }

    @Override
    public void onDrawerOpened() {
        this.stateListener.onOpen();
    }

}
