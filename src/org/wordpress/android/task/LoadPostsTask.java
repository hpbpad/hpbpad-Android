package org.wordpress.android.task;

import android.content.res.Resources;

import com.justsystems.hpb.pad.R;

import org.wordpress.android.ui.list.ViewPostsFragment;
import org.wordpress.android.ui.list.ViewPostsFragment.OnRefreshListener;

public final class LoadPostsTask extends AbsLoadPostsTask {

    public LoadPostsTask(ViewPostsFragment fragment,
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
