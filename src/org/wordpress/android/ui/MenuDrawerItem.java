/**
 * Represents a single item in the WPActionBarActivity's menu drawer. A MenuDrawerItem determines
 * the label and icon to use in the menu, its presence in the menu, its selection state, and the
 * action that happens when the item is selected.
 */
package org.wordpress.android.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

public abstract class MenuDrawerItem {

    /**
     * Signifies that the item has no unique ID so should not be tracked in the
     * last selected item preference.
     */
    public static int NO_ITEM_ID = -1;

    /**
     * Called when the menu item is selected.
     */
    abstract public void onSelectItem();

    /**
     * Determines if the menu item should be displayed in the menu. Default is
     * always true.
     */
    public Boolean isVisible() {
        return true;
    };

    /**
     * Determines if the item is selected. Default is always false.
     */
    public Boolean isSelected() {
        return false;
    }

    /**
     * Method to allow the menu item to provide additional configuration to the
     * view, default implementation does nothing.
     */
    public void onConfigureView(View view) {
    };

    // Resource id for the title string
    protected String mTitle;
    // Resource id for the icon drawable
    protected Drawable mIconRes;
    // ID for the item for remembering which item was selected
    private int mItemId;

    private final String postType;

    /**
     * Creates a MenuDrawerItem with the specific id, string resource id and
     * drawable resource id
     */
    MenuDrawerItem(Context context, int itemId, String title, int iconRes,
            String postType) {
        mTitle = title;
        mIconRes = context.getResources().getDrawable(iconRes);
        mItemId = itemId;
        this.postType = postType;
    }

    /**
     * Creates a MenuDrawerItem with the specific id, string resource id and
     * drawable resource id
     */
    MenuDrawerItem(Context context, int itemId, int stringRes, int iconRes) {
        this(context, itemId, context.getString(stringRes), iconRes, null);
    }

    /**
     * Creates a MenuDrawerItem with NO_ITEM_ID for it's id for items that
     * shouldn't be remembered between application launches.
     */
    MenuDrawerItem(Context context, int stringRes, int iconRes) {
        this(context, NO_ITEM_ID, stringRes, iconRes);
    }

    /**
     * Determines if the item has an id for remembering the last selected item
     */
    public boolean hasItemId() {
        return getItemId() != NO_ITEM_ID;
    }

    /**
     * Get's the item's unique ID
     */
    public int getItemId() {
        return mItemId;
    }

    /**
     * Returns the item's string representation (used by ArrayAdapter.getView)
     */
    public String toString() {
        return "";
    }

    /**
     * The resource id to use for the menu item's title
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * The resource id to use for the menu item's icon
     */
    public Drawable getIcon() {
        return mIconRes;
    }

    public void selectItem() {
        onSelectItem();
    }

    /**
     * Allows the menu item to do additional manipulation to the view
     */
    public void configureView(View v) {
        onConfigureView(v);
    }

    public String getPostType() {
        return postType;
    }

}
