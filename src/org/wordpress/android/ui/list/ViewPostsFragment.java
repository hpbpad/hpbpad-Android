package org.wordpress.android.ui.list;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.text.format.DateUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ViewSwitcher;

import com.justsystems.hpb.pad.R;

import org.xmlrpc.android.ApiHelper;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.CustomTypePost;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.Postable;
import org.wordpress.android.task.LoadCustomTypePostsTask;
import org.wordpress.android.task.LoadPagesTask;
import org.wordpress.android.task.LoadPostsTask;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.ui.posts.ViewPostFragment;
import org.wordpress.android.util.EscapeUtils;
import org.wordpress.android.util.WPAlertDialogFragment;

public final class ViewPostsFragment extends ListFragment {
    /** Called when the activity is first created. */
    private int mRowID = 0;
    private long mSelectedID;
    private PostListAdapter mPostListAdapter;
    private OnPostSelectedListener mOnPostSelectedListener;
    private OnRefreshListener mOnRefreshListener;
    private OnPostActionListener mOnPostActionListener;
    private AbsListActivity mParentActivity;

    public boolean inDrafts = false;
    public List<String> imageUrl = new Vector<String>();
    public String errorMsg = "";
    public int totalDrafts = 0;
    public boolean isPage = false, shouldSelectAfterLoad = false;
    private String postType;
    public int numRecords = 20;
    public int offset = 0;
    public ViewSwitcher switcher;
    public AsyncTask<?, ?, ?> getPostsTask;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Intent intent = getActivity().getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            isPage = extras.getBoolean("viewPages");
            this.postType = intent.getStringExtra("type_name");
        }

    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);

        ListView listView = getListView();
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.setBackgroundColor(getResources()
                .getColor(R.color.list_row_bg));
        listView.setSelector(getResources().getDrawable(
                R.drawable.list_bg_selector));

        // add footer before add adapter
        createSwitcher();

        mPostListAdapter = new PostListAdapter(getActivity().getBaseContext());
        listView.setAdapter(mPostListAdapter);
        listView.setOnItemClickListener(new OnItemClickListener() {

            public void onItemClick(AdapterView<?> arg0, View v, int position,
                    long id) {
                if (position < mPostListAdapter.getCount()) {
                    if (v != null && !mParentActivity.isRefreshing) {
                        mSelectedID = v.getId();
                        Postable post;
                        if (postType != null) {
                            CustomPostTypePostsActivity cst = (CustomPostTypePostsActivity) getActivity();
                            post = new CustomTypePost(WordPress.currentBlog
                                    .getId(), mSelectedID, cst.getPostType());
                        } else {
                            post = new Post(WordPress.currentBlog.getId(),
                                    mSelectedID, isPage);

                        }
                        if (post.getId() >= 0) {
                            WordPress.setCurrentPost(post);
                            mOnPostSelectedListener.onPostSelected(post);
                            mPostListAdapter.notifyDataSetChanged();
                        } else {
                            showDialog();
                        }
                    }
                }
            }

            private void showDialog() {
                if (!getActivity().isFinishing()) {
                    FragmentTransaction ft = getFragmentManager()
                            .beginTransaction();
                    WPAlertDialogFragment alert = WPAlertDialogFragment
                            .newInstance(getString(R.string.post_not_found));
                    alert.show(ft, "alert");
                }
            }
        });

        listView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {

            public void onCreateContextMenu(ContextMenu menu, View v,
                    ContextMenuInfo menuInfo) {
                AdapterView.AdapterContextMenuInfo info;
                try {
                    info = (AdapterView.AdapterContextMenuInfo) menuInfo;
                } catch (ClassCastException e) {
                    // Log.e(TAG, "bad menuInfo", e);
                    return;
                }

                if (mParentActivity.isRefreshing)
                    return;

                Object[] args = { R.id.row_post_id };

                try {
                    Method m = android.view.View.class.getMethod("getTag");
                    m.invoke(mSelectedID, args);
                } catch (NoSuchMethodException e) {
                    mSelectedID = info.targetView.getId();
                } catch (IllegalArgumentException e) {
                    mSelectedID = info.targetView.getId();
                } catch (IllegalAccessException e) {
                    mSelectedID = info.targetView.getId();
                } catch (InvocationTargetException e) {
                    mSelectedID = info.targetView.getId();
                }
                // selectedID = (String)
                // info.targetView.getTag(R.id.row_post_id);

                // Show comments menu option only if post allows commenting
                boolean allowComments = false;
                Post post = new Post(WordPress.currentBlog.getId(),
                        mSelectedID, isPage);
                if (post.getId() >= 0) {
                    allowComments = post.allowComments();
                }

                mRowID = info.position;

                if (totalDrafts > 0 && mRowID < totalDrafts) {
                    menu.clear();
                    menu.setHeaderTitle(getResources().getText(
                            R.string.draft_actions));
                    menu.add(1, 0, 0,
                            getResources().getText(R.string.edit_draft));
                    menu.add(1, 1, 0,
                            getResources().getText(R.string.delete_draft));
                } else {
                    menu.clear();
                    if (isPage) {
                        menu.setHeaderTitle(getResources().getText(
                                R.string.page_actions));
                        menu.add(2, 0, 0,
                                getResources().getText(R.string.edit_page));
                        menu.add(2, 1, 0,
                                getResources().getText(R.string.delete_page));
                        menu.add(2, 2, 0,
                                getResources().getText(R.string.share_url_page));
                        if (allowComments)
                            menu.add(2, 3, 0,
                                    getResources()
                                            .getText(R.string.add_comment));
                    } else {
                        menu.setHeaderTitle(getResources().getText(
                                R.string.post_actions));
                        menu.add(0, 0, 0,
                                getResources().getText(R.string.edit_post));
                        menu.add(0, 1, 0,
                                getResources().getText(R.string.delete_post));
                        menu.add(0, 2, 0,
                                getResources().getText(R.string.share_url));
                        if (allowComments)
                            menu.add(0, 3, 0,
                                    getResources()
                                            .getText(R.string.add_comment));
                    }
                }
            }
        });

    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            // check that the containing activity implements our callback
            mOnPostSelectedListener = (OnPostSelectedListener) activity;
            mOnRefreshListener = (OnRefreshListener) activity;
            mOnPostActionListener = (OnPostActionListener) activity;
        } catch (ClassCastException e) {
            activity.finish();
            throw new ClassCastException(activity.toString()
                    + " must implement Callback");
        }
    }

    public void onResume() {
        super.onResume();

        mParentActivity = (AbsListActivity) getActivity();

    }

    private void createSwitcher() {
        // create the ViewSwitcher in the current context
        switcher = new ViewSwitcher(getActivity().getApplicationContext());
        Button footer = (Button) View.inflate(getActivity()
                .getApplicationContext(), R.layout.list_footer_btn, null);
        footer.setText(getResources().getText(R.string.load_more)
                + " "
                + getResources().getText(
                        (isPage) ? R.string.tab_pages : R.string.tab_posts));

        footer.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {

                if (!WordPress.wpDB.findLocalChanges()) {
                    // first view is showing, show the second progress view
                    switcher.showNext();
                    // get 20 more posts
                    numRecords += 20;
                    refreshPosts(true);
                } else {
                    if (!getActivity().isFinishing()) {
                        FragmentTransaction ft = getFragmentManager()
                                .beginTransaction();
                        WPAlertDialogFragment alert = WPAlertDialogFragment
                                .newInstance(
                                        getString(R.string.remote_changes),
                                        getString(R.string.local_changes), true);
                        alert.show(ft, "alert");
                    }
                }
            }
        });

        View progress = View.inflate(getActivity().getApplicationContext(),
                R.layout.list_footer_progress, null);

        switcher.addView(footer);
        switcher.addView(progress);
    }

    public PostListAdapter getAdapter() {
        return this.mPostListAdapter;
    }

    public void refreshPosts(final boolean loadMore) {

        if (!loadMore) {
            mOnRefreshListener.onRefresh(true);
            numRecords = 20;
            offset = 0;
        }
        List<Object> apiArgs = new Vector<Object>();
        apiArgs.add(WordPress.currentBlog);
        apiArgs.add(isPage);
        apiArgs.add(numRecords);
        apiArgs.add(loadMore);
        if (postType != null) {
            LoadCustomTypePostsTask task = new LoadCustomTypePostsTask(this,
                    mOnRefreshListener, loadMore, offset, postType);
            task.executeOnMultiThread();
            getPostsTask = task;
        } else if (this.isPage) {
            LoadPagesTask task = new LoadPagesTask(this, mOnRefreshListener,
                    loadMore);
            task.executeOnMultiThread(apiArgs);
            getPostsTask = task;
        } else {
            LoadPostsTask task = new LoadPostsTask(this, mOnRefreshListener,
                    loadMore);
            task.executeOnMultiThread(apiArgs);
            getPostsTask = task;
        }
    }

    public Map<String, ?> createItem(String title, String caption) {
        Map<String, String> item = new HashMap<String, String>();
        item.put("title", title);
        item.put("caption", caption);
        return item;
    }

    public boolean loadPosts(boolean loadMore) { // loads posts from the db
        this.mPostListAdapter.clear();
        List<Map<String, Object>> loadedPosts;
        try {
            if (postType != null) {
                loadedPosts = WordPress.wpDB.loadUploadedCustomTypePosts(
                        postType, WordPress.currentBlog.getId());
            } else {
                loadedPosts = WordPress.wpDB.loadUploadedPosts(
                        WordPress.currentBlog.getId(), isPage);
            }
        } catch (Exception e1) {
            e1.printStackTrace();
            return false;
        }

        if (loadedPosts != null) {
            offset = loadedPosts.size();
            numRecords = loadedPosts.size();
        } else {
            if (mPostListAdapter != null) {
                mPostListAdapter.notifyDataSetChanged();
            }
        }
        if (loadedPosts != null) {
            Date d = new Date();
            for (int i = 0; i < loadedPosts.size(); i++) {
                Map<String, Object> contentHash = loadedPosts.get(i);
                final String mTitle = EscapeUtils.unescapeHtml(contentHash.get(
                        "title").toString());

                final int mPostID = (Integer) contentHash.get("id");

                final long localTime = (Long) contentHash
                        .get("date_created_gmt");
                final String mDateCreatedFormatted = getFormattedDate(localTime);

                String mStatus = null;
                final Object contentStatus = contentHash.get("post_status");
                if (contentStatus != null) {
                    String api_status = contentStatus.toString();
                    if (api_status.equals("publish")) {
                        if (localTime > d.getTime()) {
                            mStatus = getResources()
                                    .getText(R.string.scheduled).toString();
                        } else {
                            mStatus = getResources()
                                    .getText(R.string.published).toString();
                        }
                    } else if (api_status.equals("draft")) {
                        mStatus = getResources().getText(R.string.draft)
                                .toString();
                    } else if (api_status.equals("pending")) {
                        mStatus = getResources().getText(
                                R.string.pending_review).toString();
                    } else if (api_status.equals("private")) {
                        mStatus = getResources().getText(R.string.post_private)
                                .toString();
                    } else {
                        assert false;
                    }
                }

                this.mPostListAdapter.add(mPostID, mTitle,
                        mDateCreatedFormatted, mStatus);
            }
        }
        // load drafts
        boolean drafts = loadDrafts();

        if (drafts) {

        } else {
            if (mPostListAdapter != null) {
                mPostListAdapter.notifyDataSetChanged();
            }
        }

        if (loadedPosts != null || drafts == true) {
            ListView listView = getListView();
            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            listView.setBackgroundColor(getResources().getColor(
                    R.color.list_row_bg));
            listView.removeFooterView(switcher);
            int position = listView.getFirstVisiblePosition();
            int y = listView.getChildCount() > 0 ? listView.getChildAt(0)
                    .getTop() : 0;

            if (loadedPosts != null) {
                if (loadedPosts.size() >= 20) {
                    listView.addFooterView(switcher);
                    listView.setAdapter(mPostListAdapter);
                }
            }

            mPostListAdapter.notifyDataSetChanged();
            // restore scrolled position
            listView.setSelectionFromTop(position, y);

            if (this.shouldSelectAfterLoad) {
                if (mPostListAdapter.getCount() >= 1) {
                    final int id = (int) mPostListAdapter.getItemId(0);
                    Post post = new Post(WordPress.currentBlog.getId(),
                            Integer.valueOf(id), isPage);
                    if (post.getId() >= 0) {
                        WordPress.setCurrentPost(post);
                        mOnPostSelectedListener.onPostSelected(post);
                        FragmentManager fm = getActivity()
                                .getSupportFragmentManager();
                        ViewPostFragment f = (ViewPostFragment) fm
                                .findFragmentById(R.id.postDetail);
                        if (f != null && f.isInLayout())
                            getListView().setItemChecked(0, true);
                    }
                }
                shouldSelectAfterLoad = false;
            }

            return true;
        } else {
            // always loadedPosts == null
            refreshPosts(false);
            if (!isPage)
                new ApiHelper.RefreshBlogContentTask(getActivity(),
                        WordPress.getCurrentBlog()).executeOnMultiThread(false);

            return false;
        }

    }

    private String getFormattedDate(long localTime) {
        int flags = 0;
        flags |= android.text.format.DateUtils.FORMAT_SHOW_DATE;
        flags |= android.text.format.DateUtils.FORMAT_ABBREV_MONTH;
        flags |= android.text.format.DateUtils.FORMAT_SHOW_YEAR;
        flags |= android.text.format.DateUtils.FORMAT_SHOW_TIME;
        String formattedDate = DateUtils.formatDateTime(getActivity()
                .getApplicationContext(), localTime, flags);
        return formattedDate;
    }

    private boolean loadDrafts() { // loads drafts from the db

        List<Map<String, Object>> loadedPosts;
        if (getActivity() instanceof CustomPostTypePostsActivity) {
            loadedPosts = WordPress.wpDB.loadCustomTypeDrafts(
                    WordPress.currentBlog.getId(), postType);
        } else if (isPage) {
            loadedPosts = WordPress.wpDB.loadDrafts(
                    WordPress.currentBlog.getId(), true);
        } else {
            loadedPosts = WordPress.wpDB.loadDrafts(
                    WordPress.currentBlog.getId(), false);
        }
        if (loadedPosts == null) {
            totalDrafts = 0;
            return false;
        }

        totalDrafts = loadedPosts.size();

        for (int i = 0; i < loadedPosts.size(); i++) {
            Map<String, Object> contentHash = loadedPosts.get(i);
            final int mDraftID = (Integer) contentHash.get("id");
            final String mDraftTitle = EscapeUtils.unescapeHtml(contentHash
                    .get("title").toString());
            final String mDraftDateCreated = "";
            final String mDraftStatus = getString(R.string.local_draft);
            this.mPostListAdapter.addDraft(mDraftID, mDraftTitle,
                    mDraftDateCreated, mDraftStatus);
        }

        return true;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Postable post;
        if (this.postType != null) {
            post = new CustomTypePost(WordPress.currentBlog.getId(),
                    mSelectedID, postType);
        } else {
            post = new Post(WordPress.currentBlog.getId(), mSelectedID, isPage);
        }

        if (post.getId() < 0) {
            if (!getActivity().isFinishing()) {
                FragmentTransaction ft = getFragmentManager()
                        .beginTransaction();
                WPAlertDialogFragment alert = WPAlertDialogFragment
                        .newInstance(getString(R.string.post_not_found));
                alert.show(ft, "alert");
            }
            return false;
        }

        /* Switch on the ID of the item, to get what the user selected. */
        if (item.getGroupId() == 0) {
            switch (item.getItemId()) {
            case 0:
                Intent i2 = new Intent(getActivity().getApplicationContext(),
                        EditPostActivity.class);
                i2.putExtra("postID", mSelectedID);
                i2.putExtra("id", WordPress.currentBlog.getId());
                startActivityForResult(i2, 0);
                return true;
            case 1:
                mOnPostActionListener.onPostAction(AbsListActivity.POST_DELETE,
                        post);
                return true;
            case 2:
                mOnPostActionListener.onPostAction(AbsListActivity.POST_SHARE,
                        post);
                return true;
            case 3:
                mOnPostActionListener.onPostAction(
                        AbsListActivity.POST_COMMENT, post);
                return true;
            }

        } else if (item.getGroupId() == 2) {
            switch (item.getItemId()) {
            case 0:
                Intent i2 = new Intent(getActivity().getApplicationContext(),
                        EditPostActivity.class);
                i2.putExtra("postID", mSelectedID);
                i2.putExtra("id", WordPress.currentBlog.getId());
                i2.putExtra("isPage", true);
                startActivityForResult(i2, 0);
                return true;
            case 1:
                mOnPostActionListener.onPostAction(AbsListActivity.POST_DELETE,
                        post);
                return true;
            case 2:
                mOnPostActionListener.onPostAction(AbsListActivity.POST_SHARE,
                        post);
                return true;
            case 3:
                mOnPostActionListener.onPostAction(
                        AbsListActivity.POST_COMMENT, post);
                return true;
            }

        } else {
            switch (item.getItemId()) {
            case 0:
                Intent i2 = new Intent(getActivity().getApplicationContext(),
                        EditPostActivity.class);
                i2.putExtra("postID", mSelectedID);
                i2.putExtra("id", WordPress.currentBlog.getId());
                if (isPage) {
                    i2.putExtra("isPage", true);
                }
                i2.putExtra("localDraft", true);
                startActivityForResult(i2, 0);
                return true;
            case 1:

                mOnPostActionListener.onPostAction(AbsListActivity.POST_DELETE,
                        post);
                return true;
            }
        }

        return false;
    }

    public interface OnPostSelectedListener {
        public void onPostSelected(Postable postable);
    }

    public interface OnRefreshListener {
        public void onRefresh(boolean start);
    }

    public interface OnPostActionListener {
        public void onPostAction(int action, Postable post);
    }

}
