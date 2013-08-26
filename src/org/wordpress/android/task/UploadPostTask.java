package org.wordpress.android.task;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.support.v4.content.IntentCompat;
import android.util.Log;

import com.justsystems.hpb.pad.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import org.wordpress.android.models.Post;
import org.wordpress.android.ui.list.PagesActivity;
import org.wordpress.android.ui.list.PostsActivity;
import org.wordpress.android.util.PostUploadService;

public class UploadPostTask extends AbsUploadTask {

    private Post post;

    public UploadPostTask(PostUploadService service) {
        super(service);
    }

    @Override
    protected Boolean doInBackground(Post... posts) {

        final Resources res = context.getResources();

        post = posts[0];

        String postOrPage = (String) (post.isPage() ? res
                .getText(R.string.page_id) : res.getText(R.string.post_id));
        String message = res.getText(R.string.uploading) + " " + postOrPage;
        showNotification(post, message);

        if (post.getPostStatus() == null) {
            post.setPost_status("publish");
        }
        Boolean publishThis = false;

        String descriptionContent = makeContent(res, post,
                post.getDescription(), post.getMt_text_more(), postOrPage);

        // If media file upload failed, let's stop here and prompt the user
        if (mediaError)
            return false;

        JSONArray categories = post.getCategories();
        String[] theCategories = null;
        if (categories != null) {
            theCategories = new String[categories.length()];
            for (int i = 0; i < categories.length(); i++) {
                try {
                    theCategories[i] = categories.getString(i);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        Map<String, Object> contentStruct = new HashMap<String, Object>();

        if (!post.isPage() && post.isLocalDraft()) {
            // post format
            if (!post.getPostFormat().equals("")) {
                if (!post.getPostFormat().equals("standard"))
                    contentStruct.put("wp_post_format", post.getPostFormat());
            }
        }

        contentStruct.put("post_type", (post.isPage()) ? "page" : "post");
        contentStruct.put("title", post.getTitle());
        long pubDate = post.getDate_created_gmt();
        if (pubDate != 0) {
            Date date_created_gmt = new Date(pubDate);
            contentStruct.put("date_created_gmt", date_created_gmt);
            Date dateCreated = new Date(pubDate
                    + (date_created_gmt.getTimezoneOffset() * 60000));
            contentStruct.put("dateCreated", dateCreated);
        }

        contentStruct.put("description", descriptionContent);
        if (!post.isPage()) {
            if (post.getMt_keywords() != "") {
                contentStruct.put("mt_keywords", post.getMt_keywords());
            }
            if (theCategories != null) {
                if (theCategories.length > 0)
                    contentStruct.put("categories", theCategories);
            }
        }

        if (post.getMt_excerpt() != null)
            contentStruct.put("mt_excerpt", post.getMt_excerpt());

        contentStruct.put((post.isPage()) ? "page_status" : "post_status",
                post.getPostStatus());
        Double latitude = 0.0;
        Double longitude = 0.0;
        if (!post.isPage()) {
            latitude = (Double) post.getLatitude();
            longitude = (Double) post.getLongitude();

            if (latitude > 0) {
                Map<Object, Object> hLatitude = new HashMap<Object, Object>();
                hLatitude.put("key", "geo_latitude");
                hLatitude.put("value", latitude);

                Map<Object, Object> hLongitude = new HashMap<Object, Object>();
                hLongitude.put("key", "geo_longitude");
                hLongitude.put("value", longitude);

                Map<Object, Object> hPublic = new HashMap<Object, Object>();
                hPublic.put("key", "geo_public");
                hPublic.put("value", 1);

                Object[] geo = { hLatitude, hLongitude, hPublic };

                contentStruct.put("custom_fields", geo);
            }
        }

        // featured image
        if (featuredImageID != -1)
            contentStruct.put("wp_post_thumbnail", featuredImageID);

        XMLRPCClient client = new XMLRPCClient(post.getBlog().getUrl(), post
                .getBlog().getHttpuser(), post.getBlog().getHttppassword());

        if (post.getQuickPostType() != null)
            client.addQuickPostHeader(post.getQuickPostType());

        n.setLatestEventInfo(context, message, message, n.contentIntent);
        nm.notify(notificationID, n);
        if (post.getPassword() != null) {
            contentStruct.put("wp_password", post.getPassword());
        }
        Object[] params;

        if (post.isLocalDraft() && !post.isUploaded())
            params = new Object[] { post.getBlog().getBlogId(),
                    post.getBlog().getUsername(), post.getBlog().getPassword(),
                    contentStruct, publishThis };
        else
            params = new Object[] { post.getPostId(),
                    post.getBlog().getUsername(), post.getBlog().getPassword(),
                    contentStruct, publishThis };

        try {
            client.call(
                    (post.isLocalDraft() && !post.isUploaded()) ? "metaWeblog.newPost"
                            : "metaWeblog.editPost", params);
            post.setUploaded(true);
            post.setLocalChange(false);
            post.update();
            return true;
        } catch (final XMLRPCException e) {
            error = String.format(
                    res.getText(R.string.error_upload).toString(), post
                            .isPage() ? res.getText(R.string.page).toString()
                            : res.getText(R.string.post).toString())
                    + " " + cleanXMLRPCErrorMessage(e.getMessage());
            mediaError = false;
            Log.i("WP", error);
        }

        return false;
    }

    @Override
    void showErrorNotifination() {
        final Resources res = context.getResources();
        String postOrPage = (String) (post.isPage() ? res
                .getText(R.string.page_id) : res.getText(R.string.post_id));
        Intent notificationIntent = new Intent(context,
                (post.isPage()) ? PagesActivity.class : PostsActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | IntentCompat.FLAG_ACTIVITY_CLEAR_TASK);
        notificationIntent.setAction("android.intent.action.MAIN");
        notificationIntent.addCategory("android.intent.category.LAUNCHER");
        notificationIntent.setData((Uri
                .parse("custom://wordpressNotificationIntent"
                        + post.getBlogID())));
        notificationIntent.putExtra("fromNotification", true);
        notificationIntent.putExtra("errorMessage", error);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        n.flags |= Notification.FLAG_AUTO_CANCEL;
        String errorText = res.getText(R.string.upload_failed).toString();
        if (mediaError)
            errorText = res.getText(R.string.media) + " "
                    + res.getText(R.string.error);
        n.setLatestEventInfo(context,
                (mediaError) ? errorText : res.getText(R.string.upload_failed),
                (mediaError) ? error : postOrPage + " " + errorText + ": "
                        + error, pendingIntent);

        nm.notify(notificationID, n); // needs a unique idf
    }

    @Override
    protected Intent createNotificationIntent() {
        Intent notificationIntent = new Intent(context, PostsActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | IntentCompat.FLAG_ACTIVITY_CLEAR_TASK);
        notificationIntent.setAction("android.intent.action.MAIN");
        notificationIntent.addCategory("android.intent.category.LAUNCHER");
        notificationIntent.setData((Uri
                .parse("custom://wordpressNotificationIntent"
                        + post.getBlog().getId())));
        notificationIntent.putExtra("fromNotification", true);
        return notificationIntent;
    }

}
