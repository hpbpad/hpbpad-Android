package org.wordpress.android.ui.posts;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.Map;

import android.os.Bundle;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.internal.StringMap;
import com.google.gson.reflect.TypeToken;
import com.justsystems.hpb.pad.R;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.CustomTypePost;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.Postable;
import org.wordpress.android.ui.AuthenticatedWebViewActivity;
import org.wordpress.android.util.StringUtils;

/**
 * Activity for previewing a post or page in a webview.
 */
public class PreviewPostActivity extends AuthenticatedWebViewActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();

        boolean isPage = getIntent().getBooleanExtra("isPage", false);
        if (isPage) {
            this.setTitle(StringUtils.unescapeHTML(WordPress.currentBlog
                    .getBlogName())
                    + " - "
                    + getResources().getText(R.string.preview_page));
        } else {
            this.setTitle(StringUtils.unescapeHTML(WordPress.currentBlog
                    .getBlogName())
                    + " - "
                    + getResources().getText(R.string.preview_post));
        }

        mWebView.setWebChromeClient(new WordPressWebChromeClient(this));
        mWebView.getSettings().setJavaScriptEnabled(true);

        if (extras != null) {
            long mPostID = extras.getLong("postID");
            int mBlogID = extras.getInt("blogID");

            Post post = new Post(mBlogID, mPostID, isPage);

            if (post.getId() < 0)
                Toast.makeText(this, R.string.post_not_found,
                        Toast.LENGTH_SHORT).show();
            else
                loadPostPreview(post);
        } else if (WordPress.getCurrentPost() != null) {
            loadPostPreview(WordPress.getCurrentPost());
        } else {
            Toast.makeText(this, R.string.post_not_found, Toast.LENGTH_SHORT)
                    .show();
        }
    }

    /**
     * Load the post preview. If the post is in a non-public state (e.g. draft
     * status, part of a non-public blog, etc), load the preview as an
     * authenticated URL. Otherwise, just load the preview normally.
     * 
     * @param post
     *            Post to load the preview for.
     */
    private void loadPostPreview(Postable postable) {
        if (postable == null) {
            return;
        }
        if (postable.getType() == Postable.TYP_POST
                || postable.getType() == Postable.TYP_PAGE) {
            Post post = (Post) postable;
            String url = post.getPermaLink();
            boolean isPrivate = false;
            try {
                Gson gson = new Gson();
                Type type = new TypeToken<Map<String, Object>>() {
                }.getType();
                Map<String, Object> blogOptions = gson.fromJson(
                        mBlog.getBlogOptions(), type);
                StringMap<?> blogPublicOption = (StringMap<?>) blogOptions
                        .get("blog_public");
                String blogPublicOptionValue = blogPublicOption.get("value")
                        .toString();
                if (blogPublicOptionValue.equals("-1")) {
                    isPrivate = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            Date d = new Date();
            if (isPrivate //blog private 
                    || post.isLocalDraft()
                    || post.isLocalChange()
                    || post.getDate_created_gmt() > d.getTime() //Scheduled
                    || !post.getPostStatus().equals("publish")) {
                if (-1 == url.indexOf('?')) {
                    url = url.concat("?preview=true");
                } else {
                    url = url.concat("&preview=true");
                }
                loadAuthenticatedUrl(url);
            } else {
                loadUrl(url);
            }
        } else {
            CustomTypePost post = (CustomTypePost) postable;
            String url = post.getLink();
            boolean isPrivate = false;
            try {
                Gson gson = new Gson();
                Type type = new TypeToken<Map<String, Object>>() {
                }.getType();
                Map<String, Object> blogOptions = gson.fromJson(
                        mBlog.getBlogOptions(), type);
                StringMap<?> blogPublicOption = (StringMap<?>) blogOptions
                        .get("blog_public");
                String blogPublicOptionValue = blogPublicOption.get("value")
                        .toString();
                if (blogPublicOptionValue.equals("-1")) {
                    isPrivate = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (isPrivate || post.isLocalDraft()
                    || !post.getPostStatus().equals("publish")) {
                if (-1 == url.indexOf('?')) {
                    url = url.concat("?preview=true");
                } else {
                    url = url.concat("&preview=true");
                }
                loadAuthenticatedUrl(url);
            } else {
                loadUrl(url);
            }
        }
    }
}
