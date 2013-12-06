package com.justsystems.hpb.pad;

import java.util.ArrayList;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v4.view.PagerAdapter;
import android.text.TextUtils.TruncateAt;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.WordPress;

/**
 * カスタム投稿タイプを表示するViewPagerのアダプタ。<br>
 * 本来、ViewPagerは一画面に一アイテムしか表示させないが、今回は一画面に三アイテム表示する。<br>
 * このため、ViewPagerにはView幅の1/3のマージンをマイナスで設定する（左右にはみ出させるため)<br>
 * TextViewにはpaddingをView幅の1/3設定する。（文字列が隣のViewとかぶらないように）
 * 
 */
public class PostTypePagerAdapter extends PagerAdapter {

    private Context context;

    private boolean isFirstItem = true;

    private final ArrayList<String> labels = new ArrayList<String>();
    private final ArrayList<String> postTypes = new ArrayList<String>();

    /** TextViewを保持するコンテナ。指定したインデックスのTextViewを取得するために利用する。 */
    private final SparseArray<TextView> views = new SparseArray<TextView>();

    private int padding;

    private final int textSizeLarge;
    private final int textSizeSmall;

    public PostTypePagerAdapter(Context context, int currentBlogId) {
        this.context = context;
        onBlogChanged(currentBlogId);

        Resources r = context.getResources();
        this.textSizeLarge = r
                .getDimensionPixelSize(R.dimen.startpage_type_text_large);
        this.textSizeSmall = r
                .getDimensionPixelSize(R.dimen.startpage_type_text_small);
    }

    public void onBlogChanged(int currentBlogId) {
        this.labels.clear();
        this.postTypes.clear();

        Resources res = this.context.getResources();
        this.labels.add(res.getString(R.string.post));
        this.postTypes.add("post");

        WordPress.wpDB.getPostTypes(currentBlogId, labels, postTypes);

        this.labels.add(res.getString(R.string.page));
        this.postTypes.add("page");
        notifyDataSetChanged();
    }

    /**
     * 指定されたインデックスの投稿タイプ文字列を取得します。
     * 
     * @param index
     * @return
     */
    public String getSelectedPostType(int index) {
        return this.postTypes.get(index);
    }

    /**
     * TextViewの文字色を選択、非選択状態によって更新します。
     * 
     * @param position
     *            文字色を変えるTexiView
     * @param isSelected
     *            選択状態。
     */
    public void setSelected(int position, boolean isSelected) {
        TextView view = this.views.get(position);
        if (view == null) {
            return;
        }

        if (isSelected) {
            view.setTextColor(Color.WHITE);
            view.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizeLarge);
        } else {
            view.setTextColor(0xFF949393);
            view.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizeSmall);
        }
    }

    /**
     * テキストViewのpaddingを更新します
     * 
     * @param padding
     */
    public void setTextViewPadding(int padding) {
        this.padding = padding;
        for (int i = 0; i < this.labels.size(); i++) {
            TextView view = this.views.get(i);
            if (view == null) {
                continue;
            }
            view.setPadding(this.padding, 0, this.padding, 0);
        }
    }

    @Override
    public int getCount() {
        return labels.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return this.labels.get(position);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        String title = this.labels.get(position);

        // View を生成
        TextView textView = new TextView(context.getApplicationContext());

        if (position != this.labels.size() - 1) {
            Drawable d = this.context.getResources().getDrawable(
                    R.drawable.startpage_type_back);
            textView.setBackgroundDrawable(d);
        }

        textView.setMaxLines(1);
        textView.setEllipsize(TruncateAt.END);
        textView.setGravity(Gravity.CENTER);
        textView.setPadding(this.padding, 0, this.padding, 0);
        textView.setText(title);

        // コンテナに追加
        container.addView(textView);
        views.put(position, textView);

        if (isFirstItem) {
            setSelected(position, true);
            isFirstItem = false;
        }

        return textView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
        views.remove(position);
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        for (int i = 0; i < views.size(); i++) {
            int key = views.keyAt(i);
            setText(key);
        }
    }

    private void setText(int position) {
        if (labels.size() == 0 || position >= labels.size()) {
            return;
        }
        String title = this.labels.get(position);
        TextView textView = this.views.get(position);
        textView.setText(title);

        if (position == this.labels.size() - 1
                && textView.getBackground() != null) {
            textView.setBackgroundDrawable(null);
        } else if (position != this.labels.size() - 1
                && textView.getBackground() == null) {
            Drawable d = this.context.getResources().getDrawable(
                    R.drawable.startpage_type_back);
            textView.setBackgroundDrawable(d);
        }
        textView.setPadding(this.padding, 0, this.padding, 0);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

}
