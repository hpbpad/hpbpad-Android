package org.wordpress.android.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SpinnerAdapter;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.internal.widget.IcsAdapterView;
import com.actionbarsherlock.internal.widget.IcsSpinner;
import com.actionbarsherlock.view.MenuItem;
import com.justsystems.hpb.pad.R;

import net.simonvt.menudrawer.MenuDrawer;
import net.simonvt.menudrawer.Position;

import org.wordpress.android.Constants;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.PostType;
import org.wordpress.android.task.RefreshMenuTask;
import org.wordpress.android.ui.accounts.NewAccountActivity;
import org.wordpress.android.ui.comments.CommentsActivity;
import org.wordpress.android.ui.list.CustomPostTypePostsActivity;
import org.wordpress.android.ui.list.PagesActivity;
import org.wordpress.android.ui.list.PostsActivity;
import org.wordpress.android.ui.posts.EditCustomTypePostActivity;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.ui.prefs.PreferencesActivity;
import org.wordpress.android.ui.reader.ReaderActivity;
import org.wordpress.android.util.DeviceUtils;
import org.wordpress.android.util.EscapeUtils;

/**
 * Base class for Activities that include a standard action bar and menu drawer.
 */
public abstract class WPActionBarActivity extends SherlockFragmentActivity {

    private static final String TAG = "WPActionBarActivity";

    /**
     * Request code used when no accounts exist, and user is prompted to add an
     * account.
     */
    protected static final int ADD_ACCOUNT_REQUEST = 100;
    /**
     * Request code for reloading menu after returning from the
     * PreferencesActivity.
     */
    private static final int SETTINGS_REQUEST = 200;

    /**
     * Used to restore active activity on app creation
     */
    protected static final int READER_ACTIVITY = 0;
    protected static final int POSTS_ACTIVITY = 1;
    protected static final int PAGES_ACTIVITY = 2;
    protected static final int COMMENTS_ACTIVITY = 3;
    protected static final int STATS_ACTIVITY = 4;
    protected static final int QUICK_PHOTO_ACTIVITY = 5;
    protected static final int QUICK_VIDEO_ACTIVITY = 6;
    protected static final int VIEW_SITE_ACTIVITY = 7;
    protected static final int DASHBOARD_ACTIVITY = 8;
    protected static final int SETTINGS_ACTIVITY = 9;
    protected static final int REFRESH_MENU = 10;
    protected static final int CUSTOM_TYPE_ACTIVITY = 99;

    private static final boolean SHOW_READER = false;

    private LinearLayout menu;
    protected MenuDrawer mMenuDrawer;
    protected int[] blogIDs;
    protected String[] blogNames;
    protected boolean isAnimatingRefreshButton;
    protected boolean shouldAnimateRefreshButton;
    protected boolean mShouldFinish;
    private boolean mIsDotComBlog;
    private boolean mIsXLargeDevice;
    private boolean mBlogSpinnerInitialized;

    private MenuDrawerAdapter mAdapter;
    private ListView mListView;

    private LinearLayout spinnerWrapper;
    private IcsSpinner mBlogSpinner;

    private ImageButton setting;

