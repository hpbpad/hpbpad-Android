package org.wordpress.android.models;

import java.io.File;
import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;

import org.json.JSONArray;
import org.json.JSONException;

import org.wordpress.android.WordPress;

public final class PostType {

    private final int blogID;
    private final String name;

    private final String label;
    private boolean hierarchical;
    private boolean isPublic;
    private boolean showUI;
    private boolean builtin;
    private boolean hasArchive;
    private String[] labels;
    private String[] cap;
    private boolean map_meta_cap;
    private int menuPosition;
    private String menuIcon;
    private boolean showInMenu;

    private Taxonomy[] taxonomies;

    private Drawable icon;

    public PostType(int blogUniqueID, String label, Drawable icon) {
        this.blogID = blogUniqueID;
        this.label = label;
        this.name = null;
        this.icon = icon;
    }

    public PostType(Context context, int blogUniqueID, String label,
            String postType) {
        this.blogID = blogUniqueID;
        this.label = label;
        this.name = postType;

        try {
            String fileName = context.getFilesDir().getAbsolutePath()
                    + File.separator + blogUniqueID + File.separator + postType
                    + ".png";
            this.icon = Drawable.createFromPath(fileName);
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        }

        List<Object> list = WordPress.wpDB.loadPostType(blogUniqueID, name);
        try {
            JSONArray taxonomies = new JSONArray(list.get(16).toString());
            final int length = taxonomies.length();
            this.taxonomies = new Taxonomy[length];
            for (int i = 0; i < length; i++) {
                String taxonomyName = taxonomies.getString(i);
                Taxonomy taxonomy = new Taxonomy(blogUniqueID, taxonomyName);
                this.taxonomies[i] = taxonomy;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public PostType(int blogUniqueID, String name) {
        this.blogID = blogUniqueID;
        List<Object> list = WordPress.wpDB.loadPostType(blogUniqueID, name);
        this.name = list.get(2).toString();
        this.label = list.get(3).toString();
        this.hierarchical = (Integer) list.get(4) > 0;
        this.isPublic = (Integer) list.get(5) > 0;
        this.showUI = (Integer) list.get(6) > 0;
        this.builtin = (Integer) list.get(7) > 0;
        this.hasArchive = (Integer) list.get(8) > 0;

        // TODO supportsの実装

        try {
            JSONArray labels = new JSONArray(list.get(10).toString());
            final int length = labels.length();
            this.labels = new String[length];
            for (int i = 0; i < length; i++) {
                this.labels[i] = labels.getString(i);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            JSONArray cap = new JSONArray(list.get(11).toString());
            final int length = cap.length();
            this.cap = new String[length];
            for (int i = 0; i < length; i++) {
                this.cap[i] = cap.getString(i);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        this.map_meta_cap = (Integer) list.get(12) > 0;
        this.menuPosition = (Integer) list.get(13);
        this.menuIcon = list.get(14).toString();
        this.showInMenu = (Integer) list.get(15) > 0;

    }

    public int getBlogID() {
        return this.blogID;
    }

    public String getName() {
        return this.name;
    }

    public String getLabel() {
        return this.label;
    }

    public Drawable getIcon() {
        return this.icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public boolean isHierarchical() {
        return this.hierarchical;
    }

    public boolean isPublic() {
        return this.isPublic;
    }

    public boolean isShowUI() {
        return this.showUI;
    }

    public boolean isBuiltin() {
        return this.builtin;
    }

    public boolean hasArchive() {
        return this.hasArchive;
    }

    public String[] getLabels() {
        return this.labels;
    }

    public String[] getCap() {
        return this.cap;
    }

    public boolean isMap_meta_cap() {
        return this.map_meta_cap;
    }

    public int getMenuPosition() {
        return this.menuPosition;
    }

    public String getMenuIcon() {
        return this.menuIcon;
    }

    public boolean isShowInMenu() {
        return this.showInMenu;
    }

    public Taxonomy[] getTaxonomies() {
        return this.taxonomies;
    }
}
