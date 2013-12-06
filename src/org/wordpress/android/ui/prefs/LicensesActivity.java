package org.wordpress.android.ui.prefs;

import android.os.Bundle;

import com.justsystems.hpb.pad.R;

import org.wordpress.android.ui.WebViewActivity;
import org.wordpress.android.util.DeviceUtils;

/**
 * Display open source licenses for the application.
 */
public class LicensesActivity extends WebViewActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getResources().getText(R.string.open_source_licenses));
        if (DeviceUtils.isBlackBerry()) {
            loadUrl("file:///android_asset/licenses_bb.html");
        } else {
            loadUrl("file:///android_asset/licenses.html");
        }
    }

}
