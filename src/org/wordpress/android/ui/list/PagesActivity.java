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

public final class PagesActivity extends AbsListActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.postType = POST_TYPE_PAGE;
        setTitle(getString(R.string.pages));
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        loadingDialog = new ProgressDialog(this);
        if (id == ID_DIALOG_DELETING) {
            loadingDialog
                    .setTitle(getResources().getText(R.string.delete_page));
            loadingDialog.setMessage(getResources().getText(
                    R.string.attempt_delete_page));
            loadingDialog.setCancelable(false);
            return loadingDialog;
        } else if (id == ID_DIALOG_SHARE) {
            loadingDialog.setTitle(getString(R.string.share_url_page));
            loadingDialog.setMessage(getResources().getText(
                    R.string.attempting_fetch_url));
            loadingDialog.setCancelable(false);
            return loadingDialog;
        }
        return super.onCreateDialog(id);
    }

    @Override
    AbsShareUrlTask getShareUrlTask() {
        return new ShareURLTask(this);
    }

    @Override
    void startActivity() {
        Intent i = new Intent(this, EditPostActivity.class);
        i.putExtra("id", WordPress.currentBlog.getId());
        i.putExtra("isNew", true);
        i.putExtra("isPage", true);
        startActivityForResult(i, ACTIVITY_EDIT_POST);
    }

    @Override
    AbsDeleteTask getDeleteTask() {
        return new DeletePageTask(this);
    }

    @Override
    public boolean isPage() {
        return true;
    }

    private static class ShareURLTask extends AbsShareUrlTask {

        public ShareURLTask(AbsListActivity activity) {
            super(activity);
        }

        @Override
        protected boolean isStatusPublish(Map<?, ?> contentHash) {
            return "publish".equals(contentHash.get("page_status").toString());
        }

        @Override
        protected int getNotPublishedMessageId() {
            return R.string.page_not_published;
        }

        @Override
        protected String getMethod() {
            return "wp.getPage";
        }

        @Override
        protected Object[] getParams(Postable post) {
            Object[] params = { WordPress.currentBlog.getBlogId(),
                    post.getPostId(), WordPress.currentBlog.getUsername(),
                    WordPress.currentBlog.getPassword() };
            return params;
        }
    }

    private static class DeletePageTask extends AbsDeleteTask {

        public DeletePageTask(AbsListActivity activity) {
            super(activity);
        }

        @Override
        protected int getDeletedMessageId() {
            return R.string.page_deleted;
        }

        @Override
        protected int getMessageWhatId() {
            return R.string.page;
        }

        @Override
        protected String getMethod() {
            return "wp.deletePage";
        }

        @Override
        protected Object[] getParams(Postable post) {
            Object[] params = { WordPress.currentBlog.getBlogId(),
                    WordPress.currentBlog.getUsername(),
                    WordPress.currentBlog.getPassword(), post.getPostId() };
            return params;
        }
    }

}
