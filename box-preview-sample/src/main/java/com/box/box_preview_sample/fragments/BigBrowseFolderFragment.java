package com.box.box_preview_sample.fragments;


import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.box.androidsdk.browse.activities.BoxBrowseActivity;
import com.box.androidsdk.browse.fragments.BoxBrowseFolderFragment;
import com.box.androidsdk.browse.uidata.BoxListItem;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxListItems;
import com.box.androidsdk.content.models.BoxSession;
import com.box.box_preview_sample.R;

import java.text.DateFormat;
import java.util.Locale;


/**
 * Sample activity that demonstrates preview functionality for BoxFiles of an authenticated user
 */
public class BigBrowseFolderFragment extends BoxBrowseFolderFragment {

    protected BoxListItems mBoxListItems;

    public static BigBrowseFolderFragment newInstance(String folderId, String userId, String folderName, int limit) {
        BigBrowseFolderFragment fragment = new BigBrowseFolderFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ID, folderId);
        args.putString(ARG_USER_ID, userId);
        args.putString(ARG_NAME, folderName);
        args.putInt(ARG_LIMIT, limit);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Use this factory method to create a new instance of the Browse fragment
     * with default configurations
     *
     * @param folder  the folder to browse
     * @param session the session that the contents will be loaded for
     * @return A new instance of fragment BoxBrowseFragment.
     */
    public static BoxBrowseFolderFragment newInstance(BoxFolder folder, BoxSession session) {
        return newInstance(folder.getId(), session.getUserId(), folder.getName(), DEFAULT_LIMIT);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(com.box.androidsdk.browse.R.layout.box_browsesdk_fragment_browse, container, false);
        mSwipeRefresh = (SwipeRefreshLayout) rootView.findViewById(com.box.androidsdk.browse.R.id.box_browsesdk_swipe_reresh);
        mSwipeRefresh.setOnRefreshListener(this);
        mSwipeRefresh.setColorSchemeColors(com.box.androidsdk.browse.R.color.box_accent);
        // This is a work around to show the loading circle because SwipeRefreshLayout.onMeasure must be called before setRefreshing to show the animation
        mSwipeRefresh.setProgressViewOffset(false, 0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics()));

        mItemsView = (RecyclerView) rootView.findViewById(com.box.androidsdk.browse.R.id.box_browsesdk_items_recycler_view);
        mItemsView.addItemDecoration(new BoxItemDividerDecoration(getResources()));
        mItemsView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAdapter = new BigItemAdapter();
        mItemsView.setAdapter(mAdapter);

        if (mBoxListItems == null) {
            mAdapter.add(new BoxListItem(fetchInfo(), ACTION_FETCHED_INFO));
        } else {
            displayBoxList(mBoxListItems);

        }
        return rootView;
    }

    public void setListItem(BoxListItems mBoxListItems) {
        mBoxListItems = mBoxListItems;
        super.setListItem(mBoxListItems);
    }

    public class BigItemAdapter extends BoxItemAdapter{


        @Override
        public BoxItemViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.big_boxsdk_list_item, viewGroup, false);
            return new BoxItemViewHolder(view);
        }



    }


    private class BoxItemDividerDecoration extends RecyclerView.ItemDecoration {
        Drawable mDivider;

        public BoxItemDividerDecoration(Resources resources) {
            mDivider = resources.getDrawable(com.box.androidsdk.browse.R.drawable.box_browsesdk_item_divider);
        }

        @Override
        public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
            int left = parent.getPaddingLeft();
            int right = parent.getWidth() - parent.getPaddingRight();

            int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = parent.getChildAt(i);

                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

                int top = child.getBottom() + params.bottomMargin;
                int bottom = top + mDivider.getIntrinsicHeight();

                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(c);
            }
        }
    }

    private static final String DESCRIPTION = "Uploaded %1$s";

    /**
     * Called when a {@link BoxListItem} is bound to a ViewHolder. Customizations of UI elements
     * should be done by overriding this method. If extending from a {@link BoxBrowseActivity}
     * a custom BoxBrowseFolder fragment can be returned in
     * {@link BoxBrowseActivity#createBrowseFolderFragment(BoxItem, BoxSession)}
     *
     * @param holder the BoxItemHolder
     */
    protected void onBindBoxItemViewHolder(BoxItemViewHolder holder) {
        if (holder.getItem() == null || holder.getItem().getBoxItem() == null) {
            return;
        }

        final BoxItem item = holder.getItem().getBoxItem();
        holder.getNameView().setText(item.getName());
        String description = "";
        if (item != null) {
            String modifiedAt = item.getModifiedAt() != null ?
                    DateFormat.getDateInstance(DateFormat.SHORT).format(item.getModifiedAt()).toUpperCase() :
                    "";
            description = String.format(Locale.ENGLISH, DESCRIPTION, modifiedAt);
            mThumbnailManager.setThumbnailIntoView(holder.getThumbView(), item);
        }
        holder.getMetaDescription().setText(description);
        holder.getProgressBar().setVisibility(View.GONE);
        holder.getMetaDescription().setVisibility(View.VISIBLE);
        holder.getThumbView().setVisibility(View.VISIBLE);
        if (!holder.getItem().getIsEnabled()) {
            holder.getView().setEnabled(false);
            holder.getNameView().setTextColor(getResources().getColor(com.box.androidsdk.browse.R.color.box_browsesdk_hint));
            holder.getMetaDescription().setTextColor(getResources().getColor(com.box.androidsdk.browse.R.color.box_browsesdk_disabled_hint));
            holder.getThumbView().setAlpha(0.26f);
        } else {
            holder.getView().setEnabled(true);
            holder.getNameView().setTextColor(getResources().getColor(com.box.androidsdk.browse.R.color.box_browsesdk_primary_text));
            holder.getMetaDescription().setTextColor(getResources().getColor(com.box.androidsdk.browse.R.color.box_browsesdk_hint));
            holder.getThumbView().setAlpha(1f);
        }
    }


}
