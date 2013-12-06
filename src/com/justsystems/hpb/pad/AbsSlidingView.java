package com.justsystems.hpb.pad;

public interface AbsSlidingView {

    public boolean isOpened();

    public void animateOpen();

    public void animateClose();

    public void open();

    public void close();

    public int getTop();

    public int getHeight();

    public int getWidth();

    public void setVisibility(int visibility);

    public void setOnStateChangeListener(OnStateChengeListener listener);

    public static interface OnStateChengeListener {

        public void onOpen();

        public void onClose();
    }

}
