package org.wordpress.android.ui.posts;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.justsystems.hpb.pad.R;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.HierarchicalTerm;
import org.wordpress.android.models.PostType;
import org.wordpress.android.models.Taxonomy;

public class AddTermActivity extends Activity implements OnClickListener,
        OnItemSelectedListener {
    private int id;
    private String typeName;

    private Spinner taxonomySpinner;
    private Taxonomy[] taxonomies;
    private Spinner parentSpinner;
    private HierarchicalTerm[] terms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.add_category);

        TextView tv = (TextView) findViewById(R.id.taxonomyLabel);
        tv.setVisibility(View.VISIBLE);

        this.taxonomySpinner = (Spinner) findViewById(R.id.taxonomySpinner);
        this.taxonomySpinner.setVisibility(View.VISIBLE);
        this.taxonomySpinner.setOnItemSelectedListener(this);

        this.parentSpinner = (Spinner) findViewById(R.id.parent_category);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            id = extras.getInt("id");
            this.typeName = extras.getString("type_name");
        }

        loadTaxonomies();
        this.taxonomySpinner.setSelection(0);

        final Button okButton = (Button) findViewById(R.id.ok);
        okButton.setOnClickListener(this);
        final Button cancelButton = (Button) findViewById(R.id.cancel);
        cancelButton.setOnClickListener(this);

    }

    private void loadTaxonomies() {
        PostType type = new PostType(this, id, "", typeName);
        this.taxonomies = type.getTaxonomies();
        if (taxonomies.length > 0) {

            ArrayList<CharSequence> taxonomyList = new ArrayList<CharSequence>();
            for (Taxonomy taxonomy : taxonomies) {
                taxonomyList.add(taxonomy.getLabel());
            }

            ArrayAdapter<CharSequence> taxonomyAdapter = new ArrayAdapter<CharSequence>(
                    this, android.R.layout.simple_dropdown_item_1line,
                    taxonomyList);
            taxonomyAdapter
                    .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            this.taxonomySpinner.setAdapter(taxonomyAdapter);

        }

    }

    private void loadParent(String taxonomyName) {
        this.terms = WordPress.wpDB.getHierarchialTerm(id, taxonomyName);
        if (terms.length > 0) {
            ArrayList<CharSequence> loadTextArray = new ArrayList<CharSequence>();

            loadTextArray.add(getResources().getText(R.string.none));

            for (int i = 0; i < terms.length; i++) {
                loadTextArray.add(terms[i].getTerm().getName());
            }

            ArrayAdapter<CharSequence> categories = new ArrayAdapter<CharSequence>(
                    this, android.R.layout.simple_dropdown_item_1line,
                    loadTextArray);
            categories
                    .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            Spinner sCategories = (Spinner) findViewById(R.id.parent_category);

            sCategories.setAdapter(categories);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position,
            long id) {
        Taxonomy taxonomy = taxonomies[position];
        if (!taxonomy.isHierarchical()) {
            this.parentSpinner.setEnabled(false);
        } else if (!this.parentSpinner.isEnabled()) {
            this.parentSpinner.setEnabled(true);
        }
        final String taxonomyName = taxonomies[position].getName();
        loadParent(taxonomyName);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.ok) {
            finishSuccessfully();
        } else if (v.getId() == R.id.cancel) {
            cancel();
        }
    }

    private void finishSuccessfully() {
        EditText categoryNameET = (EditText) findViewById(R.id.category_name);
        String term_name = categoryNameET.getText().toString();

        int taxonomySelectedPositnion = this.taxonomySpinner
                .getSelectedItemPosition();
        String taxonomy_name = "";
        if (taxonomySelectedPositnion != Spinner.INVALID_POSITION) {
            taxonomy_name = this.taxonomies[taxonomySelectedPositnion]
                    .getName();
        }

        EditText categorySlugET = (EditText) findViewById(R.id.category_slug);
        String term_slug = categorySlugET.getText().toString();
        EditText categoryDescET = (EditText) findViewById(R.id.category_desc);
        String term_desc = categoryDescET.getText().toString();

        int parentSelectedPositnion = this.parentSpinner
                .getSelectedItemPosition();
        int parent_id = 0;
        if (parentSelectedPositnion > 0) {
            HierarchicalTerm term = this.terms[parentSelectedPositnion - 1];
            parent_id = Integer.parseInt(term.getTerm().getTermId());
        }

        if (term_name.replaceAll(" ", "").equals("")) {
            showWarnDialog();
        } else {
            Bundle bundle = new Bundle();

            bundle.putString("term_name", term_name);
            bundle.putString("term_taxonomy", taxonomy_name);
            bundle.putString("term_slug", term_slug);
            bundle.putString("term_desc", term_desc);
            bundle.putInt("parent_id", parent_id);
            bundle.putString("continue", "TRUE");
            Intent mIntent = new Intent();
            mIntent.putExtras(bundle);
            setResult(RESULT_OK, mIntent);
            finish();
        }
    }

    private void showWarnDialog() {
        // Name field cannot be empty

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                AddTermActivity.this);
        dialogBuilder.setTitle(getResources().getText(R.string.required_field));
        dialogBuilder.setMessage(getResources().getText(
                R.string.cat_name_required));
        dialogBuilder.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Just close the window.

                    }
                });
        dialogBuilder.setCancelable(true);
        dialogBuilder.create().show();
    }

    private void cancel() {
        Bundle bundle = new Bundle();

        bundle.putString("continue", "FALSE");
        Intent mIntent = new Intent();
        mIntent.putExtras(bundle);
        setResult(RESULT_OK, mIntent);
        finish();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

}
