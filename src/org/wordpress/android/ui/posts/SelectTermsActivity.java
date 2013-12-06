package org.wordpress.android.ui.posts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckedTextView;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.justsystems.hpb.pad.R;
import com.justsystems.hpb.pad.util.Debug;

import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.HierarchicalTerm;
import org.wordpress.android.models.PostType;
import org.wordpress.android.models.Taxonomy;
import org.wordpress.android.models.Term;
import org.wordpress.android.task.MultiAsyncTask;
import org.wordpress.android.task.RefreshMenuTask;

public class SelectTermsActivity extends SherlockListActivity implements
        OnChildClickListener {

    private ExpandableListView listView;
    private ListAdapter adapter;

    private String typeName;
    private ProgressDialog pd;

    private Blog blog;
    private int blogId;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        this.typeName = getIntent().getStringExtra("type_name");

        setContentView(R.layout.select_terms);
        setTitle(getResources().getString(R.string.select_categories));

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            blogId = extras.getInt("id");
            try {
                blog = new Blog(blogId);
            } catch (Exception e) {
                Toast.makeText(this,
                        getResources().getText(R.string.blog_not_found),
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }

        this.listView = (ExpandableListView) findViewById(android.R.id.list);
        this.adapter = new ListAdapter(this, typeName);
        this.listView.setAdapter(adapter);
        this.listView.setGroupIndicator(null);
        this.listView.setChildIndicator(null);
        // this.listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        this.listView.setItemsCanFocus(false);
        this.listView.setOnChildClickListener(this);

        Parcelable[] parcelables = getIntent().getParcelableArrayExtra("term");
        if (parcelables != null && parcelables.length > 0) {
            Term[] terms = new Term[parcelables.length];
            for (int i = 0; i < parcelables.length; i++) {
                Term term = (Term) parcelables[i];
                terms[i] = term;
            }
            loadSelectedCategories(terms);
        }

        if (this.adapter.shouldReload) {
            refreshCategories(true);
            return;
        }

        expandGroup();
    }

    private void expandGroup() {
        for (int i = 0; i < adapter.getGroupCount(); i++) {
            listView.expandGroup(i);
        }
    }

    private void loadSelectedCategories(Term[] terms) {
        final int groupCount = this.adapter.getGroupCount();
        if (groupCount > 0) {
            for (Term term : terms) {
                for (int i = 0; i < groupCount; i++) {
                    final int childCount = this.adapter.getChildrenCount(i);
                    for (int j = 0; j < childCount; j++) {
                        final Term target = ((HierarchicalTerm) this.adapter
                                .getChild(i, j)).getTerm();
                        if (target.getTermId().equals(term.getTermId())) {
                            this.adapter.setCheckState(i, j, true);
                            break;
                        }
                    }
                }
            }
        } else {
            refreshCategories(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.categories, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_refresh) {
            refreshCategories(false);
            return true;
        } else if (itemId == R.id.menu_new_category) {
            Bundle bundle = new Bundle();
            bundle.putInt("id", blogId);
            bundle.putString("type_name", typeName);
            Intent i = new Intent(SelectTermsActivity.this,
                    AddTermActivity.class);
            i.putExtras(bundle);
            startActivityForResult(i, 0);
            return true;
        } else if (itemId == android.R.id.home) {
            saveAndFinish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v,
            int groupPosition, int childPosition, long id) {
        adapter.toggleCheckState(groupPosition, childPosition);

        return true;
    }

    private void refreshCategories(boolean shouldReloadTaxonomy) {
        pd = ProgressDialog.show(SelectTermsActivity.this, getResources()
                .getText(R.string.refreshing_categories), getResources()
                .getText(R.string.attempting_categories_refresh), true, true);
        RefreshCategoryTask task = new RefreshCategoryTask();
        task.executeOnMultiThread(shouldReloadTaxonomy);
    }

    @Override
    public void onBackPressed() {
        saveAndFinish();
        super.onBackPressed();
    }

    /**
     * function addCategory
     * 
     * @param String
     *            category_name
     * @return
     * @description Adds a new category
     */
    public boolean addCategory(String term_name, String term_taxonomy,
            String term_slug, String term_desc, int parent_id) {
        // Return string

        // Store the parameters for wp.addCategory
        Map<String, Object> struct = new HashMap<String, Object>();
        struct.put("name", term_name);
        struct.put("taxonomy", term_taxonomy);
        if (term_slug != null && term_slug.length() > 0) {
            struct.put("slug", term_slug);
        }
        if (term_desc != null && term_desc.length() > 0) {
            struct.put("description", term_desc);
        }
        if (parent_id != 0) {
            struct.put("parent", parent_id);
        }

        XMLRPCClient client = new XMLRPCClient(blog.getUrl(),
                blog.getHttpuser(), blog.getHttppassword());

        Object[] params = { blog.getBlogId(), blog.getUsername(),
                blog.getPassword(), struct };

        Object result = null;
        try {
            result = client.call("wp.newTerm", params);
        } catch (XMLRPCException e) {
            e.printStackTrace();
        }

        if (result == null) {
            return false;
        }
        String term_id = result.toString();
        WordPress.wpDB.saveTerm(blogId, term_id, term_name, term_slug,
                term_taxonomy, term_desc, parent_id);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {

            final Bundle extras = data.getExtras();

            switch (requestCode) {
            case 0:

                // Add category

                // Does the user want to continue, or did he press "dismiss"?
                if (extras.getString("continue").equals("TRUE")) {
                    // Get name, slug and desc from Intent
                    final String term_name = extras.getString("term_name");
                    final String term_taxonomy = extras
                            .getString("term_taxonomy");
                    final String term_slug = extras.getString("term_slug");
                    final String term_desc = extras.getString("term_desc");
                    final int parent_id = extras.getInt("parent_id");
                    Debug.logd("term", "name:" + term_name + " taxonomy:"
                            + term_taxonomy + " slug:" + term_slug + " desc:"
                            + term_desc + " parent:" + parent_id);

                    if (this.adapter.contains(term_taxonomy, term_name)) {
                        // A category with the specified name already exists
                    } else {
                        // Add the category
                        pd = ProgressDialog.show(
                                SelectTermsActivity.this,
                                getResources().getText(
                                        R.string.cat_adding_category),
                                getResources().getText(
                                        R.string.cat_attempt_add_category),
                                true, true);
                        Thread th = new Thread() {
                            public void run() {
                                final boolean result = addCategory(term_name,
                                        term_taxonomy, term_slug, term_desc,
                                        parent_id);

                                SelectTermsActivity.this
                                        .runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (result) {
                                                    for (int i = 0; i < adapter
                                                            .getGroupCount(); i++) {
                                                        Taxonomy taxonomy = (Taxonomy) adapter
                                                                .getGroup(i);
                                                        if (taxonomy
                                                                .getName()
                                                                .equals(term_taxonomy)) {
                                                            adapter.loadTerm(i);
                                                            break;
                                                        }
                                                    }
                                                }
                                                pd.dismiss();
                                            }
                                        });
                            }
                        };
                        th.start();
                    }
                    break;
                }
            }// end null check
        }
    }

    private void saveAndFinish() {
        String selectedCategories = "";

        ArrayList<Term> result = new ArrayList<Term>();
        for (int groupPosition = 0; groupPosition < adapter.getGroupCount(); groupPosition++) {
            ArrayList<Boolean> check = adapter.checked.get(groupPosition);
            for (int childPosition = 0; childPosition < adapter
                    .getChildrenCount(groupPosition); childPosition++) {
                if (check.get(childPosition)) {
                    Term term = ((HierarchicalTerm) adapter.getChild(
                            groupPosition, childPosition)).getTerm();
                    result.add(term);
                }
            }
        }
        Term[] terms = result.toArray(new Term[0]);

        Bundle bundle = new Bundle();
        selectedCategories = selectedCategories.trim();
        if (selectedCategories.endsWith(",")) {
            selectedCategories = selectedCategories.substring(0,
                    selectedCategories.length() - 1);
        }

        bundle.putString("selectedCategories", selectedCategories);
        Intent mIntent = new Intent();
        mIntent.putExtras(bundle);
        mIntent.putExtra("term", terms);
        setResult(RESULT_OK, mIntent);
        finish();
    }

    private class RefreshCategoryTask extends
            MultiAsyncTask<Object, Integer, Object> {

        private static final int PROGRESS_TAXONOMY = -100;

        @Override
        protected Object doInBackground(Object... params) {
            final boolean shouldReloadTaxonomy = params != null ? (Boolean) params[0]
                    : false;

            XMLRPCClient client = new XMLRPCClient(blog.getUrl(),
                    blog.getHttpuser(), blog.getHttppassword());

            if (shouldReloadTaxonomy) {
                reloadTaxonomy(blog, client);
            }

            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("orderby", "name");
            Object[] parameters = { blog.getBlogId(), blog.getUsername(),
                    blog.getPassword(), null };

            for (int i = 0; i < adapter.getGroupCount(); i++) {
                Taxonomy taxonomy = (Taxonomy) adapter.getGroup(i);
                parameters[3] = taxonomy.getName();
                Object result;

                try {
                    result = client.call("wp.getTerms", parameters);
                } catch (XMLRPCException e) {
                    continue;
                }
                if (result instanceof Object[]) {
                    WordPress.wpDB.saveTerms(blogId, (Object[]) result);
                }
                publishProgress(i);
            }
            return null;
        }

        private void reloadTaxonomy(Blog blog, XMLRPCClient client) {
            final int id = blog.getId();
            final int blogId = blog.getBlogId();
            final String userName = blog.getUsername();
            final String password = blog.getPassword();

            RefreshMenuTask
                    .saveTaxonomy(client, blogId, id, userName, password);
            publishProgress(PROGRESS_TAXONOMY);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if (values == null || values.length == 0) {
                return;
            }
            int index = values[0];
            if (index == PROGRESS_TAXONOMY) {
                adapter.setTaxonomies(typeName);
            } else {
                adapter.loadTerm(index);
            }
        }

        @Override
        protected void onPostExecute(Object result) {
            super.onPostExecute(result);
            expandGroup();
            if (pd != null && pd.isShowing()) {
                pd.dismiss();
            }
        }
    }

    private static class ListAdapter extends BaseExpandableListAdapter {
        private SelectTermsActivity activity;
        private Taxonomy[] taxonomies;
        private final HashMap<Long, HierarchicalTerm[]> terms = new HashMap<Long, HierarchicalTerm[]>();
        private final ArrayList<ArrayList<Boolean>> checked = new ArrayList<ArrayList<Boolean>>();
        private boolean shouldReload = false;

        public ListAdapter(SelectTermsActivity activity, String typeName) {
            this.activity = activity;
            setTaxonomies(typeName);
        }

        public void setTaxonomies(String typeName) {
            checked.clear();
            PostType type = new PostType(activity, WordPress.getCurrentBlog()
                    .getId(), "", typeName);
            this.taxonomies = type.getTaxonomies();
            for (int i = 0; i < taxonomies.length; i++) {
                ArrayList<Boolean> checked = new ArrayList<Boolean>();
                this.checked.add(checked);
                if (!loadTerm(i)) {
                    shouldReload = true;
                }
            }
        }

        boolean contains(String taxonomyName, String termName) {
            for (int i = 0; i < this.taxonomies.length; i++) {
                Taxonomy taxonomy = taxonomies[i];
                final String label = taxonomy.getLabel();
                if (label.equals(taxonomyName)) {
                    final HierarchicalTerm[] terms = this.terms.get(taxonomy
                            .getId());
                    for (int j = 0; j < terms.length; j++) {
                        Term term = terms[j].getTerm();
                        final String targetTarmName = term.getName();
                        if (targetTarmName.equals(termName)) {
                            return true;
                        }
                    }
                    return false;
                }
            }
            return false;
        }

        void setCheckState(int groupPosition, int childPosition,
                boolean isChecked) {
            ArrayList<Boolean> childChecked = checked.get(groupPosition);
            boolean oldCheccked = childChecked.get(childPosition);
            if (oldCheccked == isChecked) {
                return;
            }
            childChecked.remove(childPosition);
            childChecked.add(childPosition, !oldCheccked);
            notifyDataSetInvalidated();
        }

        void toggleCheckState(int groupPosition, int childPosition) {
            ArrayList<Boolean> childChecked = checked.get(groupPosition);
            boolean oldCheccked = childChecked.get(childPosition);
            childChecked.remove(childPosition);
            childChecked.add(childPosition, !oldCheccked);
            notifyDataSetInvalidated();
        }

        boolean loadTerm(int groupIndex) {
            Taxonomy taxonomy = taxonomies[groupIndex];
            if (taxonomy.getLabel() == null) {
                return false;
            }

            int blogId = WordPress.getCurrentBlog().getId();

            this.terms.remove(taxonomy.getId());
            HierarchicalTerm[] hieralcialTerms = WordPress.wpDB
                    .getHierarchialTerm(blogId, taxonomy.getName());
            this.terms.put(taxonomy.getId(), hieralcialTerms);

            ArrayList<Boolean> childChecked = this.checked.get(groupIndex);
            for (int i = 0; i < hieralcialTerms.length; i++) {
                childChecked.add(false);
            }

            notifyDataSetChanged();
            return true;
        }

        @Override
        public int getGroupCount() {
            return taxonomies.length;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            final long id = taxonomies[groupPosition].getId();
            HierarchicalTerm[] term = terms.get(id);
            if (term == null) {
                return 0;
            } else {
                return terms.get(id).length;
            }
        }

        @Override
        public Object getGroup(int groupPosition) {
            return taxonomies[groupPosition];
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            final long id = taxonomies[groupPosition].getId();
            return terms.get(id)[childPosition];
        }

        @Override
        public long getGroupId(int groupPosition) {
            return taxonomies[groupPosition].getId();
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            final String id = ((HierarchicalTerm) getChild(groupPosition,
                    childPosition)).getTerm().getTermId();
            return Long.parseLong(id);
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded,
                View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(this.activity);
                convertView = inflater.inflate(R.layout.categories_title_row,
                        null);
            }
            TextView tv = (TextView) convertView;
            tv.setText(((Taxonomy) getGroup(groupPosition)).getLabel());

            return tv;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition,
                boolean isLastChild, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(this.activity);
            convertView = inflater.inflate(R.layout.categories_row, null);
            CheckedTextView textView = (CheckedTextView) convertView
                    .findViewById(R.id.categoryRowText);
            ImageView levelIndicatorView = (ImageView) convertView
                    .findViewById(R.id.categoryRowLevelIndicator);

            HierarchicalTerm term = (HierarchicalTerm) getChild(groupPosition,
                    childPosition);
            textView.setText(term.getTerm().getName());

            int level = term.getHierarchy() + 1;
            if (level == 1) { // hide ImageView
                levelIndicatorView.setVisibility(View.GONE);
            } else {
                ViewGroup.LayoutParams params = levelIndicatorView
                        .getLayoutParams();
                params.width = (params.width / 2) * level;
                levelIndicatorView.setLayoutParams(params);
            }

            boolean isChecked = checked.get(groupPosition).get(childPosition);
            textView.setChecked(isChecked);
            Debug.logd("c", "g" + groupPosition + " c" + childPosition + " che"
                    + isChecked);
            return convertView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

    }

}
