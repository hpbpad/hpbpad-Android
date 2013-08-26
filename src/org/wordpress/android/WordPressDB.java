package org.wordpress.android;

import java.lang.ref.WeakReference;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import org.wordpress.android.models.CustomField;
import org.wordpress.android.models.CustomTypePost;
import org.wordpress.android.models.HierarchicalTerm;
import org.wordpress.android.models.MediaFile;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.Term;
import org.wordpress.android.ui.MenuDrawerItem;
import org.wordpress.android.ui.posts.EditPostActivity;

public class WordPressDB {

    private static final int DATABASE_VERSION = 16;

    private static final String CREATE_TABLE_SETTINGS = "create table if not exists accounts (id integer primary key autoincrement, "
            + "url text, blogName text, username text, password text, imagePlacement text, centerThumbnail boolean, fullSizeImage boolean, maxImageWidth text, maxImageWidthId integer, lastCommentId integer, runService boolean);";
    private static final String CREATE_TABLE_EULA = "create table if not exists eula (id integer primary key autoincrement, "
            + "read integer not null, interval text, statsdate integer);";
    private static final String CREATE_TABLE_MEDIA = "create table if not exists media (id integer primary key autoincrement, "
            + "postID integer not null, filePath text default '', fileName text default '', title text default '', description text default '', caption text default '', horizontalAlignment integer default 0, width integer default 0, height integer default 0, mimeType text default '', featured boolean default false, isVideo boolean default false);";
    private static final String SETTINGS_TABLE = "accounts";
    private static final String DATABASE_NAME = "wordpress";
    private static final String MEDIA_TABLE = "media";

    private static final String CREATE_TABLE_POSTS = "create table if not exists posts (id integer primary key autoincrement, blogID text, "
            + "postid text, title text default '', dateCreated date, date_created_gmt date, categories text default '', custom_fields text default '', "
            + "description text default '', link text default '', mt_allow_comments boolean, mt_allow_pings boolean, "
            + "mt_excerpt text default '', mt_keywords text default '', mt_text_more text default '', permaLink text default '', post_status text default '', userid integer default 0, "
            + "wp_author_display_name text default '', wp_author_id text default '', wp_password text default '', wp_post_format text default '', wp_slug text default '', mediaPaths text default '', "
            + "latitude real, longitude real, localDraft boolean default 0, uploaded boolean default 0, isPage boolean default 0, wp_page_parent_id text, wp_page_parent_title text);";

    private static final String CREATE_TABLE_CUSTOM_TYPE_POSTS = "create table if not exists custom_type_posts (id integer primary key autoincrement, blogID text, "
            + "post_id text, post_title text default '', dateCreated date, date_created_gmt date, post_status text default '', post_type text default '', "
            + "post_format text default '', post_name text default '',"
            + "post_author text default '', post_password text default '', post_excerpt text default '', post_content text default '', post_parent text default '', post_mime_type text default '', "
            + "link text default '', guid text default '', menu_order number default 0, comment_status text default '', ping_status text default '', sticky bool default 0, "
            + "terms text default '', custom_fields text default '', enclosure text default '', localDraft boolean default 0, uploaded boolean default 0 ); ";

    private static final String CREATE_TABLE_POST_TYPES = "create table if not exists types (id integer primary key, blogID text default '', name text default '', label text default '', "
            + "hierarchical bool default 0, public bool default 0, show_ui bool default 0, builtin bool default 0, has_archive bool default 0, supports text default '', "
            + "labels text default '', cap text default '', map_meta_cap bool default 0, menu_position number default 0, "
            + "menu_icon text default '', show_in_menu bool default 0, taxonomies text default '' );";

    private static final String CREATE_TABLE_TAXONOMY = "create table if not exists taxonomy (id integer primary key, blogID text default '',  name text default '', label text default '', "
            + "hierarchical bool default 0, public bool default 0, show_ui bool default 0, builtin bool default 0, labels text default '', cap text default '', object_type text default '' );";

    private static final String CREATE_TABLE_TERM = "create table if not exists term (id integer primary key, blogID text default '', term_id text default '',  name text default '', "
            + "slug text default '', term_group text default '', term_taxonomy_id text default '', taxonomy text default '', description text default '', parent text default '', count integer );";

    private static final String CREATE_TABLE_COMMENTS = "create table if not exists comments (blogID text, postID text, iCommentID integer, author text, comment text, commentDate text, commentDateFormatted text, status text, url text, email text, postTitle text);";
    private static final String POSTS_TABLE = "posts";
    private static final String CUSTOM_TYPE_POSTS_TABLE = "custom_type_posts";
    private static final String TYPES_TABLE = "types";
    private static final String TAXONOMY_TABLE = "taxonomy";
    private static final String TERM_TABLE = "term";
    private static final String COMMENTS_TABLE = "comments";

    // eula
    private static final String EULA_TABLE = "eula";

    // categories
    private static final String CREATE_TABLE_CATEGORIES = "create table if not exists cats (id integer primary key autoincrement, "
            + "blog_id text, wp_id integer, category_name text not null);";
    private static final String CATEGORIES_TABLE = "cats";

    // for capturing blogID, trac ticket #
    private static final String ADD_BLOGID = "alter table accounts add blogId integer;";
    private static final String UPDATE_BLOGID = "update accounts set blogId = 1;";

    // add notification options
    private static final String ADD_SOUND_OPTION = "alter table eula add sound boolean default false;";
    private static final String ADD_VIBRATE_OPTION = "alter table eula add vibrate boolean default false;";
    private static final String ADD_LIGHT_OPTION = "alter table eula add light boolean default false;";
    private static final String ADD_TAGLINE = "alter table eula add tagline text;";
    private static final String ADD_TAGLINE_FLAG = "alter table eula add tagline_flag boolean default false;";

    // for capturing blogID, trac ticket #
    private static final String ADD_LOCATION_FLAG = "alter table accounts add location boolean default false;";

    // fix commentID data type
    private static final String ADD_NEW_COMMENT_ID = "ALTER TABLE comments ADD iCommentID INTEGER;";
    private static final String COPY_COMMENT_IDS = "UPDATE comments SET iCommentID = commentID;";

    // add wordpress.com stats login info
    private static final String ADD_DOTCOM_USERNAME = "alter table accounts add dotcom_username text;";
    private static final String ADD_DOTCOM_PASSWORD = "alter table accounts add dotcom_password text;";
    private static final String ADD_API_KEY = "alter table accounts add api_key text;";
    private static final String ADD_API_BLOGID = "alter table accounts add api_blogid text;";

    // add wordpress.com flag and version column
    private static final String ADD_DOTCOM_FLAG = "alter table accounts add dotcomFlag boolean default false;";
    private static final String ADD_WP_VERSION = "alter table accounts add wpVersion text;";

    // add httpuser and httppassword
    private static final String ADD_HTTPUSER = "alter table accounts add httpuser text;";
    private static final String ADD_HTTPPASSWORD = "alter table accounts add httppassword text;";

    // add new unique identifier to no longer use device imei
    private static final String ADD_UNIQUE_ID = "alter table eula add uuid text;";

    // add new table for QuickPress homescreen shortcuts
    private static final String CREATE_TABLE_QUICKPRESS_SHORTCUTS = "create table if not exists quickpress_shortcuts (id integer primary key autoincrement, accountId text, name text);";
    private static final String QUICKPRESS_SHORTCUTS_TABLE = "quickpress_shortcuts";

    // add field to store last used blog
    private static final String ADD_LAST_BLOG_ID = "alter table eula add last_blog_id text;";

    // add field to store last used blog
    private static final String ADD_POST_FORMATS = "alter table accounts add postFormats text default '';";

    //add scaled image settings
    private static final String ADD_SCALED_IMAGE = "alter table accounts add isScaledImage boolean default false;";
    private static final String ADD_SCALED_IMAGE_IMG_WIDTH = "alter table accounts add scaledImgWidth integer default 1024;";

    //add boolean to posts to check uploaded posts that have local changes
    private static final String ADD_LOCAL_POST_CHANGES = "alter table posts add isLocalChange boolean default 0";

    //add boolean to track if featured image should be included in the post content
    private static final String ADD_FEATURED_IN_POST = "alter table media add isFeaturedInPost boolean default false;";

    // add home url to blog settings
    private static final String ADD_HOME_URL = "alter table accounts add homeURL text default '';";

    private static final String ADD_BLOG_OPTIONS = "alter table accounts add blog_options text default '';";

    private SQLiteDatabase db;

    protected static final String PASSWORD_SECRET = "nottherealpasscode";

    public String defaultBlog = "";

    private WeakReference<Context> contextReference;

