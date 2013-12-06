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
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.WPHtml;

public class ViewPostFragment extends Fragment implements PostEditConstants,
        OnClickListener {
    /** Called when the activity is first created. */

    private OnDetailPostActionListener onDetailPostActionListener;
    AbsListActivity parentActivity;

    private ImageButton editPostButton;
    private ImageButton shareURLButton;
    private ImageButton deletePostButton;
    private ImageButton seoButton;
    private ImageButton viewPostButton;
    private ImageButton addCommentButton;

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);

    }

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
        shareURLButton.setOnClickListener(this);

        this.deletePostButton = (ImageButton) v.findViewById(R.id.deletePost);
        deletePostButton.setOnClickListener(this);

        this.seoButton = (ImageButton) v.findViewById(R.id.seo);
        this.seoButton.setOnClickListener(this);

        this.viewPostButton = (ImageButton) v.findViewById(R.id.viewPost);
        viewPostButton.setOnClickListener(this);

        this.addCommentButton = (ImageButton) v.findViewById(R.id.addComment);
        addCommentButton.setOnClickListener(this);

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
            goToSeo(currentPost);
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

    protected void loadPostPreview() {
        Postable currentPost = WordPress.getCurrentPost();
        if (currentPost != null) {
            if (currentPost.getLink() != null
                    && !currentPost.getLink().equals("")) {
                Intent i = new Intent(getActivity(), PreviewPostActivity.class);
                startActivity(i);
            }
        }

    }

    private void goToSeo(Postable currentPost) {
        if (WordPress.getCurrentPost() != null) {
            Intent i = new Intent(getActivity(), SeoResultActivity.class);
            i.putExtra("title", currentPost.getTitle());
            i.putExtra("contents", currentPost.getContent());
            String excerpt = currentPost.getExcerpt();
            if (excerpt.length() > 0) {
                i.putExtra("h1", excerpt);
            }
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
        if (postable == null || postable.getTitle() == null)
            return;

        TextView title = (TextView) getActivity().findViewById(R.id.postTitle);
        if (postable.getTitle().equals(""))
            title.setText("(" + getResources().getText(R.string.untitled) + ")");
        else
            title.setText(StringUtils.unescapeHTML(postable.getTitle()));

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

        String postContent;
        if (postable.getType() == Postable.TYP_PAGE
                || postable.getType() == Postable.TYP_POST) {
            Post post = (Post) postable;
            postContent = postable.getContent() + "\n\n"
                    + post.getMt_text_more();
        } else {
            postContent = postable.getContent();
        }

        if (postable.isLocalDraft()) {
            tv.setVisibility(View.VISIBLE);
            webView.setVisibility(View.GONE);
            shareURLButton.setVisibility(View.GONE);
            viewPostButton.setVisibility(View.GONE);
            addCommentButton.setVisibility(View.GONE);

            tv.setText(WPHtml.fromHtml(postContent.replaceAll("\uFFFC", ""),
                    getActivity().getBaseContext(), postable));
        } else {
            tv.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
            shareURLButton.setVisibility(View.VISIBLE);
            viewPostButton.setVisibility(View.VISIBLE);
            if (postable.allowComments()) {
                addCommentButton.setVisibility(View.VISIBLE);
            } else {
                addCommentButton.setVisibility(View.GONE);
            }

            String htmlText = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><html><head><link rel=\"stylesheet\" type=\"text/css\" href=\"webview.css\" /></head><body><div id=\"container\">"
                    + StringUtils.addPTags(postContent)
                    + "</div></body></html>";
            webView.loadDataWithBaseURL("file:///android_asset/", htmlText,
                    "text/html", "utf-8", null);
        }

    }

    public interface OnDetailPostActionListener {
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
