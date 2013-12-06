package org.wordpress.android.util;

import java.util.ArrayList;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import org.wordpress.android.models.CustomTypePost;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.Postable;
import org.wordpress.android.task.AbsUploadTask;
import org.wordpress.android.task.UploadCustomTypePostTask;
import org.wordpress.android.task.UploadPostTask;

public class PostUploadService extends Service {

    private static Context context;
    private static ArrayList<Postable> listOfPosts = new ArrayList<Postable>();
    private static NotificationManager nm;
    private AbsUploadTask currentTask = null;

    public static void addPostToUpload(Postable currentPost) {
        synchronized (listOfPosts) {
            listOfPosts.add(currentPost);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = this.getApplicationContext();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        synchronized (listOfPosts) {
            if (listOfPosts.size() == 0 || context == null) {
                this.stopSelf();
                return;
            }
        }
        uploadNextPost();
    }

    private void uploadNextPost() {
        synchronized (listOfPosts) {
            if (currentTask == null) { //make sure nothing is running
                if (listOfPosts.size() > 0) {
                    Postable currentPost = listOfPosts.remove(0);
                    if (currentPost.getType() == Postable.TYP_POST
                            || currentPost.getType() == Postable.TYP_PAGE) {
                        new UploadPostTask(this)
                                .executeOnMultiThread((Post) currentPost);
                    } else if (currentPost.getType() == Postable.TYP_CUSTOM_TYPE_POST) {
                        new UploadCustomTypePostTask(this,
                                (CustomTypePost) currentPost)
                                .executeOnMultiThread();
                    }
                } else {
                    this.stopSelf();
                }
            }
        }
    }

    public void postUploaded() {
        synchronized (listOfPosts) {
            currentTask = null;
        }
        uploadNextPost();
    }

    public String cleanXMLRPCErrorMessage(String message) {
        if (message != null) {
            if (message.indexOf(": ") > -1)
                message = message.substring(message.indexOf(": ") + 2,
                        message.length());
            if (message.indexOf("[code") > -1)
                message = message.substring(0, message.indexOf("[code"));
            return message;
        } else {
            return "";
        }
    }
}
