package com.justsystems.hpb.pad.seo;

import org.json.JSONException;
import org.json.JSONObject;

class Result {

    private static final String PARAM_MESSAGE = "message";
    private static final String PARAM_SCORE = "score";

    private static final int SCORE_MAX = 3;
    private static final int SCORE_MIN = 0;

    private final String message;
    private final int score;

    public Result(String message, int score) {
        this.message = message;
        this.score = Math.min(SCORE_MAX, Math.max(SCORE_MIN, score));
    }

    static Result createFromJson(JSONObject json) {
        try {
            String message = json.getString(PARAM_MESSAGE);
            if (message.endsWith("\n")) {
                message = message.substring(0, message.length() - 1);
            }
            int score = json.getInt(PARAM_SCORE);
            return new Result(message, score);
        } catch (JSONException e) {
        }
        return null;
    }

    public String getMessage() {
        return this.message;
    }

    public int getScore() {
        return this.score;
    }
}
