package org.wordpress.android.util;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.CustomTypePost;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.Postable;
import org.wordpress.android.task.UploadCustomTypePostTask;
import org.wordpress.android.task.UploadPostTask;

public class PostUploadService extends Service {

    private Postable post;
    private Context context;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        post = WordPress.getCurrentPost();
        context = this.getApplicationContext();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        if (post == null || context == null) {
            this.stopSelf();
            return;
        } else if (post.getType() == Postable.TYP_POST
                || post.getType() == Postable.TYP_PAGE) {
            new UploadPostTask(this).executeOnMultiThread((Post) post);
        } else if (post.getType() == Postable.TYP_CUSTOM_TYPE_POST) {
            new UploadCustomTypePostTask(this, (CustomTypePost) post)
                    .executeOnMultiThread();
        }
    }
}
