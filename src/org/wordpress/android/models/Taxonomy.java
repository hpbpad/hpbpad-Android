package org.wordpress.android.models;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import org.wordpress.android.WordPress;

public final class Taxonomy {

    private long id;

    private int blogID;

    private String name;
    private String label;
    private boolean hierarchical;
    private boolean isPublic;
    private boolean showUI;
    private boolean builtin;
    private String[] labels;

    private String[] cap;
    private String[] objectType;

    // public Taxonomy(String jsonArray) {
    // try {
    // JSONArray array = new JSONArray(jsonArray);
    // // this.id = array.getLong(0);
    // // this.blogID = array.getInt(1);
    // //
    // // this.name = array.getString(2);
    // // this.label = array.getString(3);
    // // this.hierarchical = array.getBoolean(4);
    // // this.isPublic = array.getBoolean(5);
    // // this.showUI = array.getBoolean(6);
    // // this.builtin = array.getBoolean(7);
    // //
    // // String labels = array.getString(8);
    //
    // this.blogID = array.getInt(0);
    //
    // this.name = array.getString(1);
    // this.label = array.getString(2);
    // this.hierarchical = array.getBoolean(3);
    // this.isPublic = array.getBoolean(4);
    // this.showUI = array.getBoolean(5);
    // this.builtin = array.getBoolean(6);
    //
    // String labels = array.getString(7);
    // JSONArray labelArray = new JSONArray(labels);
    // final int length = labelArray.length();
    // this.labels = new String[length];
    // for (int i = 0; i < length; i++) {
    // this.labels[i] = labelArray.getString(i);
    // }
    // } catch (JSONException e) {
    // e.printStackTrace();
    // }
    // }

    public Taxonomy(int blogUniqueId, String name) {
        this.blogID = blogUniqueId;
        this.name = name;
        List<Object> list = WordPress.wpDB.loadTaxonomy(blogUniqueId, name);
        if (list == null || list.size() < 11) {
            return;
        }
        this.id = (Long) list.get(0);
        this.label = list.get(3).toString();
        this.hierarchical = (Integer) list.get(4) > 0;
        this.isPublic = (Integer) list.get(5) > 0;
        this.showUI = (Integer) list.get(6) > 0;
        this.builtin = (Integer) list.get(7) > 0;

        String labels = list.get(8).toString();
        try {
            JSONArray labelArray = new JSONArray(labels);
            final int length = labelArray.length();
            this.labels = new String[length];
            for (int i = 0; i < length; i++) {
                this.labels[i] = labelArray.getString(i);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String caps = list.get(9).toString();
        try {
            JSONArray capArray = new JSONArray(caps);
            final int length = capArray.length();
            this.cap = new String[length];
            for (int i = 0; i < length; i++) {
                this.cap[i] = capArray.getString(i);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String objectType = list.get(10).toString();
        try {
            JSONArray objectTypeArray = new JSONArray(objectType);
            final int length = objectTypeArray.length();
            this.objectType = new String[length];
            for (int i = 0; i < length; i++) {
                this.objectType[i] = objectTypeArray.getString(i);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    // public static JSONArray toJsonArray(Taxonomy taxonomy) {
    // JSONArray result = new JSONArray();
    // // result.put(taxonomy.id);
    // result.put(taxonomy.blogID);
    //
    // result.put(taxonomy.name);
    // result.put(taxonomy.label);
    // result.put(taxonomy.hierarchical);
    // result.put(taxonomy.isPublic);
    // result.put(taxonomy.showUI);
    // result.put(taxonomy.builtin);
    // JSONArray array = new JSONArray();
    // for (String label : taxonomy.labels) {
    // array.put(label);
    // }
    // result.put(array.toString());
    //
    // return result;
    // }

    public long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return this.label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isHierarchical() {
        return this.hierarchical;
    }

    public void setHierarchical(boolean hierarchical) {
        this.hierarchical = hierarchical;
    }

    public boolean isPublic() {
        return this.isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public boolean isShowUI() {
        return this.showUI;
    }

    public void setShowUI(boolean showUI) {
        this.showUI = showUI;
    }

    public boolean isBuiltin() {
        return this.builtin;
    }

    public void setBuiltin(boolean builtin) {
        this.builtin = builtin;
    }

    public String[] getLabels() {
        return this.labels;
    }

    public void setLabels(String[] labels) {
        this.labels = labels;
    }

    public String[] getCap() {
        return this.cap;
    }

    public void setCap(String[] cap) {
        this.cap = cap;
    }

    public String[] getObjectType() {
        return this.objectType;
    }

    public void setObjectType(String[] objectType) {
        this.objectType = objectType;
    }

}
