package com.justsystems.hpb.pad.seo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.justsystems.hpb.pad.R;

import org.apache.http.HttpStatus;

import org.wordpress.android.util.StringUtils;

public class SeoResultActivity extends SherlockActivity {

    private static final int ID_DIALOG_PROGRESS = -1;

    private GraphView kwdGraph;

    private String title;
    private String contents;
    private String h1;
    private String metaDesc;
    private String metaKwd;

    private TextView titleSummary;
    private TextView kwdTitle;
    private TextView contentsSummary;
    private TextView h1Summary;
    private TextView metaDescSummary;
    private TextView metaKwdSummary;

    private TextView titlePrev;
    private TextView contentsPrev;

    private TextView titleDesc;
    private TextView kwdDesc;
    private TextView contentsDesc;
    private TextView h1Desc;
    private TextView metaDescDesc;
    private TextView metaKwdDesc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent != null) {
            this.title = intent.getStringExtra("title");
            this.contents = intent.getStringExtra("contents");
            if (this.contents != null) {
                this.contents = StringUtils.unescapeHTML(contents);
            }
            this.h1 = intent.getStringExtra("h1");
            this.metaDesc = intent.getStringExtra("metadescription");
            this.metaKwd = intent.getStringExtra("metakeyword");
        }

        setContentView(R.layout.seo_main);

        this.titleSummary = (TextView) findViewById(R.id.title_summary);
        this.titleSummary.setText(getString(R.string.title_text_count));
        this.titlePrev = (TextView) findViewById(R.id.title_preview);
        this.titlePrev.setText(this.title);
        this.titleDesc = (TextView) findViewById(R.id.title_description);

        this.kwdTitle = (TextView) findViewById(R.id.keyword_title);
        this.kwdGraph = (GraphView) findViewById(R.id.keyword_graph);
        this.kwdDesc = (TextView) findViewById(R.id.keyword_description);

        this.contentsSummary = (TextView) findViewById(R.id.contents_summary);
        this.contentsSummary.setText(getString(R.string.contents_text_count));
        this.contentsPrev = (TextView) findViewById(R.id.contents_preview);
        this.contentsPrev.setText(this.contents);
        this.contentsDesc = (TextView) findViewById(R.id.contents_description);

        this.h1Summary = (TextView) findViewById(R.id.h1_summary);
        this.h1Desc = (TextView) findViewById(R.id.h1_description);
        if (this.h1 == null) {
            this.h1Summary.setVisibility(View.GONE);
            this.h1Desc.setVisibility(View.GONE);
        } else {
            this.h1Summary.setText(getString(R.string.h1_text_count));
        }

        this.metaDescSummary = (TextView) findViewById(R.id.description_summary);
        this.metaDescDesc = (TextView) findViewById(R.id.description_description);
        if (this.metaDesc == null) {
            this.metaDescSummary.setVisibility(View.GONE);
            this.metaDescDesc.setVisibility(View.GONE);
        } else {
            this.metaDescSummary
                    .setText(getString(R.string.meta_desc_text_count));
        }

        this.metaKwdSummary = (TextView) findViewById(R.id.meta_keyword_summary);
        this.metaKwdDesc = (TextView) findViewById(R.id.meta_keyword_description);
        if (this.metaKwd == null) {
            this.metaKwdSummary.setVisibility(View.GONE);
            this.metaKwdDesc.setVisibility(View.GONE);
        } else {
            this.metaKwdSummary
                    .setText(getString(R.string.meta_kwd_text_count));
        }

        new SeoCheckTask(this, title, contents, h1, metaDesc, metaKwd)
                .executeOnMultiThread();

        showDialog(ID_DIALOG_PROGRESS);
    }

    void ShowErrorDialog(int errorCode) {
        dismissDialog(ID_DIALOG_PROGRESS);
        showDialog(errorCode);
    }

    void setResponce(Responce response) {
        dismissDialog(ID_DIALOG_PROGRESS);

        String title = response.getTitle().getMessage();
        titleDesc.setText(title);

        String contents = response.getContents().getMessage();
        contentsDesc.setText(contents);

        Result h1Result = response.getH1();
        if (h1Result == null) {
            this.h1Summary.setVisibility(View.GONE);
            this.h1Desc.setVisibility(View.GONE);
        } else {
            String h1 = response.getH1().getMessage();
            this.h1Desc.setText(h1);
        }

        Result metaDescResult = response.getMetadescription();
        if (metaDescResult == null) {
            this.metaDescSummary.setVisibility(View.GONE);
            this.metaDescDesc.setVisibility(View.GONE);
        } else {
            this.metaDescSummary.setText(getString(
                    R.string.meta_desc_text_count, this.metaDesc));
            String metadescription = metaDescResult.getMessage();
            this.metaDescDesc.setText(metadescription);
        }

        Result metaKwdResult = response.getMetaKeyword();
        if (metaKwdResult == null) {
            this.metaKwdSummary.setVisibility(View.GONE);
            this.metaKwdDesc.setVisibility(View.GONE);
        } else {
            String metakeyword = metaKwdResult.getMessage();
            metaKwdDesc.setText(metakeyword);
        }

        KeywordResult[] kwResult = response.getKeywordBalances();
        final int length = kwResult == null ? 0 : kwResult.length;
        if (length == 0) {
            this.kwdTitle.setVisibility(View.GONE);
            this.kwdGraph.setVisibility(View.GONE);
            this.kwdDesc.setVisibility(View.GONE);
        } else {
            this.kwdGraph.setValues(kwResult);
            this.kwdDesc.setText(getString(R.string.kwd_desc));
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id < 0) {
            switch (id) {
            case ID_DIALOG_PROGRESS:
                ProgressDialog dialog = new ProgressDialog(this);
                dialog.setTitle(R.string.seo_check);
                String message = getString(R.string.loading);
                dialog.setMessage(message);
                dialog.setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        SeoResultActivity.this.finish();
                    }
                });
                return dialog;
            }
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(false);
            builder.setTitle(R.string.error_connection);
            switch (id) {
            case HttpStatus.SC_BAD_REQUEST:
                builder.setMessage(R.string.seo_error_message_400);
                break;
            case HttpStatus.SC_FORBIDDEN:
                builder.setMessage(R.string.seo_error_message_403);
                break;
            case HttpStatus.SC_NOT_FOUND:
                builder.setMessage(R.string.seo_error_message_404);
                break;
            case HttpStatus.SC_INTERNAL_SERVER_ERROR:
            case HttpStatus.SC_SERVICE_UNAVAILABLE:
                builder.setMessage(R.string.seo_error_message_500);
                break;
            default:
                break;
            }
            builder.setPositiveButton(R.string.ok, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        dialog.dismiss();
                        finish();
                        break;
                    }
                }
            });
            return builder.create();
        }
        return null;
    }
}
