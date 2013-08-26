package com.justsystems.hpb.pad.seo;

import android.os.Bundle;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.justsystems.hpb.pad.R;

public class SeoResultActivity extends SherlockActivity {

    public static final boolean SHOW_SEO = false;

    private GraphView graph;

    private TextView pageSummary;
    private TextView titleSummary;
    private TextView metaDescSummary;
    private TextView metaKwdSummary;

    private TextView pageDesc;
    private TextView titleDesc;
    private TextView metaDescDesc;
    private TextView metaKwdDesc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.seo_main);
        setTitle("SEOチェック");

        this.graph = (GraphView) findViewById(R.id.keyword_graph);
        this.graph.setCount(5);

        this.graph.setTitle(new String[] { "デザイン", "サイト", "グラフィック", "web",
                "メルマガ" });
        this.graph.setValue(new float[] { 40.5f, 30.5f, 20.5f, 5.5f, 3.5f });

        this.pageSummary = (TextView) findViewById(R.id.page_summary);
        this.pageSummary.setText(getString(R.string.page_text_count, 0));
        this.titleSummary = (TextView) findViewById(R.id.title_summary);
        this.titleSummary.setText(getString(R.string.title_text_count, 0));
        this.metaDescSummary = (TextView) findViewById(R.id.description_summary);
        this.metaDescSummary
                .setText(getString(R.string.meta_desc_text_count, 0));
        this.metaKwdSummary = (TextView) findViewById(R.id.meta_keyword_summary);
        this.metaKwdSummary.setText(getString(R.string.meta_kwd_text_count, 0));

        this.pageDesc = (TextView) findViewById(R.id.page_description);
        this.titleDesc = (TextView) findViewById(R.id.title_description);
        this.metaDescDesc = (TextView) findViewById(R.id.description_description);
        this.metaKwdDesc = (TextView) findViewById(R.id.meta_keyword_description);
    }
}
