package org.wordpress.android.models;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;

public class CustomField {

    private int blogID;

    private String id;
    private String key;
    private String value;

    public CustomField(String id, String key, String value) {
        this.id = id;
        this.key = key;
        this.value = value;
    }

    public CustomField(String jsonArray) {
        try {
            JSONArray array = new JSONArray(jsonArray);
            this.id = array.getString(0);
            this.key = array.getString(1);
            this.value = array.getString(2);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getId() {
        return this.id;
    }

    public String getKey() {
        return this.key;
    }

    public String getValue() {
        return this.value;
    }

    public static JSONArray toJsonArray(HashMap<String, ?> map) {
        JSONArray result = new JSONArray();
        result.put(map.get("id"));
        result.put(map.get("key"));
        result.put(map.get("value"));

        return result;
    }

}
