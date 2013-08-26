package com.justsystems.hpb.pad;

import java.io.File;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.justsystems.hpb.pad.util.Debug;

import org.wordpress.android.util.DeviceUtils;

public class SiteThumbnailFragment extends Fragment {

    static final String CAPTURE_DIRECTORY = "capture";
    private LinearLayout holder = null;
    private ImageView image;
    private int blogId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        this.blogId = getArguments().getInt("id");
        this.image = (ImageView) inflater.inflate(R.layout.site_preview, null);

        if (setMainImage()) {
            return image;
        } else {
            this.holder = (LinearLayout) inflater.inflate(
                    R.layout.preview_loading, null);
            this.holder.addView(this.image);
            this.image.setVisibility(View.GONE);
            onSelected();
            return holder;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.holder = null;
        this.image = null;
    }

    public void onSelected() {
        if (holder != null) {
            if (DeviceUtils.isConnected(getActivity().getApplicationContext())) {
                holder.findViewById(R.id.progressBar1).setVisibility(
                        View.VISIBLE);
                TextView tv = (TextView) holder.findViewById(R.id.textView1);
                tv.setText(R.string.loading);
            } else {
                holder.findViewById(R.id.progressBar1).setVisibility(View.GONE);
                TextView tv = (TextView) holder.findViewById(R.id.textView1);
                tv.setText(R.string.connection_error);
            }
        }
    }

    private Drawable getPreviewPicture() {
        String fileName = getActivity().getFilesDir().getAbsolutePath()
                + File.separator + CAPTURE_DIRECTORY + File.separator + blogId
                + ".png";

        Bitmap bitmap = null;
        if (new File(fileName).exists()) {
            bitmap = tryLoadPicture(fileName);
        }
        if (bitmap == null) {
            return null;
        } else {
            return new BitmapDrawable(getActivity().getResources(), bitmap);
        }
    }

    private Bitmap tryLoadPicture(final String fileName) {

        BitmapFactory.Options options = new Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(fileName, options);

        for (int i = 0; i < 10; i++) {
            Bitmap bmp = null;
            try {
                switch (i) {
                case 0:
                    options = new Options();
                    bmp = BitmapFactory.decodeFile(fileName, options);
                    break;
                default:
                    options = new Options();
                    options.inPreferredConfig = Config.RGB_565;
                    options.inSampleSize = (int) Math.pow(2, i);
                    bmp = BitmapFactory.decodeFile(fileName, options);
                }
            } catch (OutOfMemoryError e) {
                Debug.toast(getActivity(), i + "OOM " + blogId);
                System.gc();
            }
            if (options.outWidth > 0) {
                return bmp;
            }
        }
        return null;
    }

    private static boolean isXLargeLandscape(Context context) {
        final Configuration conf = context.getResources().getConfiguration();
        final boolean xLarge = (conf.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == 4;
        final boolean isLandScape = conf.orientation == Configuration.ORIENTATION_LANDSCAPE;
        return xLarge && isLandScape;
    }

    static int getViewWidth(Context context, boolean withMargin) {
        // FIXME レイアウトに依存しすぎるコード
        Resources r = context.getResources();
        final int width;
        if (isXLargeLandscape(context)) {
            width = r.getDisplayMetrics().widthPixels
                    - r.getDimensionPixelSize(R.dimen.menu_drawer_size);
        } else {
            width = r.getDisplayMetrics().widthPixels;
        }
        final int margin = withMargin ? r
                .getDimensionPixelSize(R.dimen.startpage_page_icon_margin) : 0;
        final int padding = r
                .getDimensionPixelSize(R.dimen.startpage_slidingview_padding)
                + margin;
        return width - padding * 2;
    }

    public void optimizeImageSize(int viewWidth) {
        if (this.image == null || this.holder != null) {
            return;
        }

        if (viewWidth == -1) {
            viewWidth = getViewWidth(getActivity(), true);
        }

        Drawable d = this.image.getDrawable();

        Matrix matrix = this.image.getImageMatrix();
        if (matrix == null) {
            matrix = new Matrix();
            this.image.setImageMatrix(matrix);
        }

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            int dx = (viewWidth - d.getIntrinsicWidth()) / 2;
            matrix.setScale(1.0f, 1.0f);
            matrix.setTranslate(dx, 0);
        } else {
            final float rate = viewWidth / (float) d.getIntrinsicWidth();
            matrix.setScale(rate, rate);
        }
        this.image.invalidate();
    }

    private boolean setMainImage() {
        if (getActivity() == null) {
            return false;
        }
        final int viewWidth = getViewWidth(getActivity(), true);
        Drawable d = getPreviewPicture();
        if (d == null) {
            return false;
        } else {
            if (this.holder != null) {
                this.holder.removeAllViews();
                this.holder.addView(this.image);
                this.image.setVisibility(View.VISIBLE);
                this.holder = null;
            }
            this.image.setImageDrawable(d);
            if (d instanceof AnimationDrawable) {
                ((AnimationDrawable) d).start();
            }
            optimizeImageSize(viewWidth);
        }
        return true;
    }

    public void refreshImage() {
        setMainImage();
    }
}
