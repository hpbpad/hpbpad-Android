package org.wordpress.android.task;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.text.Spannable;
import android.text.SpannableString;
import android.webkit.MimeTypeMap;

import com.justsystems.hpb.pad.R;
import com.justsystems.hpb.pad.util.Debug;

import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.MediaFile;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.Postable;
import org.wordpress.android.util.EscapeUtils;
import org.wordpress.android.util.ImageHelper;
import org.wordpress.android.util.PostUploadService;
import org.wordpress.android.util.WPHtml;
import org.wordpress.android.util.WPImageSpan;

abstract class AbsUploadTask extends MultiAsyncTask<Post, Boolean, Boolean> {

    protected final static String MORE = "<!--more-->";

    private PostUploadService service;
    protected Context context;

    protected String error = "";
    protected boolean mediaError = false;

    protected NotificationManager nm;
    protected int notificationID;
    protected Notification n;

    protected static int featuredImageID = -1;

    public AbsUploadTask(PostUploadService service) {
        this.service = service;
        this.context = service.getApplicationContext();
    }

    @Override
    protected void onPostExecute(Boolean postUploadedSuccessfully) {
        if (postUploadedSuccessfully) {
            WordPress.postUploaded();
            nm.cancel(notificationID);
        } else {
            showErrorNotifination();
        }
        service.stopSelf();
    }

    abstract void showErrorNotifination();

    protected void showNotification(Postable post, String message) {
        // add the uploader to the notification bar
        nm = (NotificationManager) context.getSystemService("notification");

        n = new Notification(R.drawable.notification_icon, message,
                System.currentTimeMillis());

        Intent notificationIntent = createNotificationIntent();
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, Intent.FLAG_ACTIVITY_CLEAR_TOP);

        n.setLatestEventInfo(context, message, message, pendingIntent);

