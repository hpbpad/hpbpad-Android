package com.justsystems.hpb.pad.marketplace;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import com.justsystems.hpb.pad.AbsStartPageActivity;
import com.justsystems.hpb.pad.R;
import com.justsystems.hpb.pad.util.Debug;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.wordpress.android.WordPress;
import org.wordpress.android.task.MultiAsyncTask;

public final class TempleteGetTask extends
        MultiAsyncTask<Integer, Integer, Boolean> {

    // デフォルト値：タイムアウト（ミリ秒）
    private static final int DEFAULT_TIMEOUT = 10000;

    private static final String MP_URL = "https://hpbmp.jp/api/public/pad/advertise";
    private static final String PARAM_COUNT = "?count=";

    private static final String PARAM_TEMPLATE = "templates";
    private static final String PARAM_PROMOTION = "promotion";
    private static final String PARAM_ANDROID = "android";

    private static final int PROGRESS_TEMPLATE = -1;

    private WeakReference<AbsStartPageActivity> activity;
    private Context context;

    private String promotion;

    public TempleteGetTask(AbsStartPageActivity activity) {
        this.context = activity.getApplicationContext();
        this.activity = new WeakReference<AbsStartPageActivity>(activity);
    }

    @Override
    protected Boolean doInBackground(Integer... params) {
        final String mpUrl = MP_URL;
        final String url;
        if (params.length == 0) {
            Debug.logd("length0");
            url = mpUrl;
        } else {
            url = mpUrl + PARAM_COUNT + params[0];
        }

        HttpGet request = new HttpGet(url);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpParams httpParams = httpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, DEFAULT_TIMEOUT);
        HttpConnectionParams.setSoTimeout(httpParams, DEFAULT_TIMEOUT);

        String ret = "";
        try {
            ret = httpClient.execute(request, new ResponseHandler<String>() {

                @Override
                public String handleResponse(HttpResponse response)
                        throws IOException {
                    Debug.logd("posttest", "responsecode："
                            + response.getStatusLine().getStatusCode());

                    switch (response.getStatusLine().getStatusCode()) {
                    case HttpStatus.SC_OK:
                        // レスポンスデータをエンコード済みの文字列として取得する
                        return EntityUtils.toString(response.getEntity(),
                                "UTF-8");
                    case HttpStatus.SC_NOT_FOUND:
                        return null;
                    default:
                        return null;
                    }
                }
            });
        } catch (ConnectTimeoutException e) {
            Debug.logd(e.toString());
            return false;
        } catch (UnknownHostException e) {
            Debug.logd(e.toString());
            return false;
        } catch (SocketTimeoutException e) {
            Debug.logd(e.toString());
            return false;
        } catch (IOException e) {
            Debug.logd(e.toString());
            return false;
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
        if (ret == null) {
            return false;
        }

        Debug.logd(ret);

        Template[] templates;
        final long time = System.currentTimeMillis();
        try {
            JSONObject object = new JSONObject(ret);
            JSONArray jsonTemplates = object.getJSONArray(PARAM_TEMPLATE);
            final int length = jsonTemplates.length();
            templates = new Template[length];
            for (int i = 0; i < length; i++) {
                JSONObject jsonTemplate = jsonTemplates.getJSONObject(i);
                Template template = Template.createFromJson(jsonTemplate, i,
                        time);
                templates[i] = template;
            }
            JSONObject promotionObject = object.getJSONObject(PARAM_PROMOTION);
            promotion = promotionObject.getString(PARAM_ANDROID);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        WordPress.wpDB.saveMPInfo(templates);
        savePromotion(promotion, time);
        publishProgress(PROGRESS_TEMPLATE);

        return true;
    }

    private void savePromotion(String promotion, long time) {
        final Resources r = context.getResources();
        final String prefName = r.getString(R.string.mp_pref_name);
        SharedPreferences.Editor editor = context.getSharedPreferences(
                prefName, Context.MODE_PRIVATE).edit();
        final String promotionKey = r.getString(R.string.pref_promotion);
        editor.putString(promotionKey, promotion);
        final String timeKey = r.getString(R.string.pref_last_get_time);
        editor.putLong(timeKey, time);
        editor.commit();
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        if (values == null || values.length == 0) {
            return;
        }
        int target = values[0];
        if (target == PROGRESS_TEMPLATE) {
            AbsStartPageActivity activity = this.activity.get();
            if (activity != null) {
                activity.setPromotionText(promotion);
                activity.invalidateMpItem();
            }
        }
    }
}
