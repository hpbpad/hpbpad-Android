package org.wordpress.android.ui.posts;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.Spannable;
import android.text.style.CharacterStyle;
import android.widget.Spinner;

import com.justsystems.hpb.pad.R;

import org.json.JSONArray;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.CustomTypePost;
import org.wordpress.android.models.MediaFile;
import org.wordpress.android.models.Postable;
import org.wordpress.android.models.Term;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.WPHtml;
import org.wordpress.android.util.WPImageSpan;

public class EditCustomTypePostActivity extends AbsEditActivity {

    private CustomTypePost mPost;
    private Term[] terms;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCategories = new ArrayList<String>();
        if (!mIsNew) {
            this.mPost = (CustomTypePost) super.mPostable;
            this.terms = mPost.getTerms();
            if (this.terms != null) {
                for (int i = 0; i < this.terms.length; i++) {
                    mCategories.add(this.terms[i].getName());
                }
            }
        }
        populateSelectedCategories();
    }

    @Override
    protected boolean savePost(boolean isAutoSave, boolean isDraftSave) {

        String title = mTitleEditText.getText().toString();
        String password = mPasswordEditText.getText().toString();
        String pubDate = mPubDateText.getText().toString();
        String excerpt = mExcerptEditText.getText().toString();
        String content = "";

        if (mLocalDraft || mIsNew && !isAutoSave) {
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
            content = WPHtml.toHtml(e);
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

        String postFormat = "";
        if (!mIsPage) {
            // post format
            Spinner postFormatSpinner = (Spinner) findViewById(R.id.postFormat);
            postFormat = mPostFormats[postFormatSpinner
                    .getSelectedItemPosition()];
        }

        String images = "";
        boolean success = false;

        if (content.equals("") && !isAutoSave && !isDraftSave) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                    EditCustomTypePostActivity.this);
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
                // mPost.deleteMediaFiles();
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
                        if (!isAutoSave) {
                            s.removeSpan(wpIS);
                            s.insert(tagStart, "<img android-uri=\""
                                    + wpIS.getImageSource().toString()
                                    + "\" />");
                            if (mLocalDraft)
                                content = WPHtml.toHtml(s);
                            else
                                content = s.toString();
                        }
                    }
                }
            }

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

            if (mIsNew) {
                JSONArray categorisList = JSONUtil
                        .fromStringListToJSONArray(mCategories);

                mPost = new CustomTypePost(mBlogID, typeName, title, content,
                        images, pubDateTimestamp, status, password, postFormat,
                        terms, true);
                mPost.setLocalDraft(true);

                success = mPost.save();

                if (success) {
                    mIsNew = false;
                }

                // mPost.deleteMediaFiles();

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

                mPost.setTitle(title);
                mPost.setExcerpt(excerpt);
                mPost.setPost_content(content);

                // mPost.setMediaPaths(images);
                mPost.setDate_created_gmt(pubDateTimestamp);
                mPost.setPost_status(status);
                mPost.setPassword(password);
                mPost.setPost_format(postFormat);
                if (this.terms != null) {
                    mPost.setTerms(terms);
                }
                success = mPost.update();
            }
        }
        this.mPostable = mPost;
        return success;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CANCELED) {
            return;
        }

        if (data != null
                && requestCode == ACTIVITY_REQUEST_CODE_SELECT_CATEGORIES) {
            Parcelable[] parcelables = data.getParcelableArrayExtra("term");
            if (parcelables == null) {
                return;
            }
            mCategories = new ArrayList<String>();
            this.terms = new Term[parcelables.length];
            for (int i = 0; i < parcelables.length; i++) {
                Term term = (Term) parcelables[i];
                mCategories.add(term.getName());
                terms[i] = term;
            }
            populateSelectedCategories();
        }
    }

    @Override
    protected Postable createPost() {
        return new CustomTypePost(mBlogID, mPostID, typeName);
    }

    @Override
    protected String getContentHTML(Postable post) {
        return ((CustomTypePost) post).getContent();
    }

    @Override
    protected void preparePost() {
        WordPress.setCurrentPost(mPost);
    }

    @Override
    protected void startListActivity() {
        Intent i = new Intent(this, CustomPostTypePostsActivity.class);
        i.putExtra("shouldRefresh", true);

        i.putExtra("id", WordPress.currentBlog.getId());
        i.putExtra("isNew", true);
        i.putExtra("type_name", typeName);
        i.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

        setResult(RESULT_OK, i);
        startActivity(i);
    }

    @Override
    protected void startCategoryActivity() {
        Intent i1 = new Intent(this, SelectTermsActivity.class);
        i1.putExtra("term", this.terms);
        i1.putExtra("id", mBlogID);
        i1.putExtra("type_name", typeName);
        startActivityForResult(i1, ACTIVITY_REQUEST_CODE_SELECT_CATEGORIES);
    }

}