    protected static int selectedPosition = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == 4)
            mIsXLargeDevice = true;
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (isAnimatingRefreshButton) {
            isAnimatingRefreshButton = false;
        }
        if (mShouldFinish) {
            overridePendingTransition(0, 0);
            finish();
        } else {
            WordPress.shouldRestoreSelectedActivity = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // the current blog may have changed while we were away
        setupCurrentBlog();
        if (mMenuDrawer != null) {
            updateMenuDrawer();
        }

        Blog currentBlog = WordPress.getCurrentBlog();

        if (currentBlog != null && mListView != null
                && mListView.getHeaderViewsCount() > 0) {
            for (int i = 0; i < blogIDs.length; i++) {
                if (blogIDs[i] == currentBlog.getId()) {
                    mBlogSpinner.setSelection(i);
                }
            }
        }
    }

    /**
     * Create a menu drawer and attach it to the activity.
     * 
     * @param contentViewID
     *            {@link View} of the main content for the activity.
     */
    protected void createMenuDrawer(int contentViewID) {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mMenuDrawer = attachMenuDrawer();
        mMenuDrawer.setContentView(contentViewID);

        initMenuDrawer(false);
    }

    /**
     * Create a menu drawer and attach it to the activity.
     * 
     * @param contentView
     *            {@link View} of the main content for the activity.
     */
    protected void createMenuDrawer(View contentView) {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mMenuDrawer = attachMenuDrawer();
        mMenuDrawer.setContentView(contentView);

        initMenuDrawer(false);
    }

    /**
     * Attach a menu drawer to the Activity Set to be a static drawer if on a
     * landscape x-large device
     */
    private MenuDrawer attachMenuDrawer() {
        MenuDrawer menuDrawer = null;
        if (mIsXLargeDevice) {
            // on a x-large screen device
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                menuDrawer = MenuDrawer.attach(this,
                        MenuDrawer.MENU_DRAG_CONTENT, Position.LEFT, true);
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            } else {
                menuDrawer = MenuDrawer.attach(this,
                        MenuDrawer.MENU_DRAG_CONTENT);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        } else {
            menuDrawer = MenuDrawer.attach(this, MenuDrawer.MENU_DRAG_CONTENT);
        }
        int shadowSizeInPixels = getResources().getDimensionPixelSize(
                R.dimen.menu_shadow_width);
        menuDrawer.setDropShadowSize(shadowSizeInPixels);
        menuDrawer.setDropShadowColor(getResources().getColor(
                R.color.md__shadowColor));

        return menuDrawer;
    }

    /**
     * Create menu drawer ListView and listeners
     */
    private void initMenuDrawer(boolean force) {
        if (this.menu == null || force) {
            this.menu = (LinearLayout) getLayoutInflater().inflate(
                    R.layout.menu_drawer, null);
            this.mListView = (ListView) menu.findViewById(R.id.listView1);
            this.mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            this.mListView.setDivider(null);
            this.mListView.setDividerHeight(0);
            this.mListView.setCacheColorHint(android.R.color.transparent);
            this.mListView.setOnItemClickListener(mItemClickListener);

            this.setting = (ImageButton) menu.findViewById(R.id.setting);
            this.setting.setOnClickListener(mOnClickListener);

            mMenuDrawer.setMenuView(this.menu);
        }

        updateBlogs();
        mListView.removeHeaderView(this.spinnerWrapper);
        if (blogNames.length > 1) {
            mListView.setAdapter(null);
            updateBlogSpminner();
            mListView.setAdapter(this.mAdapter);
        }
        updateMenuDrawer();
    }

    private void updateBlogSpminner() {
        if (this.spinnerWrapper == null) {
            LayoutInflater layoutInflater = (LayoutInflater) this
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.spinnerWrapper = (LinearLayout) layoutInflater.inflate(
                    R.layout.blog_spinner, null);
            this.spinnerWrapper.setOnClickListener(mOnClickListener);

            mBlogSpinner = (IcsSpinner) spinnerWrapper
                    .findViewById(R.id.blog_spinner);
            mBlogSpinner.setOnItemSelectedListener(mItemSelectedListener);
        }

        ArrayAdapter<String> mSpinnerAdapter = getAdapter();
        mBlogSpinner.setAdapter(mSpinnerAdapter);
        mListView.addHeaderView(spinnerWrapper);
    }

    private ArrayAdapter<String> getAdapter() {
        ArrayAdapter<String> mSpinnerAdapter = new ArrayAdapter<String>(
                getSupportActionBar().getThemedContext(),
                R.layout.blog_list_spinner_item, blogNames);
        mSpinnerAdapter
                .setDropDownViewResource(R.layout.blog_spinner_dropdown_item);
        return mSpinnerAdapter;
    }

    protected void startActivityWithDelay(final Intent i) {

        if (mIsXLargeDevice
                && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Tablets in landscape don't need a delay because the menu drawer doesn't close
            startActivity(i);
        } else {
            // Let the menu animation finish before starting a new activity
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    startActivity(i);
                }
            }, 400);
        }
    }

    /**
     * Update all of the items in the menu drawer based on the current active
     * blog.
     */
    public void updateMenuDrawer() {
        Log.i("method call", "updateMenuDrawer()");
        if (WordPress.currentBlog == null) {
            return;
        }

        // TODO do in multitask

        mIsDotComBlog = WordPress.currentBlog != null
                && WordPress.currentBlog.isDotcomFlag();

        final ArrayList<MenuDrawerItem> items = new ArrayList<MenuDrawerItem>();
        Resources resources = getResources();
        // hide reader acvitivy
        if (SHOW_READER && mIsDotComBlog) {
            items.add(new MenuDrawerItem(resources.getString(R.string.reader),
                    resources.getDrawable(R.drawable.dashboard_icon_subs),
                    READER_ACTIVITY));
        }

        final int id = WordPress.getCurrentBlog().getId();
        WordPress.wpDB.getPostTypes(WPActionBarActivity.this, id, items);
        if (this.mAdapter == null) {
            items.add(new MenuDrawerItem(resources.getString(R.string.posts),
                    resources.getDrawable(R.drawable.ic_md_post),
                    POSTS_ACTIVITY));

            items.add(new MenuDrawerItem(resources.getString(R.string.pages),
                    resources.getDrawable(R.drawable.ic_md_page),
                    PAGES_ACTIVITY));
            items.add(new MenuDrawerItem(resources
                    .getString(R.string.tab_comments), resources
                    .getDrawable(R.drawable.ic_md_comments), COMMENTS_ACTIVITY));
            items.add(new MenuDrawerItem(resources
                    .getString(R.string.tab_stats), resources
                    .getDrawable(R.drawable.ic_md_statistics), STATS_ACTIVITY));
            items.add(new MenuDrawerItem(resources
                    .getString(R.string.view_site), resources
                    .getDrawable(R.drawable.ic_md_view_site),
                    VIEW_SITE_ACTIVITY));
            items.add(new MenuDrawerItem(resources
                    .getString(R.string.view_admin), resources
                    .getDrawable(R.drawable.ic_md_dashboard),
                    DASHBOARD_ACTIVITY));
            items.add(new MenuDrawerItem(resources
                    .getString(R.string.quick_photo), resources
                    .getDrawable(R.drawable.ic_md_photo), QUICK_PHOTO_ACTIVITY));
            items.add(new MenuDrawerItem(resources
                    .getString(R.string.quick_video), resources
                    .getDrawable(R.drawable.ic_md_video), QUICK_VIDEO_ACTIVITY));
        } else {
            final ArrayList<MenuDrawerItem> oldItems = this.mAdapter.getItems();
            int size = oldItems.size();
            for (int i = 0; i < size; i++) {
                MenuDrawerItem item = oldItems.get(i);
                if (item.getActivityTag() == CUSTOM_TYPE_ACTIVITY) {
                    oldItems.remove(item);
                    i--;
                    size--;
                }
            }
            for (MenuDrawerItem item : oldItems) {
                items.add(item);
            }
        }

        if (selectedPosition != -1) {
            SharedPreferences settings = PreferenceManager
                    .getDefaultSharedPreferences(WPActionBarActivity.this);
            final int lastActivitySelection = settings.getInt(
                    "wp_pref_last_activity", -1);
            if (lastActivitySelection != CUSTOM_TYPE_ACTIVITY) {
                for (int i = 0; i < items.size(); i++) {
                    MenuDrawerItem item = items.get(i);
                    if (item.getActivityTag() == lastActivitySelection) {
                        selectedPosition = i;
                        mListView.setItemChecked(selectedPosition, true);
                    }
                }
            } else {
                final String postType = settings.getString(
                        "wp_pref_last_activity_type", null);
                if (postType != null) {
                    for (int i = 0; i < items.size(); i++) {
                        MenuDrawerItem item = items.get(i);
                        if (postType.equals(item.getPostType().getName())) {
                            selectedPosition = i;
                            mListView.setItemChecked(selectedPosition, true);
                            break;
                        }
                    }
                }
            }
        }

        if (mAdapter == null) {
            mAdapter = new MenuDrawerAdapter(WPActionBarActivity.this, items);
            mListView.setAdapter(mAdapter);
        } else {
            int position = mListView.getFirstVisiblePosition();
            int y = mListView.getChildCount() > 0 ? mListView.getChildAt(0)
                    .getTop() : 0;
            mAdapter.setItems(items);
            mListView.setSelectionFromTop(position, y);
        }
    }

    public void invalidateList() {
        this.mAdapter.notifyDataSetInvalidated();
    }

    int getSelectedPosition() {
        return selectedPosition;
    }

    private OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == setting) {
                mShouldFinish = false;
                Intent settingsIntent = new Intent(WPActionBarActivity.this,
                        PreferencesActivity.class);
                startActivityForResult(settingsIntent, SETTINGS_REQUEST);
            } else if (v == spinnerWrapper) {
                if (mBlogSpinner != null) {
                    mBlogSpinner.performClick();
                }
            }
        }
    };

    private AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {
            // Adjust position if only one blog is in the app
            if (mListView.getHeaderViewsCount() > 0 && position > 0)
                position--;

            Log.v("position", position + "p" + selectedPosition);
            final int oldPosition = selectedPosition;

            if (position == selectedPosition) {
                // Same row selected
                mMenuDrawer.closeMenu();
                return;
            } else {
                selectedPosition = position;
                mListView.setItemChecked(selectedPosition, true);
            }

            final MenuDrawerItem item = mAdapter.getItem(position);

            //XXX really need?
            mAdapter.notifyDataSetChanged();
            Intent intent = null;

            SharedPreferences settings = PreferenceManager
                    .getDefaultSharedPreferences(WPActionBarActivity.this);
            SharedPreferences.Editor editor = settings.edit();

            switch (item.getActivityTag()) {
            case READER_ACTIVITY:
                if (!(WPActionBarActivity.this instanceof ReaderActivity))
                    mShouldFinish = true;
                int readerBlogID = WordPress.wpDB.getWPCOMBlogID();
                if (WordPress.currentBlog.isDotcomFlag()) {
                    intent = new Intent(WPActionBarActivity.this,
                            ReaderActivity.class);
                    intent.putExtra("id", readerBlogID);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    editor.putInt("wp_pref_last_activity", READER_ACTIVITY);
                }
                break;
            case POSTS_ACTIVITY:
                if (!(WPActionBarActivity.this instanceof PostsActivity)
                        || (WPActionBarActivity.this instanceof PagesActivity)
                        || (WPActionBarActivity.this instanceof CustomPostTypePostsActivity))
                    mShouldFinish = true;
                intent = new Intent(WPActionBarActivity.this,
                        PostsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                editor.putInt("wp_pref_last_activity", POSTS_ACTIVITY);
                break;
            case PAGES_ACTIVITY:
                if (!(WPActionBarActivity.this instanceof PagesActivity))
                    mShouldFinish = true;
                intent = new Intent(WPActionBarActivity.this,
                        PagesActivity.class);
                intent.putExtra("id", WordPress.currentBlog.getId());
                intent.putExtra("isNew", true);
                intent.putExtra("viewPages", true);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                editor.putInt("wp_pref_last_activity", PAGES_ACTIVITY);
                break;
            case CUSTOM_TYPE_ACTIVITY:
                final PostType postType = item.getPostType();
                final String typeName = postType.getName();
                if (!(WPActionBarActivity.this instanceof CustomPostTypePostsActivity)
                        || !((CustomPostTypePostsActivity) WPActionBarActivity.this)
                                .getPostType().equals(typeName))
                    mShouldFinish = true;
                intent = new Intent(WPActionBarActivity.this,
                        CustomPostTypePostsActivity.class);
                intent.putExtra("id", WordPress.currentBlog.getId());
                intent.putExtra("isNew", true);
                assert typeName != null;
                intent.putExtra("type_name", typeName);
                final String typeLabel = postType.getLabel();
                intent.putExtra("type_label", typeLabel);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                editor.putInt("wp_pref_last_activity", CUSTOM_TYPE_ACTIVITY);
                editor.putString("wp_pref_last_activity_type", typeName);
                break;
            case COMMENTS_ACTIVITY:
                if (!(WPActionBarActivity.this instanceof CommentsActivity))
                    mShouldFinish = true;
                intent = new Intent(WPActionBarActivity.this,
                        CommentsActivity.class);
                intent.putExtra("id", WordPress.currentBlog.getId());
                intent.putExtra("isNew", true);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                editor.putInt("wp_pref_last_activity", COMMENTS_ACTIVITY);
                break;
            case STATS_ACTIVITY:
                if (!(WPActionBarActivity.this instanceof StatsActivity))
                    mShouldFinish = true;
                intent = new Intent(WPActionBarActivity.this,
                        StatsActivity.class);
                intent.putExtra("id", WordPress.currentBlog.getId());
                intent.putExtra("isNew", true);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                editor.putInt("wp_pref_last_activity", STATS_ACTIVITY);
                break;
            case QUICK_PHOTO_ACTIVITY:
                mShouldFinish = false;
                intent = new Intent(WPActionBarActivity.this,
                        EditPostActivity.class);
                intent.putExtra(
                        "quick-media",
                        DeviceUtils.hasCamera(getApplicationContext()) ? Constants.QUICK_POST_PHOTO_CAMERA
                                : Constants.QUICK_POST_PHOTO_LIBRARY);
                intent.putExtra("isNew", true);
                break;
            case QUICK_VIDEO_ACTIVITY:
                mShouldFinish = false;
                intent = new Intent(WPActionBarActivity.this,
                        EditPostActivity.class);
                intent.putExtra(
                        "quick-media",
                        DeviceUtils.hasCamera(getApplicationContext()) ? Constants.QUICK_POST_VIDEO_CAMERA
                                : Constants.QUICK_POST_VIDEO_LIBRARY);
                intent.putExtra("isNew", true);
                break;
            case VIEW_SITE_ACTIVITY:
                if (!(WPActionBarActivity.this instanceof ViewSiteActivity))
                    mShouldFinish = true;
                intent = new Intent(WPActionBarActivity.this,
                        ViewSiteActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                editor.putInt("wp_pref_last_activity", VIEW_SITE_ACTIVITY);
                break;
            case DASHBOARD_ACTIVITY:
                if (!(WPActionBarActivity.this instanceof DashboardActivity))
                    mShouldFinish = true;
                intent = new Intent(WPActionBarActivity.this,
                        DashboardActivity.class);
                intent.putExtra("loadAdmin", true);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                editor.putInt("wp_pref_last_activity", DASHBOARD_ACTIVITY);
                break;
            case SETTINGS_ACTIVITY:
                // Settings shouldn't be launched with a delay, or close the drawer
                mShouldFinish = false;
                Intent settingsIntent = new Intent(WPActionBarActivity.this,
                        PreferencesActivity.class);
                startActivityForResult(settingsIntent, SETTINGS_REQUEST);
                return;
            case REFRESH_MENU:
                if (!RefreshMenuTask.isUnedrTask()) {
                    new RefreshMenuTask(WPActionBarActivity.this)
                            .executeOnMultiThread(WordPress.currentBlog.getId());
                }
                selectedPosition = oldPosition;
                break;
            }

            editor.commit();
            if (intent != null) {
                mMenuDrawer.closeMenu();
                startActivityWithDelay(intent);
            }
        }
    };

    protected void startNewPost(int activityTag, String typeName) {
        switch (activityTag) {
        case POSTS_ACTIVITY:
            Intent i = new Intent(this, EditPostActivity.class);
            i.putExtra("id", WordPress.currentBlog.getId());
            i.putExtra("isNew", true);
            i.putExtra("fromList", false);
            startActivity(i);
            break;
        case PAGES_ACTIVITY:
            i = new Intent(this, EditPostActivity.class);
            i.putExtra("id", WordPress.currentBlog.getId());
            i.putExtra("isNew", true);
            i.putExtra("isPage", true);
            i.putExtra("fromList", false);
            startActivity(i);
            break;
        case CUSTOM_TYPE_ACTIVITY:
            mShouldFinish = true;
            Intent intent = new Intent(this, EditCustomTypePostActivity.class);
            intent.putExtra("id", WordPress.currentBlog.getId());
            intent.putExtra("isNew", true);
            intent.putExtra("type_name", typeName);
            intent.putExtra("fromList", false);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            mMenuDrawer.closeMenu();
            startActivityWithDelay(intent);
            break;
        default:
            break;
        }
    }

    /**
     * Called when the activity has detected the user's press of the back key.
     * If the activity has a menu drawer attached that is opened or in the
     * process of opening, the back button press closes it. Otherwise, the
     * normal back action is taken.
     */
    @Override
    public void onBackPressed() {
        if (mMenuDrawer != null) {
            final int drawerState = mMenuDrawer.getDrawerState();
            if (drawerState == MenuDrawer.STATE_OPEN
                    || drawerState == MenuDrawer.STATE_OPENING) {
                mMenuDrawer.closeMenu();
                return;
            }
        }

        super.onBackPressed();
    }

    /**
     * Get the names of all the blogs configured within the application. If a
     * blog does not have a specific name, the blog URL is returned.
     * 
     * @return array of blog names
     */
    private void updateBlogs() {
        List<Map<String, Object>> accounts = WordPress.wpDB.getAccounts();

        int blogCount = accounts.size();
        this.blogIDs = new int[blogCount];
        this.blogNames = new String[blogCount];

        for (int i = 0; i < blogCount; i++) {
            Map<String, Object> account = accounts.get(i);
            String name;
            if (account.get("blogName") != null) {
                name = EscapeUtils.unescapeHtml(account.get("blogName")
                        .toString());
            } else {
                name = account.get("url").toString();
            }
            blogNames[i] = name;
            blogIDs[i] = Integer.valueOf(account.get("id").toString());
        }
    }

    /**
     * Setup the global state tracking which blog is currently active.
     * <p>
     * If the global state is not already set, try and determine the last active
     * blog from the last time the application was used. If we're not able to
     * determine the last active blog, just select the first one.
     * <p>
     * If no blogs are configured, display the "new account" activity to allow
     * the user to setup a blog.
     */
    public void setupCurrentBlog() {
        Blog currentBlog = WordPress.getCurrentBlog();

        // no blogs are configured, so display new account activity
        if (currentBlog == null) {
            Log.d(TAG,
                    "No accounts configured.  Sending user to set up an account");
            mShouldFinish = false;
            Intent i = new Intent(this, NewAccountActivity.class);
            startActivityForResult(i, ADD_ACCOUNT_REQUEST);
            return;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
        case ADD_ACCOUNT_REQUEST: {
            if (resultCode == RESULT_OK) {
                // new blog has been added, so rebuild cache of blogs and
                // setup current blog
                updateBlogs();
                setupCurrentBlog();
                initMenuDrawer(false);
                mMenuDrawer.peekDrawer(0);
            } else {
                finish();
            }
            break;
        }
        case SETTINGS_REQUEST: {
            if (resultCode == RESULT_OK) {
                if (mMenuDrawer != null) {
                    updateMenuDrawer();
                    updateBlogs();
                    // If we need to add or remove the blog spinner, init the drawer again
                    if ((blogNames.length > 1 && mListView
                            .getHeaderViewsCount() == 0)
                            || blogNames.length == 1
                            && mListView.getHeaderViewsCount() > 0) {
                        this.initMenuDrawer(false);
                    } else if (blogNames.length > 1 && mBlogSpinner != null) {
                        SpinnerAdapter mSpinnerAdapter = getAdapter();
                        mBlogSpinner.setAdapter(mSpinnerAdapter);
                    }

                    if (blogNames.length >= 1) {
                        setupCurrentBlog();
                        onBlogChanged();
                    }
                }
            }
            break;
        }
        }
    }

    private IcsAdapterView.OnItemSelectedListener mItemSelectedListener = new IcsAdapterView.OnItemSelectedListener() {

        @Override
        public void onItemSelected(IcsAdapterView<?> arg0, View arg1,
                int position, long arg3) {
            // http://stackoverflow.com/questions/5624825/spinner-onitemselected-executes-when-it-is-not-suppose-to/5918177#5918177
            if (!mBlogSpinnerInitialized) {
                mBlogSpinnerInitialized = true;
            } else {
                WordPress.setCurrentBlog(blogIDs[position]);
                updateMenuDrawer();
                onBlogChanged();
            }
        }

        @Override
        public void onNothingSelected(IcsAdapterView<?> arg0) {
        }
    };

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            if (mMenuDrawer != null) {
                mMenuDrawer.toggleMenu();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This method is called when the user changes the active blog.
     */
    public void onBlogChanged() {
        WordPress.wpDB.updateLastBlogId(WordPress.currentBlog.getId());

        Blog currentBlog = WordPress.getCurrentBlog();
        if (currentBlog != null && mListView != null
                && mListView.getHeaderViewsCount() > 0) {
            for (int i = 0; i < blogIDs.length; i++) {
                if (blogIDs[i] == currentBlog.getId()) {
                    mBlogSpinner.setSelection(i);
                }
            }
        }
    }

    public void startAnimatingRefreshButton(MenuItem refreshItem) {
        if (refreshItem != null && !isAnimatingRefreshButton) {
            isAnimatingRefreshButton = true;
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            ImageView iv = (ImageView) inflater.inflate(getResources()
                    .getLayout(R.layout.menu_refresh_view), null);
            RotateAnimation anim = new RotateAnimation(0.0f, 360.0f,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f);
            anim.setInterpolator(new LinearInterpolator());
            anim.setRepeatCount(Animation.INFINITE);
            anim.setDuration(1400);
            iv.startAnimation(anim);
            refreshItem.setActionView(iv);
        }
    }

    public void stopAnimatingRefreshButton(MenuItem refreshItem) {
        isAnimatingRefreshButton = false;
        if (refreshItem != null && refreshItem.getActionView() != null) {
            refreshItem.getActionView().clearAnimation();
            refreshItem.setActionView(null);
        }
    }

    public void startAnimation() {

    }

    public void stopAnimation() {

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {

        if (mIsXLargeDevice) {
            if (mMenuDrawer != null) {
                // Re-attach the drawer if an XLarge device is rotated, so it can be static if in landscape
                View content = mMenuDrawer.getContentContainer().getChildAt(0);
                if (content != null) {
                    mMenuDrawer.getContentContainer().removeView(content);
                    mMenuDrawer = attachMenuDrawer();
                    mMenuDrawer.setContentView(content);
                    initMenuDrawer(true);

                    Blog currentBlog = WordPress.getCurrentBlog();
                    if (currentBlog != null && mListView != null
                            && mListView.getHeaderViewsCount() > 0) {
                        for (int i = 0; i < blogIDs.length; i++) {
                            if (blogIDs[i] == currentBlog.getId()) {
                                mBlogSpinner.setSelection(i);
                            }
                        }
                    }
                }
            }
        }

        super.onConfigurationChanged(newConfig);
    }
}
