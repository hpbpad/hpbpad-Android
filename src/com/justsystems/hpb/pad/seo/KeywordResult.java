package com.justsystems.hpb.pad.seo;

import org.json.JSONException;
import org.json.JSONObject;

public class KeywordResult {

    private static final String PARAM_KEYWORD = "keyword";
    private static final String PARAM_NUM = "num";
    private static final String PARAM_PER = "per";

    private final String keyword;
    private final int num;
    private final float per;

    public KeywordResult(String keyword, int num, float per) {
        this.keyword = keyword;
        this.num = num;
        this.per = per;

    }

    static KeywordResult createFromJson(JSONObject json) {
        try {
            String keyword = json.getString(PARAM_KEYWORD);
            int num = json.getInt(PARAM_NUM);
            float per = (float) json.getDouble(PARAM_PER);
            return new KeywordResult(keyword, num, per);
        } catch (JSONException e) {
        }
        return null;
    }

    public String getKeyword() {
        return this.keyword;
    }

    public int getNum() {
        return this.num;
    }

    public float getPer() {
        return this.per;
    }
}
