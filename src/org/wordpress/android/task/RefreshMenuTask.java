package org.wordpress.android.task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.justsystems.hpb.pad.AbsStartPageActivity;
import com.justsystems.hpb.pad.util.Debug;

import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.PostType;
import org.wordpress.android.ui.WPActionBarActivity;

public final class RefreshMenuTask extends
        MultiAsyncTask<Integer, Integer, Void> {
    private final WPActionBarActivity activity;

    private static final int PROGRESS_UPDATE_LIST = 1;
    private static final int PROGRESS_UPDATE_IMAGE = 2;

    private static final boolean IS_LOAD_ICON = false;

    private static boolean isUnderTask = false;

    public RefreshMenuTask(WPActionBarActivity activity) {
        this.activity = activity;
        isUnderTask = true;
    }

    @Override
    protected Void doInBackground(Integer... params) {
        if (params.length <= 0) {
            return null;
        }

        final Map<String, String> fields = new HashMap<String, String>();
        fields.put("labels", "labels");
        fields.put("cap", "cap");
        fields.put("menu", "menu");
        fields.put("taxonomies", "taxonomies");

        final Map<String, Object> filter = new HashMap<String, Object>();
        filter.put("_builtin", false);
        filter.put("public", true);
        filter.put("show_ui", true);

        int id = params[0];
        List<Object> blogVals = WordPress.wpDB.loadSettings(id);
        final String url = blogVals.get(0).toString();
        final String httpUser = blogVals.get(4).toString();
        final String httpPassword = blogVals.get(5).toString();

        final int blogId = (Integer) blogVals.get(12);
        final String userName = blogVals.get(2).toString();
        final String password = blogVals.get(3).toString();

        XMLRPCClient client = new XMLRPCClient(url, httpUser, httpPassword);

        Object[] vParams = { blogId, userName, password, filter, fields };

        final Object versionResult;
        try {
            versionResult = client.call("wp.getPostTypes", vParams);
        } catch (XMLRPCException e) {
            Log.v("error xmlrcp", e.getMessage());
            return null;
        }

        if (versionResult == null || !(versionResult instanceof HashMap<?, ?>)) {
            Debug.logd(this.getClass().toString(), "class cast "
                    + versionResult.getClass().toString() + " cast to hashmap");

            //delete old post type
            WordPress.wpDB.savePostTypes(new HashMap<String, String>(), id);
            publishProgress(PROGRESS_UPDATE_LIST);
            return null;
        }

        HashMap<?, ?> map = (HashMap<?, ?>) versionResult;

        WordPress.wpDB.savePostTypes(map, id);
        saveTaxonomy(client, blogId, id, userName, password);

        publishProgress(PROGRESS_UPDATE_LIST);

        ArrayList<PostType> items = new ArrayList<PostType>();
        String[] names = WordPress.wpDB.getPostTypes(id);
        for (String name : names) {
            PostType type = new PostType(id, name);
            items.add(type);
        }

        if (IS_LOAD_ICON) {
            for (int i = 0; i < items.size(); i++) {
                PostType type = items.get(i);
                saveIcon(type);
            }
        }

        return null;
    }

    private void saveIcon(PostType type) {
        final String urlString = type.getMenuIcon();
        if (urlString == null) {
            return;
        }
        final Bitmap bitmap;
        try {
            URL iconUrl = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) iconUrl
                    .openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            bitmap = BitmapFactory.decodeStream(input);
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        try {
            // ローカルファイルへ保存
            File dir = new File(activity.getFilesDir().getAbsolutePath()
                    + File.separator + type.getBlogID());
            if (!dir.exists()) {
                dir.mkdir();
            }

            final FileOutputStream out = new FileOutputStream(
                    dir.getAbsolutePath() + File.separator + type.getName()
                            + ".png");
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        final Drawable d = new BitmapDrawable(this.activity.getResources(),
                bitmap);
        type.setIcon(d);

        publishProgress(PROGRESS_UPDATE_IMAGE);
    }

    public static void saveTaxonomy(XMLRPCClient client, int blogId,
            int blogUniqueId, String userName, String password) {

        final Map<String, String> fields = new HashMap<String, String>();
        fields.put("labels", "labels");
        fields.put("cap", "cap");
        fields.put("object_type", "object_type");

        final Map<String, Object> filter = new HashMap<String, Object>();
        // filter.put("_builtin", false);

        Object[] vParams = { blogId, userName, password, filter, fields };

        final Object versionResult;
        try {
            versionResult = client.call("wp.getTaxonomies", vParams);
        } catch (XMLRPCException e) {
            Log.v("error xmlrcp", e.getMessage());
            return;
        }

        if (versionResult == null) {
            return;
        }
        if (!(versionResult instanceof Object[])) {
            Debug.logd("Refresh Menu Task", "class cast "
                    + versionResult.getClass().toString() + " cast to hashmap");
            return;
        }

        Object[] maps = (Object[]) versionResult;
        Log.v("count", maps.length + "");
        if (maps.length > 0) {
            WordPress.wpDB.saveTaxonomies(maps, blogUniqueId);
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        if (values.length == 0) {
            return;
        }
        final int type = values[0];

        switch (type) {
        case PROGRESS_UPDATE_LIST:
            this.activity.updateMenuDrawer();
            if (this.activity instanceof AbsStartPageActivity) {
                ((AbsStartPageActivity) activity).onTypeUpdated();
            }
            break;
        case PROGRESS_UPDATE_IMAGE:
            this.activity.invalidateList();
            break;
        default:
            break;
        }

    }

    @Override
    protected void onPostExecute(Void v) {
        isUnderTask = false;
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        isUnderTask = false;
    }

    public static boolean isUnedrTask() {
        return isUnderTask;
    }
}