        notificationID = 22 + Integer.valueOf(post.getBlog().getId());
        nm.notify(notificationID, n); // needs a unique id
    }

    protected abstract Intent createNotificationIntent();

    protected String makeContent(Resources res, Postable postable,
            String content, String moreText, String postOrPage) {
        final boolean isMoreTextNull = moreText == null || "".equals(moreText);
        String descriptionContent = "", moreContent = null;
        String imgTags = "<img[^>]+android-uri\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>";
        Pattern pattern = Pattern.compile(imgTags);

        if (postable.isLocalDraft()) {
            descriptionContent = localDraftToHtml(res, postable, content,
                    postOrPage);
        } else {
            descriptionContent = contentToHtml(postable, content, pattern);
        }
        if (!isMoreTextNull) {
            if (postable.isLocalDraft()) {
                moreContent = localDraftToHtml(res, postable, moreText,
                        postOrPage);
            } else {
                moreContent = contentToHtml(postable, moreText, pattern);
            }
        }

        if (postable.getType() == Postable.TYP_POST && postable.isLocalDraft()) {
            // add the tagline
            SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(context);

            if (prefs.getBoolean("wp_pref_signature_enabled", false)) {
                String tagline = prefs
                        .getString("wp_pref_post_signature", null);
                if (tagline != null) {
                    String tag = "\n\n<span class=\"post_sig\">" + tagline
                            + "</span>\n\n";
                    if (isMoreTextNull)
                        descriptionContent += tag;
                    else
                        moreContent += tag;
                }
            }
        }

        if (!isMoreTextNull) {
            descriptionContent = descriptionContent.trim() + MORE + moreContent;
            if (postable.getType() == Postable.TYP_PAGE
                    || postable.getType() == Postable.TYP_POST) {
                ((Post) postable).setMt_text_more("");
            }
        }

        // get rid of the p and br tags that the editor adds.
        if (postable.isLocalDraft()) {
            descriptionContent = descriptionContent.replace("<p>", "")
                    .replace("</p>", "\n").replace("<br>", "");
        }

        // gets rid of the weird character android inserts after images
        descriptionContent = descriptionContent.replaceAll("\uFFFC", "");

        return descriptionContent;
    }

    private String localDraftToHtml(Resources res, Postable postable,
            String content, String type) {

        Spannable s = (Spannable) WPHtml.fromHtml(content, context, postable);
        WPImageSpan[] click_spans = s
                .getSpans(0, s.length(), WPImageSpan.class);

        if (click_spans.length != 0) {

            for (int i = 0; i < click_spans.length; i++) {
                String prompt = res.getText(R.string.uploading_media_item)
                        + String.valueOf(i + 1);
                n.setLatestEventInfo(context, res.getText(R.string.uploading)
                        + " " + type, prompt, n.contentIntent);
                nm.notify(notificationID, n);
                WPImageSpan wpIS = click_spans[i];
                int start = s.getSpanStart(wpIS);
                int end = s.getSpanEnd(wpIS);
                MediaFile mf = new MediaFile();
                mf.setPostID(postable.getId());
                mf.setTitle(wpIS.getTitle());
                mf.setCaption(wpIS.getCaption());
                mf.setDescription(wpIS.getDescription());
                mf.setFeatured(wpIS.isFeatured());
                mf.setFeaturedInPost(wpIS.isFeaturedInPost());
                mf.setFileName(wpIS.getImageSource().toString());
                mf.setHorizontalAlignment(wpIS.getHorizontalAlignment());
                mf.setWidth(wpIS.getWidth());

                String imgHTML = uploadMediaFile(mf, postable);
                if (imgHTML != null) {
                    SpannableString ss = new SpannableString(imgHTML);
                    s.setSpan(ss, start, end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    s.removeSpan(wpIS);
                } else {
                    s.removeSpan(wpIS);
                    mediaError = true;
                }
            }
        }
        return WPHtml.toHtml(s);
    }

    private String contentToHtml(Postable postable, String content,
            Pattern pattern) {
        Matcher matcher = pattern.matcher(content);

        List<String> imageTags = new ArrayList<String>();
        while (matcher.find()) {
            imageTags.add(matcher.group());
        }

        for (String tag : imageTags) {
            Pattern p = Pattern.compile("android-uri=\"([^\"]+)\"");
            Matcher m = p.matcher(tag);
            String imgPath = "";
            if (m.find()) {
                continue;
            }
            imgPath = m.group(1);
            if (imgPath.equals("")) {
                continue;
            }
            MediaFile mf = WordPress.wpDB.getMediaFile(imgPath,
                    Long.parseLong(postable.getPostId()));

            if (mf == null) {
                continue;
            }
            String imgHTML = uploadMediaFile(mf, postable);
            if (imgHTML != null) {
                content = content.replace(tag, imgHTML);
            } else {
                content = content.replace(tag, "");
                mediaError = true;
            }
        }
        return content;
    }

    private String uploadMediaFile(MediaFile mf, Postable post) {
        final Blog blog = post.getBlog();

        String content = null;

        // image variables
        String finalThumbnailUrl = null;
        String finalImageUrl = null;

        // check for image, and upload it
        if (mf.getFileName() == null) {
            return "";
        }
        XMLRPCClient client = new XMLRPCClient(blog.getUrl(),
                blog.getHttpuser(), blog.getHttppassword());

        String curImagePath = "";

        curImagePath = mf.getFileName();

        if (curImagePath.contains("video")) { // upload the video
            content = doVideo(mf, client, post);
            if (content == null) {
                return content;
            }
        } else {
            for (int i = 0; i < 2; i++) {

                // create temp file for media upload
                String tempFileName = "wp-" + System.currentTimeMillis();
                try {
                    context.openFileOutput(tempFileName, Context.MODE_PRIVATE);
                } catch (FileNotFoundException e) {
                    mediaError = true;
                    error = context.getString(R.string.file_not_found);
                    return null;
                }

                File tempFile = context.getFileStreamPath(tempFileName);

                curImagePath = mf.getFileName();

                if (i != 0
                        && (!blog.isFullSizeImage() || blog.getMaxImageWidth()
                                .equals("Original Size"))
                        && !blog.isScaledImage()) {
                    continue;
                }
                Uri imageUri = Uri.parse(curImagePath);
                File jpeg = null;
                String mimeType = "", orientation = "", path = "";

                if (imageUri.toString().contains("content:")) {
                    String[] projection;
                    Uri imgPath;

                    projection = new String[] { Images.Media._ID,
                            Images.Media.DATA, Images.Media.MIME_TYPE,
                            Images.Media.ORIENTATION };

                    imgPath = imageUri;

                    Cursor cur = context.getContentResolver().query(imgPath,
                            projection, null, null, null);
                    String thumbData = "";

                    if (cur.moveToFirst()) {

                        int dataColumn, mimeTypeColumn, orientationColumn;

                        dataColumn = cur.getColumnIndex(Images.Media.DATA);
                        mimeTypeColumn = cur
                                .getColumnIndex(Images.Media.MIME_TYPE);
                        orientationColumn = cur
                                .getColumnIndex(Images.Media.ORIENTATION);

                        orientation = cur.getString(orientationColumn);
                        thumbData = cur.getString(dataColumn);
                        mimeType = cur.getString(mimeTypeColumn);
                        jpeg = new File(thumbData);
                        path = thumbData;
                        mf.setFilePath(jpeg.getPath());
                    }
                    cur.close();
                } else { // file is not in media library
                    path = imageUri.toString().replace("file://", "");
                    jpeg = new File(path);
                    String extension = MimeTypeMap
                            .getFileExtensionFromUrl(path);
                    if (extension != null) {
                        MimeTypeMap mime = MimeTypeMap.getSingleton();
                        mimeType = mime.getMimeTypeFromExtension(extension);
                        if (mimeType == null)
                            mimeType = "image/jpeg";
                    }
                    mf.setFilePath(path);
                }

                // check if the file exists
                if (jpeg == null) {
                    error = context.getString(R.string.file_not_found);
                    mediaError = true;
                    return null;
                }

                ImageHelper ih = new ImageHelper();
                orientation = ih.getExifOrientation(path, orientation);

                String imageTitle = jpeg.getName();

                byte[] finalBytes = getFinalByte(i, mf, blog, jpeg, orientation);

                // try to upload the image
                Map<String, Object> m = new HashMap<String, Object>();

                m.put("name", imageTitle);
                m.put("type", mimeType);
                if (i == 0 || blog.isScaledImage()) {
                    m.put("bits", finalBytes);
                } else {
                    m.put("bits", mf);
                }
                m.put("overwrite", true);

                Object[] params = { 1, blog.getUsername(), blog.getPassword(),
                        m };

                Object result = null;

                try {
                    result = (Object) client.callUploadFile("wp.uploadFile",
                            params, tempFile);
                } catch (XMLRPCException e) {
                    error = context.getResources().getString(
                            R.string.error_media_upload)
                            + ": " + cleanXMLRPCErrorMessage(e.getMessage());
                    mediaError = true;
                    return null;
                }

                Map<?, ?> contentHash = (HashMap<?, ?>) result;

                String resultURL = contentHash.get("url").toString();

                if (mf.isFeatured()) {
                    try {
                        if (contentHash.get("id") != null) {
                            featuredImageID = Integer.parseInt(contentHash.get(
                                    "id").toString());
                            if (!mf.isFeaturedInPost())
                                return "";
                        }
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }

                if (i == 0) {
                    finalThumbnailUrl = resultURL;
                } else {
                    if (post.getBlog().isFullSizeImage()
                            || post.getBlog().isScaledImage()) {
                        finalImageUrl = resultURL;
                    } else {
                        finalImageUrl = "";
                    }
                }

                String alignment = "";
                switch (mf.getHorizontalAlignment()) {
                case 0:
                    alignment = "alignnone";
                    break;
                case 1:
                    alignment = "alignleft";
                    break;
                case 2:
                    alignment = "aligncenter";
                    break;
                case 3:
                    alignment = "alignright";
                    break;
                }

                String alignmentCSS = "class=\"" + alignment + " size-full\" ";
                if (resultURL != null) {
                    if (i != 0
                            && (blog.isFullSizeImage() || blog.isScaledImage())) {
                        content = "<a href=\"" + finalImageUrl
                                + "\"><img title=\"" + mf.getTitle() + "\" "
                                + alignmentCSS + "alt=\"image\" src=\""
                                + finalThumbnailUrl + "\" /></a>";
                    } else {
                        if (i == 0
                                && (blog.isFullSizeImage() == false && !post
                                        .getBlog().isScaledImage())
                                || (blog.getMaxImageWidth().equals(
                                        "Original Size") && !post.getBlog()
                                        .isScaledImage())) {
                            content = "<a href=\"" + finalThumbnailUrl
                                    + "\"><img title=\"" + mf.getTitle()
                                    + "\" " + alignmentCSS
                                    + "alt=\"image\" src=\""
                                    + finalThumbnailUrl + "\" /></a>";
                        }
                    }

                    // XXX
                    if (content == null) {
                        content = "";
                    }

                    if ((i == 0
                            && (!blog.isFullSizeImage() && !blog
                                    .isScaledImage()) || (blog
                            .getMaxImageWidth().equals("Original Size") && !blog
                            .isScaledImage()))
                            || i == 1) {
                        if (!mf.getCaption().equals("")) {
                            content = String
                                    .format("[caption id=\"\" align=\"%s\" width=\"%d\" caption=\"%s\"]%s[/caption]",
                                            alignment, mf.getWidth(),
                                            EscapeUtils.escapeHtml(mf
                                                    .getCaption()), content);
                        }
                    }
                }

            }// end image check
        }
        return content;
    }

    private String doVideo(MediaFile mf, XMLRPCClient client, Postable post) {

        // create temp file for media upload
        String tempFileName = "wp-" + System.currentTimeMillis();
        try {
            context.openFileOutput(tempFileName, Context.MODE_PRIVATE);
        } catch (FileNotFoundException e) {
            error = context.getResources()
                    .getString(R.string.file_error_create);
            mediaError = true;
            return null;
        }

        File tempFile = context.getFileStreamPath(tempFileName);
        final String curImagePath = mf.getFileName();

        Uri videoUri = Uri.parse(curImagePath);
        File fVideo = null;
        String mimeType = "", xRes = "", yRes = "";

        if (videoUri.toString().contains("content:")) {
            String[] projection;
            Uri imgPath;

            projection = new String[] { Video.Media._ID, Video.Media.DATA,
                    Video.Media.MIME_TYPE, Video.Media.RESOLUTION };
            imgPath = videoUri;

            Cursor cur = context.getContentResolver().query(imgPath,
                    projection, null, null, null);
            String thumbData = "";

            if (cur.moveToFirst()) {

                int mimeTypeColumn, resolutionColumn, dataColumn;

                dataColumn = cur.getColumnIndex(Video.Media.DATA);
                mimeTypeColumn = cur.getColumnIndex(Video.Media.MIME_TYPE);
                resolutionColumn = cur.getColumnIndex(Video.Media.RESOLUTION);

                mf = new MediaFile();

                thumbData = cur.getString(dataColumn);
                mimeType = cur.getString(mimeTypeColumn);
                fVideo = new File(thumbData);
                mf.setFilePath(fVideo.getPath());
                String resolution = cur.getString(resolutionColumn);
                if (resolution != null) {
                    String[] resx = resolution.split("x");
                    xRes = resx[0];
                    yRes = resx[1];
                } else {
                    // set the width of the video to the
                    // thumbnail
                    // width, else 640x480
                    if (!post.getBlog().getMaxImageWidth()
                            .equals("Original Size")) {
                        xRes = post.getBlog().getMaxImageWidth();
                        yRes = String.valueOf(Math.round(Integer.valueOf(post
                                .getBlog().getMaxImageWidth()) * 0.75));
                    } else {
                        xRes = "640";
                        yRes = "480";
                    }

                }
                cur.close();
            }
        } else { // file is not in media library
            fVideo = new File(videoUri.toString().replace("file://", ""));
        }

        if (fVideo == null) {
            error = context.getResources().getString(
                    R.string.error_media_upload)
                    + ".";
            return null;
        }

        final String imageTitle = fVideo.getName();
        Object[] params = createVideoParams(post, imageTitle, mimeType, mf);

        Object result = null;

        try {
            result = client.callUploadFile("wp.uploadFile", params, tempFile);
        } catch (XMLRPCException e) {
            error = context.getResources().getString(
                    R.string.error_media_upload)
                    + ": " + cleanXMLRPCErrorMessage(e.getMessage());
            return null;
        }

        if (result == null) {
            return null;
        }
        if (!(result instanceof HashMap<?, ?>)) {
            Debug.logd(this.getClass().toString(), "class cast "
                    + result.getClass().toString() + " cast to hashmap");
            return null;
        }

        Map<?, ?> contentHash = (HashMap<?, ?>) result;

        String resultURL = contentHash.get("url").toString();
        if (contentHash.containsKey("videopress_shortcode")) {
            resultURL = contentHash.get("videopress_shortcode").toString()
                    + "\n";
        } else {
            resultURL = String
                    .format("<video width=\"%s\" height=\"%s\" controls=\"controls\"><source src=\"%s\" type=\"%s\" /><a href=\"%s\">Click to view video</a>.</video>",
                            xRes, yRes, resultURL, mimeType, resultURL);
        }

        return resultURL;

    }

    private Object[] createVideoParams(Postable post, String imageTitle,
            String mimeType, MediaFile mf) {
        // try to upload the video
        Map<String, Object> m = new HashMap<String, Object>();

        m.put("name", imageTitle);
        m.put("type", mimeType);
        m.put("bits", mf);
        m.put("overwrite", true);

        Object[] params = { 1, post.getBlog().getUsername(),
                post.getBlog().getPassword(), m };
        return params;
    }

    private byte[] getFinalByte(int i, MediaFile mf, Blog blog, File jpeg,
            String orientation) {
        if (i != 0 && !blog.isScaledImage()) {
            return null;
        }
        byte[] result;
        byte[] bytes;
        try {
            bytes = new byte[(int) jpeg.length()];
        } catch (OutOfMemoryError er) {
            error = context.getString(R.string.out_of_memory);
            mediaError = true;
            return null;
        }

        DataInputStream in = null;
        try {
            in = new DataInputStream(new FileInputStream(jpeg));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            in.readFully(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String width = String.valueOf(i == 0 ? mf.getWidth() : blog
                .getScaledImageWidth());
        if (blog.getMaxImageWidth().equals("Original Size") && i == 0)
            width = "Original Size";

        ImageHelper ih2 = new ImageHelper();
        result = ih2.createThumbnail(bytes, width, orientation, false);

        if (result == null) {
            error = context.getString(R.string.out_of_memory);
            mediaError = true;
        }
        return result;
    }

    protected String cleanXMLRPCErrorMessage(String message) {
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