    public WordPressDB(Context ctx) {

        this.contextReference = new WeakReference<Context>(ctx);

        try {
            db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
        } catch (SQLiteException e) {
            db = null;
            return;
        }

        // db.execSQL("DROP TABLE IF EXISTS "+ SETTINGS_TABLE);
        db.execSQL(CREATE_TABLE_SETTINGS);
        // added eula to this class to fix trac #49
        db.execSQL(CREATE_TABLE_EULA);

        db.execSQL(CREATE_TABLE_POSTS);
        db.execSQL(CREATE_TABLE_COMMENTS);
        db.execSQL(CREATE_TABLE_CATEGORIES);
        db.execSQL(CREATE_TABLE_QUICKPRESS_SHORTCUTS);
        db.execSQL(CREATE_TABLE_MEDIA);
        db.execSQL(CREATE_TABLE_POST_TYPES);
        db.execSQL(CREATE_TABLE_CUSTOM_TYPE_POSTS);
        db.execSQL(CREATE_TABLE_TAXONOMY);
        db.execSQL(CREATE_TABLE_TERM);

        try {
            if (db.getVersion() < 1) { // user is new install
                db.execSQL(ADD_BLOGID);
                db.execSQL(UPDATE_BLOGID);
                db.execSQL(ADD_SOUND_OPTION);
                db.execSQL(ADD_VIBRATE_OPTION);
                db.execSQL(ADD_LIGHT_OPTION);
                db.execSQL(ADD_LOCATION_FLAG);
                db.execSQL(ADD_TAGLINE);
                db.execSQL(ADD_TAGLINE_FLAG);
                db.execSQL(ADD_DOTCOM_USERNAME);
                db.execSQL(ADD_DOTCOM_PASSWORD);
                db.execSQL(ADD_API_KEY);
                db.execSQL(ADD_API_BLOGID);
                db.execSQL(ADD_DOTCOM_FLAG);
                db.execSQL(ADD_WP_VERSION);
                db.execSQL(ADD_UNIQUE_ID);
                db.execSQL(ADD_HTTPUSER);
                db.execSQL(ADD_HTTPPASSWORD);
                db.execSQL(ADD_LAST_BLOG_ID);
                db.execSQL(ADD_POST_FORMATS);
                db.execSQL(ADD_SCALED_IMAGE);
                db.execSQL(ADD_SCALED_IMAGE_IMG_WIDTH);
                db.execSQL(ADD_LOCAL_POST_CHANGES);
                db.execSQL(ADD_FEATURED_IN_POST);
                db.execSQL(ADD_HOME_URL);
                db.execSQL(ADD_BLOG_OPTIONS);
                migratePasswords();
                db.setVersion(DATABASE_VERSION); // set to latest revision
            } else if (db.getVersion() == 1) { // v1.0 or v1.0.1
                db.delete(POSTS_TABLE, null, null);
                db.execSQL(CREATE_TABLE_POSTS);
                db.execSQL(ADD_BLOGID);
                db.execSQL(UPDATE_BLOGID);
                db.execSQL(ADD_SOUND_OPTION);
                db.execSQL(ADD_VIBRATE_OPTION);
                db.execSQL(ADD_LIGHT_OPTION);
                db.execSQL(ADD_LOCATION_FLAG);
                db.execSQL(ADD_TAGLINE);
                db.execSQL(ADD_TAGLINE_FLAG);
                db.execSQL(ADD_NEW_COMMENT_ID);
                db.execSQL(COPY_COMMENT_IDS);
                db.execSQL(ADD_DOTCOM_USERNAME);
                db.execSQL(ADD_DOTCOM_PASSWORD);
                db.execSQL(ADD_API_KEY);
                db.execSQL(ADD_API_BLOGID);
                db.execSQL(ADD_DOTCOM_FLAG);
                db.execSQL(ADD_WP_VERSION);
                db.execSQL(ADD_UNIQUE_ID);
                db.execSQL(ADD_HTTPUSER);
                db.execSQL(ADD_HTTPPASSWORD);
                db.execSQL(ADD_LAST_BLOG_ID);
                db.execSQL(ADD_POST_FORMATS);
                db.execSQL(ADD_SCALED_IMAGE);
                db.execSQL(ADD_SCALED_IMAGE_IMG_WIDTH);
                db.execSQL(ADD_LOCAL_POST_CHANGES);
                db.execSQL(ADD_FEATURED_IN_POST);
                db.execSQL(ADD_HOME_URL);
                db.execSQL(ADD_BLOG_OPTIONS);
                migratePasswords();
                migratePreferences(ctx);
                db.setVersion(DATABASE_VERSION);
            } else if (db.getVersion() == 2) {
                db.delete(POSTS_TABLE, null, null);
                db.execSQL(CREATE_TABLE_POSTS);
                db.execSQL(ADD_SOUND_OPTION);
                db.execSQL(ADD_VIBRATE_OPTION);
                db.execSQL(ADD_LIGHT_OPTION);
                db.execSQL(ADD_LOCATION_FLAG);
                db.execSQL(ADD_TAGLINE);
                db.execSQL(ADD_TAGLINE_FLAG);
                db.execSQL(ADD_NEW_COMMENT_ID);
                db.execSQL(COPY_COMMENT_IDS);
                db.execSQL(ADD_DOTCOM_USERNAME);
                db.execSQL(ADD_DOTCOM_PASSWORD);
                db.execSQL(ADD_API_KEY);
                db.execSQL(ADD_API_BLOGID);
                db.execSQL(ADD_DOTCOM_FLAG);
                db.execSQL(ADD_WP_VERSION);
                db.execSQL(ADD_UNIQUE_ID);
                db.execSQL(ADD_HTTPUSER);
                db.execSQL(ADD_HTTPPASSWORD);
                db.execSQL(ADD_LAST_BLOG_ID);
                db.execSQL(ADD_POST_FORMATS);
                db.execSQL(ADD_SCALED_IMAGE);
                db.execSQL(ADD_SCALED_IMAGE_IMG_WIDTH);
                db.execSQL(ADD_LOCAL_POST_CHANGES);
                db.execSQL(ADD_FEATURED_IN_POST);
                db.execSQL(ADD_HOME_URL);
                db.execSQL(ADD_BLOG_OPTIONS);
                migratePasswords();
                migratePreferences(ctx);
                db.setVersion(DATABASE_VERSION);
            } else if (db.getVersion() == 3) {
                db.delete(POSTS_TABLE, null, null);
                db.execSQL(CREATE_TABLE_POSTS);
                db.execSQL(ADD_LOCATION_FLAG);
                db.execSQL(ADD_TAGLINE);
                db.execSQL(ADD_TAGLINE_FLAG);
                db.execSQL(ADD_NEW_COMMENT_ID);
                db.execSQL(COPY_COMMENT_IDS);
                db.execSQL(ADD_DOTCOM_USERNAME);
                db.execSQL(ADD_DOTCOM_PASSWORD);
                db.execSQL(ADD_API_KEY);
                db.execSQL(ADD_API_BLOGID);
                db.execSQL(ADD_DOTCOM_FLAG);
                db.execSQL(ADD_WP_VERSION);
                db.execSQL(ADD_UNIQUE_ID);
                db.execSQL(ADD_HTTPUSER);
                db.execSQL(ADD_HTTPPASSWORD);
                db.execSQL(ADD_LAST_BLOG_ID);
                db.execSQL(ADD_POST_FORMATS);
                db.execSQL(ADD_SCALED_IMAGE);
                db.execSQL(ADD_SCALED_IMAGE_IMG_WIDTH);
                db.execSQL(ADD_LOCAL_POST_CHANGES);
                db.execSQL(ADD_FEATURED_IN_POST);
                db.execSQL(ADD_HOME_URL);
                db.execSQL(ADD_BLOG_OPTIONS);
                migratePasswords();
                migratePreferences(ctx);
                db.setVersion(DATABASE_VERSION);
            } else if (db.getVersion() == 4) {
                db.delete(POSTS_TABLE, null, null);
                db.execSQL(CREATE_TABLE_POSTS);
                db.execSQL(ADD_LOCATION_FLAG);
                db.execSQL(ADD_TAGLINE);
                db.execSQL(ADD_TAGLINE_FLAG);
                db.execSQL(ADD_NEW_COMMENT_ID);
                db.execSQL(COPY_COMMENT_IDS);
                db.execSQL(ADD_DOTCOM_USERNAME);
                db.execSQL(ADD_DOTCOM_PASSWORD);
                db.execSQL(ADD_API_KEY);
                db.execSQL(ADD_API_BLOGID);
                db.execSQL(ADD_DOTCOM_FLAG);
                db.execSQL(ADD_WP_VERSION);
                db.execSQL(ADD_UNIQUE_ID);
                db.execSQL(ADD_HTTPUSER);
                db.execSQL(ADD_HTTPPASSWORD);
                db.execSQL(ADD_LAST_BLOG_ID);
                db.execSQL(ADD_POST_FORMATS);
                db.execSQL(ADD_SCALED_IMAGE);
                db.execSQL(ADD_SCALED_IMAGE_IMG_WIDTH);
                db.execSQL(ADD_LOCAL_POST_CHANGES);
                db.execSQL(ADD_FEATURED_IN_POST);
                db.execSQL(ADD_HOME_URL);
                db.execSQL(ADD_BLOG_OPTIONS);
                migratePasswords();
                migratePreferences(ctx);
                db.setVersion(DATABASE_VERSION);
            } else if (db.getVersion() == 5) {
                db.delete(POSTS_TABLE, null, null);
                db.execSQL(CREATE_TABLE_POSTS);
                db.execSQL(ADD_TAGLINE);
                db.execSQL(ADD_TAGLINE_FLAG);
                db.execSQL(ADD_NEW_COMMENT_ID);
                db.execSQL(COPY_COMMENT_IDS);
                db.execSQL(ADD_DOTCOM_USERNAME);
                db.execSQL(ADD_DOTCOM_PASSWORD);
                db.execSQL(ADD_API_KEY);
                db.execSQL(ADD_API_BLOGID);
                db.execSQL(ADD_DOTCOM_FLAG);
                db.execSQL(ADD_WP_VERSION);
                db.execSQL(ADD_UNIQUE_ID);
                db.execSQL(ADD_HTTPUSER);
                db.execSQL(ADD_HTTPPASSWORD);
                db.execSQL(ADD_LAST_BLOG_ID);
                db.execSQL(ADD_POST_FORMATS);
                db.execSQL(ADD_SCALED_IMAGE);
                db.execSQL(ADD_SCALED_IMAGE_IMG_WIDTH);
                db.execSQL(ADD_LOCAL_POST_CHANGES);
                db.execSQL(ADD_FEATURED_IN_POST);
                db.execSQL(ADD_HOME_URL);
                db.execSQL(ADD_BLOG_OPTIONS);
                migratePasswords();
                migratePreferences(ctx);
                db.setVersion(DATABASE_VERSION);
            } else if (db.getVersion() == 6) {
                db.delete(POSTS_TABLE, null, null);
                db.execSQL(CREATE_TABLE_POSTS);
                db.execSQL(ADD_NEW_COMMENT_ID);
                db.execSQL(COPY_COMMENT_IDS);
                db.execSQL(ADD_DOTCOM_USERNAME);
                db.execSQL(ADD_DOTCOM_PASSWORD);
                db.execSQL(ADD_API_KEY);
                db.execSQL(ADD_API_BLOGID);
                db.execSQL(ADD_DOTCOM_FLAG);
                db.execSQL(ADD_WP_VERSION);
                db.execSQL(ADD_UNIQUE_ID);
                db.execSQL(ADD_HTTPUSER);
                db.execSQL(ADD_HTTPPASSWORD);
                db.execSQL(ADD_LAST_BLOG_ID);
                db.execSQL(ADD_POST_FORMATS);
                db.execSQL(ADD_SCALED_IMAGE);
                db.execSQL(ADD_SCALED_IMAGE_IMG_WIDTH);
                db.execSQL(ADD_LOCAL_POST_CHANGES);
                db.execSQL(ADD_FEATURED_IN_POST);
                db.execSQL(ADD_HOME_URL);
                db.execSQL(ADD_BLOG_OPTIONS);
                migratePasswords();
                migratePreferences(ctx);
                db.setVersion(DATABASE_VERSION);
            } else if (db.getVersion() == 7) {
                db.delete(POSTS_TABLE, null, null);
                db.execSQL(CREATE_TABLE_POSTS);
                db.execSQL(ADD_UNIQUE_ID);
                db.execSQL(ADD_HTTPUSER);
                db.execSQL(ADD_HTTPPASSWORD);
                db.execSQL(ADD_LAST_BLOG_ID);
                db.execSQL(ADD_POST_FORMATS);
                db.execSQL(ADD_SCALED_IMAGE);
                db.execSQL(ADD_SCALED_IMAGE_IMG_WIDTH);
                db.execSQL(ADD_LOCAL_POST_CHANGES);
                db.execSQL(ADD_FEATURED_IN_POST);
                db.execSQL(ADD_HOME_URL);
                db.execSQL(ADD_BLOG_OPTIONS);
                migratePasswords();
                migratePreferences(ctx);
                db.setVersion(DATABASE_VERSION);
            } else if (db.getVersion() == 8) {
                db.delete(POSTS_TABLE, null, null);
                db.execSQL(CREATE_TABLE_POSTS);
                db.execSQL(ADD_HTTPUSER);
                db.execSQL(ADD_HTTPPASSWORD);
                db.execSQL(ADD_LAST_BLOG_ID);
                db.execSQL(ADD_POST_FORMATS);
                db.execSQL(ADD_SCALED_IMAGE);
                db.execSQL(ADD_SCALED_IMAGE_IMG_WIDTH);
                db.execSQL(ADD_LOCAL_POST_CHANGES);
                db.execSQL(ADD_FEATURED_IN_POST);
                db.execSQL(ADD_HOME_URL);
                db.execSQL(ADD_BLOG_OPTIONS);
                migratePasswords();
                migratePreferences(ctx);
                db.setVersion(DATABASE_VERSION);
            } else if (db.getVersion() == 9) {
                db.delete(POSTS_TABLE, null, null);
                db.execSQL(CREATE_TABLE_POSTS);
                db.execSQL(ADD_HTTPUSER);
                db.execSQL(ADD_HTTPPASSWORD);
                db.execSQL(ADD_LAST_BLOG_ID);
                db.execSQL(ADD_POST_FORMATS);
                db.execSQL(ADD_SCALED_IMAGE);
                db.execSQL(ADD_SCALED_IMAGE_IMG_WIDTH);
                db.execSQL(ADD_LOCAL_POST_CHANGES);
                db.execSQL(ADD_FEATURED_IN_POST);
                db.execSQL(ADD_HOME_URL);
                db.execSQL(ADD_BLOG_OPTIONS);
                migratePasswords();
                migratePreferences(ctx);
                db.setVersion(DATABASE_VERSION);
            } else if (db.getVersion() == 10) {
                db.delete(POSTS_TABLE, null, null);
                db.execSQL(CREATE_TABLE_POSTS);

                try {
                    // migrate drafts

                    Cursor c = db.query("localdrafts", new String[] { "blogID",
                            "title", "content", "picturePaths", "date",
                            "categories", "tags", "status", "password",
                            "latitude", "longitude" }, null, null, null, null,
                            "id desc");
                    int numRows = c.getCount();
                    c.moveToFirst();

                    for (int i = 0; i < numRows; ++i) {
                        if (c.getString(0) != null) {
                            Post post = new Post(c.getInt(0), c.getString(1),
                                    c.getString(2), c.getString(3),
                                    c.getLong(4), c.getString(5),
                                    c.getString(6), c.getString(7),
                                    c.getString(8), c.getDouble(9),
                                    c.getDouble(10), false, "", false, false);
                            post.setLocalDraft(true);
                            post.setPost_status("localdraft");
                            savePost(post, c.getInt(0));
                        }
                        c.moveToNext();
                    }
                    c.close();

                    db.delete("localdrafts", null, null);

                    // pages
                    c = db.query("localpagedrafts", new String[] { "blogID",
                            "title", "content", "picturePaths", "date",
                            "status", "password" }, null, null, null, null,
                            "id desc");
                    numRows = c.getCount();
                    c.moveToFirst();

                    for (int i = 0; i < numRows; ++i) {
                        if (c.getString(0) != null) {
                            Post post = new Post(c.getInt(0), c.getString(1),
                                    c.getString(2), c.getString(3),
                                    c.getLong(4), c.getString(5), "", "",
                                    c.getString(6), 0, 0, true, "", false,
                                    false);
                            post.setLocalDraft(true);
                            post.setPost_status("localdraft");
                            post.setPage(true);
                            savePost(post, c.getInt(0));
                        }
                        c.moveToNext();
                    }
                    c.close();
                    db.delete("localpagedrafts", null, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                db.execSQL(ADD_LAST_BLOG_ID);
                db.execSQL(ADD_POST_FORMATS);
                db.execSQL(ADD_SCALED_IMAGE);
                db.execSQL(ADD_SCALED_IMAGE_IMG_WIDTH);
                db.execSQL(ADD_LOCAL_POST_CHANGES);
                db.execSQL(ADD_FEATURED_IN_POST);
                db.execSQL(ADD_HOME_URL);
                db.execSQL(ADD_BLOG_OPTIONS);
                migratePreferences(ctx);
                db.setVersion(DATABASE_VERSION);
            } else if (db.getVersion() == 11) {
                db.execSQL(ADD_SCALED_IMAGE);
                db.execSQL(ADD_SCALED_IMAGE_IMG_WIDTH);
                db.execSQL(ADD_LOCAL_POST_CHANGES);
                db.execSQL(ADD_FEATURED_IN_POST);
                db.execSQL(ADD_HOME_URL);
                db.execSQL(ADD_BLOG_OPTIONS);
                migratePreferences(ctx);
                db.setVersion(DATABASE_VERSION);
            } else if (db.getVersion() == 12) {
                db.execSQL(ADD_FEATURED_IN_POST);
                db.execSQL(ADD_HOME_URL);
                db.execSQL(ADD_BLOG_OPTIONS);
                migratePreferences(ctx);
                db.setVersion(DATABASE_VERSION);
            } else if (db.getVersion() == 13) {
                db.execSQL(ADD_HOME_URL);
                db.execSQL(ADD_BLOG_OPTIONS);
                migratePreferences(ctx);
                db.setVersion(DATABASE_VERSION);
            } else if (db.getVersion() == 14) {
                db.execSQL(ADD_BLOG_OPTIONS);
                migratePreferences(ctx);
                db.setVersion(DATABASE_VERSION);
            } else if (db.getVersion() == 15) {
                migratePreferences(ctx);
                db.setVersion(DATABASE_VERSION);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    private void migratePreferences(Context ctx) {
        // Migrate preferences out of the db
        Map<?, ?> notificationOptions = getNotificationOptions();
        if (notificationOptions != null) {
            SharedPreferences settings = PreferenceManager
                    .getDefaultSharedPreferences(ctx);
            SharedPreferences.Editor editor = settings.edit();
            String interval = getInterval();
            if (interval != "") {
                editor.putString("wp_pref_notifications_interval", interval);
            }
            editor.putBoolean(
                    "wp_pref_notification_sound",
                    (notificationOptions.get("sound").toString().equals("1")) ? true
                            : false);
            editor.putBoolean(
                    "wp_pref_notification_vibrate",
                    (notificationOptions.get("vibrate").toString().equals("1")) ? true
                            : false);
            editor.putBoolean(
                    "wp_pref_notification_light",
                    (notificationOptions.get("light").toString().equals("1")) ? true
                            : false);
            editor.putBoolean("wp_pref_signature_enabled", (notificationOptions
                    .get("tagline_flag").toString().equals("1")) ? true : false);

            String tagline = notificationOptions.get("tagline").toString();
            if (tagline != "") {
                editor.putString("wp_pref_post_signature", tagline);
            }
            editor.commit();
        }
    }

    public long addAccount(String url, String homeURL, String blogName,
            String username, String password, String httpuser,
            String httppassword, String imagePlacement,
            boolean centerThumbnail, boolean fullSizeImage,
            String maxImageWidth, int maxImageWidthId, boolean runService,
            int blogId, boolean wpcom, String wpVersion) {

        ContentValues values = new ContentValues();
        values.put("url", url);
        values.put("homeURL", homeURL);
        values.put("blogName", blogName);
        values.put("username", username);
        values.put("password", encryptPassword(password));
        values.put("httpuser", httpuser);
        values.put("httppassword", encryptPassword(httppassword));
        values.put("imagePlacement", imagePlacement);
        values.put("centerThumbnail", centerThumbnail);
        values.put("fullSizeImage", fullSizeImage);
        values.put("maxImageWidth", maxImageWidth);
        values.put("maxImageWidthId", maxImageWidthId);
        values.put("runService", runService);
        values.put("blogId", blogId);
        values.put("dotcomFlag", wpcom);
        values.put("wpVersion", wpVersion);
        return db.insert(SETTINGS_TABLE, null, values);
    }

    public List<Map<String, Object>> getAccounts() {

        Cursor c = db.query(SETTINGS_TABLE, new String[] { "id", "blogName",
                "username", "runService", "blogId", "url" }, null, null, null,
                null, null);
        int id;
        String blogName, username, url;
        int blogId;
        int runService;
        int numRows = c.getCount();
        c.moveToFirst();
        List<Map<String, Object>> accounts = new Vector<Map<String, Object>>();
        for (int i = 0; i < numRows; i++) {

            id = c.getInt(0);
            blogName = c.getString(1);
            username = c.getString(2);
            runService = c.getInt(3);
            blogId = c.getInt(4);
            url = c.getString(5);
            if (id > 0) {
                Map<String, Object> thisHash = new HashMap<String, Object>();
                thisHash.put("id", id);
                thisHash.put("blogName", blogName);
                thisHash.put("username", username);
                thisHash.put("runService", runService);
                thisHash.put("blogId", blogId);
                thisHash.put("url", url);
                accounts.add(thisHash);
            }
            c.moveToNext();
        }
        c.close();

        return accounts;
    }

    public boolean checkMatch(String blogName, String blogURL, String username) {

        Cursor c = db.query(SETTINGS_TABLE, new String[] { "blogName", "url" },
                "blogName='" + addSlashes(blogName) + "' AND url='"
                        + addSlashes(blogURL) + "'" + " AND username='"
                        + username + "'", null, null, null, null);
        int numRows = c.getCount();
        boolean result = false;

        if (numRows > 0) {
            // this account is already saved, yo!
            result = true;
        }

        c.close();

        return result;
    }

    public static String addSlashes(String text) {
        final StringBuffer sb = new StringBuffer(text.length() * 2);
        final StringCharacterIterator iterator = new StringCharacterIterator(
                text);

        char character = iterator.current();

        while (character != StringCharacterIterator.DONE) {
            if (character == '"')
                sb.append("\\\"");
            else if (character == '\'')
                sb.append("\'\'");
            else if (character == '\\')
                sb.append("\\\\");
            else if (character == '\n')
                sb.append("\\n");
            else if (character == '{')
                sb.append("\\{");
            else if (character == '}')
                sb.append("\\}");
            else
                sb.append(character);

            character = iterator.next();
        }

        return sb.toString();
    }

    public boolean saveSettings(String id, String url, String homeURL,
            String username, String password, String httpuser,
            String httppassword, String imagePlacement,
            boolean isFeaturedImageCapable, boolean fullSizeImage,
            String maxImageWidth, int maxImageWidthId, boolean location,
            boolean isWPCom, String originalUsername, String postFormats,
            String dotcomUsername, String dotcomPassword, String apiBlogID,
            String apiKey, boolean isScaledImage, int scaledImgWidth,
            String blogOptions) {

        ContentValues values = new ContentValues();
        values.put("url", url);
        values.put("homeURL", homeURL);
        values.put("username", username);
        values.put("password", encryptPassword(password));
        values.put("httpuser", httpuser);
        values.put("httppassword", encryptPassword(httppassword));
        values.put("imagePlacement", imagePlacement);
        values.put("centerThumbnail", isFeaturedImageCapable);
        values.put("fullSizeImage", fullSizeImage);
        values.put("maxImageWidth", maxImageWidth);
        values.put("maxImageWidthId", maxImageWidthId);
        values.put("location", location);
        values.put("postFormats", postFormats);
        values.put("dotcom_username", dotcomUsername);
        values.put("dotcom_password", encryptPassword(dotcomPassword));
        values.put("api_blogid", apiBlogID);
        values.put("api_key", apiKey);
        values.put("isScaledImage", isScaledImage);
        values.put("scaledImgWidth", scaledImgWidth);
        values.put("blog_options", blogOptions);

        boolean returnValue = db.update(SETTINGS_TABLE, values, "id=" + id,
                null) > 0;
        if (isWPCom) {
            // update the login for other wordpress.com accounts
            ContentValues userPass = new ContentValues();
            userPass.put("username", username);
            userPass.put("password", encryptPassword(password));
            returnValue = db.update(SETTINGS_TABLE, userPass, "username=\""
                    + originalUsername + "\" AND dotcomFlag=1", null) > 0;
        }

        return (returnValue);
    }

    public boolean deleteAccount(Context ctx, int id) {

        int rowsAffected = 0;
        try {
            rowsAffected = db.delete(SETTINGS_TABLE, "id=" + id, null);
        } finally {

        }

        boolean returnValue = false;
        if (rowsAffected > 0) {
            returnValue = true;
        }

        // delete QuickPress homescreen shortcuts connected with this account
        List<Map<String, Object>> shortcuts = getQuickPressShortcuts(id);
        for (int i = 0; i < shortcuts.size(); i++) {
            Map<String, Object> shortcutHash = shortcuts.get(i);

            Intent shortcutIntent = new Intent();
            shortcutIntent.setClassName(EditPostActivity.class.getPackage()
                    .getName(), EditPostActivity.class.getName());
            shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            shortcutIntent.setAction(Intent.ACTION_VIEW);
            Intent broadcastShortcutIntent = new Intent();
            broadcastShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT,
                    shortcutIntent);
            broadcastShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME,
                    shortcutHash.get("name").toString());
            broadcastShortcutIntent.putExtra("duplicate", false);
            broadcastShortcutIntent
                    .setAction("com.android.launcher.action.UNINSTALL_SHORTCUT");
            ctx.sendBroadcast(broadcastShortcutIntent);

            deleteQuickPressShortcut(shortcutHash.get("id").toString());
        }

        return (returnValue);
    }

    public List<Object> loadSettings(int id) {

        Cursor c = db.query(SETTINGS_TABLE, new String[] { "url", "blogName",
                "username", "password", "httpuser", "httppassword",
                "imagePlacement", "centerThumbnail", "fullSizeImage",
                "maxImageWidth", "maxImageWidthId", "runService", "blogId",
                "location", "dotcomFlag", "dotcom_username", "dotcom_password",
                "api_key", "api_blogid", "wpVersion", "postFormats",
                "lastCommentId", "isScaledImage", "scaledImgWidth", "homeURL",
                "blog_options" }, "id=" + id, null, null, null, null);

        int numRows = c.getCount();
        c.moveToFirst();

        List<Object> returnVector = new Vector<Object>();
        if (numRows > 0) {
            if (c.getString(0) != null) {
                returnVector.add(c.getString(0));
                returnVector.add(c.getString(1));
                returnVector.add(c.getString(2));
                returnVector.add(decryptPassword(c.getString(3)));
                if (c.getString(4) == null) {
                    returnVector.add("");
                } else {
                    returnVector.add(c.getString(4));
                }
                if (c.getString(5) == null) {
                    returnVector.add("");
                } else {
                    returnVector.add(decryptPassword(c.getString(5)));
                }
                returnVector.add(c.getString(6));
                returnVector.add(c.getInt(7));
                returnVector.add(c.getInt(8));
                returnVector.add(c.getString(9));
                returnVector.add(c.getInt(10));
                returnVector.add(c.getInt(11));
                returnVector.add(c.getInt(12));
                returnVector.add(c.getInt(13));
                returnVector.add(c.getInt(14));
                returnVector.add(c.getString(15));
                returnVector.add(decryptPassword(c.getString(16)));
                returnVector.add(c.getString(17));
                returnVector.add(c.getString(18));
                returnVector.add(c.getString(19));
                returnVector.add(c.getString(20));
                returnVector.add(c.getInt(21));
                returnVector.add(c.getInt(22));
                returnVector.add(c.getInt(23));
                returnVector.add(c.getString(24));
                returnVector.add(c.getString(25));
            } else {
                returnVector = null;
            }
        } else {
            returnVector = null;
        }
        c.close();

        return returnVector;
    }

    public List<String> loadStatsLogin(int id) {

        Cursor c = db.query(SETTINGS_TABLE, new String[] { "dotcom_username",
                "dotcom_password" }, "id=" + id, null, null, null, null);

        c.moveToFirst();

        List<String> returnVector = new Vector<String>();
        if (c.getString(0) != null) {
            returnVector.add(c.getString(0));
            returnVector.add(decryptPassword(c.getString(1)));
        } else {
            returnVector = null;
        }
        c.close();

        return returnVector;
    }

    public boolean updateLatestCommentID(int id, Integer newCommentID) {

        boolean returnValue = false;

        synchronized (this) {
            ContentValues values = new ContentValues();
            values.put("lastCommentId", newCommentID);

            returnValue = db.update(SETTINGS_TABLE, values, "id=" + id, null) > 0;
        }

        return (returnValue);

    }

    public List<Integer> getNotificationAccounts() {

        Cursor c = null;
        try {
            c = db.query(SETTINGS_TABLE, new String[] { "id" }, "runService=1",
                    null, null, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        int numRows = c.getCount();
        c.moveToFirst();

        List<Integer> returnVector = new Vector<Integer>();
        for (int i = 0; i < numRows; ++i) {
            int tempID = c.getInt(0);
            returnVector.add(tempID);
            c.moveToNext();
        }

        c.close();

        return returnVector;
    }

    public String getAccountName(String accountID) {

        String accountName = "";
        Cursor c = db.query(SETTINGS_TABLE, new String[] { "blogName" }, "id="
                + accountID, null, null, null, null);
        c.moveToFirst();
        if (c.getString(0) != null) {
            accountName = c.getString(0);
        }
        c.close();

        return accountName;
    }

    public void updateNotificationFlag(int id, boolean flag) {

        ContentValues values = new ContentValues();
        int iFlag = 0;
        if (flag) {
            iFlag = 1;
        }
        values.put("runService", iFlag);

        boolean returnValue = db.update(SETTINGS_TABLE, values,
                "id=" + String.valueOf(id), null) > 0;
        if (returnValue) {
        }

    }

    public void updateNotificationSettings(String interval, boolean sound,
            boolean vibrate, boolean light, boolean tagline_flag, String tagline) {

        ContentValues values = new ContentValues();
        values.put("interval", interval);
        values.put("sound", sound);
        values.put("vibrate", vibrate);
        values.put("light", light);
        values.put("tagline_flag", tagline_flag);
        values.put("tagline", tagline);

        boolean returnValue = db.update(EULA_TABLE, values, null, null) > 0;
        if (returnValue) {
        }
        ;

    }

    public String getInterval() {

        Cursor c = db.query("eula", new String[] { "interval" }, "id=0", null,
                null, null, null);
        int numRows = c.getCount();
        c.moveToFirst();
        String returnValue = "";
        if (numRows == 1) {
            if (c.getString(0) != null) {
                returnValue = c.getString(0);
            }
        }
        c.close();

        return returnValue;

    }

    public Map<String, Object> getNotificationOptions() {

        Cursor c = db.query(EULA_TABLE, new String[] { "id", "sound",
                "vibrate", "light", "tagline_flag", "tagline" }, "id=0", null,
                null, null, null);
        int sound, vibrate, light;
        String tagline;
        Map<String, Object> thisHash = new HashMap<String, Object>();
        int numRows = c.getCount();
        if (numRows >= 1) {
            c.moveToFirst();

            sound = c.getInt(1);
            vibrate = c.getInt(2);
            light = c.getInt(3);
            tagline = c.getString(5);
            thisHash.put("sound", sound);
            thisHash.put("vibrate", vibrate);
            thisHash.put("light", light);
            thisHash.put("tagline_flag", c.getInt(4));
            if (tagline != null) {
                thisHash.put("tagline", tagline);
            } else {
                thisHash.put("tagline", "");
            }

        } else {
            return null;
        }

        c.close();

        return thisHash;
    }

    /**
     * Set the ID of the most recently active blog. This value will persist
     * between application launches.
     * 
     * @param id
     *            ID of the most recently active blog.
     */
    public void updateLastBlogId(int id) {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(contextReference.get());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("last_blog_id", id);
        editor.commit();
    }

    /**
     * Delete the ID for the most recently active blog.
     */
    public void deleteLastBlogId(Context context) {
        updateLastBlogId(-1);
        // Clear the last selected activity
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("wp_pref_last_activity", -1);
        editor.commit();
    }

    /**
     * Get the ID of the most recently active blog. -1 is returned if there is
     * no recently active blog.
     */
    public int getLastBlogId() {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(contextReference.get());
        return preferences.getInt("last_blog_id", -1);
    }

    public List<Map<String, Object>> loadDrafts(int blogID, boolean loadPages) {

        List<Map<String, Object>> returnVector = new Vector<Map<String, Object>>();
        Cursor c;
        if (loadPages)
            c = db.query(POSTS_TABLE, new String[] { "id", "title",
                    "post_status", "uploaded", "date_created_gmt",
                    "post_status" }, "blogID=" + blogID
                    + " AND localDraft=1 AND uploaded=0 AND isPage=1", null,
                    null, null, null);
        else
            c = db.query(POSTS_TABLE, new String[] { "id", "title",
                    "post_status", "uploaded", "date_created_gmt",
                    "post_status" }, "blogID=" + blogID
                    + " AND localDraft=1 AND uploaded=0 AND isPage=0", null,
                    null, null, null);

        int numRows = c.getCount();
        c.moveToFirst();

        for (int i = 0; i < numRows; ++i) {
            if (c.getString(0) != null) {
                Map<String, Object> returnHash = new HashMap<String, Object>();
                returnHash.put("id", c.getInt(0));
                returnHash.put("title", c.getString(1));
                returnHash.put("status", c.getString(2));
                returnHash.put("uploaded", c.getInt(3));
                returnHash.put("date_created_gmt", c.getLong(4));
                returnHash.put("post_status", c.getString(5));
                returnVector.add(i, returnHash);
            }
            c.moveToNext();
        }
        c.close();

        if (numRows == 0) {
            returnVector = null;
        }

        return returnVector;
    }

    public boolean deletePost(Post post) {

        boolean returnValue = false;

        int result = 0;
        result = db.delete(POSTS_TABLE, "blogID=" + post.getBlogID()
                + " AND id=" + post.getId(), null);

        if (result == 1) {
            returnValue = true;
        }

        return returnValue;
    }

    public boolean savePosts(List<?> postValues, int blogID, boolean isPage) {
        boolean returnValue = false;
        if (postValues.size() == 0) {
            return returnValue;
        }
        for (int i = 0; i < postValues.size(); i++) {
            try {
                ContentValues values = new ContentValues();
                Map<?, ?> thisHash = (Map<?, ?>) postValues.get(i);
                values.put("blogID", blogID);
                if (thisHash.get((isPage) ? "page_id" : "postid") == null)
                    return false;
                String postID = thisHash.get((isPage) ? "page_id" : "postid")
                        .toString();
                values.put("postid", postID);
                values.put("title", thisHash.get("title").toString());
                Date d;
                try {
                    d = (Date) thisHash.get("dateCreated");
                    values.put("dateCreated", d.getTime());
                } catch (Exception e) {
                    Date now = new Date();
                    values.put("dateCreated", now.getTime());
                }
                try {
                    d = (Date) thisHash.get("date_created_gmt");
                    values.put("date_created_gmt", d.getTime());
                } catch (Exception e) {
                    d = new Date((Long) values.get("dateCreated"));
                    values.put("date_created_gmt",
                            d.getTime() + (d.getTimezoneOffset() * 60000));
                }
                values.put("description", thisHash.get("description")
                        .toString());
                values.put("link", thisHash.get("link").toString());
                values.put("permaLink", thisHash.get("permaLink").toString());

                Object[] cats = (Object[]) thisHash.get("categories");
                JSONArray jsonArray = new JSONArray();
                if (cats != null) {
                    for (int x = 0; x < cats.length; x++) {
                        jsonArray.put(cats[x].toString());
                    }
                }
                values.put("categories", jsonArray.toString());

                Object[] custom_fields = (Object[]) thisHash
                        .get("custom_fields");
                jsonArray = new JSONArray();
                if (custom_fields != null) {
                    for (int x = 0; x < custom_fields.length; x++) {
                        jsonArray.put(custom_fields[x].toString());
                        // Update geo_long and geo_lat from custom fields, if
                        // found:
                        Map<?, ?> customField = (Map<?, ?>) custom_fields[x];
                        if (customField.get("key") != null
                                && customField.get("value") != null) {
                            if (customField.get("key").equals("geo_longitude"))
                                values.put("longitude", customField
                                        .get("value").toString());
                            if (customField.get("key").equals("geo_latitude"))
                                values.put("latitude", customField.get("value")
                                        .toString());
                        }
                    }
                }
                values.put("custom_fields", jsonArray.toString());

                values.put("mt_excerpt",
                        thisHash.get((isPage) ? "excerpt" : "mt_excerpt")
                                .toString());
                values.put("mt_text_more",
                        thisHash.get((isPage) ? "text_more" : "mt_text_more")
                                .toString());
                values.put("mt_allow_comments",
                        (Integer) thisHash.get("mt_allow_comments"));
                values.put("mt_allow_pings",
                        (Integer) thisHash.get("mt_allow_pings"));
                values.put("wp_slug", thisHash.get("wp_slug").toString());
                values.put("wp_password", thisHash.get("wp_password")
                        .toString());
                values.put("wp_author_id", thisHash.get("wp_author_id")
                        .toString());
                values.put("wp_author_display_name",
                        thisHash.get("wp_author_display_name").toString());
                values.put("post_status",
                        thisHash.get((isPage) ? "page_status" : "post_status")
                                .toString());
                values.put("userid", thisHash.get("userid").toString());

                int isPageInt = 0;
                if (isPage) {
                    isPageInt = 1;
                    values.put("isPage", true);
                    values.put("wp_page_parent_id",
                            thisHash.get("wp_page_parent_id").toString());
                    values.put("wp_page_parent_title",
                            thisHash.get("wp_page_parent_title").toString());
                } else {
                    values.put("mt_keywords", thisHash.get("mt_keywords")
                            .toString());
                    try {
                        values.put("wp_post_format",
                                thisHash.get("wp_post_format").toString());
                    } catch (Exception e) {
                        values.put("wp_post_format", "");
                    }
                }

                final String where = "blogID=" + blogID + " AND postid='"
                        + postID + "' AND isPage=" + isPageInt;

                int result = db.update(POSTS_TABLE, values, where, null);
                if (result == 0)
                    returnValue = db.insert(POSTS_TABLE, null, values) > 0;
                else
                    returnValue = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return (returnValue);
    }

    public long savePost(Post post, int blogID) {
        long returnValue = -1;
        if (post != null) {

            ContentValues values = new ContentValues();
            values.put("blogID", blogID);
            values.put("title", post.getTitle());
            values.put("date_created_gmt", post.getDate_created_gmt());
            values.put("description", post.getDescription());
            values.put("mt_text_more", post.getMt_text_more());

            if (post.getCategories() != null) {
                JSONArray jsonArray = null;
                try {
                    jsonArray = new JSONArray(post.getCategories().toString());
                    values.put("categories", jsonArray.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            values.put("localDraft", post.isLocalDraft());
            values.put("mediaPaths", post.getMediaPaths());
            values.put("mt_keywords", post.getMt_keywords());
            values.put("wp_password", post.getPassword());
            values.put("post_status", post.getPostStatus());
            values.put("uploaded", post.isUploaded());
            values.put("isPage", post.isPage());
            values.put("wp_post_format", post.getPostFormat());
            values.put("latitude", post.getLatitude());
            values.put("longitude", post.getLongitude());
            values.put("isLocalChange", post.isLocalChange());

            returnValue = db.insert(POSTS_TABLE, null, values);

        }
        return (returnValue);
    }

    public int updatePost(Post post, int blogID) {
        int success = 0;
        if (post != null) {

            ContentValues values = new ContentValues();
            values.put("blogID", blogID);
            values.put("title", post.getTitle());
            values.put("date_created_gmt", post.getDate_created_gmt());
            values.put("description", post.getDescription());
            if (post.getMt_text_more() != null)
                values.put("mt_text_more", post.getMt_text_more());
            values.put("uploaded", post.isUploaded());

            if (post.getCategories() != null) {
                JSONArray jsonArray = null;
                try {
                    jsonArray = new JSONArray(post.getCategories().toString());
                    values.put("categories", jsonArray.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            values.put("localDraft", post.isLocalDraft());
            values.put("mediaPaths", post.getMediaPaths());
            values.put("mt_keywords", post.getMt_keywords());
            values.put("wp_password", post.getPassword());
            values.put("post_status", post.getPostStatus());
            values.put("isPage", post.isPage());
            values.put("wp_post_format", post.getPostFormat());
            values.put("isLocalChange", post.isLocalChange());

            int pageInt = 0;
            if (post.isPage())
                pageInt = 1;

            success = db.update(POSTS_TABLE, values,
                    "blogID=" + post.getBlogID() + " AND id=" + post.getId()
                            + " AND isPage=" + pageInt, null);

        }
        return (success);
    }

    public List<Map<String, Object>> loadUploadedPosts(int blogID,
            boolean loadPages) {

        List<Map<String, Object>> returnVector = new Vector<Map<String, Object>>();
        Cursor c;
        if (loadPages)
            c = db.query(POSTS_TABLE,
                    new String[] { "id", "blogID", "postid", "title",
                            "date_created_gmt", "dateCreated", "post_status" },
                    "blogID=" + blogID + " AND localDraft != 1 AND isPage=1",
                    null, null, null, null);
        else
            c = db.query(POSTS_TABLE,
                    new String[] { "id", "blogID", "postid", "title",
                            "date_created_gmt", "dateCreated", "post_status" },
                    "blogID=" + blogID + " AND localDraft != 1 AND isPage=0",
                    null, null, null, null);

        int numRows = c.getCount();
        c.moveToFirst();

        for (int i = 0; i < numRows; ++i) {
            if (c.getString(0) != null) {
                Map<String, Object> returnHash = new HashMap<String, Object>();
                returnHash.put("id", c.getInt(0));
                returnHash.put("blogID", c.getString(1));
                returnHash.put("postID", c.getString(2));
                returnHash.put("title", c.getString(3));
                returnHash.put("date_created_gmt", c.getLong(4));
                returnHash.put("dateCreated", c.getLong(5));
                returnHash.put("post_status", c.getString(6));
                returnVector.add(i, returnHash);
            }
            c.moveToNext();
        }
        c.close();

        if (numRows == 0) {
            returnVector = null;
        }

        return returnVector;
    }

    public void deleteUploadedPosts(int blogID, boolean isPage) {

        if (isPage)
            db.delete(POSTS_TABLE, "blogID=" + blogID
                    + " AND localDraft != 1 AND isPage=1", null);
        else
            db.delete(POSTS_TABLE, "blogID=" + blogID
                    + " AND localDraft != 1 AND isPage=0", null);

    }

    public List<Object> loadPost(int blogID, boolean isPage, long id) {
        List<Object> values = null;

        int pageInt = 0;
        if (isPage)
            pageInt = 1;
        Cursor c = db.query(POSTS_TABLE, null, "blogID=" + blogID + " AND id="
                + id + " AND isPage=" + pageInt, null, null, null, null);

        if (c.getCount() > 0) {
            c.moveToFirst();
            if (c.getString(0) != null) {
                values = new Vector<Object>();
                values.add(c.getLong(0));
                values.add(c.getString(1));
                values.add(c.getString(2));
                values.add(c.getString(3));
                values.add(c.getLong(4));
                values.add(c.getLong(5));
                values.add(c.getString(6));
                values.add(c.getString(7));
                values.add(c.getString(8));
                values.add(c.getString(9));
                values.add(c.getInt(10));
                values.add(c.getInt(11));
                values.add(c.getString(12));
                values.add(c.getString(13));
                values.add(c.getString(14));
                values.add(c.getString(15));
                values.add(c.getString(16));
                values.add(c.getString(17));
                values.add(c.getString(18));
                values.add(c.getString(19));
                values.add(c.getString(20));
                values.add(c.getString(21));
                values.add(c.getString(22));
                values.add(c.getString(23));
                values.add(c.getDouble(24));
                values.add(c.getDouble(25));
                values.add(c.getInt(26));
                values.add(c.getInt(27));
                values.add(c.getInt(28));
                values.add(c.getInt(29));
            }
        }
        c.close();

        return values;
    }

    public void saveCustomTypePosts(Object[] posts, int blogID) {
        if (posts.length == 0) {
            return;
        }
        for (int i = 0; i < posts.length; i++) {
            ContentValues values = new ContentValues();
            Map<?, ?> thisHash = (Map<?, ?>) posts[i];
            values.put("blogID", blogID);

            String postID = thisHash.get("post_id").toString();
            values.put("post_id", postID);
            values.put("post_title", thisHash.get("post_title").toString());
            Date d;
            try {
                d = (Date) thisHash.get("post_date");
                values.put("dateCreated", d.getTime());
            } catch (Exception e) {
                Date now = new Date();
                values.put("dateCreated", now.getTime());
            }
            try {
                d = (Date) thisHash.get("post_date_gmt");
                values.put("date_created_gmt", d.getTime());
            } catch (Exception e) {
                d = new Date((Long) values.get("post_date"));
                values.put("date_created_gmt",
                        d.getTime() + (d.getTimezoneOffset() * 60000));
            }
            values.put("post_status", thisHash.get("post_status").toString());
            final String postType = thisHash.get("post_type").toString();
            values.put("post_type", postType);
            values.put("post_format", thisHash.get("post_format").toString());
            values.put("post_name", thisHash.get("post_name").toString());
            values.put("post_author", thisHash.get("post_author").toString());
            values.put("post_password", thisHash.get("post_password")
                    .toString());
            values.put("post_excerpt", thisHash.get("post_excerpt").toString());
            values.put("post_content", thisHash.get("post_content").toString());
            values.put("post_parent", thisHash.get("post_parent").toString());
            values.put("post_mime_type", thisHash.get("post_mime_type")
                    .toString());
            values.put("link", thisHash.get("link").toString());
            values.put("guid", thisHash.get("guid").toString());
            values.put("menu_order", (Integer) thisHash.get("menu_order"));
            values.put("comment_status", thisHash.get("comment_status")
                    .toString());
            values.put("ping_status", thisHash.get("ping_status").toString());
            values.put("sticky", (Boolean) thisHash.get("sticky"));

            final Object terms = thisHash.get("terms");
            if (terms != null) {
                Object[] termsArray = (Object[]) terms;
                JSONArray jsonArray = new JSONArray();
                for (Object term : termsArray) {
                    final String array = Term.toJsonArray(
                            (HashMap<String, ?>) term).toString();
                    jsonArray.put(array);
                }
                values.put("terms", jsonArray.toString());
            }

            final Object customFields = thisHash.get("custom_fields");
            if (customFields != null) {
                Object[] fieldsArray = (Object[]) customFields;
                JSONArray jsonArray = new JSONArray();
                for (Object field : fieldsArray) {
                    final String array = CustomField.toJsonArray(
                            (HashMap<String, ?>) field).toString();
                    jsonArray.put(array);
                }
                values.put("custom_fields", jsonArray.toString());
            }

            int result = db.update(CUSTOM_TYPE_POSTS_TABLE, values, "post_id="
                    + postID + " AND post_type='" + postType + "'", null);
            if (result == 0) {
                db.insert(CUSTOM_TYPE_POSTS_TABLE, null, values);
            }
        }
    }

    public long saveCustomTypePosts(CustomTypePost post, int blogID) {
        long returnValue = -1;
        if (post != null) {

            // if null value save,null check is needed in constracter
            ContentValues values = new ContentValues();
            values.put("blogID", blogID);
            values.put("post_title", post.getTitle());

            values.put("dateCreated", post.getPost_date());

            values.put("post_status", post.getPostStatus());
            values.put("post_type", post.getPost_type());
            values.put("post_format", post.getPostFormat());
            // values.put("post_name", post.getPost_name());
            // values.put("post_author", post.getPost_author());
            values.put("post_password", post.getPassword());
            // values.put("post_excerpt", post.getPost_excerpt());
            values.put("post_content", post.getContent());
            // values.put("post_parent", post.getPost_parent());
            // values.put("post_mime_type", post.getPost_mime_type());
            // values.put("comment_status", post.getComment_status());
            // values.put("ping_status", post.getPing_status());
            values.put("sticky", post.isSticky());
            values.put("uploaded", post.isUploaded());
            values.put("localDraft", post.isLocalDraft());
            Term[] terms = post.getTerms();
            if (terms != null) {
                JSONArray array = new JSONArray();
                for (int i = 0; i < terms.length; i++) {
                    Term term = terms[i];
                    array.put(term.toJsonArray().toString());
                }
                values.put("terms", array.toString());
            }

            returnValue = db.insert(CUSTOM_TYPE_POSTS_TABLE, null, values);

        }
        return returnValue;
    }

    public int updateCustomTypePosts(CustomTypePost post, int blogID) {
        int success = 0;
        if (post != null) {

            ContentValues values = new ContentValues();
            values.put("post_id", post.getPostId());
            values.put("post_title", post.getTitle());

            values.put("dateCreated", post.getPost_date());

            values.put("post_status", post.getPostStatus());
            final String postType = post.getPost_type();
            values.put("post_type", postType);
            values.put("post_format", post.getPostFormat());
            if (post.getPost_name() != null) {
                values.put("post_name", post.getPost_name());
            }
            if (post.getPost_author() != null) {
                values.put("post_author", post.getPost_author());
            }
            values.put("post_password", post.getPassword());
            if (post.getPost_excerpt() != null) {
                values.put("post_excerpt", post.getPost_excerpt());
            }
            values.put("post_content", post.getContent());
            if (post.getPost_parent() != null) {
                values.put("post_parent", post.getPost_parent());
            }
            if (post.getPost_mime_type() != null) {
                values.put("post_mime_type", post.getPost_mime_type());
            }
            if (post.getComment_status() != null) {
                values.put("comment_status", post.getComment_status());
            }
            if (post.getPing_status() != null) {
                values.put("ping_status", post.getPing_status());
            }
            values.put("sticky", post.isSticky());
            values.put("uploaded", post.isUploaded());
            values.put("localDraft", post.isLocalDraft());
            Term[] terms = post.getTerms();
            if (terms != null) {
                JSONArray array = new JSONArray();
                for (int i = 0; i < terms.length; i++) {
                    Term term = terms[i];
                    array.put(term.toJsonArray().toString());
                }
                values.put("terms", array.toString());
            }

            success = db.update(CUSTOM_TYPE_POSTS_TABLE, values, "blogID="
                    + post.getBlogID() + " AND id=" + post.getId()
                    + " AND post_type='" + postType + "'", null);

        }
        return (success);
    }

    public List<Map<String, Object>> loadUploadedCustomTypePosts(
            String typePost, int blogID) {
        List<Map<String, Object>> returnVector = new Vector<Map<String, Object>>();
        Cursor c;
        c = db.query(CUSTOM_TYPE_POSTS_TABLE, new String[] { "id", "blogID",
                "post_id", "post_title", "date_created_gmt", "dateCreated",
                "post_status" }, "blogID=" + blogID
                + " AND localDraft != 1 AND post_type='" + typePost + "'",
                null, null, null, null);

        int numRows = c.getCount();
        c.moveToFirst();

        for (int i = 0; i < numRows; ++i) {
            if (c.getString(0) != null) {
                Map<String, Object> returnHash = new HashMap<String, Object>();
                returnHash.put("id", c.getInt(0));
                returnHash.put("blogID", c.getString(1));
                returnHash.put("postID", c.getString(2));
                returnHash.put("title", c.getString(3));
                returnHash.put("date_created_gmt", c.getLong(4));
                returnHash.put("dateCreated", c.getLong(5));
                returnHash.put("post_status", c.getString(6));
                returnVector.add(i, returnHash);
            }
            c.moveToNext();
        }
        c.close();

        if (numRows == 0) {
            returnVector = null;
        }

        return returnVector;
    }

    public List<Object> loadCustomTypePost(String typePost, int blogID, long id) {
        List<Object> values = null;

        Cursor c = db.query(CUSTOM_TYPE_POSTS_TABLE, null, "blogID=" + blogID
                + " AND id=" + id + " AND post_type='" + typePost + "'", null,
                null, null, null);

        if (c.getCount() > 0) {
            if (c.moveToFirst() && c.getString(0) != null) {
                values = new Vector<Object>();
                values.add(c.getLong(0));
                values.add(c.getString(1));
                values.add(c.getString(2));
                values.add(c.getString(3));
                values.add(c.getLong(4));
                values.add(c.getLong(5));
                values.add(c.getString(6));
                values.add(c.getString(7));
                values.add(c.getString(8));
                values.add(c.getString(9));
                values.add(c.getString(10));
                values.add(c.getString(11));
                values.add(c.getString(12));
                values.add(c.getString(13));
                values.add(c.getString(14));
                values.add(c.getString(15));
                values.add(c.getString(16));
                values.add(c.getString(17));
                values.add(c.getInt(18));
                values.add(c.getString(19));
                values.add(c.getString(20));
                values.add(c.getInt(21));
                values.add(c.getString(22));
                values.add(c.getString(23));
                values.add(c.getString(24));
                values.add(c.getInt(25));
                values.add(c.getInt(26));
            }
        }
        c.close();

        return values;
    }

    public boolean deleteCustomTypePost(CustomTypePost post) {

        boolean returnValue = false;

        int result = 0;
        result = db.delete(CUSTOM_TYPE_POSTS_TABLE,
                "blogID=" + post.getBlogID() + " AND id=" + post.getId()
                        + " AND post_type='" + post.getPost_type() + "'", null);

        if (result == 1) {
            returnValue = true;
        }

        return returnValue;
    }

    public void deleteUploadedCustomTypePosts(int blogID, String typeName) {
        db.delete(CUSTOM_TYPE_POSTS_TABLE, "blogID=" + blogID
                + " AND localDraft != 1 AND post_type='" + typeName + "'", null);
    }

    public List<Map<String, Object>> loadCustomTypeDrafts(int blogID,
            String typeName) {

        List<Map<String, Object>> returnVector = new Vector<Map<String, Object>>();
        Cursor c = db.query(CUSTOM_TYPE_POSTS_TABLE, new String[] { "id",
                "post_title", "post_status", "uploaded", "date_created_gmt",
                "post_status" }, "blogID=" + blogID
                + " AND localDraft=1 AND uploaded=0 AND post_type='" + typeName
                + "'", null, null, null, null);

        int numRows = c.getCount();
        c.moveToFirst();

        for (int i = 0; i < numRows; ++i) {
            if (c.getString(0) != null) {
                Map<String, Object> returnHash = new HashMap<String, Object>();
                returnHash.put("id", c.getInt(0));
                returnHash.put("title", c.getString(1));
                returnHash.put("status", c.getString(2));
                returnHash.put("uploaded", c.getInt(3));
                returnHash.put("date_created_gmt", c.getLong(4));
                returnHash.put("post_status", c.getString(5));
                returnVector.add(i, returnHash);
            }
            c.moveToNext();
        }
        c.close();

        if (numRows == 0) {
            returnVector = null;
        }

        return returnVector;
    }

    public List<Map<String, Object>> loadComments(int blogID) {

        List<Map<String, Object>> returnVector = new Vector<Map<String, Object>>();
        Cursor c = db.query(COMMENTS_TABLE,
                new String[] { "blogID", "postID", "iCommentID", "author",
                        "comment", "commentDate", "commentDateFormatted",
                        "status", "url", "email", "postTitle" }, "blogID="
                        + blogID, null, null, null, null);

        int numRows = c.getCount();
        c.moveToFirst();

        for (int i = 0; i < numRows; i++) {
            if (c.getString(0) != null) {
                Map<String, Object> returnHash = new HashMap<String, Object>();
                returnHash.put("blogID", c.getString(0));
                returnHash.put("postID", c.getInt(1));
                returnHash.put("commentID", c.getInt(2));
                returnHash.put("author", c.getString(3));
                returnHash.put("comment", c.getString(4));
                returnHash.put("commentDate", c.getString(5));
                returnHash.put("commentDateFormatted", c.getString(6));
                returnHash.put("status", c.getString(7));
                returnHash.put("url", c.getString(8));
                returnHash.put("email", c.getString(9));
                returnHash.put("postTitle", c.getString(10));
                returnVector.add(i, returnHash);
            }
            c.moveToNext();
        }
        c.close();

        if (numRows == 0) {
            returnVector = null;
        }

        return returnVector;
    }

    public boolean saveComments(List<?> commentValues) {
        boolean returnValue = false;

        Map<?, ?> firstHash = (Map<?, ?>) commentValues.get(0);
        String blogID = firstHash.get("blogID").toString();
        // delete existing values, if user hit refresh button

        try {
            db.delete(COMMENTS_TABLE, "blogID=" + blogID, null);
        } catch (Exception e) {

            return false;
        }

        for (int i = 0; i < commentValues.size(); i++) {
            try {
                ContentValues values = new ContentValues();
                Map<?, ?> thisHash = (Map<?, ?>) commentValues.get(i);
                values.put("blogID", thisHash.get("blogID").toString());
                values.put("postID", thisHash.get("postID").toString());
                values.put("iCommentID", thisHash.get("commentID").toString());
                values.put("author", thisHash.get("author").toString());
                values.put("comment", thisHash.get("comment").toString());
                values.put("commentDate", thisHash.get("commentDate")
                        .toString());
                values.put("commentDateFormatted",
                        thisHash.get("commentDateFormatted").toString());
                values.put("status", thisHash.get("status").toString());
                values.put("url", thisHash.get("url").toString());
                values.put("email", thisHash.get("email").toString());
                values.put("postTitle", thisHash.get("postTitle").toString());
                synchronized (this) {
                    try {
                        returnValue = db.insert(COMMENTS_TABLE, null, values) > 0;
                    } catch (Exception e) {

                        return false;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return (returnValue);

    }

    public void updateComment(int blogID, int id, Map<?, ?> commentHash) {

        ContentValues values = new ContentValues();
        values.put("author", commentHash.get("author").toString());
        values.put("comment", commentHash.get("comment").toString());
        values.put("status", commentHash.get("status").toString());
        values.put("url", commentHash.get("url").toString());
        values.put("email", commentHash.get("email").toString());

        synchronized (this) {
            db.update(COMMENTS_TABLE, values, "blogID=" + blogID
                    + " AND iCommentID=" + id, null);
        }

    }

    public void updateCommentStatus(int blogID, int id, String newStatus) {

        ContentValues values = new ContentValues();
        values.put("status", newStatus);
        synchronized (this) {
            db.update(COMMENTS_TABLE, values, "blogID=" + blogID
                    + " AND iCommentID=" + id, null);
        }

    }

    public void clearPosts(String blogID) {

        // delete existing values
        db.delete(POSTS_TABLE, "blogID=" + blogID, null);

    }

    // eula table

    public void setStatsDate() {

        ContentValues values = new ContentValues();
        values.put("statsdate", System.currentTimeMillis()); // set to current
                                                             // time
        synchronized (this) {
            db.update(EULA_TABLE, values, "id=0", null);
        }

    }

    public long getStatsDate() {

        Cursor c = db.query(EULA_TABLE, new String[] { "statsdate" }, "id=0",
                null, null, null, null);
        int numRows = c.getCount();
        c.moveToFirst();
        long returnValue = 0;
        if (numRows == 1) {
            returnValue = c.getLong(0);
        }
        c.close();

        return returnValue;
    }

    // categories
    public boolean insertCategory(int id, int wp_id, String category_name) {

        ContentValues values = new ContentValues();
        values.put("blog_id", id);
        values.put("wp_id", wp_id);
        values.put("category_name", category_name.toString());
        boolean returnValue = false;
        synchronized (this) {
            returnValue = db.insert(CATEGORIES_TABLE, null, values) > 0;
        }

        return (returnValue);
    }

    public List<String> loadCategories(int id) {

        Cursor c = db.query(CATEGORIES_TABLE, new String[] { "id", "wp_id",
                "category_name" }, "blog_id=" + id, null, null, null, null);
        int numRows = c.getCount();
        c.moveToFirst();
        List<String> returnVector = new Vector<String>();
        for (int i = 0; i < numRows; ++i) {
            String category_name = c.getString(2);
            if (category_name != null) {
                returnVector.add(category_name);
            }
            c.moveToNext();
        }
        c.close();

        return returnVector;
    }

    public int getCategoryId(int id, String category) {

        Cursor c = db.query(CATEGORIES_TABLE, new String[] { "wp_id" },
                "category_name=\"" + category + "\" AND blog_id=" + id, null,
                null, null, null);
        if (c.getCount() == 0)
            return 0;
        c.moveToFirst();
        int categoryID = 0;
        categoryID = c.getInt(0);

        return categoryID;
    }

    public void clearCategories(int id) {

        // clear out the table since we are refreshing the whole enchilada
        db.delete(CATEGORIES_TABLE, "blog_id=" + id, null);

    }

    // unique identifier queries
    public void updateUUID(String uuid) {

        ContentValues values = new ContentValues();
        values.put("uuid", uuid);
        synchronized (this) {
            db.update("eula", values, null, null);
        }

    }

    public String getUUID() {

        Cursor c = db.query("eula", new String[] { "uuid" }, "id=0", null,
                null, null, null);
        int numRows = c.getCount();
        c.moveToFirst();
        String returnValue = "";
        if (numRows == 1) {
            if (c.getString(0) != null) {
                returnValue = c.getString(0);
            }
        }
        c.close();

        return returnValue;

    }

    public boolean addQuickPressShortcut(int accountId, String name) {

        ContentValues values = new ContentValues();
        values.put("accountId", accountId);
        values.put("name", name);
        boolean returnValue = false;
        synchronized (this) {
            returnValue = db.insert(QUICKPRESS_SHORTCUTS_TABLE, null, values) > 0;
        }

        return (returnValue);
    }

    public List<Map<String, Object>> getQuickPressShortcuts(int accountId) {

        Cursor c = db.query(QUICKPRESS_SHORTCUTS_TABLE, new String[] { "id",
                "accountId", "name" }, "accountId = " + accountId, null, null,
                null, null);
        String id, name;
        int numRows = c.getCount();
        c.moveToFirst();
        List<Map<String, Object>> accounts = new Vector<Map<String, Object>>();
        for (int i = 0; i < numRows; i++) {

            id = c.getString(0);
            name = c.getString(2);
            if (id != null) {
                Map<String, Object> thisHash = new HashMap<String, Object>();

                thisHash.put("id", id);
                thisHash.put("name", name);
                accounts.add(thisHash);
            }
            c.moveToNext();
        }
        c.close();

        return accounts;
    }

    public boolean deleteQuickPressShortcut(String id) {

        int rowsAffected = db.delete(QUICKPRESS_SHORTCUTS_TABLE, "id=" + id,
                null);

        boolean returnValue = false;
        if (rowsAffected > 0) {
            returnValue = true;
        }

        return (returnValue);
    }

    public static String encryptPassword(String clearText) {
        try {
            DESKeySpec keySpec = new DESKeySpec(
                    PASSWORD_SECRET.getBytes("UTF-8"));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey key = keyFactory.generateSecret(keySpec);

            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            String encrypedPwd = Base64
                    .encodeToString(
                            cipher.doFinal(clearText.getBytes("UTF-8")),
                            Base64.DEFAULT);
            return encrypedPwd;
        } catch (Exception e) {
        }
        return clearText;
    }

    protected String decryptPassword(String encryptedPwd) {
        try {
            DESKeySpec keySpec = new DESKeySpec(
                    PASSWORD_SECRET.getBytes("UTF-8"));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey key = keyFactory.generateSecret(keySpec);

            byte[] encryptedWithoutB64 = Base64.decode(encryptedPwd,
                    Base64.DEFAULT);
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] plainTextPwdBytes = cipher.doFinal(encryptedWithoutB64);
            return new String(plainTextPwdBytes);
        } catch (Exception e) {
        }
        return encryptedPwd;
    }

    private void migratePasswords() {

        Cursor c = db.query(SETTINGS_TABLE, new String[] { "id", "password",
                "httppassword", "dotcom_password" }, null, null, null, null,
                null);
        int numRows = c.getCount();
        c.moveToFirst();

        for (int i = 0; i < numRows; i++) {
            ContentValues values = new ContentValues();

            if (c.getString(1) != null) {
                values.put("password", encryptPassword(c.getString(1)));
            }
            if (c.getString(2) != null) {
                values.put("httppassword", encryptPassword(c.getString(2)));
            }
            if (c.getString(3) != null) {
                values.put("dotcom_password", encryptPassword(c.getString(3)));
            }

            db.update(SETTINGS_TABLE, values, "id=" + c.getInt(0), null);

            c.moveToNext();
        }
        c.close();
    }

    public int getUnmoderatedCommentCount(int blogID) {
        int commentCount = 0;

        Cursor c = db
                .rawQuery(
                        "select count(*) from comments where blogID=? AND status='hold'",
                        new String[] { String.valueOf(blogID) });
        int numRows = c.getCount();
        c.moveToFirst();

        if (numRows > 0) {
            commentCount = c.getInt(0);
        }

        c.close();

        return commentCount;
    }

    public boolean saveMediaFile(MediaFile mf) {
        boolean returnValue = false;

        ContentValues values = new ContentValues();
        values.put("postID", mf.getPostID());
        values.put("filePath", mf.getFileName());
        values.put("fileName", mf.getFileName());
        values.put("title", mf.getTitle());
        values.put("description", mf.getDescription());
        values.put("caption", mf.getCaption());
        values.put("horizontalAlignment", mf.getHorizontalAlignment());
        values.put("width", mf.getWidth());
        values.put("height", mf.getHeight());
        values.put("mimeType", mf.getMIMEType());
        values.put("featured", mf.isFeatured());
        values.put("isVideo", mf.isVideo());
        values.put("isFeaturedInPost", mf.isFeaturedInPost());
        synchronized (this) {
            int result = db.update(
                    MEDIA_TABLE,
                    values,
                    "postID=" + mf.getPostID() + " AND filePath='"
                            + mf.getFileName() + "'", null);
            if (result == 0)
                returnValue = db.insert(MEDIA_TABLE, null, values) > 0;
        }

        return (returnValue);
    }

    public MediaFile[] getMediaFilesForPost(Post p) {

        Cursor c = db.query(MEDIA_TABLE, null, "postID=" + p.getId(), null,
                null, null, null);
        int numRows = c.getCount();
        c.moveToFirst();
        MediaFile[] mediaFiles = new MediaFile[numRows];
        for (int i = 0; i < numRows; i++) {

            MediaFile mf = new MediaFile();
            mf.setPostID(c.getInt(1));
            mf.setFilePath(c.getString(2));
            mf.setFileName(c.getString(3));
            mf.setTitle(c.getString(4));
            mf.setDescription(c.getString(5));
            mf.setCaption(c.getString(6));
            mf.setHorizontalAlignment(c.getInt(7));
            mf.setWidth(c.getInt(8));
            mf.setHeight(c.getInt(9));
            mf.setMIMEType(c.getString(10));
            mf.setFeatured(c.getInt(11) > 0);
            mf.setVideo(c.getInt(12) > 0);
            mf.setFeaturedInPost(c.getInt(13) > 0);
            mediaFiles[i] = mf;
            c.moveToNext();
        }
        c.close();

        return mediaFiles;
    }

    public boolean deleteMediaFile(MediaFile mf) {

        boolean returnValue = false;

        int result = 0;
        result = db.delete(MEDIA_TABLE, "id=" + mf.getId(), null);

        if (result == 1) {
            returnValue = true;
        }

        return returnValue;
    }

    public MediaFile getMediaFile(String src, long postId) {

        Cursor c = db.query(MEDIA_TABLE, null, "postID=" + postId
                + " AND filePath='" + src + "'", null, null, null, null);
        int numRows = c.getCount();
        c.moveToFirst();
        MediaFile mf = new MediaFile();
        if (numRows == 1) {
            mf.setPostID(c.getInt(1));
            mf.setFilePath(c.getString(2));
            mf.setFileName(c.getString(3));
            mf.setTitle(c.getString(4));
            mf.setDescription(c.getString(5));
            mf.setCaption(c.getString(6));
            mf.setHorizontalAlignment(c.getInt(7));
            mf.setWidth(c.getInt(8));
            mf.setHeight(c.getInt(9));
            mf.setMIMEType(c.getString(10));
            mf.setFeatured(c.getInt(11) > 0);
            mf.setVideo(c.getInt(12) > 0);
            mf.setFeaturedInPost(c.getInt(13) > 0);
        } else {
            c.close();
            return null;
        }
        c.close();

        return mf;
    }

    public void deleteMediaFilesForPost(Post post) {

        db.delete(MEDIA_TABLE, "postID=" + post.getId(), null);

    }

    public void savePostTypes(HashMap<?, ?> map, int blogID) {

        synchronized (this) {
            db.delete(TYPES_TABLE, "blogID=" + blogID, null);
        }

        Set<?> keySet = map.keySet();
        for (Object o : keySet) {
            Object value = map.get(o);
            savePostType((HashMap<?, ?>) value, blogID);
        }
    }

    public long savePostType(HashMap<?, ?> map, int blogID) {

        ContentValues values = new ContentValues();
        values.put("blogID", blogID);
        String name = map.get("name").toString();
        values.put("name", name);
        values.put("label", map.get("label").toString());
        values.put("hierarchical", (Boolean) map.get("hierarchial"));
        values.put("public", (Boolean) map.get("public"));
        values.put("show_ui", (Boolean) map.get("show_ui"));
        values.put("builtin", (Boolean) map.get("_builtin"));
        values.put("has_archive", (Boolean) map.get("has_archive"));

        HashMap<?, ?> labels = (HashMap<?, ?>) map.get("labels");
        Set<?> labelKeySet = labels.keySet();
        JSONArray array = new JSONArray();
        for (Object o : labelKeySet) {
            array.put(labels.get(o).toString());
        }
        values.put("labels", array.toString());

        array = new JSONArray();
        HashMap<?, ?> caps = (HashMap<?, ?>) map.get("cap");
        Set<?> capKeySet = labels.keySet();
        for (Object o : capKeySet) {
            array.put(caps.get(o));
        }
        values.put("cap", array.toString());

        values.put("map_meta_cap", (Boolean) map.get("map_meta_cap"));
        values.put("menu_position", (Integer) map.get("menu_position"));
        values.put("menu_icon", map.get("menu_icon").toString());
        values.put("show_in_menu", (Boolean) map.get("show_in_menu"));

        array = new JSONArray();
        Object[] taxonomies = (Object[]) map.get("taxonomies");
        for (Object o : taxonomies) {
            array.put(o);
        }
        values.put("taxonomies", array.toString());

        long result;
        synchronized (this) {
            result = db.update(TYPES_TABLE, values, "blogID=" + blogID
                    + " AND name='" + name + "'", null);
            if (result == 0)
                result = db.insert(TYPES_TABLE, null, values);
        }

        return result;
    }

    public void getPostTypes(Context context, int blogID,
            ArrayList<MenuDrawerItem> items) {
        Cursor c = db.query(TYPES_TABLE, new String[] { "label", "name", },
                "blogID =" + blogID, null, null, null, null);
        if (c.moveToFirst()) {
            do {
                String label = c.getString(0);
                String postType = c.getString(1);
                items.add(new MenuDrawerItem(context, label, postType));

            } while (c.moveToNext());
        }
        c.close();
    }

    public List<Object> loadPostType(int blogID, String name) {
        List<Object> values = null;

        Cursor c = db.query(TYPES_TABLE, null, "blogID =" + blogID
                + " AND name='" + name + "'", null, null, null, null);

        if (c.getCount() > 0) {
            if (c.moveToFirst() && c.getString(0) != null) {
                values = new Vector<Object>();
                values.add(c.getLong(0));
                values.add(c.getString(1));
                values.add(c.getString(2));
                values.add(c.getString(3));
                values.add(c.getInt(4));
                values.add(c.getInt(5));
                values.add(c.getInt(6));
                values.add(c.getInt(7));
                values.add(c.getInt(8));
                values.add(c.getString(9));
                values.add(c.getString(10));
                values.add(c.getString(11));
                values.add(c.getInt(12));
                values.add(c.getInt(13));
                values.add(c.getString(14));
                values.add(c.getInt(15));
                values.add(c.getString(16));
            }
        }
        c.close();

        return values;
    }

    public String[] getPostTypes(int blogID) {
        Cursor c = db.query(TYPES_TABLE, new String[] { "name" }, "blogID ="
                + blogID, null, null, null, null);

        final int count = c.getCount();
        final String[] result = new String[count];
        if (count > 0 && c.moveToFirst()) {
            for (int i = 0; i < count; i++, c.moveToNext()) {
                result[i] = c.getString(0);
            }
        }
        c.close();
        return result;
    }

    public void getPostTypes(int blogID, ArrayList<String> labels,
            ArrayList<String> postTypes) {
        Cursor c = db.query(TYPES_TABLE, new String[] { "label", "name" },
                "blogID =" + blogID, null, null, null, null);

        final int count = c.getCount();
        if (count > 0 && c.moveToFirst()) {
            for (int i = 0; i < count; i++, c.moveToNext()) {
                labels.add(c.getString(0));
                postTypes.add(c.getString(1));
            }
        }
        c.close();
    }

    public void saveTaxonomies(Object[] maps, int blogUniqueID) {

        for (int i = 0; i < maps.length; i++) {
            HashMap<?, ?> map = (HashMap<?, ?>) maps[i];

            ContentValues values = new ContentValues();
            values.put("blogID", blogUniqueID);
            String name = map.get("name").toString();
            values.put("name", name);
            values.put("label", map.get("label").toString());
            values.put("hierarchical", (Boolean) map.get("hierarchical"));
            values.put("public", (Boolean) map.get("public"));
            values.put("show_ui", (Boolean) map.get("show_ui"));
            values.put("builtin", (Boolean) map.get("_builtin"));

            HashMap<?, ?> labels = (HashMap<?, ?>) map.get("labels");
            Set<?> labelKeySet = labels.keySet();
            JSONArray array = new JSONArray();
            for (Object o : labelKeySet) {
                array.put(labels.get(o).toString());
            }
            values.put("labels", array.toString());

            array = new JSONArray();
            HashMap<?, ?> caps = (HashMap<?, ?>) map.get("cap");
            Set<?> capKeySet = labels.keySet();
            for (Object o : capKeySet) {
                array.put(caps.get(o));
            }
            values.put("cap", array.toString());

            array = new JSONArray();
            Object[] objectType = (Object[]) map.get("object_type");
            for (Object o : objectType) {
                array.put(o);
            }
            values.put("object_type", array.toString());

            long result;
            synchronized (this) {
                result = db.update(TAXONOMY_TABLE, values, "blogID="
                        + blogUniqueID + " AND name='" + name + "'", null);
                if (result == 0)
                    result = db.insert(TAXONOMY_TABLE, null, values);
            }
        }
    }

    public List<Object> loadTaxonomy(int blogID, String name) {
        List<Object> values = null;

        Cursor c = db.query(TAXONOMY_TABLE, null, "blogID =" + blogID
                + " AND name='" + name + "'", null, null, null, null);

        Log.v("loadCustomTypePost", "" + c.getCount());
        if (c.getCount() > 0) {
            if (c.moveToFirst() && c.getString(0) != null) {
                values = new Vector<Object>();
                values.add(c.getLong(0));
                values.add(c.getString(1));
                values.add(c.getString(2));
                values.add(c.getString(3));
                values.add(c.getInt(4));
                values.add(c.getInt(5));
                values.add(c.getInt(6));
                values.add(c.getInt(7));
                values.add(c.getString(8));
                values.add(c.getString(9));
                values.add(c.getString(10));
            }
        }
        c.close();

        return values;
    }

    public Term[] getTerms(int blogID, String taxonomyName) {
        Cursor c = db.query(TERM_TABLE, null, "blogID =" + blogID
                + " AND taxonomy='" + taxonomyName + "'", null, null, null,
                null);
        if (c == null | c.getCount() == 0 || !c.moveToFirst()) {
            return new Term[0];
        }
        Term[] result = new Term[c.getCount()];
        int i = 0;
        do {
            final long id = c.getLong(0);
            final int blogId = Integer.parseInt(c.getString(1));
            final String termId = c.getString(2);
            final String name = c.getString(3);
            final String slug = c.getString(4);
            final String termGroup = c.getString(5);
            final String termTaxonomyId = c.getString(6);
            final String taxonomy = c.getString(7);
            final String description = c.getString(8);
            final String parent = c.getString(9);
            final int count = c.getInt(10);
            Term term = new Term(id, blogId, termId, name, slug, termGroup,
                    termTaxonomyId, taxonomy, description, parent, count);
            result[i] = term;
            i++;

        } while (c.moveToNext());
        c.close();
        return result;
    }

    public HierarchicalTerm[] getHierarchialTerm(int blogID, String taxonomyName) {
        Term[] terms = getTerms(blogID, taxonomyName);
        if (terms.length == 0) {
            return new HierarchicalTerm[0];
        }
        HierarchicalTerm[] result = toHieralcial(terms);

        return result;
    }

    private HierarchicalTerm[] toHieralcial(Term[] terms) {
        HierarchicalTerm[] tmp = new HierarchicalTerm[terms.length];
        for (int i = 0; i < terms.length; i++) {
            tmp[i] = new HierarchicalTerm(terms[i]);
        }
        for (int i = 0; i < tmp.length; i++) {
            HierarchicalTerm term = tmp[i];
            final String parent = term.getTerm().getParent();
            if ("0".equals(parent)) {
                continue;
            }
            for (int j = 0; j < tmp.length; j++) {
                HierarchicalTerm targetTerm = tmp[j];
                if (targetTerm.getTerm().getTermId().equals(parent)) {
                    term.setParent(targetTerm);
                    break;
                }
            }
        }
        HierarchicalTerm[] result = new HierarchicalTerm[tmp.length];
        fill(tmp, result, 0, null);
        return result;
    }

    private int fill(HierarchicalTerm[] base, HierarchicalTerm[] result,
            int filled, HierarchicalTerm parent) {
        if (filled >= result.length) {
            return filled;
        }
        if (parent == null) {
            for (int i = 0; i < result.length; i++) {
                HierarchicalTerm term = base[i];
                final String parentStr = term.getTerm().getParent();
                if ("0".equals(parentStr)) {
                    result[filled] = term;
                    filled = fill(base, result, filled + 1, result[filled]);
                }
            }
        } else {
            for (int i = 0; i < result.length; i++) {
                HierarchicalTerm term = base[i];
                final String parentStr = term.getTerm().getParent();
                if (parentStr.equals(parent.getTerm().getTermId())) {
                    result[filled] = term;
                    filled = fill(base, result, filled + 1, term);
                }
            }
        }
        return filled;
    }

    public void saveTerms(int blogID, Object[] array) {
        for (Object o : array) {
            HashMap<?, ?> map = (HashMap<?, ?>) o;
            ContentValues values = new ContentValues();
            values.put("blogID", blogID);
            String termId = map.get("term_id").toString();
            values.put("term_id", termId);
            values.put("name", map.get("name").toString());
            values.put("slug", map.get("slug").toString());
            values.put("term_group", map.get("term_group").toString());
            values.put("term_taxonomy_id", map.get("term_taxonomy_id")
                    .toString());
            String taxonomy = map.get("taxonomy").toString();
            values.put("taxonomy", taxonomy);
            values.put("description", map.get("description").toString());
            values.put("parent", map.get("parent").toString());
            values.put("count", (Integer) map.get("count"));

            long result = db.update(TERM_TABLE, values, "blogID=" + blogID
                    + " AND term_id=" + termId + " AND taxonomy='" + taxonomy
                    + "'", null);
            if (result == 0) {
                result = db.insert(TERM_TABLE, null, values);
            }
        }
    }

    public void saveTerm(int blogID, String termId, String name, String slug,
            String taxonomy, String desc, int parent) {
        ContentValues values = new ContentValues();
        values.put("blogID", blogID);
        values.put("term_id", termId);
        values.put("name", name);
        values.put("slug", slug);
        values.put("taxonomy", taxonomy);
        values.put("description", desc);
        values.put("parent", parent);

        db.insert(TERM_TABLE, null, values);
    }

    public String getIconUrl(int blogID, String name) {
        String url = null;
        Cursor c = db.query(TYPES_TABLE, new String[] { "menu_icon" },
                "blogID =" + blogID + " AND name='" + name + "'", null, null,
                null, null);
        if (c.moveToFirst() && c.getCount() > 0) {
            url = c.getString(0);
        }
        c.close();
        return url;
    }

    public int getWPCOMBlogID() {
        int id = -1;
        Cursor c = db.query(SETTINGS_TABLE, new String[] { "id" },
                "dotcomFlag=1", null, null, null, null);
        int numRows = c.getCount();
        c.moveToFirst();
        if (numRows > 0) {
            id = c.getInt(0);
        }

        c.close();

        return id;
    }

    public void clearComments(int blogID) {

        db.delete(COMMENTS_TABLE, "blogID=" + blogID, null);

    }

    public boolean findLocalChanges() {
        Cursor c = db.query(POSTS_TABLE, null, "isLocalChange=1", null, null,
                null, null);
        int numRows = c.getCount();
        if (numRows > 0) {
            return true;
        }
        c.close();

        return false;
    }

}
