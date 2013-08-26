package org.wordpress.android.models;

import java.util.HashMap;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

public final class Term implements Parcelable {

    private long id;
    private int blogID;

    private String termId;
    private String name;
    private String slug;
    private String termGroup;
    private String termTaxonomyId;
    private String taxonomy;
    private String description;
    private String parent;
    private int count;

    public Term(String jsonArray) {
        try {
            JSONArray array = new JSONArray(jsonArray);
            this.termId = array.getString(0);
            this.name = array.getString(1);
            this.slug = array.getString(2);
            this.termGroup = array.getString(3);
            this.termTaxonomyId = array.getString(4);
            this.taxonomy = array.getString(5);
            this.description = array.getString(6);
            this.parent = array.getString(7);
            this.count = array.getInt(8);

            Log.d("term", "termId" + termId + " name " + name + " slug" + slug
                    + " termGroup" + termGroup + " termTaxonomyId"
                    + termTaxonomyId + " taxonomy" + taxonomy + " descriptoin"
                    + description + " parent" + parent + " count" + count);
        } catch (JSONException e) {
        }
    }

    public Term(long id, int blogId, String termId, String name, String slug,
            String termGroup, String termTaxonomyId, String taxonomy,
            String description, String parent, int count) {
        this.id = id;
        this.blogID = blogId;
        this.termId = termId;
        this.name = name;
        this.slug = slug;
        this.termGroup = termGroup;
        this.termTaxonomyId = termTaxonomyId;
        this.taxonomy = taxonomy;
        this.description = description;
        this.parent = parent;
        this.count = count;

    }

    public String getTermId() {
        return this.termId;
    }

    public String getName() {
        return this.name;
    }

    public String getSlug() {
        return this.slug;
    }

    public String getTermGroup() {
        return this.termGroup;
    }

    public String getTermTaxonomyId() {
        return this.termTaxonomyId;
    }

    public String getTaxonomy() {
        return this.taxonomy;
    }

    public String getDescriptoin() {
        return this.description;
    }

    public String getParent() {
        return this.parent;
    }

    public int getCount() {
        return this.count;
    }

    public static JSONArray toJsonArray(HashMap<String, ?> map) {
        JSONArray result = new JSONArray();
        result.put(map.get("term_id"));
        result.put(map.get("name"));
        result.put(map.get("slug"));
        result.put(map.get("term_group"));
        result.put(map.get("term_taxonomy_id"));
        result.put(map.get("taxonomy"));
        result.put(map.get("description"));
        result.put(map.get("parent"));
        result.put(map.get("count"));

        return result;
    }

    public JSONArray toJsonArray() {
        JSONArray result = new JSONArray();
        result.put(this.termId);
        result.put(this.name);
        result.put(this.slug);
        result.put(this.termGroup);
        result.put(this.termTaxonomyId);
        result.put(this.taxonomy);
        result.put(this.description);
        result.put(this.parent);
        result.put(this.count);

        return result;
    }

    public HashMap<String, ?> toHashMap() {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("term_id", this.termId);
        map.put("name", this.name);
        map.put("slug", this.slug);
        map.put("term_group", this.termGroup);
        map.put("term_taxonomy_id", this.termTaxonomyId);
        map.put("taxonomy", this.taxonomy);
        map.put("description", this.description);
        map.put("parent", this.parent);
        map.put("count", this.count);

        return map;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeInt(this.blogID);

        dest.writeString(this.termId);
        dest.writeString(this.name);
        dest.writeString(this.slug);
        dest.writeString(this.termGroup);
        dest.writeString(this.termTaxonomyId);
        dest.writeString(this.taxonomy);
        dest.writeString(this.description);
        dest.writeString(this.parent);
        dest.writeInt(this.count);
    }

    public static final Parcelable.Creator<Term> CREATOR = new Parcelable.Creator<Term>() {
        public Term createFromParcel(Parcel in) {
            long id = in.readLong();
            int blogId = in.readInt();
            String termId = in.readString();
            String name = in.readString();
            String slug = in.readString();
            String termGroup = in.readString();
            String termTaxonomyId = in.readString();
            String taxonomy = in.readString();
            String description = in.readString();
            String parent = in.readString();
            int count = in.readInt();
            return new Term(id, blogId, termId, name, slug, termGroup,
                    termTaxonomyId, taxonomy, description, parent, count);
        }

        public Term[] newArray(int size) {
            return new Term[size];
        }
    };
}
