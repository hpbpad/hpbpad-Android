package org.wordpress.android.models;

public interface Postable {

    public static final int TYP_PAGE = 0;
    public static final int TYP_POST = 1;
    public static final int TYP_CUSTOM_TYPE_POST = 2;

    public abstract long getId();

    public abstract String getPostId();

    public abstract int getType();

    public abstract String getTitle();

    public abstract String getContent();

    public abstract String getPostStatus();

    public abstract String getPostFormat();

    public abstract long getDate_created_gmt();

    public abstract boolean isLocalDraft();

    public abstract boolean allowComments();

    public boolean isUploaded();

    public abstract Blog getBlog();

    public abstract String getPassword();

    public abstract String getLink();

    public abstract boolean save();

    public abstract boolean update();

    public abstract void delete();
}
