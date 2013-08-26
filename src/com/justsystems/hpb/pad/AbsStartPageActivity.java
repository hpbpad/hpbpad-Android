package com.justsystems.hpb.pad;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ListView;

import com.actionbarsherlock.app.ActionBar;
import com.justsystems.hpb.pad.SlidingView.OnStateChengeListener;
import com.justsystems.hpb.pad.util.Debug;

import org.wordpress.android.Constants;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.task.MultiAsyncTask;
import org.wordpress.android.task.RefreshMenuTask;
import org.wordpress.android.ui.ViewSiteActivity;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.list.CustomPostTypePostsActivity;
import org.wordpress.android.ui.list.PagesActivity;
import org.wordpress.android.ui.list.PostsActivity;
import org.wordpress.android.ui.posts.EditCustomTypePostActivity;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.util.DeviceUtils;

public class AbsStartPageActivity extends WPActionBarActivity implements
        OnClickListener, OnStateChengeListener, OnPageChangeListener,
        OnGestureListener {

    private static final int WHAT_LOAD_CAPTURE = 1;
    private static final int WHAT_LOAD_CAPTURE_AND_TYPE = 2;

    /** 読み込みの遅延時間（長） */
    private static final int DELAY_LOADING_SHORT = 1000;
    /** 読み込みの遅延時間（短） */
    private static final int DELAY_LOADING_LONG = 3000;

    /** キャプチャの読み込み時に投稿タイプも読み込むか */
    private static final boolean LOAD_TYPE_WITH_CAPTURE = true;
    /** ドロワーを下ろすかのフラグ。起動時にドロワーが上から降りてくる際に利用する。 */
    private boolean shouldOpenDrawer = true;

    private SlidingView drawer;
    private WebView webView;
    private MyWebviewClient client;

    private ActionBarView barView;

    private ViewPager preview;
    private CustomFragmentStatePagerAdapter siteAdapter;

    private ViewPager postType;
    private PostTypePagerAdapter typeAdapter;
    private ImageView blogLeft;
    private ImageView blogRight;

    private String loadingUrl;

    private Handler loadPageTask;

    private ImageView list;
    private ImageView newPost;
    private ImageView camera;

    private GestureDetector detector;

    /** サイトのプレビューを更新するかのフラグ */
    private boolean refreshImageFlg;
    /** 投稿タイプのViewを更新するかのフラグ */
    private boolean typeUpdateFlg;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WPActionBarActivity.selectedPosition = -1;

        createMenuDrawer(R.layout.new_start_page);

        ListView listView = (ListView) findViewById(R.id.template);
        LayoutInflater inflater = getLayoutInflater();
        ImageView logo = (ImageView) inflater.inflate(R.layout.mp_logo, null);
        listView.addHeaderView(logo);
        listView.setAdapter(new MarketPlaceAdapter(this));

        this.drawer = (SlidingView) findViewById(R.id.slidingDrawer1);
        this.drawer.setOnStateChangeListener(this);

        this.preview = (ViewPager) findViewById(R.id.preview);
        this.preview.setOnTouchListener(new OnTouchListener() {
            private final GestureDetector tapGestureDetector = new GestureDetector(
                    AbsStartPageActivity.this,
                    new GestureDetector.SimpleOnGestureListener() {
                        @Override
                        public boolean onSingleTapConfirmed(MotionEvent e) {
                            startViewSite();
                            return true;
                        }
                    });

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                this.tapGestureDetector.onTouchEvent(event);
                return false;
            }
        });

        this.blogLeft = (ImageView) findViewById(R.id.type_left);
        this.blogLeft.setOnClickListener(this);
        this.blogRight = (ImageView) findViewById(R.id.type_right);
        this.blogRight.setOnClickListener(this);

        this.postType = (ViewPager) findViewById(R.id.postType);

        this.webView = (WebView) findViewById(R.id.webview);
        this.client = new MyWebviewClient(this);
        this.webView.setWebViewClient(this.client);
        final WebSettings settings = this.webView.getSettings();
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        this.list = (ImageView) findViewById(R.id.list);
        this.list.setOnClickListener(this);
        this.newPost = (ImageView) findViewById(R.id.newPost);
        this.newPost.setOnClickListener(this);
        this.camera = (ImageView) findViewById(R.id.quick_photo);
        this.camera.setOnClickListener(this);

        ActionBar bar = getSupportActionBar();
        bar.setDisplayShowTitleEnabled(false);
        bar.setDisplayShowCustomEnabled(true);
        bar.setBackgroundDrawable(new ColorDrawable(0xFF4A4848));

        //3.xと4.0はアニメーション関連の不具合があるので、特別に処理する
        if (DeviceUtils.isOverEqualThanHoneycomb()
                && DeviceUtils.isLessThanJB()) {
            this.barView = new ActionBarViewICS(this);
        } else {
            this.barView = new ActionBarView(this);
        }
        this.barView.setImageResource(R.drawable.ic_actionbar_logo);
        this.barView.setOnTouchListener(touchListener);
        ActionBar.LayoutParams params = new ActionBar.LayoutParams(
                ActionBar.LayoutParams.MATCH_PARENT,
                ActionBar.LayoutParams.MATCH_PARENT);
        final int margin = getResources().getDimensionPixelSize(
                R.dimen.startpage_actionbar_margin);
        params.rightMargin = margin;
        bar.setCustomView(this.barView, params);

        this.detector = new GestureDetector(this, this);
    }

    private void updateUI() {
        final Blog currentBlog = WordPress.getCurrentBlog();
        if (currentBlog == null) {
            return;
        }
        final int currentBlogId = currentBlog.getId();
        final int[] blogIDs = super.blogIDs;
        int currentblogIndex = -1;
        for (int i = 0; i < blogIDs.length; i++) {
            if (blogIDs[i] == currentBlogId) {
                currentblogIndex = i;
                break;
            }
        }
        this.siteAdapter = new CustomFragmentStatePagerAdapter(
                getSupportFragmentManager(), blogIDs);
        this.preview.setAdapter(this.siteAdapter);
        this.preview.setCurrentItem(currentblogIndex);
        if (currentblogIndex == 0) {
            blogLeft.setVisibility(View.GONE);
        }
        if (currentblogIndex == siteAdapter.getCount() - 1) {
            blogRight.setVisibility(View.GONE);
        }
        this.preview.setOnPageChangeListener(new OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                final int blogId = siteAdapter.getCurrentBlogId(position);
                WordPress.setCurrentBlog(blogId);
                onBlogChanged();
                getCurrentFragment().onSelected();
                if (position == 0) {
                    blogLeft.setVisibility(View.GONE);
                } else if (blogLeft.getVisibility() == View.GONE) {
                    blogLeft.setVisibility(View.VISIBLE);
                }
                if (position == siteAdapter.getCount() - 1) {
                    blogRight.setVisibility(View.GONE);
                } else if (blogRight.getVisibility() == View.GONE) {
                    blogRight.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
            }

            @Override
            public void onPageScrollStateChanged(int arg0) {
            }
        });

        this.typeAdapter = new PostTypePagerAdapter(this, currentBlog.getId());
        this.postType.setAdapter(this.typeAdapter);
        setTypeMargin();
        this.postType.setOnPageChangeListener(this);
        this.postType.setOffscreenPageLimit(5);
        this.postType.setCurrentItem(0);

        saveCaptureWithDelay(currentBlog.getHomeURL(), currentBlog.getId());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.webView.stopLoading();
        this.webView.setWebViewClient(null);
        ((ViewGroup) this.webView.getParent()).removeView(this.webView);
        this.webView.destroy();
        this.webView = null;
    }

    private SiteThumbnailFragment getCurrentFragment() {
        final int index = this.preview.getCurrentItem();
        return (SiteThumbnailFragment) this.siteAdapter.getItem(index);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && this.shouldOpenDrawer) {
            this.drawer.closeWitoutAnimation();
            this.drawer.setVisibility(View.VISIBLE);
            this.drawer.open();
            this.shouldOpenDrawer = false;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (!this.drawer.isOpen()) {
            // viewに設定したoffsetがviewPagerの更新によってリセットされる問題があるため回避
            this.drawer.openWitoutAnimation();
            onOpen();
        }
        super.onConfigurationChanged(newConfig);
        if (this.siteAdapter != null) {
            this.siteAdapter.onConfigurationChanged();
        }
        if (this.typeAdapter != null) {
            setTypeMargin();
        }
        this.barView.onConfigraionChanged();
    }

    @Override
    public void onClick(View v) {
        if (v == this.blogLeft) {
            final int current = this.preview.getCurrentItem();
            if (current > 0) {
                this.preview.setCurrentItem(current - 1);
            }
        } else if (v == this.blogRight) {
            final int current = this.preview.getCurrentItem();
            if (this.siteAdapter.getCount() > current + 1) {
                this.preview.setCurrentItem(current + 1);
            }
        } else if (v == this.list) {
            startList();
        } else if (v == this.newPost) {
            startNewPost();
        } else if (v == this.camera) {
            startCamera();
        }
    }

    @Override
    public void onBlogChanged() {
        if (!this.drawer.isOpen()) {
            // viewに設定したoffsetがviewPagerの更新によってリセットされる問題があるため回避
            this.drawer.openWitoutAnimation();
            onOpen();
        }
        super.onBlogChanged();
        if (this.client.saveCaptureTask != null) {
            this.client.saveCaptureTask.cancel(false);
        }
        this.webView.stopLoading();

        Handler task = this.client.captureDelayTask;
        if (task != null) {
            task.removeMessages(MyWebviewClient.WHAT_CAPTURE);
        }

        final Blog currentBlog = WordPress.getCurrentBlog();
        saveCaptureWithDelay(currentBlog.getHomeURL(), currentBlog.getId());

        this.typeAdapter.onBlogChanged(currentBlog.getId());
        this.postType.setCurrentItem(0, false);
    }

    @Override
    public void onOpen() {
        this.barView.moveLogoCenter();
        if (this.refreshImageFlg) {
            getCurrentFragment().refreshImage();
            this.refreshImageFlg = false;
        }
        if (typeUpdateFlg) {
            onTypeUpdated();
            this.typeUpdateFlg = false;
        }
    }

    @Override
    public void onClose() {
        this.barView.moveLogoRight();
    }

    @Override
    public void onPageScrollStateChanged(int arg0) {
    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {
    }

    @Override
    public void onPageSelected(int position) {
        this.typeAdapter.setSelected(position, true);

        this.typeAdapter.setSelected(position - 1, false);
        this.typeAdapter.setSelected(position + 1, false);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        Debug.logd("onSingleTapUp");
        if (!this.drawer.isOpen()) {
            this.drawer.open();
        }
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
            float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
            float velocityY) {
        Debug.logd("onFling");
        if (velocityY > 0 && velocityY > Math.abs(velocityX)) {
            final float difY = e2.getY() - e1.getY();
            final float distY = Math.abs(difY);
            final long time = e2.getEventTime() - e1.getEventTime();
            final float restY = drawer.getHeight() - distY;
            Debug.logd("restY" + restY + " time " + time + " distY" + distY);
            final int animationDuration = (int) (time * restY / distY);
            Debug.logd("animationDuration" + animationDuration);

            if (!this.drawer.isOpen()) {
                this.drawer.open(animationDuration);
            }
            return true;
        }
        return false;
    }

    private OnTouchListener touchListener = new OnTouchListener() {
        private int prevY;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (detector.onTouchEvent(event)) {
                return true;
            }

            if (drawer.isOpen()) {
                return true;
            }

            final int action = event.getAction();
            final int y = (int) event.getY();
            final int offset = y - prevY;

            final int top = drawer.getTop();
            final int height = drawer.getHeight();

            switch (action) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                if (y > barView.getBottom()) {
                    if (top + offset < 0) {
                        drawer.offsetTopAndBottom(offset);
                    } else {
                        drawer.offsetTopAndBottom(-top);
                    }
                    invalidateDrawer();

                    if (prevY < barView.getBottom()
                            && !DeviceUtils.isLessThanJB()) {
                        TranslateAnimation anim = new TranslateAnimation(0, 0,
                                0, 0);
                        anim.setDuration(10);
                        drawer.startAnimation(anim);
                    }
                } else {
                    if (top > -height) {
                        drawer.offsetTopAndBottom(-top - height);
                        invalidateDrawer();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (y > barView.getBottom() + height / 2) {
                    drawer.open();
                } else {
                    drawer.offsetTopAndBottom(-top - height);
                    invalidateDrawer();
                }
                break;
            default:
                break;
            }
            this.prevY = y;
            return true;
        }

        private void invalidateDrawer() {
            final int top = drawer.getTop();
            final int height = drawer.getHeight();
            drawer.invalidate(0, -top, drawer.getWidth(), height - top);
        }
    };

    public void onTypeUpdated() {
        if (!drawer.isOpen()) {
            // viewに設定したoffsetがviewPagerの更新によってリセットされる問題があるため回避
            this.typeUpdateFlg = true;
            return;
        }
        final Blog currentBlog = WordPress.getCurrentBlog();
        this.typeAdapter = new PostTypePagerAdapter(this, currentBlog.getId());
        setTypeMargin();
        this.postType.setAdapter(this.typeAdapter);
    }

    private void setTypeMargin() {
        final int viewWidth = SiteThumbnailFragment.getViewWidth(this, false);
        final int margin = viewWidth * 2 / 3;
        this.postType.setPageMargin(-margin);
        this.typeAdapter.setTextViewPadding(margin / 2);
    }

    private void saveCaptureWithDelay(final String url, final int blogId) {
        if (this.loadPageTask != null) {
            this.loadPageTask.removeMessages(WHAT_LOAD_CAPTURE);
            this.loadPageTask.removeMessages(WHAT_LOAD_CAPTURE_AND_TYPE);
        }
        this.loadPageTask = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                loadPageTask = null;
                switch (msg.what) {
                case WHAT_LOAD_CAPTURE_AND_TYPE:
                    Blog currentBlog = WordPress.getCurrentBlog();
                    if (!currentBlog.isDotcomFlag()) {
                        new RefreshMenuTask(AbsStartPageActivity.this)
                                .executeOnMultiThread(blogId);
                    }
                    // do not break
                default:
                    saveCaptureInner(url, blogId);
                    break;
                }

            }
        };

        File preview = new File(getFilesDir().getAbsolutePath()
                + File.separator + SiteThumbnailFragment.CAPTURE_DIRECTORY
                + File.separator + blogId + ".png");
        if (!LOAD_TYPE_WITH_CAPTURE && preview.exists()) {
            this.loadPageTask.sendEmptyMessageDelayed(WHAT_LOAD_CAPTURE,
                    DELAY_LOADING_LONG);
        } else {
            this.loadPageTask.sendEmptyMessageDelayed(
                    WHAT_LOAD_CAPTURE_AND_TYPE, DELAY_LOADING_SHORT);
        }
    }

    private void saveCaptureInner(String url, int blogId) {
        if (isFinishing()) {
            return;
        }
        this.client.loadingBlogId = blogId;
        this.client.hasError = false;
        this.loadingUrl = url;
        this.webView.loadUrl(url);
    }

    private void startViewSite() {
        Intent intent = new Intent(this, ViewSiteActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        finish();
    }

    private void startList() {
        final int selected = this.postType.getCurrentItem();
        final String postType = this.typeAdapter.getSelectedPostType(selected);
        if (postType.equals("post")) {
            Intent intent = new Intent(this, PostsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
        } else if (postType.equals("page")) {
            Intent intent = new Intent(this, PagesActivity.class);
            intent.putExtra("id", WordPress.currentBlog.getId());
            intent.putExtra("isNew", true);
            intent.putExtra("viewPages", true);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, CustomPostTypePostsActivity.class);
            intent.putExtra("id", WordPress.currentBlog.getId());
            intent.putExtra("isNew", true);
            intent.putExtra("type_name", postType);
            final String typeLabel = typeAdapter.getPageTitle(selected)
                    .toString();
            intent.putExtra("type_label", typeLabel);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
        }
        finish();
    }

    private void startNewPost() {
        final int selected = this.postType.getCurrentItem();
        final String postType = this.typeAdapter.getSelectedPostType(selected);
        if (postType.equals("post")) {
            startNewPost(POSTS_ACTIVITY, null);
        } else if (postType.equals("page")) {
            startNewPost(PAGES_ACTIVITY, null);
        } else {
            startNewPost(CUSTOM_TYPE_ACTIVITY, postType);
        }
        finish();
    }

    private void startCamera() {

        final int selected = this.postType.getCurrentItem();
        final String postType = this.typeAdapter.getSelectedPostType(selected);
        if (postType.equals("post")) {
            Intent intent = new Intent(this, EditPostActivity.class);
            intent.putExtra(
                    "quick-media",
                    DeviceUtils.hasCamera(getApplicationContext()) ? Constants.QUICK_POST_PHOTO_CAMERA
                            : Constants.QUICK_POST_PHOTO_LIBRARY);
            intent.putExtra("isNew", true);
            intent.putExtra("fromList", false);
            startActivity(intent);
        } else if (postType.equals("page")) {
            Intent intent = new Intent(this, EditPostActivity.class);
            intent.putExtra(
                    "quick-media",
                    DeviceUtils.hasCamera(getApplicationContext()) ? Constants.QUICK_POST_PHOTO_CAMERA
                            : Constants.QUICK_POST_PHOTO_LIBRARY);
            intent.putExtra("isNew", true);
            intent.putExtra("isPage", true);
            intent.putExtra("fromList", false);
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, EditCustomTypePostActivity.class);
            intent.putExtra(
                    "quick-media",
                    DeviceUtils.hasCamera(getApplicationContext()) ? Constants.QUICK_POST_PHOTO_CAMERA
                            : Constants.QUICK_POST_PHOTO_LIBRARY);
            intent.putExtra("isNew", true);
            intent.putExtra("fromList", false);
            intent.putExtra("type_name", postType);
            final String typeLabel = typeAdapter.getPageTitle(selected)
                    .toString();
            intent.putExtra("type_label", typeLabel);
            startActivity(intent);
        }
        finish();
    }

    private static final class SaveCaptureTask extends
            MultiAsyncTask<Picture, Picture, Bitmap> {
        private WeakReference<AbsStartPageActivity> activity;
        private WeakReference<SiteThumbnailFragment> fragment;
        private final int blogId;

        public SaveCaptureTask(AbsStartPageActivity activity, int blogId) {
            this.blogId = blogId;
            this.activity = new WeakReference<AbsStartPageActivity>(activity);
            SiteThumbnailFragment fragment = activity.getCurrentFragment();
            this.fragment = new WeakReference<SiteThumbnailFragment>(fragment);
        }

        @Override
        protected Bitmap doInBackground(Picture... params) {
            if (params == null || params.length == 0) {
                return null;
            }
            AbsStartPageActivity act = this.activity.get();
            if (act == null) {
                return null;
            }

            //そのままだと幅1024で保存され、読み込みの際にOOMを引き起こしやすくなる。
            //回避するために画面幅の正方形として保存する。
            final int width = DeviceUtils.getSmallestWidthPixcel(act
                    .getResources());

            Picture pic = params[0];
            Bitmap bmp = null;
            try {
                bmp = Bitmap
                        .createBitmap(width, width, Bitmap.Config.ARGB_8888);
            } catch (OutOfMemoryError e) {
                //GCしてもう一回トライする
                System.gc();
            }
            if (bmp == null) {
                try {
                    bmp = Bitmap.createBitmap(width, width,
                            Bitmap.Config.ARGB_8888);
                } catch (OutOfMemoryError e) {
                    return null;
                }
            }

            final float scale = width / (float) pic.getWidth();
            Canvas canvas = new Canvas(bmp);
            canvas.save();
            canvas.scale(scale, scale);
            pic.draw(canvas);
            canvas.restore();

            try {
                File dir = new File(act.getFilesDir().getAbsolutePath()
                        + File.separator
                        + SiteThumbnailFragment.CAPTURE_DIRECTORY);
                if (!dir.exists()) {
                    dir.mkdir();
                }

                final FileOutputStream out = new FileOutputStream(
                        dir.getAbsolutePath() + File.separator + this.blogId
                                + ".png");
                bmp.compress(CompressFormat.PNG, 100, out);
                out.close();
            } catch (FileNotFoundException e) {
                return null;
            } catch (IOException e) {
                return null;
            }

            return bmp;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            super.onPostExecute(result);
            if (!isCancelled() && result != null) {
                SiteThumbnailFragment frag = this.fragment.get();
                AbsStartPageActivity activity = this.activity.get();
                // viewに設定したoffsetがviewPagerの更新によってリセットされる問題があるため回避
                if (frag != null && activity != null
                        && activity.drawer.isOpen()) {
                    frag.refreshImage();
                } else if (activity != null) {
                    activity.refreshImageFlg = true;
                }
            }
        }
    }

    private static class MyWebviewClient extends WebViewClient {
        private static final int WHAT_CAPTURE = 1;

        private AbsStartPageActivity activity;

        private boolean hasError;

        private int loadingBlogId;

        private Handler captureDelayTask;

        private SaveCaptureTask saveCaptureTask;

        public MyWebviewClient(AbsStartPageActivity activity) {
            this.activity = activity;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode,
                String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            hasError = true;
        }

        @Override
        public void onPageFinished(final WebView view, String url) {
            super.onPageFinished(view, url);
            Handler task = activity.loadPageTask;
            if ((task != null && task.hasMessages(WHAT_LOAD_CAPTURE))
                    || hasError) {
                return;
            }

            // 念のためURLではじく。
            if (!url.equals(activity.loadingUrl)) {
                return;
            }

            if (this.captureDelayTask != null) {
                this.captureDelayTask.removeMessages(WHAT_CAPTURE);
            }
            this.captureDelayTask = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    if (activity == null) {
                        return;
                    }
                    final Picture pic = view.capturePicture();
                    if (pic == null) {
                        Debug.toast(activity,
                                "something was happened. picture null");
                        return;
                    } else if (pic.getWidth() <= 10 || pic.getHeight() <= 10) {
                        //404エラーの際にここにくることがある。
                        Debug.toast(activity,
                                "did not save width:" + pic.getWidth()
                                        + " height:" + pic.getHeight());
                        return;
                    }
                    saveCaptureTask = new SaveCaptureTask(activity,
                            loadingBlogId);
                    saveCaptureTask.executeOnMultiThread(pic);
                }
            };
            this.captureDelayTask.sendEmptyMessageDelayed(WHAT_CAPTURE, 1000);
        }
    }

}
