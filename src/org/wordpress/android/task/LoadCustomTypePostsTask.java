package org.wordpress.android.task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.res.Resources;
import android.util.Log;

import com.justsystems.hpb.pad.R;

import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.list.ViewPostsFragment;
import org.wordpress.android.ui.list.ViewPostsFragment.OnRefreshListener;

public final class LoadCustomTypePostsTask extends AbsLoadPostsTask {

    private ViewPostsFragment fragment;

    private int offset;
    private String typeName;
    private boolean loadMore;

    public String errorMsg = "";

    public LoadCustomTypePostsTask(ViewPostsFragment fragment,
            OnRefreshListener mOnRefreshListener, boolean loadMore, int offset,
            String typeName) {
        super(fragment, mOnRefreshListener, loadMore);
        this.fragment = fragment;
        this.offset = offset;
        this.typeName = typeName;
        this.loadMore = loadMore;
    }

    @Override
    protected Boolean doInBackground(List<?>... params) {

        final Map<String, String> fields = new HashMap<String, String>();
        fields.put("post", "post");
        fields.put("taxonomies", "taxonomies");
        fields.put("custom_fields", "custom_fields");

        final Map<String, Object> filter = new HashMap<String, Object>();
        filter.put("post_type", typeName);
        filter.put("number", 20);
        filter.put("offset", offset);

        final Blog currentBlog = WordPress.currentBlog;
        final int id = WordPress.currentBlog.getId();
        final String url = currentBlog.getUrl();
        final String httpUser = currentBlog.getHttpuser();
        final String httpPassword = currentBlog.getHttppassword();
        Log.v("blog", "url:" + url + " httpUser:" + httpUser + " httpPassword:"
                + httpPassword);

        final int blogId = currentBlog.getBlogId();
        final String userName = currentBlog.getUsername();
        final String password = currentBlog.getPassword();
        Log.v("blog", "userName:" + userName + " password:" + password);

        XMLRPCClient client = new XMLRPCClient(url, httpUser, httpPassword);

        Object[] vParams = { blogId, userName, password, filter, fields };

        final Object versionResult;
        try {
            versionResult = client.call("wp.getPosts", vParams);
        } catch (XMLRPCException e) {
            e.printStackTrace();
            errorMsg = e.getMessage();
            if (errorMsg == null) {
                errorMsg = this.fragment.getString(R.string.error_generic);
            }
            return false;
        }

        if (versionResult == null || !(versionResult instanceof Object[])) {
            return false;
        }

        Object[] posts = (Object[]) versionResult;
        if (!loadMore) {
            WordPress.wpDB.deleteUploadedCustomTypePosts(
                    WordPress.currentBlog.getId(), typeName);
        }
        WordPress.wpDB.saveCustomTypePosts(posts, id);
        Log.v("posts", "size:" + posts.length);
        for (int i = 0; i < posts.length; i++) {
            HashMap<?, ?> args = (HashMap<?, ?>) posts[i];
            Set<?> argKeys = args.keySet();
            for (Object argKey : argKeys) {
                Object argValue = args.get(argKey);

                if (argValue instanceof Object[]) {
                    Log.v("aaa", "key:" + argKey.toString());
                    for (Object o : (Object[]) argValue) {
                        Log.v("object", " " + o.toString() + " "
                                + o.getClass().toString());
                    }
                } else if (argValue instanceof HashMap<?, ?>) {
                    HashMap<?, ?> aMap = (HashMap<?, ?>) argValue;
                    Log.v("aaa", "key:" + argKey.toString());
                    Set<?> aKeys = aMap.keySet();
                    for (Object o : aKeys) {
                        Log.v("object", " " + o.toString() + " "
                                + o.getClass().toString());
                    }
                } else {
                    Log.v("aaa", "key:" + argKey.toString() + " value:"
                            + argValue.toString() + " "
                            + argValue.getClass().getCanonicalName());
                }
            }
        }
        return true;
    }

    @Override
    String getTitle(Resources res) {
        return res.getString(R.id.post);
    }

    @Override
    String getMethod() {
        return null;
    }
}
