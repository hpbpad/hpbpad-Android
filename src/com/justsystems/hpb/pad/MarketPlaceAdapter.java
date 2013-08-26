package com.justsystems.hpb.pad;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

public class MarketPlaceAdapter extends BaseAdapter {
    private static final int COUNT = 2;

    private Context context;

    private int[] ids = { R.drawable.mp_top1, R.drawable.mp_top2 };

    public MarketPlaceAdapter(Context context) {
        this.context = context;

    }

    @Override
    public int getCount() {
        return COUNT;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return ids[position];
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView image = (ImageView) convertView;
        if (image == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            image = (ImageView) inflater.inflate(R.layout.mp_item, null);
        }

        final int resId = (int) getItemId(position);

        final Drawable d = this.context.getResources().getDrawable(resId);
        image.setImageDrawable(d);

        return image;
    }
}
