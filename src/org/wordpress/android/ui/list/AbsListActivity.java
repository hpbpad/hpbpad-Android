package org.wordpress.android.ui.list;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.justsystems.hpb.pad.R;

import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import org.wordpress.android.PostEditConstants;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Postable;
import org.wordpress.android.task.AbsDeleteTask;
import org.wordpress.android.task.AbsShareUrlTask;
import org.wordpress.android.task.MultiAsyncTask;
import org.wordpress.android.ui.DashboardActivity;
import org.wordpress.android.ui.StatsActivity;
import org.wordpress.android.ui.ViewSiteActivity;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.comments.AddCommentActivity;
import org.wordpress.android.ui.comments.CommentsActivity;
import org.wordpress.android.ui.list.ViewPostsFragment.OnPostActionListener;
import org.wordpress.android.ui.list.ViewPostsFragment.OnPostSelectedListener;
import org.wordpress.android.ui.list.ViewPostsFragment.OnRefreshListener;
import org.wordpress.android.ui.posts.ViewPostFragment;
import org.wordpress.android.ui.posts.ViewPostFragment.OnDetailPostActionListener;
import org.wordpress.android.ui.reader.ReaderActivity;
import org.wordpress.android.util.WPAlertDialogFragment.OnDialogConfirmListener;

