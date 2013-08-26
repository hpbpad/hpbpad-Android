package com.justsystems.hpb.pad;

import java.lang.ref.WeakReference;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

public class CustomFragmentStatePagerAdapter extends FragmentStatePagerAdapter {

    private final int[] blogIds;
    private final WeakReference<SiteThumbnailFragment>[] fragments;

    public CustomFragmentStatePagerAdapter(FragmentManager fm, int[] blogIds) {
        super(fm);
        this.blogIds = blogIds;
        this.fragments = (WeakReference<SiteThumbnailFragment>[]) new WeakReference<?>[blogIds.length];
    }

    @Override
    public Fragment getItem(int index) {
        WeakReference<SiteThumbnailFragment> reference = fragments[index];
        SiteThumbnailFragment fragment;
        if (reference == null || (fragment = reference.get()) == null) {
            fragment = new SiteThumbnailFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("id", this.blogIds[index]);
            fragment.setArguments(bundle);
            this.fragments[index] = new WeakReference<SiteThumbnailFragment>(
                    fragment);
        }
        return fragment;
    }

    @Override
    public int getCount() {
        return this.blogIds.length;
    }

    /**
     * 指定されたインデックスのブログIDを返します。
     * 
     * @param position
     * @return
     */
    public int getCurrentBlogId(int position) {
        return this.blogIds[position];
    }

    public void onConfigurationChanged() {
        for (int i = 0; i < fragments.length; i++) {
            WeakReference<SiteThumbnailFragment> reference = fragments[i];
            SiteThumbnailFragment fragment;
            if (reference == null || (fragment = reference.get()) == null) {
                continue;
            }
            fragment.optimizeImageSize(-1);
        }
    }
}
