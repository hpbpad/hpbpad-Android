package org.wordpress.android.task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.support.v4.app.FragmentTransaction;

import com.justsystems.hpb.pad.R;

import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.CustomTypePost;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.Postable;
import org.wordpress.android.ui.list.PostListAdapter;
import org.wordpress.android.ui.list.ViewPostsFragment;
import org.wordpress.android.ui.list.ViewPostsFragment.OnRefreshListener;
import org.wordpress.android.util.WPAlertDialogFragment;

abstract class AbsLoadPostsTask extends MultiAsyncTask<List<?>, Void, Boolean> {

    private Context context;
    private ViewPostsFragment fragment;

    private OnRefreshListener mOnRefreshListener;

    private boolean loadMore;
    private String errorMsg = "";

    AbsLoadPostsTask(ViewPostsFragment fragment,
            OnRefreshListener mOnRefreshListener, boolean loadMore) {
        this.context = fragment.getActivity().getApplicationContext();
        this.fragment = fragment;
        this.mOnRefreshListener = mOnRefreshListener;
        this.loadMore = loadMore;
    }

    @Override
    protected Boolean doInBackground(List<?>... args) {
        List<?> arguments = args[0];
        final Blog currentBlog = (Blog) arguments.get(0);
        WordPress.currentBlog = currentBlog;
        boolean isPage = (Boolean) arguments.get(1);
        int recordCount = (Integer) arguments.get(2);

        final int id = WordPress.currentBlog.getId();
        final String url = currentBlog.getUrl();
        final String httpUser = currentBlog.getHttpuser();
        final String httpPassword = currentBlog.getHttppassword();

        final int blogId = currentBlog.getBlogId();
        final String userName = currentBlog.getUsername();
        final String password = currentBlog.getPassword();

        XMLRPCClient client = new XMLRPCClient(url, httpUser, httpPassword);

        Object[] params = { blogId, userName, password, recordCount };

        Object[] result = null;
        try {
            result = (Object[]) client.call(getMethod(), params);
        } catch (XMLRPCException e) {
            errorMsg = e.getMessage();
            if (errorMsg == null)
                errorMsg = this.context.getResources().getString(
                        R.string.error_generic);
        }
        if (result == null) {
            errorMsg = this.context.getResources().getString(
                    R.string.error_generic);
            return false;
        }
        if (result.length > 0) {
            Map<?, ?> contentHash = new HashMap<Object, Object>();
            List<Map<?, ?>> dbVector = new Vector<Map<?, ?>>();
            if (!loadMore) {
                WordPress.wpDB.deleteUploadedPosts(
                        WordPress.currentBlog.getId(), isPage);
            }
            for (int ctr = 0; ctr < result.length; ctr++) {
                Map<String, Object> dbValues = new HashMap<String, Object>();
                contentHash = (Map<?, ?>) result[ctr];
                dbValues.put("blogID", blogId);
                dbVector.add(ctr, contentHash);
            }

            WordPress.wpDB.savePosts(dbVector, id, isPage);
        } else {
            PostListAdapter adapter = fragment.getAdapter();
            if (adapter != null) {
                if (adapter.getCount() == 2) {
                    try {
                        // XXX
                        Postable postable = WordPress.getCurrentPost();
                        if (postable.getType() == Postable.TYP_PAGE
                                || postable.getType() == Postable.TYP_POST) {
                            WordPress.wpDB.deleteUploadedPosts(id,
                                    ((Post) postable).isPage());
                        } else {
                            WordPress.wpDB.deleteUploadedCustomTypePosts(id,
                                    ((CustomTypePost) postable).getPost_type());
                        }
                        // XXX
                        /*
                         * mOnPostActionListener.onPostAction(AbsListActivity.
                         * POST_CLEAR, WordPress.currentPost);
                         */
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    WordPress.setCurrentPost(null);
                }
            }
        }
        return true;
    }

    protected void onPostExecute(Boolean result) {
        if (isCancelled() || !result) {
            mOnRefreshListener.onRefresh(false);
            final Activity activity = this.fragment.getActivity();
            if (activity == null) {
                return;
            }
            final Resources res = this.context.getResources();
            if (errorMsg != "" && !activity.isFinishing()) {
                FragmentTransaction ft = this.fragment.getFragmentManager()
                        .beginTransaction();
                WPAlertDialogFragment alert = WPAlertDialogFragment
                        .newInstance(String.format(
                                res.getString(R.string.error_refresh),
                                getTitle(res)), errorMsg);
                try {
                    alert.show(ft, "alert");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                errorMsg = "";
            }
            return;
        }
        if (loadMore)
            this.fragment.switcher.showPrevious();
        mOnRefreshListener.onRefresh(false);
        if (this.fragment.isAdded())
            this.fragment.loadPosts(loadMore);
    }

    abstract String getTitle(Resources res);

    abstract String getMethod();
}