public abstract class AbsListActivity extends WPActionBarActivity implements
        OnPostSelectedListener, OnRefreshListener, OnPostActionListener,
        OnDetailPostActionListener, OnDialogConfirmListener, PostEditConstants {

    private ViewPostsFragment postList;
    public static final int ID_DIALOG_DELETING = 1;
    public static final int ID_DIALOG_SHARE = 2;
    protected static final int ID_DIALOG_COMMENT = 3;

    public ProgressDialog loadingDialog;
    // public boolean isPage = false;
    public String errorMsg = "";
    public boolean isRefreshing = false;
    private MenuItem refreshMenuItem;
    protected int ACTIVITY_EDIT_POST = 0;
    private int ACTIVITY_ADD_COMMENT = 1;

    protected int postType;

    protected static final int POST_TYPE_POST = 1;
    protected static final int POST_TYPE_PAGE = 2;
    protected static final int POST_TYPE_CUSTOM = 3;

    private WordPress.OnPostUploadedListener uploadedListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Special check for a null database (see #507)
        if (WordPress.wpDB == null) {
            Toast.makeText(this, R.string.fatal_db_error, Toast.LENGTH_LONG)
                    .show();
            finish();
            return;
        }

        // Restore last selection on app creation
        if (false && WordPress.shouldRestoreSelectedActivity
                && WordPress.getCurrentBlog() != null
                && !(this instanceof PagesActivity)) {
            // Refresh blog content when returning to the app
            new ApiHelper.RefreshBlogContentTask(this,
                    WordPress.getCurrentBlog()).execute(false);

            WordPress.shouldRestoreSelectedActivity = false;
            SharedPreferences settings = PreferenceManager
                    .getDefaultSharedPreferences(this);
            int lastActivitySelection = settings.getInt(
                    "wp_pref_last_activity", -1);
            if (lastActivitySelection >= 0) {
                Intent i = null;
                switch (lastActivitySelection) {
                case READER_ACTIVITY:
                    i = new Intent(this, ReaderActivity.class);
                    break;
                case PAGES_ACTIVITY:
                    i = new Intent(this, PagesActivity.class);
                    i.putExtra("viewPages", true);
                    break;
                case COMMENTS_ACTIVITY:
                    i = new Intent(this, CommentsActivity.class);
                    break;
                case STATS_ACTIVITY:
                    i = new Intent(this, StatsActivity.class);
                    break;
                case VIEW_SITE_ACTIVITY:
                    i = new Intent(this, ViewSiteActivity.class);
                    break;
                case DASHBOARD_ACTIVITY:
                    i = new Intent(this, DashboardActivity.class);
                    break;
                case CUSTOM_TYPE_ACTIVITY:
                    i = new Intent(this, CustomPostTypePostsActivity.class);
                    String postType = settings.getString(
                            "wp_pref_last_activity_type", null);
                    i.putExtra("type_name", postType);
                    break;
                }
                if (i != null) {
                    startActivity(i);
                    finish();
                }
            }
        }

        createMenuDrawer(R.layout.posts);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);

        FragmentManager fm = getSupportFragmentManager();
        postList = (ViewPostsFragment) fm.findFragmentById(R.id.postList);
        postList.setListShown(true);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String errorMessage = extras.getString("errorMessage");
            if (errorMessage != null)
                showPostUploadErrorAlert(errorMessage);
        }

        WordPress.setCurrentPost(null);

        this.uploadedListener = new WordPress.OnPostUploadedListener() {
            @Override
            public void OnPostUploaded() {
                if (isFinishing())
                    return;

                checkForLocalChanges(false);
            }
        };
        WordPress.setOnPostUploadedListener(this.uploadedListener);

        attemptToSelectPost();
    }

    private void showPostUploadErrorAlert(String errorMessage) {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                AbsListActivity.this);
        dialogBuilder.setTitle(getResources().getText(R.string.error));
        dialogBuilder.setMessage(errorMessage);
        dialogBuilder.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Just close the window.
                    }
                });
        dialogBuilder.setCancelable(true);
        if (!isFinishing())
            dialogBuilder.create().show();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Bundle extras = intent.getExtras();
        if (extras != null) {
            String errorMessage = extras.getString("errorMessage");
            if (errorMessage != null)
                showPostUploadErrorAlert(errorMessage);
        }

    }

    public void checkForLocalChanges(boolean shouldPrompt) {
        boolean hasLocalChanges = WordPress.wpDB.findLocalChanges();
        if (hasLocalChanges) {
            if (!shouldPrompt)
                return;
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                    AbsListActivity.this);
            dialogBuilder.setTitle(getResources().getText(
                    R.string.local_changes));
            dialogBuilder.setMessage(getResources().getText(
                    R.string.remote_changes));
            dialogBuilder.setPositiveButton(getResources()
                    .getText(R.string.yes),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                            popPostDetail();
                            attemptToSelectPost();
                            postList.refreshPosts(false);
                        }
                    });
            dialogBuilder.setNegativeButton(
                    getResources().getText(R.string.no),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                            // just close the window
                        }
                    });
            dialogBuilder.setCancelable(true);
            if (!isFinishing()) {
                dialogBuilder.create().show();
            }
        } else {
            popPostDetail();
            attemptToSelectPost();
            shouldAnimateRefreshButton = true;
            postList.refreshPosts(false);
        }
    }

    public void popPostDetail() {
        FragmentManager fm = getSupportFragmentManager();
        ViewPostFragment f = (ViewPostFragment) fm
                .findFragmentById(R.id.postDetail);
        if (f == null) {
            try {
                fm.popBackStack();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (postList.getListView().getCount() == 0) {
            postList.loadPosts(false);
        }
        if (WordPress.postsShouldRefresh) {
            checkForLocalChanges(false);
            WordPress.postsShouldRefresh = false;
        }
        attemptToSelectPost();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isRefreshing)
            stopAnimatingRefreshButton(refreshMenuItem);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (postList.getPostsTask != null)
            postList.getPostsTask.cancel(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (WordPress.onPostUploadedListener == this.uploadedListener) {
            WordPress.onPostUploadedListener = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.posts, menu);
        refreshMenuItem = menu.findItem(R.id.menu_refresh);
        if (shouldAnimateRefreshButton) {
            shouldAnimateRefreshButton = false;
            startAnimatingRefreshButton(refreshMenuItem);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_refresh) {
            checkForLocalChanges(true);
            new ApiHelper.RefreshBlogContentTask(this, WordPress.currentBlog)
                    .executeOnMultiThread(false);
            return true;
        } else if (itemId == R.id.menu_new_post) {
            startActivity();
            return true;
        } else if (itemId == android.R.id.home) {
            FragmentManager fm = getSupportFragmentManager();
            if (fm.getBackStackEntryCount() > 0) {
                popPostDetail();
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    abstract void startActivity();

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null) {
            if (requestCode == ACTIVITY_EDIT_POST && resultCode == RESULT_OK) {
                if (data.getBooleanExtra("shouldRefresh", false))
                    postList.loadPosts(false);
            } else if (requestCode == ACTIVITY_ADD_COMMENT) {

                Bundle extras = data.getExtras();

                final String returnText = extras.getString("commentText");

                if (!returnText.equals("CANCEL")) {
                    // Add comment to the server if user didn't cancel.
                    final String postID = extras.getString("postID");
                    new AbsListActivity.addCommentTask().executeOnMultiThread(
                            postID, returnText);
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void attemptToSelectPost() {

        FragmentManager fm = getSupportFragmentManager();
        ViewPostFragment f = (ViewPostFragment) fm
                .findFragmentById(R.id.postDetail);

        if (f != null && f.isInLayout()) {
            postList.shouldSelectAfterLoad = true;
        }

    }

    @Override
    public void onPostSelected(Postable post) {
        FragmentManager fm = getSupportFragmentManager();
        ViewPostFragment f = (ViewPostFragment) fm
                .findFragmentById(R.id.postDetail);

        if (post != null) {

            WordPress.setCurrentPost(post);
            if (f == null || !f.isInLayout()) {
                FragmentTransaction ft = fm.beginTransaction();
                ft.hide(postList);
                f = new ViewPostFragment();
                ft.add(R.id.postDetailFragmentContainer, f);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.addToBackStack(null);
                ft.commit();
            } else {
                f.loadPost(post);
            }
        }
    }

    @Override
    public void onRefresh(boolean start) {
        if (start) {
            attemptToSelectPost();
            shouldAnimateRefreshButton = true;
            startAnimatingRefreshButton(refreshMenuItem);
            isRefreshing = true;
        } else {
            stopAnimatingRefreshButton(refreshMenuItem);
            isRefreshing = false;
        }

    }

    @Override
    protected Dialog onCreateDialog(int id) {
        loadingDialog = new ProgressDialog(this);
        if (id == ID_DIALOG_DELETING) {
            // do nothing
        } else if (id == ID_DIALOG_SHARE) {
            // do nothing
        } else if (id == ID_DIALOG_COMMENT) {
            loadingDialog
                    .setTitle(getResources().getText(R.string.add_comment));
            loadingDialog.setMessage(getResources().getText(
                    R.string.attempting_add_comment));
            loadingDialog.setCancelable(false);
            return loadingDialog;
        }

        return super.onCreateDialog(id);
    }

    public class addCommentTask extends MultiAsyncTask<String, Void, Boolean> {

        String postid;
        String comment;

        @Override
        protected void onPreExecute() {
            showDialog(ID_DIALOG_COMMENT);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            dismissDialog(ID_DIALOG_COMMENT);
            attemptToSelectPost();
            if (result) {
                Toast.makeText(AbsListActivity.this,
                        getResources().getText(R.string.comment_added),
                        Toast.LENGTH_SHORT).show();
                // If successful, attempt to refresh comments
                refreshComments();
            } else {
                Toast.makeText(AbsListActivity.this,
                        getResources().getText(R.string.connection_error),
                        Toast.LENGTH_SHORT).show();

                Intent i = new Intent(AbsListActivity.this,
                        AddCommentActivity.class);
                i.putExtra("postID", postid);
                i.putExtra("comment", comment);
                startActivityForResult(i, ACTIVITY_ADD_COMMENT);
            }

        }

        @Override
        protected Boolean doInBackground(String... params) {
            boolean result = false;
            postid = params[0];
            comment = params[1];
            XMLRPCClient client = new XMLRPCClient(
                    WordPress.currentBlog.getUrl(),
                    WordPress.currentBlog.getHttpuser(),
                    WordPress.currentBlog.getHttppassword());

            Map<String, Object> commentHash = new HashMap<String, Object>();
            commentHash.put("content", comment);
            commentHash.put("author", "");
            commentHash.put("author_url", "");
            commentHash.put("author_email", "");

            Object[] commentParams = { WordPress.currentBlog.getBlogId(),
                    WordPress.currentBlog.getUsername(),
                    WordPress.currentBlog.getPassword(),
                    Integer.valueOf(postid), commentHash };

            try {
                int newCommentID = (Integer) client.call("wp.newComment",
                        commentParams);
                if (newCommentID >= 0) {
                    WordPress.wpDB.updateLatestCommentID(
                            WordPress.currentBlog.getId(), newCommentID);
                    result = true;
                }
            } catch (final XMLRPCException e) {
                errorMsg = getResources().getText(R.string.error_generic)
                        .toString();
                result = false;
            }
            return result;
        }

    }

    public class refreshCommentsTask extends MultiAsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            Object[] commentParams = { WordPress.currentBlog.getBlogId(),
                    WordPress.currentBlog.getUsername(),
                    WordPress.currentBlog.getPassword() };

            try {
                ApiHelper.refreshComments(AbsListActivity.this, commentParams);
            } catch (final XMLRPCException e) {
                errorMsg = getResources().getText(R.string.error_generic)
                        .toString();
            }
            return null;
        }

    }

    private void refreshComments() {
        new AbsListActivity.refreshCommentsTask().executeOnMultiThread();
    }

    public String getShortlinkTagHref(String urlString) {
        String html = getHTML(urlString);

        if (html != "") {
            try {
                int location = html.indexOf("http://wp.me");
                String shortlink = html.substring(location, location + 30);
                shortlink = shortlink.substring(0, shortlink.indexOf("'"));
                return shortlink;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null; // never found the shortlink tag
    }

    public String getHTML(String urlSource) {
        URL url;
        HttpURLConnection conn;
        BufferedReader rd;
        String line;
        String result = "";
        try {
            url = new URL(urlSource);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            rd = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            while ((line = rd.readLine()) != null) {
                result += line;
            }
            rd.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public void onPostAction(int action, final Postable post) {
        if (postList.getPostsTask != null) {
            postList.getPostsTask.cancel(true);
            // titleBar.stopRotatingRefreshIcon();
            isRefreshing = false;
        }

        // No post? No service.
        if (post == null) {
            Toast.makeText(AbsListActivity.this, R.string.post_not_found,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (action == POST_DELETE) {
            if (post.isLocalDraft()) {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                        AbsListActivity.this);
                dialogBuilder.setTitle(getResources().getText(
                        R.string.delete_draft));
                dialogBuilder.setMessage(getResources().getText(
                        R.string.delete_sure)
                        + " '" + post.getTitle() + "'?");
                dialogBuilder.setPositiveButton(
                        getResources().getText(R.string.yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                                post.delete();
                                attemptToSelectPost();
                                postList.loadPosts(false);
                            }
                        });
                dialogBuilder.setNegativeButton(
                        getResources().getText(R.string.no),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                                // Just close the window.

                            }
                        });
                dialogBuilder.setCancelable(true);
                if (!isFinishing()) {
                    dialogBuilder.create().show();
                }

            } else {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                        AbsListActivity.this);

                switch (post.getType()) {
                case Postable.TYP_PAGE:
                    dialogBuilder.setTitle(getResources().getText(
                            R.string.delete_page));
                    dialogBuilder.setMessage(getResources().getText(
                            R.string.delete_sure_page)
                            + " '" + post.getTitle() + "'?");
                    break;
                case Postable.TYP_POST:
                case Postable.TYP_CUSTOM_TYPE_POST:
                    dialogBuilder.setTitle(getResources().getText(
                            R.string.delete_post));
                    dialogBuilder.setMessage(getResources().getText(
                            R.string.delete_sure_post)
                            + " '" + post.getTitle() + "'?");
                    break;
                }

                dialogBuilder.setPositiveButton(
                        getResources().getText(R.string.yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                                getDeleteTask().executeOnMultiThread(post);
                            }
                        });
                dialogBuilder.setNegativeButton(
                        getResources().getText(R.string.no),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                                // Just close the window.

                            }
                        });
                dialogBuilder.setCancelable(true);
                if (!isFinishing()) {
                    dialogBuilder.create().show();
                }

            }
        } else if (action == POST_SHARE) {
            getShareUrlTask().executeOnMultiThread(post);
        } else if (action == POST_CLEAR) {
            FragmentManager fm = getSupportFragmentManager();
            ViewPostFragment f = (ViewPostFragment) fm
                    .findFragmentById(R.id.postDetail);
            if (f != null) {
                f.clearContent();
            }
        } else if (action == POST_COMMENT) {
            Intent i = new Intent(AbsListActivity.this,
                    AddCommentActivity.class);
            i.putExtra("postID", post.getPostId());
            startActivityForResult(i, ACTIVITY_ADD_COMMENT);
        }
    }

    @Override
    public void onDetailPostAction(int action, Postable post) {

        onPostAction(action, post);

    }

    @Override
    public void onDialogConfirm() {
        postList.switcher.showNext();
        postList.numRecords += 30;
        postList.refreshPosts(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBlogChanged() {
        super.onBlogChanged();
        popPostDetail();
        attemptToSelectPost();
        postList.loadPosts(false);
        new ApiHelper.RefreshBlogContentTask(this, WordPress.currentBlog)
                .executeOnMultiThread(false);
    }

    abstract AbsShareUrlTask getShareUrlTask();

    abstract AbsDeleteTask getDeleteTask();

    public boolean isPage() {
        return false;
    }
}
