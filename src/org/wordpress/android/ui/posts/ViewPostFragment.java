package org.wordpress.android.ui.posts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.TextView;

import com.justsystems.hpb.pad.R;
import com.justsystems.hpb.pad.seo.SeoResultActivity;

import org.wordpress.android.PostEditConstants;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.CustomTypePost;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.Postable;
import org.wordpress.android.ui.list.AbsListActivity;
import org.wordpress.android.util.EscapeUtils;
import org.wordpress.android.util.StringHelper;

public final class ViewPostFragment extends Fragment implements
        PostEditConstants, OnClickListener {
    /** Called when the activity is first created. */

    private OnDetailPostActionListener onDetailPostActionListener;
    private AbsListActivity parentActivity;

    private ImageButton editPostButton;
    private ImageButton shareURLButton;
    private ImageButton deletePostButton;
    private ImageButton seoButton;
    private ImageButton viewPostButton;
    private ImageButton addCommentButton;

    @Override
    public void onResume() {
        super.onResume();

        if (WordPress.getCurrentPost() != null) {
            loadPost(WordPress.getCurrentPost());
        }

        parentActivity = (AbsListActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.viewpost, container, false);

        // button listeners here
        this.editPostButton = (ImageButton) v.findViewById(R.id.editPost);
        this.editPostButton.setOnClickListener(this);

        this.shareURLButton = (ImageButton) v.findViewById(R.id.sharePostLink);
        this.shareURLButton.setOnClickListener(this);

        this.deletePostButton = (ImageButton) v.findViewById(R.id.deletePost);
        this.deletePostButton.setOnClickListener(this);

        this.seoButton = (ImageButton) v.findViewById(R.id.seo);
        this.seoButton.setOnClickListener(this);
        if (!SeoResultActivity.SHOW_SEO) {
            this.seoButton.setVisibility(View.GONE);
        }

        this.viewPostButton = (ImageButton) v.findViewById(R.id.viewPost);
        this.viewPostButton.setOnClickListener(this);

        this.addCommentButton = (ImageButton) v.findViewById(R.id.addComment);
        this.addCommentButton.setOnClickListener(this);

        return v;
    }

    @Override
    public void onClick(View v) {
        Postable currentPost = WordPress.getCurrentPost();
        if (currentPost == null || parentActivity.isRefreshing) {
            return;
        }

        if (v == this.editPostButton) {
            editPost(currentPost);
        } else if (v == this.shareURLButton) {
            onDetailPostActionListener.onDetailPostAction(POST_SHARE,
                    currentPost);
        } else if (v == this.deletePostButton) {
            onDetailPostActionListener.onDetailPostAction(POST_DELETE,
                    currentPost);
        } else if (v == this.seoButton) {
            goToSeo();
        } else if (v == this.viewPostButton) {
            loadPostPreview();
        } else if (v == this.addCommentButton) {
            onDetailPostActionListener.onDetailPostAction(POST_COMMENT,
                    currentPost);
        }
    }

    private void editPost(Postable postable) {
        if (postable == null) {
            return;
        }
        if (postable.getType() == Postable.TYP_PAGE
                || postable.getType() == Postable.TYP_POST) {
            Post currentPost = (Post) postable;
            onDetailPostActionListener.onDetailPostAction(POST_EDIT,
                    currentPost);
            Intent i = new Intent(getActivity().getApplicationContext(),
                    EditPostActivity.class);
            i.putExtra("isPage", currentPost.isPage());
            i.putExtra("postID", currentPost.getId());
            i.putExtra("localDraft", currentPost.isLocalDraft());
            startActivityForResult(i, 0);
        } else if (postable.getType() == Postable.TYP_CUSTOM_TYPE_POST) {
            CustomTypePost currentPost = (CustomTypePost) postable;
            onDetailPostActionListener.onDetailPostAction(POST_EDIT,
                    currentPost);
            Intent i = new Intent(getActivity().getApplicationContext(),
                    EditCustomTypePostActivity.class);
            i.putExtra("type_name", currentPost.getPost_type());
            i.putExtra("postID", currentPost.getId());
            i.putExtra("localDraft", currentPost.isLocalDraft());
            startActivityForResult(i, 0);
        }
    }

    private void loadPostPreview() {

        if (WordPress.getCurrentPost() != null) {
            if (WordPress.getCurrentPost().getLink() != null
                    && !WordPress.getCurrentPost().getLink().equals("")) {
                Intent i = new Intent(getActivity(), PreviewPostActivity.class);
                startActivity(i);
            }
        }
    }

    private void goToSeo() {
        if (WordPress.getCurrentPost() != null) {
            Intent i = new Intent(getActivity(), SeoResultActivity.class);
            startActivity(i);
        }
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            // check that the containing activity implements our callback
            onDetailPostActionListener = (OnDetailPostActionListener) activity;
        } catch (ClassCastException e) {
            activity.finish();
            throw new ClassCastException(activity.toString()
                    + " must implement Callback");
        }
    }

    public void loadPost(Postable postable) {

        // Don't load if the Post object of title are null, see #395
        if (postable == null || postable.getTitle() == null) {
            return;
        }

        TextView title = (TextView) getActivity().findViewById(R.id.postTitle);
        if (postable.getTitle().equals(""))
            title.setText("(" + getResources().getText(R.string.untitled) + ")");
        else
            title.setText(EscapeUtils.unescapeHtml(postable.getTitle()));

        WebView webView = (WebView) getActivity().findViewById(
                R.id.viewPostWebView);
        TextView tv = (TextView) getActivity().findViewById(
                R.id.viewPostTextView);
        ImageButton shareURLButton = (ImageButton) getActivity().findViewById(
                R.id.sharePostLink);
        ImageButton viewPostButton = (ImageButton) getActivity().findViewById(
                R.id.viewPost);
        ImageButton addCommentButton = (ImageButton) getActivity()
                .findViewById(R.id.addComment);

        tv.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);

        String html;
        if (postable.getType() == Postable.TYP_PAGE
                || postable.getType() == Postable.TYP_POST) {
            Post post = (Post) postable;
            html = StringHelper.addPTags(postable.getContent() + "\n\n"
                    + post.getMt_text_more());
        } else {
            html = StringHelper.addPTags(postable.getContent());
        }

        String htmlText = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><html><head><link rel=\"stylesheet\" type=\"text/css\" href=\"webview.css\" /></head><body><div id=\"container\">"
                + html + "</div></body></html>";
        webView.loadDataWithBaseURL("file:///android_asset/", htmlText,
                "text/html", "utf-8", null);

        if (postable.isLocalDraft()) {
            shareURLButton.setVisibility(View.GONE);
            viewPostButton.setVisibility(View.GONE);
            addCommentButton.setVisibility(View.GONE);
        } else {
            shareURLButton.setVisibility(View.VISIBLE);
            viewPostButton.setVisibility(View.VISIBLE);
            if (postable.allowComments()) {
                addCommentButton.setVisibility(View.VISIBLE);
            } else {
                addCommentButton.setVisibility(View.GONE);
            }
        }

    }

    public void loadPost(CustomTypePost post) {

        // Don't load if the Post object of title are null, see #395
        if (post == null || post.getTitle() == null)
            return;

        TextView title = (TextView) getActivity().findViewById(R.id.postTitle);
        if (post.getTitle().equals(""))
            title.setText("(" + getResources().getText(R.string.untitled) + ")");
        else
            title.setText(EscapeUtils.unescapeHtml(post.getTitle()));

        WebView webView = (WebView) getActivity().findViewById(
                R.id.viewPostWebView);
        TextView tv = (TextView) getActivity().findViewById(
                R.id.viewPostTextView);
        ImageButton shareURLButton = (ImageButton) getActivity().findViewById(
                R.id.sharePostLink);
        ImageButton viewPostButton = (ImageButton) getActivity().findViewById(
                R.id.viewPost);
        ImageButton addCommentButton = (ImageButton) getActivity()
                .findViewById(R.id.addComment);

        tv.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        // String html = StringHelper
        // .addPTags(post.getPost_content() + "\n\n" + post.getMt_text_more());
        String html = StringHelper.addPTags(post.getContent());

        String htmlText = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><html><head><link rel=\"stylesheet\" type=\"text/css\" href=\"webview.css\" /></head><body><div id=\"container\">"
                + html + "</div></body></html>";
        webView.loadDataWithBaseURL("file:///android_asset/", htmlText,
                "text/html", "utf-8", null);

        if (post.isLocalDraft()) {
            shareURLButton.setVisibility(View.GONE);
            viewPostButton.setVisibility(View.GONE);
            addCommentButton.setVisibility(View.GONE);
        } else {
            shareURLButton.setVisibility(View.VISIBLE);
            viewPostButton.setVisibility(View.VISIBLE);
            if ("open".equals(post.getComment_status())) {
                addCommentButton.setVisibility(View.VISIBLE);
            } else {
                addCommentButton.setVisibility(View.GONE);
            }
        }

    }

    public static interface OnDetailPostActionListener {
        public void onDetailPostAction(int action, Postable post);
    }

    public void clearContent() {
        TextView title = (TextView) getActivity().findViewById(R.id.postTitle);
        title.setText("");
        WebView webView = (WebView) getActivity().findViewById(
                R.id.viewPostWebView);
        TextView tv = (TextView) getActivity().findViewById(
                R.id.viewPostTextView);
        tv.setText("");
        String htmlText = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><html><head><link rel=\"stylesheet\" type=\"text/css\" href=\"webview.css\" /></head><body><div id=\"container\"></div></body></html>";
        webView.loadDataWithBaseURL("file:///android_asset/", htmlText,
                "text/html", "utf-8", null);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }
        super.onSaveInstanceState(outState);
    }

}
