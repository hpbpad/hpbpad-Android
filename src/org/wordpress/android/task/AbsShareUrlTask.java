package org.wordpress.android.task;

import java.util.HashMap;
import java.util.Map;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;

import com.justsystems.hpb.pad.R;
import com.justsystems.hpb.pad.util.Debug;

import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.Postable;
import org.wordpress.android.ui.list.AbsListActivity;

public abstract class AbsShareUrlTask extends
        MultiAsyncTask<Postable, Void, String> {

    protected final AbsListActivity activity;
    private final Resources res;

    private String errorMsg;

    public AbsShareUrlTask(AbsListActivity activity) {
        this.activity = activity;
        this.res = activity.getResources();
    }

    private Postable post;

    @Override
    protected void onPreExecute() {
        activity.showDialog(AbsListActivity.ID_DIALOG_SHARE);
    }

    @Override
    protected String doInBackground(Postable... params) {
        String result = null;
        post = params[0];
        if (post == null)
            return null;
        XMLRPCClient client = new XMLRPCClient(WordPress.currentBlog.getUrl(),
                WordPress.currentBlog.getHttpuser(),
                WordPress.currentBlog.getHttppassword());

        Object versionResult = new Object();
        try {
            versionResult = (Object) client.call(getMethod(), getParams(post));
        } catch (XMLRPCException e) {
            errorMsg = this.res.getText(R.string.error_generic).toString();
            return null;
        }

        if (versionResult == null) {
            return null;
        }
        if (!(versionResult instanceof HashMap<?, ?>)) {
            Debug.logd(this.getClass().toString(), "class cast "
                    + versionResult.getClass().toString() + " cast to hashmap");
            return null;
        }

        try {
            Map<?, ?> contentHash = (Map<?, ?>) versionResult;

            if (!isStatusPublish(contentHash)) {
                final int id = getNotPublishedMessageId();
                errorMsg = this.res.getText(id).toString();
                return null;
            } else {
                String postURL = contentHash.get("link").toString();
                String shortlink = activity.getShortlinkTagHref(postURL);
                if (shortlink == null) {
                    result = postURL;
                } else {
                    result = shortlink;
                }
            }
        } catch (Exception e) {
            errorMsg = this.res.getText(R.string.error_generic).toString();
            return null;
        }

        return result;
    }

    @Override
    protected void onPostExecute(String shareURL) {
        activity.dismissDialog(AbsListActivity.ID_DIALOG_SHARE);
        if (shareURL == null) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                    this.activity);
            dialogBuilder.setTitle(res.getText(R.string.connection_error));
            dialogBuilder.setMessage(errorMsg);
            dialogBuilder.setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                            // Just close the window.
                        }
                    });
            dialogBuilder.setCancelable(true);
            if (!activity.isFinishing()) {
                dialogBuilder.create().show();
            }
        } else {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_SUBJECT, post.getTitle());
            share.putExtra(Intent.EXTRA_TEXT, shareURL);
            activity.startActivity(Intent.createChooser(share,
                    this.res.getText(R.string.share_url)));

        }

    }

    protected abstract boolean isStatusPublish(Map<?, ?> contentHash);

    protected abstract int getNotPublishedMessageId();

    protected abstract String getMethod();

    protected abstract Object[] getParams(Postable post);
}
