package org.wordpress.android.task;

import android.content.res.Resources;

import com.justsystems.hpb.pad.R;

import org.wordpress.android.ui.posts.PostsListFragment;
import org.wordpress.android.ui.posts.PostsListFragment.OnRefreshListener;

public final class LoadPostsTask extends AbsLoadPostsTask {

    public LoadPostsTask(PostsListFragment fragment,
            OnRefreshListener mOnRefreshListener, boolean loadMore) {
        super(fragment, mOnRefreshListener, loadMore);
    }

    @Override
    String getTitle(Resources res) {
        return res.getString(R.string.post);
    }

    @Override
    String getMethod() {
        return "metaWeblog.getRecentPosts";
    }
}
