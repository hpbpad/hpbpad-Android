package org.wordpress.android.task;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.widget.Toast;

import com.justsystems.hpb.pad.R;

import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.Postable;
import org.wordpress.android.ui.list.AbsListActivity;

public abstract class AbsDeleteTask extends
        MultiAsyncTask<Postable, Void, Boolean> {
    protected final AbsListActivity activity;
    private final Resources res;

    private String errorMsg;

    public AbsDeleteTask(AbsListActivity activity) {
        this.activity = activity;
        this.res = activity.getResources();
    }

    @Override
    protected void onPreExecute() {
        // pop out of the detail view if on a smaller screen
        activity.popPostDetail();
        activity.showDialog(AbsListActivity.ID_DIALOG_DELETING);
    }

    @Override
    protected Boolean doInBackground(Postable... params) {
        boolean result = false;
        Postable post = params[0];
        XMLRPCClient client = new XMLRPCClient(WordPress.currentBlog.getUrl(),
                WordPress.currentBlog.getHttpuser(),
                WordPress.currentBlog.getHttppassword());

        try {

            client.call(getMethod(), getParams(post));

            result = true;
        } catch (final XMLRPCException e) {
            errorMsg = String.format(res.getString(R.string.error_delete_post),
                    res.getText(getMessageWhatId()));
            result = false;
        }
        return result;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        activity.dismissDialog(AbsListActivity.ID_DIALOG_DELETING);
        activity.attemptToSelectPost();
        if (result) {
            Toast.makeText(activity, res.getText(getDeletedMessageId()),
                    Toast.LENGTH_SHORT).show();
            activity.checkForLocalChanges(false);
        } else {
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
        }

    }

    protected abstract int getMessageWhatId();

    protected abstract int getDeletedMessageId();

    protected abstract String getMethod();

    protected abstract Object[] getParams(Postable post);

}
