package com.justsystems.hpb.pad.seo;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import com.justsystems.hpb.pad.util.Debug;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import org.wordpress.android.task.MultiAsyncTask;

final class SeoCheckTask extends MultiAsyncTask<Void, Integer, Responce> {

    // デフォルト値：タイムアウト（ミリ秒）
    private static final int DEFAULT_TIMEOUT = 10000;

    private static final String SEO_URL = "https://api.masteraxis.com/app/util/hpb_mobile_wp/";

    private SeoResultActivity activity;

    private final String title;
    private final String contents;
    private final String h1;
    private final String metadescription;
    private final String metakeyword;

    private int errorCode;

    public SeoCheckTask(SeoResultActivity activity, String title,
            String contents, String h1, String metadescription,
            String metaKeyword) {
        this.activity = activity;
        this.title = title;
        this.contents = contents;
        this.h1 = h1;
        this.metadescription = metadescription;
        this.metakeyword = metaKeyword;
    }

    @Override
    protected Responce doInBackground(Void... params) {

        HttpPost request = new HttpPost(SEO_URL);

        ArrayList<NameValuePair> pairs = new ArrayList<NameValuePair>();
        pairs.add(new BasicNameValuePair(Responce.PARAM_TITLE, this.title));
        pairs.add(new BasicNameValuePair(Responce.PARAM_CONTENTS, this.contents));
        if (this.h1 != null) {
            pairs.add(new BasicNameValuePair(Responce.PARAM_H1, this.h1));
        }
        if (this.metadescription != null) {
            pairs.add(new BasicNameValuePair(Responce.PARAM_METADESCRIPTION,
                    this.metadescription));
        }
        if (this.metakeyword != null) {
            pairs.add(new BasicNameValuePair(Responce.PARAM_METAKEYWORD,
                    this.metakeyword));
        }

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpParams httpParams = httpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, DEFAULT_TIMEOUT);
        HttpConnectionParams.setSoTimeout(httpParams, DEFAULT_TIMEOUT);

        String ret = "";
        try {
            request.setEntity(new UrlEncodedFormEntity(pairs, "utf-8"));
            ret = httpClient.execute(request, new ResponseHandler<String>() {

                @Override
                public String handleResponse(HttpResponse response)
                        throws IOException {
                    int responseStatus = response.getStatusLine()
                            .getStatusCode();
                    Debug.logd("posttest", "レスポンスコード：" + responseStatus);

                    switch (responseStatus) {
                    case HttpStatus.SC_OK:
                        // レスポンスデータをエンコード済みの文字列として取得する
                        return EntityUtils.toString(response.getEntity(),
                                "UTF-8");
                    default:
                        errorCode = responseStatus;
                        Debug.logd("posttest", "通信エラー");
                        return null;
                    }
                }
            });
        } catch (ConnectTimeoutException e) {
            Debug.logd(e.toString());
            return null;
        } catch (UnknownHostException e) {
            Debug.logd(e.toString());
            return null;
        } catch (SocketTimeoutException e) {
            Debug.logd(e.toString());
            return null;
        } catch (IOException e) {
            Debug.logd(e.toString());
            return null;
        } finally {
            httpClient.getConnectionManager().shutdown();
        }

        if (ret == null) {
            return null;
        }
        Debug.logd(ret);

        Responce res = Responce.createFromJson(ret);
        return res;
    }

    @Override
    protected void onPostExecute(Responce result) {
        super.onPostExecute(result);

        if (activity.isFinishing()) {
            return;
        }

        if (result == null) {
            this.activity.ShowErrorDialog(errorCode);
        } else {
            this.activity.setResponce(result);
        }
    }
}
