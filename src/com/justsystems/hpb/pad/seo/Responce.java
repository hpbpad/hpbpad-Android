package com.justsystems.hpb.pad.seo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class Responce {

    static final String PARAM_TITLE = "title";
    static final String PARAM_CONTENTS = "contents";
    static final String PARAM_H1 = "h1";
    static final String PARAM_METADESCRIPTION = "metadescription";
    static final String PARAM_METAKEYWORD = "metakeyword";
    static final String PARAM_KEYWORD_BALANCE = "keywordbalance";

    private final Result title;
    private final Result contents;
    private final Result h1;
    private final Result metadescription;
    private final Result metakeyword;
    private final KeywordResult[] keywordBalances;

    Responce(Result title, Result contents, Result h1, Result metadescription,
            Result metakeyword, KeywordResult[] keywordBalances) {
        this.title = title;
        this.contents = contents;
        this.h1 = h1;
        this.metadescription = metadescription;
        this.metakeyword = metakeyword;
        this.keywordBalances = keywordBalances;
    }

    static Responce createFromJson(String jsonStr) {
        try {
            JSONObject jsonObject = new JSONObject(jsonStr);
            Result title = Result.createFromJson(jsonObject
                    .getJSONObject(PARAM_TITLE));
            Result contents = Result.createFromJson(jsonObject
                    .getJSONObject(PARAM_CONTENTS));
            Result h1 = Result.createFromJson(jsonObject
                    .getJSONObject(PARAM_H1));
            Result metadescription = Result.createFromJson(jsonObject
                    .getJSONObject(PARAM_METADESCRIPTION));
            Result metakeyword = Result.createFromJson(jsonObject
                    .getJSONObject(PARAM_METAKEYWORD));
            JSONArray kwResultsJson = jsonObject
                    .getJSONArray(PARAM_KEYWORD_BALANCE);
            final int length = kwResultsJson.length();
            KeywordResult[] kwResults = new KeywordResult[length];
            for (int i = 0; i < length; i++) {
                JSONObject result = kwResultsJson.getJSONObject(i);
                kwResults[i] = KeywordResult.createFromJson(result);
            }

            return new Responce(title, contents, h1, metadescription,
                    metakeyword, kwResults);
        } catch (JSONException e) {
        }
        return null;
    }

    public Result getTitle() {
        return this.title;
    }

    public Result getContents() {
        return this.contents;
    }

    public Result getH1() {
        return this.h1;
    }

    public Result getMetadescription() {
        return this.metadescription;
    }

    public Result getMetaKeyword() {
        return this.metakeyword;
    }

    public KeywordResult[] getKeywordBalances() {
        return this.keywordBalances;
    }
}
