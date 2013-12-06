package org.wordpress.android.task;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.support.v4.content.IntentCompat;
import android.util.Log;

import com.justsystems.hpb.pad.R;

import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import org.wordpress.android.models.CustomTypePost;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.Term;
import org.wordpress.android.ui.posts.CustomPostTypePostsActivity;
import org.wordpress.android.util.PostUploadService;

public final class UploadCustomTypePostTask extends AbsUploadTask {

    private CustomTypePost customPost;

    public UploadCustomTypePostTask(PostUploadService service,
            CustomTypePost customPost) {
        super(service);
        this.customPost = customPost;
    }

    @Override
    protected Boolean doInBackground(Post... posts) {

        final Resources res = context.getResources();

        // add the uploader to the notification bar
        nm = (NotificationManager) context.getSystemService("notification");

        String post = (String) res.getText(R.string.post_id);
        String message = res.getText(R.string.uploading) + " " + post;
        showNotification(customPost, post);

        if (customPost.getPostStatus() == null) {
            customPost.setPost_status("publish");
        }

        String textBeforeMore, textAfterMore;
        final String content = customPost.getContent();
        int moreIndex = content.indexOf(MORE);
        if (moreIndex == -1) {
            textBeforeMore = customPost.getContent();
            textAfterMore = null;
        } else {
            textBeforeMore = content.substring(0, moreIndex);
            textAfterMore = content.substring(moreIndex + MORE.length());
        }
        String descriptionContent = makeContent(res, customPost,
                textBeforeMore, textAfterMore, post);

        // If media file upload failed, let's stop here and prompt the user
        if (mediaError)
            return false;

        Map<String, Object> contentStruct = new HashMap<String, Object>();

        if (customPost.isLocalDraft()) {
            // post format
            if (!customPost.getPostFormat().equals("")) {
                if (!customPost.getPostFormat().equals("standard"))
                    contentStruct.put("wp_post_format",
                            customPost.getPostFormat());
            }
        }

        contentStruct.put("post_type", customPost.getPost_type());
        contentStruct.put("post_status", customPost.getPostStatus());
        contentStruct.put("post_title", customPost.getTitle());
        // contentStruct.put("post_author", customPost.getPost_author());
        if (customPost.getExcerpt() != null)
            contentStruct.put("post_excerpt", customPost.getExcerpt());

        contentStruct.put("post_content", descriptionContent);

        long pubDate = customPost.getDate_created_gmt();
        if (pubDate != 0) {
            Date date_created_gmt = new Date(pubDate);
            contentStruct.put("post_date_gmt ", date_created_gmt);
            Date dateCreated = new Date(pubDate
                    + (date_created_gmt.getTimezoneOffset() * 60000));
            contentStruct.put("dateCreated", dateCreated);
        }

        contentStruct.put("post_format", customPost.getPostFormat());

        if (customPost.getPost_name() != null) {
            contentStruct.put("post_name", customPost.getPost_name());
        }

        contentStruct.put("post_password", customPost.getPassword());

        if (customPost.getComment_status() != null) {
            contentStruct.put("comment_status", customPost.getComment_status());
        }
        if (customPost.getPing_status() != null) {
            contentStruct.put("ping_status", customPost.getPing_status());
        }

        contentStruct.put("sticky", customPost.isSticky());

        Term[] terms = customPost.getTerms();
        if (terms != null) {
            HashMap<String, Object> o = new HashMap<String, Object>();
            if (terms.length == 0) {
                // how can i remove alrealy set term
            } else {
                HashMap<String, ArrayList<String>> taxonomyMap = new HashMap<String, ArrayList<String>>();

                for (int i = 0; i < terms.length; i++) {
                    Term term = terms[i];
                    boolean addTaxonomy = true;
                    for (String key : taxonomyMap.keySet()) {
                        if (key.equals(term.getTaxonomy())) {
                            taxonomyMap.get(key).add(term.getTermId());
                            Log.d("taxonomy" + i, "taxonomy:" + key
                                    + " term.name" + term.getName());
                            addTaxonomy = false;
                            break;
                        }
                    }
                    if (addTaxonomy) {
                        ArrayList<String> list = new ArrayList<String>();
                        list.add(term.getTermId());
                        taxonomyMap.put(term.getTaxonomy(), list);
                        Log.d("taxonomy" + i, "taxonomy:" + term.getTaxonomy()
                                + " term.name" + term.getName());
                    }
                }

                for (String key : taxonomyMap.keySet()) {
                    Object[] termArray = taxonomyMap.get(key).toArray(
                            new Object[0]);
                    o.put(key, termArray);
                }
            }
            contentStruct.put("terms", o);
        }

        // featured image
        if (featuredImageID != -1) {
            contentStruct.put("post_thumbnail", featuredImageID);
        }

        // contentStruct.put("post_parent", customPost.getPost_parent());

        XMLRPCClient client = new XMLRPCClient(customPost.getBlog().getUrl(),
                customPost.getBlog().getHttpuser(), customPost.getBlog()
                        .getHttppassword());

        n.setLatestEventInfo(context, message, message, n.contentIntent);
        nm.notify(notificationID, n);

        Object[] params;

        if (customPost.isLocalDraft() && !customPost.isUploaded())
            params = new Object[] { customPost.getBlog().getBlogId(),
                    customPost.getBlog().getUsername(),
                    customPost.getBlog().getPassword(), contentStruct };
        else
            params = new Object[] { customPost.getBlog().getBlogId(),
                    customPost.getBlog().getUsername(),
                    customPost.getBlog().getPassword(), customPost.getPostId(),
                    contentStruct };

        try {
            client.call(
                    (customPost.isLocalDraft() && !customPost.isUploaded()) ? "wp.newPost"
                            : "wp.editPost", params);
            customPost.setUploaded(true);
            customPost.update();
            return true;
        } catch (final XMLRPCException e) {
            error = String.format(
                    res.getText(R.string.error_upload).toString(),
                    res.getText(R.string.post).toString())
                    + " " + cleanXMLRPCErrorMessage(e.getMessage());
            mediaError = false;
            Log.i("WP", error);
        }

        return false;
    }

    @Override
    void showErrorNotifination() {
        final Resources res = context.getResources();
        String postOrPage = (String) res.getText(R.string.post_id);
        Intent notificationIntent = new Intent(context,
                CustomPostTypePostsActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | IntentCompat.FLAG_ACTIVITY_CLEAR_TASK);
        notificationIntent.setAction("android.intent.action.MAIN");
        notificationIntent.addCategory("android.intent.category.LAUNCHER");
        notificationIntent.setData((Uri
                .parse("custom://wordpressNotificationIntent"
                        + customPost.getBlogID())));
        notificationIntent.putExtra("fromNotification", true);
        notificationIntent.putExtra("errorMessage", error);
        notificationIntent.putExtra("type_name", customPost.getPost_type());
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

        nm.notify(notificationID, n);
    }

    @Override
    protected Intent createNotificationIntent() {
        Intent notificationIntent = new Intent(context,
                CustomPostTypePostsActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | IntentCompat.FLAG_ACTIVITY_CLEAR_TASK);
        notificationIntent.setAction("android.intent.action.MAIN");
        notificationIntent.addCategory("android.intent.category.LAUNCHER");
        notificationIntent.setData((Uri
                .parse("custom://wordpressNotificationIntent"
                        + customPost.getBlog().getId())));
        notificationIntent.putExtra("fromNotification", true);
        notificationIntent.putExtra("type_name", customPost.getPost_type());
        return notificationIntent;
    }
}
