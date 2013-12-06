package com.justsystems.hpb.pad.marketplace;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.justsystems.hpb.pad.R;
import com.justsystems.hpb.pad.util.Debug;

import org.wordpress.android.WordPress;
import org.wordpress.android.task.MultiAsyncTask;
import org.wordpress.android.util.DeviceUtils;

public class MarketPlaceAdapter extends BaseAdapter {

    private boolean usePreinPicture = true;

    private static final int[] PREIN_IDS = { R.drawable.mp_top1,
            R.drawable.mp_top2 };

    private Context context;

    /** テンプレートのリスト。keyはposition */
    private final SparseArray<Template> templates = new SparseArray<Template>();
    /** テンプレートのサムネイルのキャッシュ。keyはposition */
    private final SparseArray<Drawable> cache = new SparseArray<Drawable>();

    private ThumbnailGetTask task;

    public MarketPlaceAdapter(Context context) {
        this.context = context;
        setItem();
    }

    public void setItem() {
        this.templates.clear();
        Template[] templates = WordPress.wpDB.getTemplates();
        if (templates.length == 0) {
            usePreinPicture = true;
        } else {
            SparseArray<Template> noImageTemplates = null;
            for (int i = 0; i < templates.length; i++) {
                Template template = templates[i];
                this.templates.put(i, template);
                File image = new File(template.getThumbnailFullPath(context));
                Debug.logd(template.getThumbnailFullPath(context) + " "
                        + image.exists());
                if (!image.exists()) {
                    if (noImageTemplates == null) {
                        noImageTemplates = new SparseArray<Template>();
                    }
                    noImageTemplates.put(i, template);
                }
            }
            if (noImageTemplates != null) {
                if (this.task != null && !this.task.isCancelled()) {
                    this.task.cancel(true);
                }
                this.task = new ThumbnailGetTask(context, this,
                        noImageTemplates);
                this.task.executeOnMultiThread();
            }
            usePreinPicture = false;
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return usePreinPicture ? PREIN_IDS.length : templates.size();
    }

    @Override
    public Template getItem(int position) {
        return usePreinPicture ? null : templates.get(position);
    }

    @Override
    public long getItemId(int position) {
        return usePreinPicture ? PREIN_IDS[position] : templates.get(position)
                .getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView image = (ImageView) convertView;
        if (image == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            image = (ImageView) inflater.inflate(R.layout.mp_item, null);
        }

        if (usePreinPicture) {
            final int resId = (int) getItemId(position);

            final Drawable d = this.context.getResources().getDrawable(resId);
            image.setImageDrawable(d);
        } else {
            Template template = templates.get(position);
            String filePath = template.getThumbnailFullPath(context);
            if (!filePath.equals(image.getTag())) {
                //画像を更新する場合orViewが再利用される場合

                Drawable d = this.cache.get(position);
                if (d == null) {
                    //画像を更新する場合
                    if (new File(filePath).exists()) {
                        d = new BitmapDrawable(context.getResources(), filePath);
                        this.cache.put(position, d);
                    } else {
                        //画像がない場合orまだ取得できていない場合
                        //nullをsetImageDrawableするので古い画像は消える。
                    }
                }

                if (d != null) {
                    image.setTag(filePath);
                    image.setImageDrawable(d);
                } else {
                    image.setTag(null);
                    image.setImageDrawable(null);
                }
            }
        }

        return image;
    }

    private static final class ThumbnailGetTask extends
            MultiAsyncTask<Void, Void, Void> {
        private Context context;
        private MarketPlaceAdapter adapter;
        private SparseArray<Template> urls;

        private final int templateWidth;

        public ThumbnailGetTask(Context context, MarketPlaceAdapter adapter,
                SparseArray<Template> urls) {
            this.context = context;
            this.adapter = adapter;
            this.urls = urls;

            this.templateWidth = DeviceUtils.getSmallestWidthPixcel(context
                    .getResources());
        }

        @Override
        protected Void doInBackground(Void... params) {
            for (int i = 0; i < this.urls.size(); i++) {
                if (isCancelled()) {
                    return null;
                }
                int key = this.urls.keyAt(i);
                Template template = this.urls.get(key);
                //以前の画像は消す
                removeOldFile(key, template);
                adapter.cache.remove(key);

                getImage(template);

                publishProgress();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);

            adapter.notifyDataSetInvalidated();
        }

        private void removeOldFile(int key, Template template) {
            File dir = new File(Template.getThumbnailDir(context));
            if (!dir.exists()) {
                return;
            }
            File[] files = dir.listFiles();
            for (File file : files) {
                if (file.getName().startsWith(String.valueOf(key))
                        && !file.getAbsolutePath().equals(
                                template.getThumbnailFullPath(context))) {
                    file.delete();
                }
            }
        }

        private boolean getImage(Template template) {
            BufferedInputStream in;
            try {
                in = new BufferedInputStream((InputStream) (new URL(
                        template.getThumbnail()).getContent()));
                Bitmap rawBitmap;
                try {
                    Options opt = new Options();
                    opt.inPreferredConfig = Config.RGB_565;
                    rawBitmap = BitmapFactory.decodeStream(in, null, opt);
                } catch (OutOfMemoryError e) {
                    Debug.logd("OOM", "decodeStream");
                    return false;
                }
                in.close();
                if (rawBitmap == null) {
                    return false;
                }

                File dir = new File(Template.getThumbnailDir(context));
                if (!dir.exists()) {
                    dir.mkdir();
                }

                final int orginWidth = rawBitmap.getWidth();
                final int orginHeight = rawBitmap.getHeight();

                Bitmap bmp = null;
                if (orginWidth > templateWidth) {
                    final float scale = templateWidth / (float) orginWidth;
                    //通常は縦横比が1:1となるように
                    final int templateHeight = Math.min(
                            (int) (orginHeight * scale), templateWidth);

                    try {
                        bmp = Bitmap.createBitmap(templateWidth,
                                templateHeight, Bitmap.Config.RGB_565);
                    } catch (OutOfMemoryError e) {
                        //GCしてもう一回トライする
                        System.gc();
                    }
                    if (bmp == null) {
                        try {
                            bmp = Bitmap.createBitmap(templateWidth,
                                    templateHeight, Bitmap.Config.RGB_565);
                        } catch (OutOfMemoryError e) {
                            return false;
                        }
                    }

                    Canvas canvas = new Canvas(bmp);
                    canvas.save();
                    canvas.scale(scale, scale);
                    canvas.drawBitmap(rawBitmap, 0, 0, null);
                    canvas.restore();
                } else if (orginHeight > templateWidth) {
                    //横は画面幅以下だが縦長

                    try {
                        //縦横比が1:1となるように
                        bmp = Bitmap.createBitmap(orginWidth, orginWidth,
                                Bitmap.Config.RGB_565);
                    } catch (OutOfMemoryError e) {
                        //GCしてもう一回トライする
                        System.gc();
                    }
                    if (bmp == null) {
                        try {
                            bmp = Bitmap.createBitmap(orginWidth, orginWidth,
                                    Bitmap.Config.RGB_565);
                        } catch (OutOfMemoryError e) {
                            return false;
                        }
                    }

                    Canvas canvas = new Canvas(bmp);
                    canvas.save();
                    canvas.drawBitmap(rawBitmap, 0, 0, null);
                    canvas.restore();
                } else {
                    bmp = rawBitmap;
                }

                final FileOutputStream out = new FileOutputStream(
                        template.getThumbnailFullPath(context));
                bmp.compress(CompressFormat.PNG, 100, out);
                out.close();

                rawBitmap.recycle();
                bmp.recycle();
                return true;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
    }
}