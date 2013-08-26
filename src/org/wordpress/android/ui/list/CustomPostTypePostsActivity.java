package org.wordpress.android.ui.list;

import java.util.Map;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;

import com.justsystems.hpb.pad.R;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.PostType;
import org.wordpress.android.models.Postable;
import org.wordpress.android.task.AbsDeleteTask;
import org.wordpress.android.task.AbsShareUrlTask;
import org.wordpress.android.ui.posts.EditCustomTypePostActivity;

public final class CustomPostTypePostsActivity extends AbsListActivity {
    // Exists to distinguish pages from posts in menu drawer

    private String typeName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        String typeLabel;
        if (intent != null) {
            this.typeName = intent.getStringExtra("type_name");
            typeLabel = intent.getStringExtra("type_label");
            if (typeLabel == null) {
                PostType postType = new PostType(WordPress.currentBlog.getId(),
                        typeName);
                typeLabel = postType.getLabel();
            }
        } else {
            throw new IllegalArgumentException();
        }
        super.postType = POST_TYPE_CUSTOM;
        setTitle(typeLabel);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        loadingDialog = new ProgressDialog(this);
        if (id == ID_DIALOG_DELETING) {
            loadingDialog
                    .setTitle(getResources().getText(R.string.delete_post));
            loadingDialog.setMessage(getResources().getText(
                    R.string.attempt_delete_post));
            loadingDialog.setCancelable(false);
            return loadingDialog;
        } else if (id == ID_DIALOG_SHARE) {
            loadingDialog.setTitle(getString(R.string.share_url));
            loadingDialog.setMessage(getResources().getText(
                    R.string.attempting_fetch_url));
            loadingDialog.setCancelable(false);
            return loadingDialog;
        }
        return super.onCreateDialog(id);
    }

    @Override
    void startActivity() {
        Intent i = new Intent(this, EditCustomTypePostActivity.class);
        i.putExtra("id", WordPress.currentBlog.getId());
        i.putExtra("isNew", true);
        i.putExtra("type_name", typeName);
        startActivityForResult(i, ACTIVITY_EDIT_POST);
    }

    @Override
    AbsShareUrlTask getShareUrlTask() {
        return new ShareURLTask(this);
    }

    @Override
    AbsDeleteTask getDeleteTask() {
        return new DeletePostTask(this);
    }

    public String getPostType() {
        return typeName;
    }

    private static class ShareURLTask extends AbsShareUrlTask {

        public ShareURLTask(AbsListActivity activity) {
            super(activity);
        }

        @Override
        protected boolean isStatusPublish(Map<?, ?> contentHash) {
            return "publish".equals(contentHash.get("post_status").toString());
        }

        @Override
        protected int getNotPublishedMessageId() {
            return R.string.post_not_published;
        }

        @Override
        protected String getMethod() {
            return "wp.getPost";
        }

        @Override
        protected Object[] getParams(Postable post) {
            Object[] o = new Object[1];
            o[0] = "post";

            Object[] params = { WordPress.currentBlog.getId(),
                    WordPress.currentBlog.getUsername(),
                    WordPress.currentBlog.getPassword(), post.getPostId(), o };
            return params;
        }
    }

    private static class DeletePostTask extends AbsDeleteTask {

        public DeletePostTask(AbsListActivity activity) {
            super(activity);
        }

        @Override
        protected int getDeletedMessageId() {
            return R.string.post_deleted;
        }

        @Override
        protected int getMessageWhatId() {
            return R.string.post;
        }

        @Override
        protected String getMethod() {
            return "wp.deletePost";
        }

        @Override
        protected Object[] getParams(Postable post) {
            Blog blog = WordPress.currentBlog;
            Object[] postParams = { blog.getId(), blog.getUsername(),
                    blog.getPassword(), post.getPostId() };
            return postParams;
        }
    }
}
