package org.wordpress.android.ui;

import java.util.ArrayList;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import com.justsystems.hpb.pad.R;

import org.wordpress.android.WordPress;
import org.wordpress.android.task.RefreshMenuTask;

class MenuDrawerAdapter extends BaseAdapter implements OnClickListener {

    private static final int TYPE_NORAML = 0;
    private static final int TYPE_COMMENT = 1;
    private static final int TYPE_ADDABLE = 2;

    private WPActionBarActivity activity;
    private ArrayList<MenuDrawerItem> mItems;

    MenuDrawerAdapter(WPActionBarActivity activity,
            ArrayList<MenuDrawerItem> items) {
        this.activity = activity;
        this.mItems = items;
    }

    public void setItems(ArrayList<MenuDrawerItem> items) {
        this.mItems = items;
        notifyDataSetChanged();
    }

    public void clear() {
        mItems.clear();
    }

    public ArrayList<MenuDrawerItem> getItems() {
        return this.mItems;
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public MenuDrawerItem getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        final int type = getItem(position).getItemId();
        switch (type) {
        case WPActionBarActivity.PAGES_ACTIVITY:
        case WPActionBarActivity.POSTS_ACTIVITY:
        case WPActionBarActivity.CUSTOM_TYPE_ACTIVITY:
            return TYPE_ADDABLE;
        case WPActionBarActivity.COMMENTS_ACTIVITY:
            return TYPE_COMMENT;
        default:
            return TYPE_NORAML;
        }
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        final ViewHolder holder;
        final MenuDrawerItem item = getItem(position);
        final int viewType = getItemViewType(position);
        final int activityTag = item.getItemId();

        if (v == null) {
            switch (viewType) {
            case TYPE_ADDABLE:
                v = activity.getLayoutInflater().inflate(
                        R.layout.menu_drawer_custom_row, parent, false);
                ImageView newPost = (ImageView) v.findViewById(R.id.new_post);
                newPost.setOnClickListener(this);
                break;
            case TYPE_COMMENT:
                v = activity.getLayoutInflater().inflate(
                        R.layout.menu_drawer_comment_row, parent, false);
                break;
            default:
                v = activity.getLayoutInflater().inflate(
                        R.layout.menu_drawer_row, parent, false);
                break;
            }
            holder = new ViewHolder();
            holder.icon = (ImageView) v.findViewById(R.id.menu_row_icon);
            holder.title = (TextView) v.findViewById(R.id.menu_row_title);
            holder.bagde = (TextView) v.findViewById(R.id.menu_row_badge);
            holder.newPost = (ImageView) v.findViewById(R.id.new_post);

            v.setTag(holder);
        } else {
            holder = (ViewHolder) v.getTag();
        }

        holder.title.setText(item.getTitle());

        ImageView iconImageView = holder.icon;
        Drawable d = item.getIcon();
        if (d != null) {
            iconImageView.setScaleType(ScaleType.CENTER_CROP);
            iconImageView.setImageDrawable(d);
        } else {
            iconImageView.setImageDrawable(activity.getResources().getDrawable(
                    R.drawable.ic_md_post));
        }
        // TODO fix waste of resource
        if (RefreshMenuTask.isUnedrTask()
                && activityTag == WPActionBarActivity.REFRESH_MENU) {
            if (iconImageView.getAnimation() == null) {
                RotateAnimation anim = new RotateAnimation(0.0f, 360.0f,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f);
                anim.setInterpolator(new LinearInterpolator());
                anim.setRepeatCount(Animation.INFINITE);
                anim.setDuration(1400);
                iconImageView.startAnimation(anim);
            }
        } else if ((!RefreshMenuTask.isUnedrTask() || activityTag != WPActionBarActivity.REFRESH_MENU)
                && iconImageView.getAnimation() != null) {
            iconImageView.getAnimation().cancel();
            iconImageView.setAnimation(null);
        }

        if (item.isSelected()) {
            // http://stackoverflow.com/questions/5890379/setbackgroundresource-discards-my-xml-layout-attributes
            int bottom = v.getPaddingBottom();
            int top = v.getPaddingTop();
            int right = v.getPaddingRight();
            int left = v.getPaddingLeft();
            v.setBackgroundColor(v.getResources().getColor(
                    R.color.md_list_selected));
            v.setPadding(left, top, right, bottom);
        } else {
            int bottom = v.getPaddingBottom();
            int top = v.getPaddingTop();
            int right = v.getPaddingRight();
            int left = v.getPaddingLeft();
            v.setBackgroundResource(R.drawable.md_list_selector);
            v.setPadding(left, top, right, bottom);
        }

        TextView bagdeTextView = holder.bagde;
        if (activityTag == WPActionBarActivity.COMMENTS_ACTIVITY
                && WordPress.currentBlog != null) {
            int commentCount = WordPress.currentBlog
                    .getUnmoderatedCommentCount();
            if (commentCount > 0) {
                bagdeTextView.setVisibility(View.VISIBLE);
            } else {
                bagdeTextView.setVisibility(View.GONE);
            }
            bagdeTextView.setText(String.valueOf(commentCount));
        }

        ImageView newPost = holder.newPost;
        if (newPost != null) {
            newPost.setTag(position);
        }

        return v;
    }

    @Override
    public void onClick(View v) {
        final int index = (Integer) v.getTag();
        final MenuDrawerItem item = mItems.get(index);
        final int tag = item.getItemId();
        final String typeName = item.getPostType();
        activity.startNewPost(tag, typeName);
    }

    private static class ViewHolder {
        private ImageView icon;
        private TextView title;
        private TextView bagde;
        private ImageView newPost;
    }
}
