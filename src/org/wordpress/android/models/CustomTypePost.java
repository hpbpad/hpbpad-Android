package org.wordpress.android.models;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import org.wordpress.android.WordPress;

public final class CustomTypePost implements Postable {

    private static final String COMMENT_OPEN = "open";

    private long id;
    private int blogID;
    private String post_id;
    private String post_title;
    private long post_date;
    private long post_date_gmt;
    // datetime post_modified;
    // datetime post_modified_gmt;
    private String post_status;
    private String post_type;
    private String post_format;
    private String post_name;
    private String post_author;
    private String post_password;
    private String post_excerpt;
    private String post_content;
    private String post_parent;
    private String post_mime_type;
    private String link;
    private String guid;
    private int menu_order;
    private String comment_status;
    private String ping_status;
    private boolean sticky;

    private boolean localDraft;
    private boolean uploaded;

    private Term[] terms;
    private CustomField[] custom_fields;
    private Enclosure enclosure;

    private Blog blog;

    private String quickPostType;

    public CustomTypePost(int blog_id, long post_id, String postType) {
        // load an existing post
        List<Object> postVals = WordPress.wpDB.loadCustomTypePost(postType,
                blog_id, post_id);
        if (postVals != null) {

            this.id = (Long) postVals.get(0);
            this.blogID = blog_id;
            if (postVals.get(2) != null)
                this.post_id = postVals.get(2).toString();
            this.post_title = postVals.get(3).toString();
            this.post_date = (Long) postVals.get(4);
            this.post_date_gmt = (Long) postVals.get(5);
            this.post_status = postVals.get(6).toString();
            this.post_type = postVals.get(7).toString();
            this.post_format = postVals.get(8).toString();
            this.post_name = postVals.get(9).toString();
            this.post_author = postVals.get(10).toString();
            this.post_password = postVals.get(11).toString();
            this.post_excerpt = postVals.get(12).toString();
            this.post_content = postVals.get(13).toString();
            this.post_parent = postVals.get(14).toString();
            this.post_mime_type = postVals.get(15).toString();
            this.link = postVals.get(16).toString();
            this.guid = postVals.get(17).toString();
            this.menu_order = (Integer) postVals.get(18);
            this.comment_status = postVals.get(19).toString();
            this.ping_status = postVals.get(20).toString();
            this.sticky = (Integer) postVals.get(21) > 0;

            final Object terms = postVals.get(22);
            if (terms != null) {
                try {
                    final JSONArray termsArray = new JSONArray(terms.toString());
                    final int length = termsArray.length();
                    this.terms = new Term[length];
                    for (int i = 0; i < length; i++) {
                        Term term = new Term(termsArray.getString(i));
                        this.terms[i] = term;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            final Object fields = postVals.get(23);
            if (fields != null) {
                try {
                    final JSONArray fieldsArray = new JSONArray(
                            fields.toString());
                    final int length = fieldsArray.length();
                    this.custom_fields = new CustomField[length];
                    for (int i = 0; i < length; i++) {
                        CustomField customField = new CustomField(
                                fieldsArray.getString(i));
                        this.custom_fields[i] = customField;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            this.localDraft = (Integer) postVals.get(25) > 0;
            this.uploaded = (Integer) postVals.get(26) > 0;

            try {
                this.blog = new Blog(blog_id);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            this.id = -1;
        }
    }

    public CustomTypePost(int blog_id, String postType, String title,
            String content, String picturePaths, long date, String status,
            String password, String postFormat, Term[] terms,
            boolean createBlogReference) {
        // create a new post
        if (createBlogReference) {
            try {
                this.blog = new Blog(blog_id);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.blogID = blog_id;
        this.post_type = postType;
        this.post_title = title;
        this.post_content = content;
        // this.mediaPaths = picturePaths;
        this.post_date_gmt = date;
        this.post_status = status;
        this.post_password = password;
        this.post_format = postFormat;
        this.terms = terms;
    }

    public long getId() {
        return this.id;
    }

    @Override
    public int getType() {
        return TYP_CUSTOM_TYPE_POST;
    }

    public int getBlogID() {
        return this.blogID;
    }

    @Override
    public String getPostId() {
        return this.post_id;
    }

    public String getTitle() {
        return this.post_title;
    }

    public void setTitle(String title) {
        this.post_title = title;
    }

    public long getPost_date() {
        return this.post_date;
    }

    public long getDate_created_gmt() {
        return this.post_date_gmt;
    }

    public void setDate_created_gmt(long dateCreatedGmt) {
        this.post_date_gmt = dateCreatedGmt;
    }

    public String getPostStatus() {
        return this.post_status;
    }

    public void setPost_status(String post_status) {
        this.post_status = post_status;
    }

    public String getPost_type() {
        return this.post_type;
    }

    public String getPostFormat() {
        return this.post_format;
    }

    public void setPost_format(String post_format) {
        this.post_format = post_format;
    }

    public String getPost_name() {
        return this.post_name;
    }

    public String getPost_author() {
        return this.post_author;
    }

    public String getPassword() {
        return this.post_password;
    }

    public void setPassword(String password) {
        this.post_password = password;
    }

    public String getExcerpt() {
        return this.post_excerpt;
    }

    public void setExcerpt(String mtExcerpt) {
        this.post_excerpt = mtExcerpt;
    }

    public String getContent() {
        return this.post_content;
    }

    public void setPost_content(String post_content) {
        this.post_content = post_content;
    }

    public String getPost_parent() {
        return this.post_parent;
    }

    public String getPost_mime_type() {
        return this.post_mime_type;
    }

    public String getLink() {
        return this.link;
    }

    public String getGuid() {
        return this.guid;
    }

    public int getMenu_order() {
        return this.menu_order;
    }

    public String getComment_status() {
        return this.comment_status;
    }

    @Override
    public boolean allowComments() {
        return COMMENT_OPEN.equals(this.comment_status);
    }

    public String getPing_status() {
        return this.ping_status;
    }

    public boolean isSticky() {
        return this.sticky;
    }

    public boolean isLocalDraft() {
        return localDraft;
    }

    public void setLocalDraft(boolean localDraft) {
        this.localDraft = localDraft;
    }

    public boolean isUploaded() {
        return uploaded;
    }

    public void setUploaded(boolean uploaded) {
        this.uploaded = uploaded;
    }

    public Term[] getTerms() {
        return this.terms;
    }

    public void setTerms(Term[] terms) {
        this.terms = terms;
    }

    public boolean save() {
        long newPostID = WordPress.wpDB.saveCustomTypePosts(this, this.blogID);

        if (newPostID >= 0 && this.isLocalDraft() && !this.isUploaded()) {
            this.id = newPostID;
            return true;
        }

        return false;
    }

    public boolean update() {
        int success = WordPress.wpDB.updateCustomTypePosts(this, this.blogID);

        return success > 0;
    }

    public void delete() {
        // deletes a post/page draft
        WordPress.wpDB.deleteCustomTypePost(this);
    }

    public void setQuickPostType(String type) {
        this.quickPostType = type;
    }

    public Blog getBlog() {
        return blog;
    }

};
