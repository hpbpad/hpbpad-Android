package org.wordpress.android.task;

import android.content.res.Resources;

import com.justsystems.hpb.pad.R;

import org.wordpress.android.ui.posts.PostsListFragment;
import org.wordpress.android.ui.posts.PostsListFragment.OnRefreshListener;

public class LoadPagesTask extends AbsLoadPostsTask {

    public LoadPagesTask(PostsListFragment fragment,
            OnRefreshListener mOnRefreshListener, boolean loadMore) {
        super(fragment, mOnRefreshListener, loadMore);
    }

    @Override
    String getTitle(Resources res) {
        return res.getString(R.string.page);
    }

    @Override
    String getMethod() {
        return "wp.getPages";
    }
}
