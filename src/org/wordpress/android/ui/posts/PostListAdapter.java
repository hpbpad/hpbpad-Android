package org.wordpress.android.ui.posts;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.justsystems.hpb.pad.R;

public class PostListAdapter extends BaseAdapter {

    private Context context;

    private final ArrayList<Holder> items = new ArrayList<Holder>();

    public PostListAdapter(Context context) {
        this.context = context;
    }

    public void clear() {
        this.items.clear();
    }

    public int getCount() {
        return items.size();
    }

    public Holder getItem(int position) {
        return items.get(position);
    }

    public long getItemId(int position) {
        return getItem(position).mPostID;
    }

    public void add(int mPostID, String mTitle, String mDateCreatedFormatted,
            String mStatus) {
        Holder holder = new Holder(mPostID, mTitle, mDateCreatedFormatted,
                mStatus);
        items.add(holder);
    }

    public void addDraft(int mPostID, String mTitle,
            String mDateCreatedFormatted, String mStatus) {
        Holder holder = new Holder(mPostID, mTitle, mDateCreatedFormatted,
                mStatus);
        items.add(0, holder);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View pv = convertView;
        ViewWrapper wrapper = null;
        if (pv == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            pv = inflater.inflate(R.layout.row_post_page, parent, false);
            wrapper = new ViewWrapper(pv);
            pv.setTag(wrapper);
        } else {
            wrapper = (ViewWrapper) pv.getTag();
        }

        final Holder holder = getItem(position);

        String date = holder.mDateCreatedFormatted;
        String status_text = holder.mStatus;

        pv.setTag(R.id.row_post_id, holder.mPostID);
        pv.setId(Integer.valueOf(holder.mPostID));
        String titleText = holder.mTitle;
        if (titleText == "")
            titleText = "(" + context.getResources().getText(R.string.untitled)
                    + ")";
        wrapper.title.setText(titleText);
        wrapper.date.setText(date);
        wrapper.status.setText(status_text);

        return pv;
    }

    private static class Holder {
        private int mPostID;
        private String mTitle;
        private String mDateCreatedFormatted;
        private String mStatus;

        private Holder(int mPostID, String mTitle,
                String mDateCreatedFormatted, String mStatus) {
            this.mPostID = mPostID;
            this.mTitle = mTitle;
            this.mDateCreatedFormatted = mDateCreatedFormatted;
            this.mStatus = mStatus;
        }
    }

    private static class ViewWrapper {
        private TextView title = null;
        private TextView date = null;
        private TextView status = null;

        private ViewWrapper(View base) {
            this.title = (TextView) base.findViewById(R.id.title);
            this.date = (TextView) base.findViewById(R.id.date);
            this.status = (TextView) base.findViewById(R.id.status);
        }
    }
}
