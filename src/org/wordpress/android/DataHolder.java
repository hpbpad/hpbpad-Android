package org.wordpress.android;

import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

import org.wordpress.android.models.Blog;

final class DataHolder {

    private static DataHolder this_;

    final private WordPressDB wpDB;

    private final String versionName;

    private Blog currentBlog;

    private DataHolder(Context context) {
        wpDB = new WordPressDB(context);

        PackageManager pm = context.getPackageManager();
        String tmpName = null;
        try {
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            tmpName = pi.versionName;
        } catch (NameNotFoundException e) {
            tmpName = "";
        } finally {
            this.versionName = tmpName;
        }
    }

    public static synchronized DataHolder getInstance(Context context) {
        if (this_ == null) {
            this_ = new DataHolder(context);
        }
        return this_;
    }

    public String getVersionName() {
        return this.versionName;
    }

    public WordPressDB getDB() {
        return this.wpDB;
    }

    /**
     * Get the currently active blog.
     * <p>
     * If the current blog is not already set, try and determine the last active
     * blog from the last time the application was used. If we're not able to
     * determine the last active blog, just select the first one.
     */
    public Blog getCurrentBlog() {
        if (currentBlog == null) {
            // attempt to restore the last active blog
            setCurrentBlogToLastActive();

            // fallback to just using the first blog
            List<Map<String, Object>> accounts = wpDB.getAccounts();
            if (currentBlog == null && accounts.size() > 0) {
                int id = Integer.valueOf(accounts.get(0).get("id").toString());
                setCurrentBlog(id);
                wpDB.updateLastBlogId(id);
            }
        }

        return currentBlog;
    }

    /**
     * Set the last active blog as the current blog.
     * 
     * @return the current blog
     */
    public Blog setCurrentBlogToLastActive() {
        List<Map<String, Object>> accounts = wpDB.getAccounts();

        int lastBlogId = wpDB.getLastBlogId();
        if (lastBlogId != -1) {
            for (Map<String, Object> account : accounts) {
                int id = Integer.valueOf(account.get("id").toString());
                if (id == lastBlogId) {
                    setCurrentBlog(id);
                }
            }
        }

        return currentBlog;
    }

    /**
     * Set the blog with the specified id as the current blog.
     * 
     * @param id
     *            id of the blog to set as current
     * @return the current blog
     */
    public Blog setCurrentBlog(int id) {
        try {
            currentBlog = new Blog(id);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return currentBlog;
    }

}
