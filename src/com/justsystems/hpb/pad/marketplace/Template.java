package com.justsystems.hpb.pad.marketplace;

import java.io.File;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

public final class Template {
    private static final String THUMBNAIL_DIRECTORY = "mp_thumbnail";

    private static final String PARAM_ID = "id";
    private static final String PARAM_LINK = "link";
    private static final String PARAM_THUMBNAIL = "thumbnail";
    private static final String PARAM_THUMBNAIL_PC = "pc";
    private static final String PARAM_THUMBNAIL_SMT = "smt";

    private static final boolean USE_PC_THUMBNAIL = false;

    private final int id;
    private final int position;
    private final String link;
    private final String thumbnail;
    private final long time;

    public Template(int id, int position, String link, String thumbnail,
            long time) {
        this.id = id;
        this.position = position;
        this.link = link;
        this.thumbnail = thumbnail.replace("-smt_01.png", "_sp_01.jpg");
        this.time = time;
    }

    public static Template createFromJson(JSONObject object, int position,
            long time) {
        try {
            int id = object.getInt(PARAM_ID);
            String link = object.getString(PARAM_LINK);
            JSONObject thumbnailJson = object.getJSONObject(PARAM_THUMBNAIL);
            String thumbnail;
            if (USE_PC_THUMBNAIL) {
                thumbnail = thumbnailJson.getString(PARAM_THUMBNAIL_PC);
            } else {
                thumbnail = thumbnailJson.getString(PARAM_THUMBNAIL_SMT);
            }
            return new Template(id, position, link, thumbnail, time);
        } catch (JSONException e) {
            return null;
        }
    }

    public int getId() {
        return this.id;
    }

    public int getPosition() {
        return this.position;
    }

    public String getLink() {
        return this.link;
    }

    public static String getThumbnailDir(Context context) {
        return context.getFilesDir().getAbsolutePath() + File.separator
                + THUMBNAIL_DIRECTORY;
    }

    public String getThumbnailFullPath(Context context) {
        return getThumbnailDir(context) + File.separator + this.position + "_"
                + this.time + ".png";
    }

    public String getThumbnail() {
        return this.thumbnail;
    }

    public long getTime() {
        return this.time;
    }
}
