package org.wordpress.android.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.PostType;

public final class MenuDrawerItem {

    private final PostType postType;
    private final int activityTag;

    public MenuDrawerItem(Context context, String label, String postType) {
        final int id = WordPress.getCurrentBlog().getId();
        this.postType = new PostType(context, id, label, postType);
        this.activityTag = WPActionBarActivity.CUSTOM_TYPE_ACTIVITY;
    }

    MenuDrawerItem(String label, Drawable d, int activityTag) {
        final int id = WordPress.getCurrentBlog().getId();
        this.postType = new PostType(id, label, d);
        this.activityTag = activityTag;
    }

    public PostType getPostType() {
        return postType;
    }

    public int getActivityTag() {
        return this.activityTag;
    }
}
