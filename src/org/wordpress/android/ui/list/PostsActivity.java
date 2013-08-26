package org.wordpress.android.ui.list;

import java.util.Map;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;

import com.justsystems.hpb.pad.R;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.Postable;
import org.wordpress.android.task.AbsDeleteTask;
import org.wordpress.android.task.AbsShareUrlTask;
import org.wordpress.android.ui.posts.EditPostActivity;

public final class PostsActivity extends AbsListActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.postType = POST_TYPE_POST;
        setTitle(getString(R.string.posts));
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
        Intent i = new Intent(this, EditPostActivity.class);
        i.putExtra("id", WordPress.currentBlog.getId());
        i.putExtra("isNew", true);
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
            return "metaWeblog.getPost";
        }

        @Override
        protected Object[] getParams(Postable post) {
            Object[] params = { post.getPostId(),
                    WordPress.currentBlog.getUsername(),
                    WordPress.currentBlog.getPassword() };
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
            return "blogger.deletePost";
        }

        @Override
        protected Object[] getParams(Postable post) {
            Object[] postParams = { "", post.getPostId(),
                    WordPress.currentBlog.getUsername(),
                    WordPress.currentBlog.getPassword() };
            return postParams;
        }
    }
}
