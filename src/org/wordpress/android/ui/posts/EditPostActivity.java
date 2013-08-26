package org.wordpress.android.ui.posts;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.style.CharacterStyle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.justsystems.hpb.pad.R;

import org.json.JSONArray;
import org.json.JSONException;

import org.wordpress.android.Constants;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.MediaFile;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.Postable;
import org.wordpress.android.task.MultiAsyncTask;
import org.wordpress.android.ui.list.PagesActivity;
import org.wordpress.android.ui.list.PostsActivity;
import org.wordpress.android.util.EscapeUtils;
import org.wordpress.android.util.LocationHelper;
import org.wordpress.android.util.LocationHelper.LocationResult;
import org.wordpress.android.util.WPHtml;
import org.wordpress.android.util.WPImageSpan;

public final class EditPostActivity extends AbsEditActivity {

    private Post mPost;

    private Location mCurrentLocation;
    private LocationHelper mLocationHelper;
    private List<String> mSelectedCategories;

    private EditText mTagsEditText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTagsEditText = (EditText) findViewById(R.id.tags);
        mTagsEditText.setVisibility(View.VISIBLE);

        mCategories = new JSONArray();
        mSelectedCategories = new Vector<String>();

        getLocationProvider();
        if (!mIsNew) {
            this.mPost = (Post) super.mPostable;
            if (!mIsPage) {
                if (mPost.getCategories() != null) {
                    mCategories = mPost.getCategories();
                    if (!mCategories.equals("")) {

                        for (int i = 0; i < mCategories.length(); i++) {
                            try {
                                mSelectedCategories.add(mCategories
                                        .getString(i));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        mCategoriesText.setText(getCategoriesCSV(mCategories));
                    }
                }

                Double latitude = mPost.getLatitude();
                Double longitude = mPost.getLongitude();

                if (latitude != 0.0) {
                    new getAddressTask().executeOnMultiThread(latitude,
                            longitude);
                }
            }
            String tags = mPost.getMt_keywords();
            if (!tags.equals("")) {
                mTagsEditText.setText(tags);
            }
        }
    }

    private void getLocationProvider() {
        boolean hasLocationProvider = false;
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        for (String providerName : providers) {
            if (providerName.equals(LocationManager.GPS_PROVIDER)
                    || providerName.equals(LocationManager.NETWORK_PROVIDER)) {
                hasLocationProvider = true;
            }
        }
        if (hasLocationProvider && mBlog.isLocation() && !mIsPage) {
            enableLBSButtons();
        }
    }

    private void enableLBSButtons() {
        mLocationHelper = new LocationHelper();
        ((RelativeLayout) findViewById(R.id.section3))
                .setVisibility(View.VISIBLE);
        Button viewMap = (Button) findViewById(R.id.viewMap);
        Button updateLocation = (Button) findViewById(R.id.updateLocation);
        Button removeLocation = (Button) findViewById(R.id.removeLocation);
        updateLocation.setOnClickListener(this);
        removeLocation.setOnClickListener(this);
        viewMap.setOnClickListener(this);
        if (mIsNew)
            mLocationHelper.getLocation(EditPostActivity.this, locationResult);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mLocationHelper != null)
            mLocationHelper.cancelTimer();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.viewMap) {
            Double latitude = 0.0;
            try {
                latitude = mCurrentLocation.getLatitude();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (latitude != 0.0) {
                String uri = "geo:" + latitude + ","
                        + mCurrentLocation.getLongitude();
                startActivity(new Intent(android.content.Intent.ACTION_VIEW,
                        Uri.parse(uri)));
            } else {
                Toast.makeText(EditPostActivity.this,
                        getResources().getText(R.string.location_toast),
                        Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.updateLocation) {
            mLocationHelper.getLocation(EditPostActivity.this, locationResult);
        } else if (id == R.id.removeLocation) {
            if (mCurrentLocation != null) {
                mCurrentLocation.setLatitude(0.0);
                mCurrentLocation.setLongitude(0.0);
            }
            if (mPost != null) {
                mPost.setLatitude(0.0);
                mPost.setLongitude(0.0);
            }
            mLocationText.setText("");
        } else {
            super.onClick(v);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CANCELED) {
            return;
        }

        if (data != null
                && requestCode == ACTIVITY_REQUEST_CODE_SELECT_CATEGORIES) {
            Bundle extras = data.getExtras();
            String cats = extras.getString("selectedCategories");
            String[] splitCats = cats.split(",");
            if (splitCats.length < 1)
                return;
            mCategories = new JSONArray();
            for (int i = 0; i < splitCats.length; i++) {
                mCategories.put(splitCats[i]);
            }
            final String str = getCategoriesCSV(mCategories);
            if (str != null && str.length() > 0) {
                mCategoriesText.setText(getCategoriesCSV(mCategories));
            } else {
                mCategoriesText.setText(getString(R.string.none));
            }
        }
    }

    private LocationResult locationResult = new LocationResult() {
        @Override
        public void gotLocation(Location location) {
            if (location != null) {
                mCurrentLocation = location;
                new getAddressTask().executeOnMultiThread(
                        mCurrentLocation.getLatitude(),
                        mCurrentLocation.getLongitude());
            } else {
                runOnUiThread(new Runnable() {
                    public void run() {
                        mLocationText
                                .setText(getString(R.string.location_not_found));
                    }
                });
            }
        }
    };

    protected boolean savePost(boolean autoSave) {

        String title = mTitleEditText.getText().toString();
        String password = mPasswordEditText.getText().toString();
        String pubDate = mPubDateText.getText().toString();
        String content = "";

        if (mLocalDraft || mIsNew && !autoSave) {
            Editable e = mContentEditText.getText();
            if (android.os.Build.VERSION.SDK_INT >= 14) {
                // remove suggestion spans, they cause craziness in
                // WPHtml.toHTML().
                CharacterStyle[] style = e.getSpans(0, e.length(),
                        CharacterStyle.class);
                for (int i = 0; i < style.length; i++) {
                    if (style[i].getClass().getName()
                            .equals("android.text.style.SuggestionSpan"))
                        e.removeSpan(style[i]);
                }
            }
            content = EscapeUtils.unescapeHtml(WPHtml.toHtml(e));
            // replace duplicate <p> tags so there's not duplicates, trac #86
            content = content.replace("<p><p>", "<p>");
            content = content.replace("</p></p>", "</p>");
            content = content.replace("<br><br>", "<br>");
            // sometimes the editor creates extra tags
            content = content.replace("</strong><strong>", "")
                    .replace("</em><em>", "").replace("</u><u>", "")
                    .replace("</strike><strike>", "")
                    .replace("</blockquote><blockquote>", "");
        } else {
            content = mContentEditText.getText().toString();
        }

        long pubDateTimestamp = 0;
        if (!pubDate.equals(getResources().getText(R.string.immediately))) {
            if (mIsCustomPubDate)
                pubDateTimestamp = mCustomPubDate;
            else if (!mIsNew)
                pubDateTimestamp = mPost.getDate_created_gmt();
        }

        String tags = "", postFormat = "";
        if (!mIsPage) {
            tags = mTagsEditText.getText().toString();
            // post format
            Spinner postFormatSpinner = (Spinner) findViewById(R.id.postFormat);
            postFormat = mPostFormats[postFormatSpinner
                    .getSelectedItemPosition()];
        }

        String images = "";
        boolean success = false;

        if (content.equals("") && !autoSave) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                    EditPostActivity.this);
            dialogBuilder.setTitle(getResources()
                    .getText(R.string.empty_fields));
            dialogBuilder.setMessage(getResources().getText(
                    R.string.title_post_required));
            dialogBuilder.setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                            dialog.dismiss();
                        }
                    });
            dialogBuilder.setCancelable(true);
            dialogBuilder.create().show();
        } else {

            if (!mIsNew) {
                // update the images
                mPost.deleteMediaFiles();
                Editable s = mContentEditText.getText();
                WPImageSpan[] click_spans = s.getSpans(0, s.length(),
                        WPImageSpan.class);

                if (click_spans.length != 0) {

                    for (int i = 0; i < click_spans.length; i++) {
                        WPImageSpan wpIS = click_spans[i];
                        images += wpIS.getImageSource().toString() + ",";

                        MediaFile mf = new MediaFile();
                        mf.setPostID(mPost.getId());
                        mf.setTitle(wpIS.getTitle());
                        mf.setCaption(wpIS.getCaption());
                        mf.setDescription(wpIS.getDescription());
                        mf.setFeatured(wpIS.isFeatured());
                        mf.setFeaturedInPost(wpIS.isFeaturedInPost());
                        mf.setFileName(wpIS.getImageSource().toString());
                        mf.setHorizontalAlignment(wpIS.getHorizontalAlignment());
                        mf.setWidth(wpIS.getWidth());
                        mf.save();

                        int tagStart = s.getSpanStart(wpIS);
                        if (!autoSave) {
                            s.removeSpan(wpIS);
                            s.insert(tagStart, "<img android-uri=\""
                                    + wpIS.getImageSource().toString()
                                    + "\" />");
                            if (mLocalDraft)
                                content = EscapeUtils.unescapeHtml(WPHtml
                                        .toHtml(s));
                            else
                                content = s.toString();
                        }
                    }
                }
            }

            final String moreTag = "<!--more-->";
            int selectedStatus = mStatusSpinner.getSelectedItemPosition();
            String status = "";

            switch (selectedStatus) {
            case 0:
                status = "publish";
                break;
            case 1:
                status = "draft";
                break;
            case 2:
                status = "pending";
                break;
            case 3:
                status = "private";
                break;
            case 4:
                status = "localdraft";
                break;
            }

            Double latitude = 0.0;
            Double longitude = 0.0;
            if (mBlog.isLocation()) {

                // attempt to get the device's location
                try {
                    latitude = mCurrentLocation.getLatitude();
                    longitude = mCurrentLocation.getLongitude();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (mIsNew) {
                mPost = new Post(mBlogID, title, content, images,
                        pubDateTimestamp, mCategories.toString(), tags, status,
                        password, latitude, longitude, mIsPage, postFormat,
                        true, false);
                mPost.setLocalDraft(true);

                // split up the post content if there's a more tag
                if (content.indexOf(moreTag) >= 0) {
                    mPost.setDescription(content.substring(0,
                            content.indexOf(moreTag)));
                    mPost.setMt_text_more(content.substring(
                            content.indexOf(moreTag) + moreTag.length(),
                            content.length()));
                }

                success = mPost.save();

                if (success) {
                    mIsNew = false;
                    mIsNewDraft = true;
                }

                mPost.deleteMediaFiles();

                Spannable s = mContentEditText.getText();
                WPImageSpan[] image_spans = s.getSpans(0, s.length(),
                        WPImageSpan.class);

                if (image_spans.length != 0) {

                    for (int i = 0; i < image_spans.length; i++) {
                        WPImageSpan wpIS = image_spans[i];
                        images += wpIS.getImageSource().toString() + ",";

                        MediaFile mf = new MediaFile();
                        mf.setPostID(mPost.getId());
                        mf.setTitle(wpIS.getTitle());
                        mf.setCaption(wpIS.getCaption());
                        // mf.setDescription(wpIS.getDescription());
                        mf.setFeatured(wpIS.isFeatured());
                        mf.setFeaturedInPost(wpIS.isFeaturedInPost());
                        mf.setFileName(wpIS.getImageSource().toString());
                        mf.setFilePath(wpIS.getImageSource().toString());
                        mf.setHorizontalAlignment(wpIS.getHorizontalAlignment());
                        mf.setWidth(wpIS.getWidth());
                        mf.setVideo(wpIS.isVideo());
                        mf.save();
                    }
                }

                WordPress.setCurrentPost(mPost);

            } else {

                if (mCurrentLocation == null) {
                    latitude = mPost.getLatitude();
                    longitude = mPost.getLongitude();
                }

                mPost.setTitle(title);
                // split up the post content if there's a more tag
                if (mLocalDraft && content.indexOf(moreTag) >= 0) {
                    mPost.setDescription(content.substring(0,
                            content.indexOf(moreTag)));
                    mPost.setMt_text_more(content.substring(
                            content.indexOf(moreTag) + moreTag.length(),
                            content.length()));
                } else {
                    mPost.setDescription(content);
                    mPost.setMt_text_more("");
                }
                mPost.setMediaPaths(images);
                mPost.setDate_created_gmt(pubDateTimestamp);
                mPost.setCategories(mCategories);
                mPost.setMt_keywords(tags);
                mPost.setPost_status(status);
                mPost.setWP_password(password);
                mPost.setLatitude(latitude);
                mPost.setLongitude(longitude);
                mPost.setWP_post_form(postFormat);
                if (!mPost.isLocalDraft())
                    mPost.setLocalChange(true);
                success = mPost.update();
            }
        }
        this.mPostable = mPost;
        return success;
    }

    private class getAddressTask extends MultiAsyncTask<Double, Void, String> {

        @Override
        protected String doInBackground(Double... args) {
            Geocoder gcd = new Geocoder(EditPostActivity.this,
                    Locale.getDefault());
            String finalText = "";
            List<Address> addresses;
            try {
                addresses = gcd.getFromLocation(args[0], args[1], 1);
                String locality = "", adminArea = "", country = "";
                if (addresses.get(0).getLocality() != null)
                    locality = addresses.get(0).getLocality();
                if (addresses.get(0).getAdminArea() != null)
                    adminArea = addresses.get(0).getAdminArea();
                if (addresses.get(0).getCountryName() != null)
                    country = addresses.get(0).getCountryName();

                if (addresses.size() > 0) {
                    finalText = ((locality.equals("")) ? locality : locality
                            + ", ")
                            + ((adminArea.equals("")) ? adminArea : adminArea
                                    + " ") + country;
                    if (finalText.equals(""))
                        finalText = getString(R.string.location_not_found);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return finalText;
        }

        protected void onPostExecute(String result) {
            mLocationText.setText(result);
        }
    }

    @Override
    protected Postable createPost() {
        return new Post(mBlogID, mPostID, mIsPage);
    }

    @Override
    protected String getContentHTML(Postable post) {
        Post mPost = (Post) post;
        if (!mPost.getMt_text_more().equals("")) {
            if (mPost.isLocalDraft())
                return mPost.getDescription() + "\n&lt;!--more--&gt;\n"
                        + mPost.getMt_text_more();
            else
                return mPost.getDescription() + "\n<!--more-->\n"
                        + mPost.getMt_text_more();
        } else
            return mPost.getDescription();
    }

    @Override
    protected void preparePost() {
        if (mQuickMediaType >= 0) {
            if (mQuickMediaType == Constants.QUICK_POST_PHOTO_CAMERA
                    || mQuickMediaType == Constants.QUICK_POST_PHOTO_LIBRARY)
                mPost.setQuickPostType("QuickPhoto");
            else if (mQuickMediaType == Constants.QUICK_POST_VIDEO_CAMERA
                    || mQuickMediaType == Constants.QUICK_POST_VIDEO_LIBRARY)
                mPost.setQuickPostType("QuickVideo");
        }
        WordPress.setCurrentPost(mPost);
    }

    @Override
    protected void startListActivity() {
        Intent intent;
        if (mIsPage) {
            intent = new Intent(this, PagesActivity.class);
        } else {
            intent = new Intent(this, PostsActivity.class);
        }
        intent.putExtra("id", WordPress.currentBlog.getId());
        intent.putExtra("isNew", true);
        intent.putExtra("viewPages", mIsPage);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        setResult(RESULT_OK, intent);
        startActivity(intent);
    }

    @Override
    protected void startCategoryActivity() {
        Bundle bundle = new Bundle();
        bundle.putInt("id", mBlogID);
        if (mCategories.length() > 0) {
            bundle.putString("categoriesCSV", getCategoriesCSV(mCategories));
        }
        Intent i1 = new Intent(EditPostActivity.this,
                SelectCategoriesActivity.class);
        i1.putExtras(bundle);
        startActivityForResult(i1, ACTIVITY_REQUEST_CODE_SELECT_CATEGORIES);
    }
}
