package org.wordpress.android.ui.posts;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.text.method.ArrowKeyMovementMethod;
import android.text.style.AlignmentSpan;
import android.text.style.QuoteSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.view.ContextMenu;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuInflater;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.justsystems.hpb.pad.R;
import com.justsystems.hpb.pad.seo.SeoResultActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.xmlrpc.android.ApiHelper;

import org.wordpress.android.Constants;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Postable;
import org.wordpress.android.ui.accounts.NewAccountActivity;
import org.wordpress.android.ui.list.AbsListActivity;
import org.wordpress.android.ui.list.PagesActivity;
import org.wordpress.android.util.DeviceUtils;
import org.wordpress.android.util.EscapeUtils;
import org.wordpress.android.util.ImageHelper;
import org.wordpress.android.util.PostUploadService;
import org.wordpress.android.util.StringHelper;
import org.wordpress.android.util.WPEditText;
import org.wordpress.android.util.WPHtml;
import org.wordpress.android.util.WPImageSpan;
import org.wordpress.android.util.WPUnderlineSpan;

public abstract class AbsEditActivity extends SherlockActivity implements
        OnClickListener, OnTouchListener, TextWatcher,
        WPEditText.OnSelectionChangedListener, OnFocusChangeListener,
        WPEditText.EditTextImeBackListener {

    private static final int AUTOSAVE_DELAY_MILLIS = 60000;

    protected static final int ACTIVITY_REQUEST_CODE_PICTURE_LIBRARY = 0;
    protected static final int ACTIVITY_REQUEST_CODE_TAKE_PHOTO = 1;
    protected static final int ACTIVITY_REQUEST_CODE_VIDEO_LIBRARY = 2;
    protected static final int ACTIVITY_REQUEST_CODE_TAKE_VIDEO = 3;
    protected static final int ACTIVITY_REQUEST_CODE_CREATE_LINK = 4;
    protected static final int ACTIVITY_REQUEST_CODE_SELECT_CATEGORIES = 5;

    private static final int ID_DIALOG_DATE = 0;
    private static final int ID_DIALOG_TIME = 1;
    private static final int ID_DIALOG_LOADING = 2;

    protected Blog mBlog;
    protected Postable mPostable;

    protected JSONArray mCategories;

    protected WPEditText mContentEditText;
    protected ImageButton mAddPictureButton;
    protected Spinner mStatusSpinner;
    protected EditText mTitleEditText, mPasswordEditText;
    protected TextView mLocationText, mCategoriesText, mPubDateText;
    private ToggleButton mBoldToggleButton, mEmToggleButton,
            mBquoteToggleButton;
    private ToggleButton mUnderlineToggleButton, mStrikeToggleButton;
    private Button mPubDateButton, mLinkButton, mMoreButton, seoButton;
    private RelativeLayout mFormatBar;

    private Handler mAutoSaveHandler;

    protected boolean mIsPage = false;
    protected boolean mIsNew = false;
    protected String typeName;
    protected boolean mLocalDraft = false;
    protected boolean mIsCustomPubDate = false;
    private boolean mIsBackspace = false;
    private boolean mScrollDetected = false;
    protected boolean mIsNewDraft = false;
    private boolean mIsExternalInstance = false;

    private String mAccountName = "";
    protected int mQuickMediaType = -1;
    private String mMediaCapturePath = "";

    protected String[] mPostFormats = null;
    private String[] mPostFormatTitles = null;

    protected int mBlogID = -1;
    protected long mPostID = -1;
    protected long mCustomPubDate = 0;

    private int mYear, mMonth, mDay, mHour, mMinute;
    private int mStyleStart, mSelectionStart, mSelectionEnd;
    private int mLastPosition = -1;
    private int mCurrentActivityRequest = -1;

    private float mLastYPos = 0;

    private boolean calledFromList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        Calendar c = Calendar.getInstance();
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);
        mHour = c.get(Calendar.HOUR_OF_DAY);
        mMinute = c.get(Calendar.MINUTE);
        mAutoSaveHandler = new Handler();

        String action = getIntent().getAction();
        if (Intent.ACTION_SEND.equals(action)
                || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            // we arrived here from a share action
            mIsExternalInstance = true;
            if (!selectBlogForShareAction())
                return;
        } else {
            initBlog();
            if (extras != null) {
                mAccountName = EscapeUtils.unescapeHtml(extras
                        .getString("accountName"));
                mPostID = extras.getLong("postID");
                mLocalDraft = extras.getBoolean("localDraft", false);
                mIsPage = extras.getBoolean("isPage", false);
                mIsNew = extras.getBoolean("isNew", false);
                calledFromList = extras.getBoolean("fromList", true);
                typeName = getIntent().getStringExtra("type_name");

                if (savedInstanceState != null) {
                    mCurrentActivityRequest = savedInstanceState
                            .getInt("currentActivityRequest");
                    if (savedInstanceState.getString("mediaCapturePath") != null)
                        mMediaCapturePath = savedInstanceState
                                .getString("mediaCapturePath");
                } else {
                    mQuickMediaType = extras.getInt("quick-media", -1);
                }

                if (extras.getBoolean("isQuickPress")) {
                    mIsExternalInstance = true;
                    mBlogID = extras.getInt("id");
                } else {
                    mBlogID = WordPress.currentBlog.getId();
                }

                try {
                    mBlog = new Blog(mBlogID);
                    WordPress.currentBlog = mBlog;
                } catch (Exception e) {
                    showBlogErrorAndFinish();
                    return;
                }

                if (!mIsNew) {
                    try {
                        mPostable = createPost();
                        if (mPostable == null) {
                            // big oopsie
                            Toast.makeText(
                                    this,
                                    getResources().getText(
                                            R.string.post_not_found),
                                    Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        } else {
                            WordPress.setCurrentPost(mPostable);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        finish();
                    }
                }
            }

            if (mIsNew) {
                mLocalDraft = true;
                setTitle(EscapeUtils.unescapeHtml(WordPress.currentBlog
                        .getBlogName())
                        + " - "
                        + getString((mIsPage) ? R.string.new_page
                                : R.string.new_post));
            } else {
                setTitle(EscapeUtils.unescapeHtml(WordPress.currentBlog
                        .getBlogName())
                        + " - "
                        + getString((mIsPage) ? R.string.edit_page
                                : R.string.edit_post));
            }
        }

        setContentView(R.layout.edit);
        mContentEditText = (WPEditText) findViewById(R.id.postContent);
        mTitleEditText = (EditText) findViewById(R.id.title);
        mPasswordEditText = (EditText) findViewById(R.id.post_password);
        mLocationText = (TextView) findViewById(R.id.locationText);
        mBoldToggleButton = (ToggleButton) findViewById(R.id.bold);
        mEmToggleButton = (ToggleButton) findViewById(R.id.em);
        mBquoteToggleButton = (ToggleButton) findViewById(R.id.bquote);
        mUnderlineToggleButton = (ToggleButton) findViewById(R.id.underline);
        mStrikeToggleButton = (ToggleButton) findViewById(R.id.strike);
        mCategoriesText = (TextView) findViewById(R.id.selectedCategories);
        mAddPictureButton = (ImageButton) findViewById(R.id.addPictureButton);
        mPubDateButton = (Button) findViewById(R.id.pubDateButton);
        mPubDateText = (TextView) findViewById(R.id.pubDate);
        seoButton = (Button) findViewById(R.id.seoButton);

        mLinkButton = (Button) findViewById(R.id.link);
        mMoreButton = (Button) findViewById(R.id.more);
        mStatusSpinner = (Spinner) findViewById(R.id.status);
        mFormatBar = (RelativeLayout) findViewById(R.id.formatBar);

        // Set header labels to upper case
        ((TextView) findViewById(R.id.statusLabel)).setText(getResources()
                .getString(R.string.status).toUpperCase());
        ((TextView) findViewById(R.id.postFormatLabel)).setText(getResources()
                .getString(R.string.post_format).toUpperCase());
        ((TextView) findViewById(R.id.pubDateLabel)).setText(getResources()
                .getString(R.string.publish_date).toUpperCase());

        if (mIsPage) { // remove post specific views
            ((LinearLayout) findViewById(R.id.section2))
                    .setVisibility(View.GONE);
            ((RelativeLayout) findViewById(R.id.section3))
                    .setVisibility(View.GONE);
            ((TextView) findViewById(R.id.postFormatLabel))
                    .setVisibility(View.GONE);
            ((Spinner) findViewById(R.id.postFormat)).setVisibility(View.GONE);
        } else {
            if (mBlog.getPostFormats().equals("")) {
                List<Object> args = new Vector<Object>();
                args.add(mBlog);
                args.add(this);
                new ApiHelper.getPostFormatsTask().execute(args);
                mPostFormatTitles = getResources().getStringArray(
                        R.array.post_formats_array);
                String defaultPostFormatTitles[] = { "aside", "audio", "chat",
                        "gallery", "image", "link", "quote", "standard",
                        "status", "video" };
                mPostFormats = defaultPostFormatTitles;
            } else {
                try {
                    Gson gson = new Gson();
                    Type type = new TypeToken<Map<String, String>>() {
                    }.getType();
                    Map<String, String> jsonPostFormats = gson.fromJson(
                            mBlog.getPostFormats(), type);
                    mPostFormats = new String[jsonPostFormats.size()];
                    mPostFormatTitles = new String[jsonPostFormats.size()];
                    int i = 0;
                    for (Map.Entry<String, String> entry : jsonPostFormats
                            .entrySet()) {
                        String key = entry.getKey();
                        String val = entry.getValue();
                        mPostFormats[i] = key;
                        mPostFormatTitles[i] = val;
                        i++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Spinner pfSpinner = (Spinner) findViewById(R.id.postFormat);
            ArrayAdapter<String> pfAdapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_spinner_item, mPostFormatTitles);
            pfAdapter
                    .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            pfSpinner.setAdapter(pfAdapter);
            String activePostFormat = "standard";
            if (!mIsNew) {
                try {
                    if (!mPostable.getPostFormat().equals(""))
                        activePostFormat = mPostable.getPostFormat();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            for (int i = 0; i < mPostFormats.length; i++) {
                if (mPostFormats[i].equals(activePostFormat))
                    pfSpinner.setSelection(i);
            }

            if (Intent.ACTION_SEND.equals(action)
                    || Intent.ACTION_SEND_MULTIPLE.equals(action))
                setContent();
        }

        String[] items = new String[] {
                getResources().getString(R.string.publish_post),
                getResources().getString(R.string.draft),
                getResources().getString(R.string.pending_review),
                getResources().getString(R.string.post_private),
                getResources().getString(R.string.local_draft) };

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mStatusSpinner.setAdapter(adapter);

        if (mIsNew) {
            if (mQuickMediaType >= 0) {
                // User selected a 'Quick (media type)' option in the menu
                // drawer
                if (mQuickMediaType == Constants.QUICK_POST_PHOTO_CAMERA)
                    launchCamera();
                else if (mQuickMediaType == Constants.QUICK_POST_PHOTO_LIBRARY)
                    launchPictureLibrary();
                else if (mQuickMediaType == Constants.QUICK_POST_VIDEO_CAMERA)
                    launchVideoCamera();
                else if (mQuickMediaType == Constants.QUICK_POST_VIDEO_LIBRARY)
                    launchVideoLibrary();
                mLocalDraft = extras.getBoolean("localDraft");
            }
        } else {
            mTitleEditText.setText(mPostable.getTitle());

            if (mPostable.isUploaded()) {
                items = new String[] {
                        getResources().getString(R.string.publish_post),
                        getResources().getString(R.string.draft),
                        getResources().getString(R.string.pending_review),
                        getResources().getString(R.string.post_private) };
                adapter = new ArrayAdapter<String>(this,
                        android.R.layout.simple_spinner_item, items);
                mStatusSpinner.setAdapter(adapter);
            }

            String contentHTML = getContentHTML(mPostable);

            try {
                if (mPostable.isLocalDraft())
                    mContentEditText.setText(WPHtml.fromHtml(
                            contentHTML.replaceAll("\uFFFC", ""),
                            AbsEditActivity.this, mPostable));
                else
                    mContentEditText.setText(contentHTML.replaceAll("\uFFFC",
                            ""));
            } catch (Exception e) {
                e.printStackTrace();
            }

            long pubDate = mPostable.getDate_created_gmt();
            if (pubDate != 0) {
                try {
                    int flags = 0;
                    flags |= android.text.format.DateUtils.FORMAT_SHOW_DATE;
                    flags |= android.text.format.DateUtils.FORMAT_ABBREV_MONTH;
                    flags |= android.text.format.DateUtils.FORMAT_SHOW_YEAR;
                    flags |= android.text.format.DateUtils.FORMAT_SHOW_TIME;
                    String formattedDate = DateUtils.formatDateTime(
                            AbsEditActivity.this, pubDate, flags);
                    mPubDateText.setText(formattedDate);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (mPostable.getPassword() != null)
                mPasswordEditText.setText(mPostable.getPassword());

            if (mPostable.getPostStatus() != null) {
                String status = mPostable.getPostStatus();

                if (status.equals("publish")) {
                    mStatusSpinner.setSelection(0, true);
                } else if (status.equals("draft")) {
                    mStatusSpinner.setSelection(1, true);
                } else if (status.equals("pending")) {
                    mStatusSpinner.setSelection(2, true);
                } else if (status.equals("private")) {
                    mStatusSpinner.setSelection(3, true);
                } else if (status.equals("localdraft")) {
                    mStatusSpinner.setSelection(4, true);
                }
            }

        }

        if (!mIsPage) {
            Button selectCategories = (Button) findViewById(R.id.selectCategories);
            selectCategories.setOnClickListener(this);
        }

        if (!SeoResultActivity.SHOW_SEO) {
            RelativeLayout seoWrapper = ((RelativeLayout) findViewById(R.id.section_seo));
            final int count = seoWrapper.getChildCount();
            for (int i = 0; i < count; i++) {
                View v = seoWrapper.getChildAt(i);
                v.setVisibility(View.GONE);
            }
        }

        registerForContextMenu(mAddPictureButton);
        mContentEditText.setOnSelectionChangedListener(this);
        mContentEditText.setOnEditTextImeBackListener(this);
        mContentEditText.setOnTouchListener(this);
        mContentEditText.addTextChangedListener(this);
        mContentEditText.setOnFocusChangeListener(this);
        mAddPictureButton.setOnClickListener(this);
        mPubDateButton.setOnClickListener(this);
        seoButton.setOnClickListener(this);
        mBoldToggleButton.setOnClickListener(this);
        mLinkButton.setOnClickListener(this);
        mEmToggleButton.setOnClickListener(this);
        mUnderlineToggleButton.setOnClickListener(this);
        mStrikeToggleButton.setOnClickListener(this);
        mBquoteToggleButton.setOnClickListener(this);
        mMoreButton.setOnClickListener(this);
    }

    protected abstract Postable createPost();

    protected abstract String getContentHTML(Postable post);

    @Override
    protected void onResume() {
        super.onResume();
        if (mAutoSaveHandler != null)
            mAutoSaveHandler.postDelayed(autoSaveRunnable, 60000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAutoSaveHandler != null)
            mAutoSaveHandler.removeCallbacks(autoSaveRunnable);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt("currentActivityRequest",
                mCurrentActivityRequest);
        if (!mMediaCapturePath.equals(""))
            savedInstanceState.putString("mediaCapturePath", mMediaCapturePath);
    }

    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenu.ContextMenuInfo menuInfo) {
        menu.add(0, 0, 0, getResources().getText(R.string.select_photo));
        if (DeviceUtils.hasCamera(getApplicationContext())) {
            menu.add(0, 1, 0, getResources().getText(R.string.take_photo));
        }
        menu.add(0, 2, 0, getResources().getText(R.string.select_video));
        if (DeviceUtils.hasCamera(getApplicationContext())) {
            menu.add(0, 3, 0, getResources().getText(R.string.take_video));
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case 0:
            launchPictureLibrary();
            return true;
        case 1:
            launchCamera();
            return true;
        case 2:
            launchVideoLibrary();
            return true;
        case 3:
            launchVideoCamera();
            return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.edit, menu);
        return true;
    }

    // Menu actions
    @Override
    public boolean onOptionsItemSelected(
            com.actionbarsherlock.view.MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_edit_post) {
            if (mAutoSaveHandler != null)
                mAutoSaveHandler.removeCallbacks(autoSaveRunnable);
            if (savePost(false)) {
                if (mPostable.isUploaded()
                        || !mPostable.getPostStatus().equals("localdraft")) {

                    preparePost();
                    startService(new Intent(this, PostUploadService.class));
                }
                if (!calledFromList) {
                    startListActivity();
                } else {
                    Intent i = new Intent();
                    i.putExtra("shouldRefresh", true);
                    setResult(RESULT_OK, i);
                }
                finish();
            }
            return true;
        } else if (itemId == android.R.id.home) {
            showCancelAlert(true);
            return true;
        }
        return false;
    }

    protected abstract void startListActivity();

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus && mFormatBar.getVisibility() != View.VISIBLE)
            showFormatBar();
        else if (!hasFocus && mFormatBar.getVisibility() == View.VISIBLE)
            hideFormatBar();
    }

    @Override
    public void onImeBack(WPEditText ctrl, String text) {
        if (mFormatBar.getVisibility() == View.VISIBLE)
            hideFormatBar();
    }

    private void showFormatBar() {
        mFormatBar.setVisibility(View.VISIBLE);
        AlphaAnimation fadeInAnimation = new AlphaAnimation(0.0f, 1.0f);
        fadeInAnimation.setDuration(500);
        mFormatBar.startAnimation(fadeInAnimation);
    }

    private void hideFormatBar() {
        AlphaAnimation fadeOutAnimation = new AlphaAnimation(1.0f, 0.0f);
        fadeOutAnimation.setDuration(500);
        mFormatBar.startAnimation(fadeOutAnimation);
        mFormatBar.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.bold) {
            formatBtnClick(mBoldToggleButton, "strong");
        } else if (id == R.id.em) {
            formatBtnClick(mEmToggleButton, "em");
        } else if (id == R.id.underline) {
            formatBtnClick(mUnderlineToggleButton, "u");
        } else if (id == R.id.strike) {
            formatBtnClick(mStrikeToggleButton, "strike");
        } else if (id == R.id.bquote) {
            formatBtnClick(mBquoteToggleButton, "blockquote");
        } else if (id == R.id.more) {
            mSelectionEnd = mContentEditText.getSelectionEnd();
            Editable str = mContentEditText.getText();
            str.insert(mSelectionEnd, "\n<!--more-->\n");
        } else if (id == R.id.link) {
            mSelectionStart = mContentEditText.getSelectionStart();
            mStyleStart = mSelectionStart;
            mSelectionEnd = mContentEditText.getSelectionEnd();
            if (mSelectionStart > mSelectionEnd) {
                int temp = mSelectionEnd;
                mSelectionEnd = mSelectionStart;
                mSelectionStart = temp;
            }
            Intent i = new Intent(AbsEditActivity.this, EditLinkActivity.class);
            if (mSelectionEnd > mSelectionStart) {
                String selectedText = mContentEditText.getText()
                        .subSequence(mSelectionStart, mSelectionEnd).toString();
                i.putExtra("selectedText", selectedText);
            }
            startActivityForResult(i, ACTIVITY_REQUEST_CODE_CREATE_LINK);
        } else if (id == R.id.addPictureButton) {
            mAddPictureButton.performLongClick();
        } else if (id == R.id.pubDateButton) {
            showDialog(ID_DIALOG_DATE);
        } else if (id == R.id.post) {
            if (mAutoSaveHandler != null)
                mAutoSaveHandler.removeCallbacks(autoSaveRunnable);
            if (savePost(false)) {
                if (mPostable.isUploaded()
                        || !mPostable.getPostStatus().equals("localdraft")) {
                    preparePost();
                    startService(new Intent(this, PostUploadService.class));
                }
                finish();
            }
        } else if (id == R.id.selectCategories) {
            startCategoryActivity();
        } else if (id == R.id.seoButton) {
            goToSeo();
        }
    }

    protected abstract void startCategoryActivity();

    protected abstract void preparePost();

    private void goToSeo() {
        Intent i = new Intent(this, SeoResultActivity.class);
        startActivity(i);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        if (mFormatBar.getVisibility() != View.VISIBLE)
            showFormatBar();

        float pos = event.getY();

        if (event.getAction() == 0)
            mLastYPos = pos;

        if (event.getAction() > 1) {
            if (((mLastYPos - pos) > 2.0f) || ((pos - mLastYPos) > 2.0f))
                mScrollDetected = true;
        }

        mLastYPos = pos;

        if (event.getAction() == 1 && !mScrollDetected) {
            Layout layout = ((TextView) v).getLayout();
            int x = (int) event.getX();
            int y = (int) event.getY();

            x += v.getScrollX();
            y += v.getScrollY();
            if (layout != null) {
                int line = layout.getLineForVertical(y);
                int charPosition = layout.getOffsetForHorizontal(line, x);

                final Spannable s = mContentEditText.getText();
                // check if image span was tapped
                WPImageSpan[] image_spans = s.getSpans(charPosition,
                        charPosition, WPImageSpan.class);

                if (image_spans.length != 0) {
                    final WPImageSpan span = image_spans[0];
                    if (!span.isVideo()) {
                        LayoutInflater factory = LayoutInflater
                                .from(AbsEditActivity.this);
                        final View alertView = factory.inflate(
                                R.layout.alert_image_options, null);
                        final TextView imageWidthText = (TextView) alertView
                                .findViewById(R.id.imageWidthText);
                        final EditText titleText = (EditText) alertView
                                .findViewById(R.id.title);
                        // final EditText descText = (EditText)
                        // alertView.findViewById(R.id.description);
                        final EditText caption = (EditText) alertView
                                .findViewById(R.id.caption);
                        final CheckBox featuredCheckBox = (CheckBox) alertView
                                .findViewById(R.id.featuredImage);
                        final CheckBox featuredInPostCheckBox = (CheckBox) alertView
                                .findViewById(R.id.featuredInPost);

                        // show featured image checkboxes if theme support it
                        if (WordPress.currentBlog.isFeaturedImageCapable()) {
                            featuredCheckBox.setVisibility(View.VISIBLE);
                            featuredInPostCheckBox.setVisibility(View.VISIBLE);
                        }

                        featuredCheckBox
                                .setOnCheckedChangeListener(new OnCheckedChangeListener() {
                                    @Override
                                    public void onCheckedChanged(
                                            CompoundButton buttonView,
                                            boolean isChecked) {
                                        if (isChecked) {
                                            featuredInPostCheckBox
                                                    .setVisibility(View.VISIBLE);
                                        } else {
                                            featuredInPostCheckBox
                                                    .setVisibility(View.GONE);
                                        }

                                    }
                                });

                        final SeekBar seekBar = (SeekBar) alertView
                                .findViewById(R.id.imageWidth);
                        final Spinner alignmentSpinner = (Spinner) alertView
                                .findViewById(R.id.alignment_spinner);
                        ArrayAdapter<CharSequence> adapter = ArrayAdapter
                                .createFromResource(AbsEditActivity.this,
                                        R.array.alignment_array,
                                        android.R.layout.simple_spinner_item);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        alignmentSpinner.setAdapter(adapter);

                        imageWidthText.setText(String.valueOf(span.getWidth())
                                + "px");
                        seekBar.setProgress(span.getWidth());
                        titleText.setText(span.getTitle());
                        // descText.setText(span.getDescription());
                        caption.setText(span.getCaption());
                        featuredCheckBox.setChecked(span.isFeatured());

                        if (span.isFeatured())
                            featuredInPostCheckBox.setVisibility(View.VISIBLE);
                        else
                            featuredInPostCheckBox.setVisibility(View.GONE);

                        featuredInPostCheckBox.setChecked(span
                                .isFeaturedInPost());

                        alignmentSpinner.setSelection(
                                span.getHorizontalAlignment(), true);

                        seekBar.setMax(100);
                        if (span.getWidth() != 0)
                            seekBar.setProgress(span.getWidth() / 10);
                        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

                            @Override
                            public void onStopTrackingTouch(SeekBar seekBar) {
                            }

                            @Override
                            public void onStartTrackingTouch(SeekBar seekBar) {
                            }

                            @Override
                            public void onProgressChanged(SeekBar seekBar,
                                    int progress, boolean fromUser) {
                                if (progress == 0)
                                    progress = 1;
                                imageWidthText.setText(progress * 10 + "px");
                            }
                        });

                        AlertDialog ad = new AlertDialog.Builder(
                                AbsEditActivity.this)
                                .setTitle(getString(R.string.image_settings))
                                .setView(alertView)
                                .setPositiveButton(getString(R.string.ok),
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(
                                                    DialogInterface dialog,
                                                    int whichButton) {

                                                span.setTitle(titleText
                                                        .getText().toString());
                                                // span.setDescription(descText.getText().toString());
                                                span.setHorizontalAlignment(alignmentSpinner
                                                        .getSelectedItemPosition());
                                                span.setWidth(seekBar
                                                        .getProgress() * 10);
                                                span.setCaption(caption
                                                        .getText().toString());
                                                span.setFeatured(featuredCheckBox
                                                        .isChecked());
                                                if (featuredCheckBox
                                                        .isChecked()) {
                                                    // remove featured flag from
                                                    // all
                                                    // other images
                                                    WPImageSpan[] click_spans = s.getSpans(
                                                            0, s.length(),
                                                            WPImageSpan.class);
                                                    if (click_spans.length > 1) {
                                                        for (int i = 0; i < click_spans.length; i++) {
                                                            WPImageSpan verifySpan = click_spans[i];
                                                            if (verifySpan != span) {
                                                                verifySpan
                                                                        .setFeatured(false);
                                                                verifySpan
                                                                        .setFeaturedInPost(false);
                                                            }
                                                        }
                                                    }
                                                }
                                                span.setFeaturedInPost(featuredInPostCheckBox
                                                        .isChecked());
                                            }
                                        })
                                .setNegativeButton(getString(R.string.cancel),
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(
                                                    DialogInterface dialog,
                                                    int whichButton) {
                                                dialog.dismiss();
                                            }
                                        }).create();
                        ad.getWindow()
                                .setSoftInputMode(
                                        WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
                        ad.show();
                        mScrollDetected = false;
                        return true;
                    }

                } else {
                    mContentEditText.setMovementMethod(ArrowKeyMovementMethod
                            .getInstance());
                    mContentEditText.setSelection(mContentEditText
                            .getSelectionStart());
                }
            }
        } else if (event.getAction() == 1) {
            mScrollDetected = false;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        showCancelAlert(false);
    }

    private void showCancelAlert(final boolean isUpPress) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                AbsEditActivity.this);
        dialogBuilder.setTitle(getResources().getText(R.string.cancel_edit));
        dialogBuilder.setMessage(getResources().getText(
                (mIsPage) ? R.string.sure_to_cancel_edit_page
                        : R.string.sure_to_cancel_edit));
        dialogBuilder.setPositiveButton(getResources().getText(R.string.yes),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (mIsNewDraft)
                            mPostable.delete();
                        if (isUpPress && mIsExternalInstance) {
                            Intent intent = new Intent(AbsEditActivity.this,
                                    (mIsPage) ? PagesActivity.class
                                            : AbsListActivity.class);
                            if (mIsPage)
                                intent.putExtra("viewPages", true);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                        } else {
                            if (!calledFromList) {
                                startListActivity();
                            } else {
                                Bundle bundle = new Bundle();
                                bundle.putString("returnStatus", "CANCEL");
                                Intent mIntent = new Intent();
                                mIntent.putExtras(bundle);
                                setResult(RESULT_OK, mIntent);
                            }
                        }
                        finish();
                    }
                });
        dialogBuilder.setNegativeButton(getResources().getText(R.string.no),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                });
        dialogBuilder.setCancelable(true);
        dialogBuilder.create().show();
    }

    private void initBlog() {
        if (WordPress.getCurrentBlog() == null)
            showBlogErrorAndFinish();
    }

    private boolean selectBlogForShareAction() {

        mIsNew = true;
        mLocalDraft = true;

        List<Map<String, Object>> accounts = WordPress.wpDB.getAccounts();

        if (accounts.size() > 0) {

            final String blogNames[] = new String[accounts.size()];
            final int accountIDs[] = new int[accounts.size()];

            for (int i = 0; i < accounts.size(); i++) {

                Map<String, Object> curHash = accounts.get(i);
                try {
                    blogNames[i] = EscapeUtils.unescapeHtml(curHash.get(
                            "blogName").toString());
                } catch (Exception e) {
                    blogNames[i] = curHash.get("url").toString();
                }
                accountIDs[i] = (Integer) curHash.get("id");
                try {
                    mBlog = new Blog(accountIDs[i]);
                } catch (Exception e) {
                    showBlogErrorAndFinish();
                    return false;
                }
            }

            // Don't prompt if they have one blog only
            if (accounts.size() > 1) {
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        AbsEditActivity.this);
                builder.setCancelable(false);
                builder.setTitle(getResources().getText(R.string.select_a_blog));
                builder.setItems(blogNames,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                mBlogID = accountIDs[item];
                                try {
                                    mBlog = new Blog(mBlogID);
                                } catch (Exception e) {
                                    showBlogErrorAndFinish();
                                }
                                WordPress.currentBlog = mBlog;
                                WordPress.wpDB
                                        .updateLastBlogId(WordPress.currentBlog
                                                .getId());
                                mAccountName = blogNames[item];
                                setTitle(EscapeUtils.unescapeHtml(mAccountName)
                                        + " - "
                                        + getResources().getText(
                                                (mIsPage) ? R.string.new_page
                                                        : R.string.new_post));
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            } else {
                mBlogID = accountIDs[0];
                try {
                    mBlog = new Blog(mBlogID);
                } catch (Exception e) {
                    showBlogErrorAndFinish();
                    return false;
                }
                WordPress.currentBlog = mBlog;
                WordPress.wpDB.updateLastBlogId(WordPress.currentBlog.getId());
                mAccountName = blogNames[0];
                setTitle(EscapeUtils.unescapeHtml(mAccountName)
                        + " - "
                        + getResources().getText(
                                (mIsPage) ? R.string.new_page
                                        : R.string.new_post));
            }
            ;
            return true;
        } else {
            // no account, load main view to load new account view
            Toast.makeText(getApplicationContext(),
                    getResources().getText(R.string.no_account),
                    Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, NewAccountActivity.class));
            finish();
            return false;
        }
    }

    private void showBlogErrorAndFinish() {
        Toast.makeText(this, getResources().getText(R.string.blog_not_found),
                Toast.LENGTH_SHORT).show();
        finish();
    }

    private void formatBtnClick(ToggleButton toggleButton, String tag) {
        try {
            Spannable s = mContentEditText.getText();
            int selectionStart = mContentEditText.getSelectionStart();
            mStyleStart = selectionStart;
            int selectionEnd = mContentEditText.getSelectionEnd();

            if (selectionStart > selectionEnd) {
                int temp = selectionEnd;
                selectionEnd = selectionStart;
                selectionStart = temp;
            }

            if (mLocalDraft) {
                if (selectionEnd > selectionStart) {
                    Spannable str = mContentEditText.getText();
                    if (tag.equals("strong")) {
                        StyleSpan[] ss = str.getSpans(selectionStart,
                                selectionEnd, StyleSpan.class);

                        boolean exists = false;
                        for (int i = 0; i < ss.length; i++) {
                            int style = ((StyleSpan) ss[i]).getStyle();
                            if (style == android.graphics.Typeface.BOLD) {
                                str.removeSpan(ss[i]);
                                exists = true;
                            }
                        }

                        if (!exists) {
                            str.setSpan(new StyleSpan(
                                    android.graphics.Typeface.BOLD),
                                    selectionStart, selectionEnd,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        toggleButton.setChecked(false);
                    } else if (tag.equals("em")) {
                        StyleSpan[] ss = str.getSpans(selectionStart,
                                selectionEnd, StyleSpan.class);

                        boolean exists = false;
                        for (int i = 0; i < ss.length; i++) {
                            int style = ((StyleSpan) ss[i]).getStyle();
                            if (style == android.graphics.Typeface.ITALIC) {
                                str.removeSpan(ss[i]);
                                exists = true;
                            }
                        }

                        if (!exists) {
                            str.setSpan(new StyleSpan(
                                    android.graphics.Typeface.ITALIC),
                                    selectionStart, selectionEnd,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        toggleButton.setChecked(false);
                    } else if (tag.equals("u")) {

                        WPUnderlineSpan[] ss = str.getSpans(selectionStart,
                                selectionEnd, WPUnderlineSpan.class);

                        boolean exists = false;
                        for (int i = 0; i < ss.length; i++) {
                            str.removeSpan(ss[i]);
                            exists = true;
                        }

                        if (!exists) {
                            str.setSpan(new WPUnderlineSpan(), selectionStart,
                                    selectionEnd,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }

                        toggleButton.setChecked(false);
                    } else if (tag.equals("strike")) {

                        StrikethroughSpan[] ss = str.getSpans(selectionStart,
                                selectionEnd, StrikethroughSpan.class);

                        boolean exists = false;
                        for (int i = 0; i < ss.length; i++) {
                            str.removeSpan(ss[i]);
                            exists = true;
                        }

                        if (!exists) {
                            str.setSpan(new StrikethroughSpan(),
                                    selectionStart, selectionEnd,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }

                        toggleButton.setChecked(false);
                    } else if (tag.equals("blockquote")) {

                        QuoteSpan[] ss = str.getSpans(selectionStart,
                                selectionEnd, QuoteSpan.class);

                        boolean exists = false;
                        for (int i = 0; i < ss.length; i++) {
                            str.removeSpan(ss[i]);
                            exists = true;
                        }

                        if (!exists) {
                            str.setSpan(new QuoteSpan(), selectionStart,
                                    selectionEnd,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }

                        toggleButton.setChecked(false);
                    }
                } else if (!toggleButton.isChecked()) {

                    if (tag.equals("strong") || tag.equals("em")) {

                        StyleSpan[] ss = s.getSpans(mStyleStart - 1,
                                mStyleStart, StyleSpan.class);

                        for (int i = 0; i < ss.length; i++) {
                            int tagStart = s.getSpanStart(ss[i]);
                            int tagEnd = s.getSpanEnd(ss[i]);
                            if (ss[i].getStyle() == android.graphics.Typeface.BOLD
                                    && tag.equals("strong")) {
                                tagStart = s.getSpanStart(ss[i]);
                                tagEnd = s.getSpanEnd(ss[i]);
                                s.removeSpan(ss[i]);
                                s.setSpan(new StyleSpan(
                                        android.graphics.Typeface.BOLD),
                                        tagStart, tagEnd,
                                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                            if (ss[i].getStyle() == android.graphics.Typeface.ITALIC
                                    && tag.equals("em")) {
                                tagStart = s.getSpanStart(ss[i]);
                                tagEnd = s.getSpanEnd(ss[i]);
                                s.removeSpan(ss[i]);
                                s.setSpan(new StyleSpan(
                                        android.graphics.Typeface.ITALIC),
                                        tagStart, tagEnd,
                                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                        }
                    } else if (tag.equals("u")) {
                        WPUnderlineSpan[] us = s.getSpans(mStyleStart - 1,
                                mStyleStart, WPUnderlineSpan.class);
                        for (int i = 0; i < us.length; i++) {
                            int tagStart = s.getSpanStart(us[i]);
                            int tagEnd = s.getSpanEnd(us[i]);
                            s.removeSpan(us[i]);
                            s.setSpan(new WPUnderlineSpan(), tagStart, tagEnd,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    } else if (tag.equals("strike")) {
                        StrikethroughSpan[] ss = s.getSpans(mStyleStart - 1,
                                mStyleStart, StrikethroughSpan.class);
                        for (int i = 0; i < ss.length; i++) {
                            int tagStart = s.getSpanStart(ss[i]);
                            int tagEnd = s.getSpanEnd(ss[i]);
                            s.removeSpan(ss[i]);
                            s.setSpan(new StrikethroughSpan(), tagStart,
                                    tagEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    } else if (tag.equals("blockquote")) {
                        QuoteSpan[] ss = s.getSpans(mStyleStart - 1,
                                mStyleStart, QuoteSpan.class);
                        for (int i = 0; i < ss.length; i++) {
                            int tagStart = s.getSpanStart(ss[i]);
                            int tagEnd = s.getSpanEnd(ss[i]);
                            s.removeSpan(ss[i]);
                            s.setSpan(new QuoteSpan(), tagStart, tagEnd,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }
                }
            } else {
                String startTag = "<" + tag + ">";
                String endTag = "</" + tag + ">";
                Editable content = mContentEditText.getText();
                if (selectionEnd > selectionStart) {
                    content.insert(selectionStart, startTag);
                    content.insert(selectionEnd + startTag.length(), endTag);
                    toggleButton.setChecked(false);
                    mContentEditText.setSelection(selectionEnd
                            + startTag.length() + endTag.length());
                } else if (toggleButton.isChecked()) {
                    content.insert(selectionStart, startTag);
                    mContentEditText.setSelection(selectionEnd
                            + startTag.length());
                } else if (!toggleButton.isChecked()) {
                    content.insert(selectionEnd, endTag);
                    mContentEditText.setSelection(selectionEnd
                            + endTag.length());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void launchPictureLibrary() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        mCurrentActivityRequest = ACTIVITY_REQUEST_CODE_PICTURE_LIBRARY;
        startActivityForResult(photoPickerIntent,
                ACTIVITY_REQUEST_CODE_PICTURE_LIBRARY);
    }

    private void launchCamera() {
        String state = android.os.Environment.getExternalStorageState();
        if (!state.equals(android.os.Environment.MEDIA_MOUNTED)) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                    AbsEditActivity.this);
            dialogBuilder.setTitle(getResources()
                    .getText(R.string.sdcard_title));
            dialogBuilder.setMessage(getResources().getText(
                    R.string.sdcard_message));
            dialogBuilder.setPositiveButton(getString(R.string.ok),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                            dialog.dismiss();
                        }
                    });
            dialogBuilder.setCancelable(true);
            dialogBuilder.create().show();
        } else {
            String dcimFolderName = Environment.DIRECTORY_DCIM;
            if (dcimFolderName == null)
                dcimFolderName = "DCIM";
            mMediaCapturePath = Environment.getExternalStorageDirectory()
                    + File.separator + dcimFolderName + File.separator
                    + "Camera" + File.separator + "wp-"
                    + System.currentTimeMillis() + ".jpg";
            Intent takePictureFromCameraIntent = new Intent(
                    MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureFromCameraIntent.putExtra(
                    android.provider.MediaStore.EXTRA_OUTPUT,
                    Uri.fromFile(new File(mMediaCapturePath)));

            // make sure the directory we plan to store the recording in exists
            File directory = new File(mMediaCapturePath).getParentFile();
            if (!directory.exists() && !directory.mkdirs()) {
                try {
                    throw new IOException("Path to file could not be created.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            mCurrentActivityRequest = ACTIVITY_REQUEST_CODE_TAKE_PHOTO;
            startActivityForResult(takePictureFromCameraIntent,
                    ACTIVITY_REQUEST_CODE_TAKE_PHOTO);
        }
    }

    private void launchVideoLibrary() {
        Intent videoPickerIntent = new Intent(Intent.ACTION_PICK);
        videoPickerIntent.setType("video/*");
        mCurrentActivityRequest = ACTIVITY_REQUEST_CODE_VIDEO_LIBRARY;
        startActivityForResult(videoPickerIntent,
                ACTIVITY_REQUEST_CODE_VIDEO_LIBRARY);
    }

    private void launchVideoCamera() {
        mCurrentActivityRequest = ACTIVITY_REQUEST_CODE_TAKE_VIDEO;
        startActivityForResult(new Intent(MediaStore.ACTION_VIDEO_CAPTURE),
                ACTIVITY_REQUEST_CODE_TAKE_VIDEO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_CANCELED) {
            if (mQuickMediaType >= 0) {
                setResult(Activity.RESULT_CANCELED, new Intent());
                finish();
            }
            if (mFormatBar.getVisibility() == View.VISIBLE)
                hideFormatBar();
            return;
        }

        if (data != null
                || ((requestCode == ACTIVITY_REQUEST_CODE_TAKE_PHOTO || requestCode == ACTIVITY_REQUEST_CODE_TAKE_VIDEO))) {
            Bundle extras;

            switch (requestCode) {
            case ACTIVITY_REQUEST_CODE_PICTURE_LIBRARY:
                Uri imageUri = data.getData();
                String imgPath = imageUri.toString();
                addMedia(imgPath, imageUri);
                break;
            case ACTIVITY_REQUEST_CODE_TAKE_PHOTO:
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        File f = new File(mMediaCapturePath);
                        Uri capturedImageUri = Uri.fromFile(f);
                        f = null;
                        addMedia(capturedImageUri.toString(), capturedImageUri);
                        sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
                                Uri.parse("file://"
                                        + Environment
                                                .getExternalStorageDirectory())));
                    } catch (Exception e) {
                        e.printStackTrace();
                    } catch (OutOfMemoryError e) {
                        e.printStackTrace();
                    }
                }
                break;
            case ACTIVITY_REQUEST_CODE_VIDEO_LIBRARY:
                Uri videoUri = data.getData();
                String videoPath = videoUri.toString();
                addMedia(videoPath, videoUri);
                break;
            case ACTIVITY_REQUEST_CODE_TAKE_VIDEO:
                if (resultCode == Activity.RESULT_OK) {
                    Uri capturedVideo = data.getData();
                    addMedia(capturedVideo.toString(), capturedVideo);
                }
                break;
            case ACTIVITY_REQUEST_CODE_CREATE_LINK:
                try {
                    extras = data.getExtras();
                    String linkURL = extras.getString("linkURL");
                    if (!linkURL.equals("http://") && !linkURL.equals("")) {

                        if (mSelectionStart > mSelectionEnd) {
                            int temp = mSelectionEnd;
                            mSelectionEnd = mSelectionStart;
                            mSelectionStart = temp;
                        }
                        Editable str = mContentEditText.getText();
                        if (mLocalDraft) {
                            if (extras.getString("linkText") == null) {
                                if (mSelectionStart < mSelectionEnd)
                                    str.delete(mSelectionStart, mSelectionEnd);
                                str.insert(mSelectionStart, linkURL);
                                str.setSpan(new URLSpan(linkURL),
                                        mSelectionStart, mSelectionStart
                                                + linkURL.length(),
                                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                mContentEditText.setSelection(mSelectionStart
                                        + linkURL.length());
                            } else {
                                String linkText = extras.getString("linkText");
                                if (mSelectionStart < mSelectionEnd)
                                    str.delete(mSelectionStart, mSelectionEnd);
                                str.insert(mSelectionStart, linkText);
                                str.setSpan(new URLSpan(linkURL),
                                        mSelectionStart, mSelectionStart
                                                + linkText.length(),
                                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                mContentEditText.setSelection(mSelectionStart
                                        + linkText.length());
                            }
                        } else {
                            if (extras.getString("linkText") == null) {
                                if (mSelectionStart < mSelectionEnd)
                                    str.delete(mSelectionStart, mSelectionEnd);
                                String urlHTML = "<a href=\"" + linkURL + "\">"
                                        + linkURL + "</a>";
                                str.insert(mSelectionStart, urlHTML);
                                mContentEditText.setSelection(mSelectionStart
                                        + urlHTML.length());
                            } else {
                                String linkText = extras.getString("linkText");
                                if (mSelectionStart < mSelectionEnd)
                                    str.delete(mSelectionStart, mSelectionEnd);
                                String urlHTML = "<a href=\"" + linkURL + "\">"
                                        + linkText + "</a>";
                                str.insert(mSelectionStart, urlHTML);
                                mContentEditText.setSelection(mSelectionStart
                                        + urlHTML.length());
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
        }// end null check
    }

    protected String getCategoriesCSV(JSONArray mCategories) {
        String csv = "";
        if (mCategories.length() > 0) {
            for (int i = 0; i < mCategories.length(); i++) {
                try {
                    csv += EscapeUtils.unescapeHtml(mCategories.getString(i))
                            + ",";
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            csv = csv.substring(0, csv.length() - 1);
        }
        return csv;
    }

    @Override
    protected Dialog onCreateDialog(int id) {

        switch (id) {
        case ID_DIALOG_DATE:
            DatePickerDialog dpd = new DatePickerDialog(this, mDateSetListener,
                    mYear, mMonth, mDay);
            dpd.setTitle("");
            return dpd;
        case ID_DIALOG_TIME:
            TimePickerDialog tpd = new TimePickerDialog(this, mTimeSetListener,
                    mHour, mMinute, false);
            tpd.setTitle("");
            return tpd;
        case ID_DIALOG_LOADING:
            ProgressDialog loadingDialog = new ProgressDialog(this);
            loadingDialog.setMessage(getResources().getText(R.string.loading));
            loadingDialog.setIndeterminate(true);
            loadingDialog.setCancelable(true);
            return loadingDialog;
        }
        return super.onCreateDialog(id);
    }

    protected abstract boolean savePost(boolean autoSave);

    protected void setContent() {
        Intent intent = getIntent();
        String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        String title = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        if (text != null) {

            if (title != null) {
                mTitleEditText.setText(title);
            }

            if (text.contains("youtube_gdata")) {
                // Just use the URL for YouTube links for oEmbed support
                mContentEditText.setText(text);
            } else {
                // add link tag around URLs, trac #64
                text = text.replaceAll("((http|https|ftp|mailto):\\S+)",
                        "<a href=\"$1\">$1</a>");
                mContentEditText.setText(WPHtml.fromHtml(
                        StringHelper.addPTags(text), AbsEditActivity.this,
                        mPostable));
            }
        } else {
            String action = intent.getAction();
            final String type = intent.getType();
            final ArrayList<Uri> multi_stream;
            if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                multi_stream = intent
                        .getParcelableArrayListExtra((Intent.EXTRA_STREAM));
            } else {
                multi_stream = new ArrayList<Uri>();
                multi_stream.add((Uri) intent
                        .getParcelableExtra(Intent.EXTRA_STREAM));
            }

            List<Serializable> params = new Vector<Serializable>();
            params.add(multi_stream);
            params.add(type);
            new processAttachmentsTask().execute(params);
        }
    }

    private class processAttachmentsTask extends
            AsyncTask<List<?>, Void, SpannableStringBuilder> {

        protected void onPreExecute() {
            showDialog(ID_DIALOG_LOADING);
        }

        @Override
        protected SpannableStringBuilder doInBackground(List<?>... args) {
            ArrayList<?> multi_stream = (ArrayList<?>) args[0].get(0);
            String type = (String) args[0].get(1);
            SpannableStringBuilder ssb = new SpannableStringBuilder();
            for (int i = 0; i < multi_stream.size(); i++) {
                Uri curStream = (Uri) multi_stream.get(i);
                if (curStream != null && type != null) {
                    String imgPath = curStream.getEncodedPath();
                    ssb = addMediaFromShareAction(imgPath, curStream, ssb);
                }
            }
            return ssb;
        }

        protected void onPostExecute(SpannableStringBuilder result) {
            dismissDialog(ID_DIALOG_LOADING);
            if (result != null) {
                if (result.length() > 0) {
                    mContentEditText.setText(result);
                }
            } else {
                Toast.makeText(AbsEditActivity.this,
                        getResources().getText(R.string.gallery_error),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void addMedia(String imgPath, Uri curStream) {

        if (mFormatBar.getVisibility() == View.VISIBLE)
            hideFormatBar();

        Bitmap resizedBitmap = null;
        ImageHelper ih = new ImageHelper();
        Display display = getWindowManager().getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();
        if (width > height)
            width = height;

        Map<String, Object> mediaData = ih.getImageBytesForPath(imgPath,
                AbsEditActivity.this);

        if (mediaData == null) {
            // data stream not returned
            Toast.makeText(AbsEditActivity.this,
                    getResources().getText(R.string.gallery_error),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        byte[] bytes = (byte[]) mediaData.get("bytes");
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);

        float conversionFactor = 0.25f;

        if (opts.outWidth > opts.outHeight)
            conversionFactor = 0.40f;

        byte[] finalBytes = ih.createThumbnail(bytes,
                String.valueOf((int) (width * conversionFactor)),
                (String) mediaData.get("orientation"), true);

        if (finalBytes == null) {
            Toast.makeText(AbsEditActivity.this,
                    getResources().getText(R.string.out_of_memory),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        resizedBitmap = BitmapFactory.decodeByteArray(finalBytes, 0,
                finalBytes.length);

        int selectionStart = mContentEditText.getSelectionStart();
        mStyleStart = selectionStart;
        int selectionEnd = mContentEditText.getSelectionEnd();

        if (selectionStart > selectionEnd) {
            int temp = selectionEnd;
            selectionEnd = selectionStart;
            selectionStart = temp;
        }

        Editable s = mContentEditText.getText();
        WPImageSpan is = new WPImageSpan(AbsEditActivity.this, resizedBitmap,
                curStream);

        String imageWidth = WordPress.currentBlog.getMaxImageWidth();
        if (!imageWidth.equals("Original Size")) {
            try {
                is.setWidth(Integer.valueOf(imageWidth));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        is.setTitle((String) mediaData.get("title"));
        is.setImageSource(curStream);
        if (imgPath.contains("video")) {
            is.setVideo(true);
        }

        int line = 0, column = 0;
        try {
            line = mContentEditText.getLayout()
                    .getLineForOffset(selectionStart);
            column = mContentEditText.getSelectionStart()
                    - mContentEditText.getLayout().getLineStart(line);
        } catch (Exception ex) {
        }

        WPImageSpan[] image_spans = s.getSpans(selectionStart, selectionEnd,
                WPImageSpan.class);
        if (image_spans.length != 0) {
            // insert a few line breaks if the cursor is already on an image
            s.insert(selectionEnd, "\n\n");
            selectionStart = selectionStart + 2;
            selectionEnd = selectionEnd + 2;
        } else if (column != 0) {
            // insert one line break if the cursor is not at the first column
            s.insert(selectionEnd, "\n");
            selectionStart = selectionStart + 1;
            selectionEnd = selectionEnd + 1;
        }

        s.insert(selectionStart, " ");
        s.setSpan(is, selectionStart, selectionEnd + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        AlignmentSpan.Standard as = new AlignmentSpan.Standard(
                Layout.Alignment.ALIGN_CENTER);
        s.setSpan(as, selectionStart, selectionEnd + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.insert(selectionEnd + 1, "\n\n");
        try {
            mContentEditText.setSelection(s.length());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public SpannableStringBuilder addMediaFromShareAction(String imgPath,
            Uri curStream, SpannableStringBuilder ssb) {
        initBlog();
        Bitmap resizedBitmap = null;
        String imageTitle = "";

        ImageHelper ih = new ImageHelper();
        Display display = getWindowManager().getDefaultDisplay();
        int width = display.getWidth();

        Map<String, Object> mediaData = ih.getImageBytesForPath(imgPath,
                AbsEditActivity.this);

        if (mediaData == null) {
            // data stream not returned
            return null;
        }

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        byte[] bytes = (byte[]) mediaData.get("bytes");
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);

        float conversionFactor = 0.25f;

        if (opts.outWidth > opts.outHeight)
            conversionFactor = 0.40f;

        byte[] finalBytes = ih.createThumbnail((byte[]) mediaData.get("bytes"),
                String.valueOf((int) (width * conversionFactor)),
                (String) mediaData.get("orientation"), true);

        if (finalBytes == null) {
            Toast.makeText(AbsEditActivity.this,
                    getResources().getText(R.string.file_error_encountered),
                    Toast.LENGTH_SHORT).show();
            return null;
        }

        resizedBitmap = BitmapFactory.decodeByteArray(finalBytes, 0,
                finalBytes.length);

        WPImageSpan is = new WPImageSpan(AbsEditActivity.this, resizedBitmap,
                curStream);

        String imageWidth = WordPress.currentBlog.getMaxImageWidth();
        if (!imageWidth.equals("Original Size")) {
            try {
                is.setWidth(Integer.valueOf(imageWidth));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        is.setTitle(imageTitle);
        is.setImageSource(curStream);
        is.setVideo(imgPath.contains("video"));
        ssb.append(" ");
        ssb.setSpan(is, ssb.length() - 1, ssb.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        AlignmentSpan.Standard as = new AlignmentSpan.Standard(
                Layout.Alignment.ALIGN_CENTER);
        ssb.setSpan(as, ssb.length() - 1, ssb.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.append("\n");
        return ssb;
    }

    private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {
        public void onDateSet(DatePicker view, int year, int monthOfYear,
                int dayOfMonth) {
            mYear = year;
            mMonth = monthOfYear;
            mDay = dayOfMonth;
            showDialog(ID_DIALOG_TIME);
        }
    };

    private TimePickerDialog.OnTimeSetListener mTimeSetListener = new TimePickerDialog.OnTimeSetListener() {

        public void onTimeSet(TimePicker view, int hour, int minute) {
            mHour = hour;
            mMinute = minute;

            Date d = new Date(mYear - 1900, mMonth, mDay, mHour, mMinute);
            long timestamp = d.getTime();

            try {
                int flags = 0;
                flags |= android.text.format.DateUtils.FORMAT_SHOW_DATE;
                flags |= android.text.format.DateUtils.FORMAT_ABBREV_MONTH;
                flags |= android.text.format.DateUtils.FORMAT_SHOW_YEAR;
                flags |= android.text.format.DateUtils.FORMAT_SHOW_TIME;
                String formattedDate = DateUtils.formatDateTime(
                        AbsEditActivity.this, timestamp, flags);
                mCustomPubDate = timestamp;
                mPubDateText.setText(formattedDate);
                mIsCustomPubDate = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private Runnable autoSaveRunnable = new Runnable() {
        @Override
        public void run() {
            savePost(true);
            mAutoSaveHandler.postDelayed(this, AUTOSAVE_DELAY_MILLIS);
        }
    };

    @Override
    public void afterTextChanged(Editable s) {

        try {
            int position = Selection.getSelectionStart(mContentEditText
                    .getText());
            if ((mIsBackspace && position != 1) || mLastPosition == position
                    || !mLocalDraft)
                return;

            if (position < 0) {
                position = 0;
            }
            mLastPosition = position;
            if (position > 0) {

                if (mStyleStart > position) {
                    mStyleStart = position - 1;
                }
                boolean exists = false;
                if (mBoldToggleButton.isChecked()) {
                    StyleSpan[] ss = s.getSpans(mStyleStart, position,
                            StyleSpan.class);
                    exists = false;
                    for (int i = 0; i < ss.length; i++) {
                        if (ss[i].getStyle() == android.graphics.Typeface.BOLD) {
                            exists = true;
                        }
                    }
                    if (!exists)
                        s.setSpan(
                                new StyleSpan(android.graphics.Typeface.BOLD),
                                mStyleStart, position,
                                Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                }
                if (mEmToggleButton.isChecked()) {
                    StyleSpan[] ss = s.getSpans(mStyleStart, position,
                            StyleSpan.class);
                    exists = false;
                    for (int i = 0; i < ss.length; i++) {
                        if (ss[i].getStyle() == android.graphics.Typeface.ITALIC) {
                            exists = true;
                        }
                    }
                    if (!exists)
                        s.setSpan(new StyleSpan(
                                android.graphics.Typeface.ITALIC), mStyleStart,
                                position, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                }
                if (mEmToggleButton.isChecked()) {
                    StyleSpan[] ss = s.getSpans(mStyleStart, position,
                            StyleSpan.class);
                    exists = false;
                    for (int i = 0; i < ss.length; i++) {
                        if (ss[i].getStyle() == android.graphics.Typeface.ITALIC) {
                            exists = true;
                        }
                    }
                    if (!exists)
                        s.setSpan(new StyleSpan(
                                android.graphics.Typeface.ITALIC), mStyleStart,
                                position, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                }
                if (mUnderlineToggleButton.isChecked()) {
                    WPUnderlineSpan[] ss = s.getSpans(mStyleStart, position,
                            WPUnderlineSpan.class);
                    exists = false;
                    for (int i = 0; i < ss.length; i++) {
                        exists = true;
                    }
                    if (!exists)
                        s.setSpan(new WPUnderlineSpan(), mStyleStart, position,
                                Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                }
                if (mStrikeToggleButton.isChecked()) {
                    StrikethroughSpan[] ss = s.getSpans(mStyleStart, position,
                            StrikethroughSpan.class);
                    exists = false;
                    for (int i = 0; i < ss.length; i++) {
                        exists = true;
                    }
                    if (!exists)
                        s.setSpan(new StrikethroughSpan(), mStyleStart,
                                position, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                }
                if (mBquoteToggleButton.isChecked()) {

                    QuoteSpan[] ss = s.getSpans(mStyleStart, position,
                            QuoteSpan.class);
                    exists = false;
                    for (int i = 0; i < ss.length; i++) {
                        exists = true;
                    }
                    if (!exists)
                        s.setSpan(new QuoteSpan(), mStyleStart, position,
                                Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count,
            int after) {

        if ((count - after == 1) || (s.length() == 0))
            mIsBackspace = true;
        else
            mIsBackspace = false;
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void onSelectionChanged() {
        if (!mLocalDraft)
            return;

        final Spannable s = mContentEditText.getText();
        // set toggle buttons if cursor is inside of a matching span
        mStyleStart = mContentEditText.getSelectionStart();
        Object[] spans = s.getSpans(mContentEditText.getSelectionStart(),
                mContentEditText.getSelectionStart(), Object.class);

        mBoldToggleButton.setChecked(false);
        mEmToggleButton.setChecked(false);
        mBquoteToggleButton.setChecked(false);
        mUnderlineToggleButton.setChecked(false);
        mStrikeToggleButton.setChecked(false);
        for (Object span : spans) {
            if (span instanceof StyleSpan) {
                StyleSpan ss = (StyleSpan) span;
                if (ss.getStyle() == android.graphics.Typeface.BOLD) {
                    mBoldToggleButton.setChecked(true);
                }
                if (ss.getStyle() == android.graphics.Typeface.ITALIC) {
                    mEmToggleButton.setChecked(true);
                }
            }
            if (span instanceof QuoteSpan) {
                mBquoteToggleButton.setChecked(true);
            }
            if (span instanceof WPUnderlineSpan) {
                mUnderlineToggleButton.setChecked(true);
            }
            if (span instanceof StrikethroughSpan) {
                mStrikeToggleButton.setChecked(true);
            }
        }
    }
}
