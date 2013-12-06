package org.wordpress.android.task;

import android.os.AsyncTask;

import org.wordpress.android.util.DeviceUtils;

public abstract class MultiAsyncTask<Params, Progress, Result> extends
        AsyncTask<Params, Progress, Result> {

    public final AsyncTask<Params, Progress, Result> executeOnMultiThread(
            Params... params) {
        if (DeviceUtils.isOverEqualThanHoneycomb()) {
            return executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        } else {
            return execute(params);
        }
    }
}
