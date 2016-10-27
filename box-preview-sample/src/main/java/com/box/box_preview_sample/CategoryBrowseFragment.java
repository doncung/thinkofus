package com.box.box_preview_sample;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.box.androidsdk.browse.fragments.BoxBrowseFragment;
import com.box.androidsdk.browse.uidata.BoxListItem;
import com.box.androidsdk.content.BoxApiFolder;
import com.box.androidsdk.content.BoxConstants;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.models.BoxFile;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxListItems;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Activity displaying default categories, wit
 */
public class CategoryBrowseFragment  extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private enum DefaultCategory {
        PERSONAL (R.drawable.ic_person_outline_24dp), HEALTH (R.drawable.ic_health_200dp),
        EDUCATION (R.drawable.ic_school_200dp), WORK(R.drawable.ic_work_200dp), NOTES(R.drawable.ic_notes_200dp);
        int iconResId;

        DefaultCategory(int iconId) {
            iconResId = iconId;
        }
    }

        public static final String ARG_USER_ID = "argUserId";
        public static final String ARG_LIMIT = "argLimit";

        public static final String TAG = CategoryBrowseFragment.class.getName();
        protected static final int DEFAULT_LIMIT = 1000;

        protected static final String ACTION_FETCHED_ITEMS = "FetchedItems";
        protected static final String ACTION_ADDED_DEFAULT = "AddedDefault";
        protected static final String EXTRA_SUCCESS = "ArgSuccess";
        protected static final String EXTRA_OFFSET = "ArgOffset";
        protected static final String EXTRA_LIMIT = "ArgLimit";
        protected static final String EXTRA_COLLECTION = "Collection";

        protected String mUserId;
        protected BoxSession mSession;
        protected int mLimit = DEFAULT_LIMIT;

        private BoxListItems mBoxListItems;

        protected BoxBrowseFragment.OnFragmentInteractionListener mListener;

        protected BoxItemAdapter mAdapter;
        protected GridView mItemsView;
        protected SwipeRefreshLayout mSwipeRefresh;

        protected LocalBroadcastManager mLocalBroadcastManager;
        private boolean mWaitingForConnection;
        private boolean mIsConnected;


        private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(ACTION_FETCHED_ITEMS)) {
                    onItemsFetched(intent);
                } else if (intent.getAction().equals(ACTION_ADDED_DEFAULT)) {

                }

                // Remove refreshing icon
                if (mSwipeRefresh != null) {
                    mSwipeRefresh.setRefreshing(false);
                }

            }
        };

        private BroadcastReceiver mConnectivityReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                    ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                    mIsConnected = (networkInfo != null && networkInfo.isConnected());
                    if (mWaitingForConnection && mIsConnected) {
                        mWaitingForConnection = false;
                        onRefresh();
                    }
                }
            }
        };

        private static ThreadPoolExecutor mApiExecutor;
        private static ThreadPoolExecutor mThumbnailExecutor;

    protected ThreadPoolExecutor getApiExecutor() {
        if (mApiExecutor == null || mApiExecutor.isShutdown()) {
            mApiExecutor = new ThreadPoolExecutor(1, 1, 3600, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        }
        return mApiExecutor;
    }


    protected IntentFilter initializeIntentFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_FETCHED_ITEMS);
        return filter;
    }

    public CategoryBrowseFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize broadcast managers
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
        if (getArguments() != null) {
            mUserId = getArguments().getString(ARG_USER_ID);
            mSession = new BoxSession(getActivity(), mUserId);
            mLimit = getArguments().getInt(ARG_LIMIT);
        }
        if (savedInstanceState != null) {
            setListItem((BoxListItems) savedInstanceState.getSerializable(EXTRA_COLLECTION));
        }
        getApiExecutor().execute(fetchItems(0, mLimit));
        setRetainInstance(true);
    }

    @Override
    public void onResume() {
        getActivity().registerReceiver(mConnectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, initializeIntentFilters());
        super.onResume();
    }

    @Override
    public void onPause() {
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
        getActivity().unregisterReceiver(mConnectivityReceiver);
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(EXTRA_COLLECTION, mBoxListItems);
        super.onSaveInstanceState(outState);
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.categories_layout, container, false);
        mSwipeRefresh = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_refresh);
        mSwipeRefresh.setOnRefreshListener(this);
        mSwipeRefresh.setColorSchemeColors(R.color.app_blue);
        // This is a work around to show the loading circle because SwipeRefreshLayout.onMeasure must be called before setRefreshing to show the animation
        mSwipeRefresh.setProgressViewOffset(false, 0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics()));

        mItemsView = (GridView) rootView.findViewById(R.id.grid_view);


        if (mBoxListItems != null) {
            setListItem(mBoxListItems);
            displayBoxList(mBoxListItems);
        }
        return rootView;
    }

    protected void setListItem(final BoxListItems items) {
        mBoxListItems = items;
        if (mAdapter == null) {
            mAdapter = new BoxItemAdapter();
            mAdapter.addAll(items);
            mItemsView.setAdapter(mAdapter);
        }
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity) {
            try {
                mListener = (BoxBrowseFragment.OnFragmentInteractionListener) context;
            } catch (ClassCastException e) {
                throw new ClassCastException(context.toString() + " must implement OnFragmentInteractionListener");
            }
        }

    }

    @Override
    public void onRefresh() {
        mSwipeRefresh.setRefreshing(true);
        getApiExecutor().execute(fetchItems(0, mLimit));
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    /**
     * Handle showing a collection in the given intent.
     *
     * @param intent an intent that contains a collection in EXTRA_COLLECTION.
     */
    protected void onItemsFetched(Intent intent) {
        checkConnectivity();
        displayBoxList((BoxListItems) intent.getSerializableExtra(EXTRA_COLLECTION));
        mSwipeRefresh.setRefreshing(false);
    }

    /**
     * Call on loading error and refresh if loss of connectivity is the suspect.
     */
    protected void checkConnectivity() {
        mWaitingForConnection = !mIsConnected;
    }
    /**
     * show in this fragment a box list of items.
     */
    protected void displayBoxList(final BoxListItems items) {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (items.isEmpty()) {
            //First install upload default categories
            getApiExecutor().execute( new FutureTask<Intent>(new Callable<Intent>() {

                @Override
                public Intent call() throws Exception {
                    Intent intent = new Intent();
                    intent.setAction(ACTION_ADDED_DEFAULT);
                    try {
                        BoxApiFolder folder = new BoxApiFolder(mSession);
                        BoxRequestBatch request = new BoxRequestBatch();
                        for (DefaultCategory category : DefaultCategory.values()) {
                            request.addRequest(folder.getCreateRequest(BoxConstants.ROOT_FOLDER_ID, category.name()));
                        }
                        request.send();
                        intent.putExtra(EXTRA_SUCCESS, true);
                        intent.putExtra(EXTRA_COLLECTION, items);
                    } catch (BoxException e) {
                        e.printStackTrace();
                        intent.putExtra(EXTRA_SUCCESS, false);
                    } finally {
                        mLocalBroadcastManager.sendBroadcast(intent);
                    }

                    return intent;
                }
            }));
            return;
        }

        // if we are trying to display the original list no need to add.
        if (items == mBoxListItems) {

            //  mBoxListItems.addAll(items);
            if (mAdapter.getCount() < 1) {
                mAdapter.addAll(items);
            }
        } else {
            if (mBoxListItems == null) {
                setListItem(items);
            }
            mBoxListItems.addAll(items);
            mAdapter.addAll(items);
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
            }
        });

        if (mBoxListItems.size() < items.fullSize()) {
            // if not all entries were fetched add a task to fetch more items if user scrolls to last entry.
            mAdapter.add(new BoxListItem(fetchItems(mBoxListItems.size(), mLimit), ACTION_FETCHED_ITEMS));
        }

    }

    public FutureTask<Intent> fetchItems(final int offset, final int limit) {
        return new FutureTask<Intent>(new Callable<Intent>() {

            @Override
            public Intent call() throws Exception {
                Intent intent = new Intent();
                intent.setAction(ACTION_FETCHED_ITEMS);
                intent.putExtra(EXTRA_OFFSET, offset);
                intent.putExtra(EXTRA_LIMIT, limit);
                try {

                    // this call the collection is just BoxObjectItems and each does not appear to be an instance of BoxItem.
                    ArrayList<String> itemFields = new ArrayList<String>();
                    String[] fields = new String[]{BoxFile.FIELD_NAME, BoxFile.FIELD_SIZE, BoxFile.FIELD_OWNED_BY, BoxFolder.FIELD_HAS_COLLABORATIONS, BoxFolder.FIELD_IS_EXTERNALLY_OWNED, BoxFolder.FIELD_PARENT};
                    BoxApiFolder api = new BoxApiFolder(mSession);
                    BoxListItems items = api.getItemsRequest(BoxConstants.ROOT_FOLDER_ID).setLimit(limit).setOffset(offset).setFields(fields).send();
                    intent.putExtra(EXTRA_SUCCESS, true);
                    intent.putExtra(EXTRA_COLLECTION, items);
                } catch (BoxException e) {
                    e.printStackTrace();
                    intent.putExtra(EXTRA_SUCCESS, false);
                } finally {
                    mLocalBroadcastManager.sendBroadcast(intent);
                }

                return intent;
            }
        });
    }

    protected  class BoxItemAdapter extends BaseAdapter {
        protected ArrayList<BoxListItem> mListItems = new ArrayList<BoxListItem>();
        protected HashMap<String, BoxListItem> mItemsMap = new HashMap<String, BoxListItem>();




        public BoxListItem get(String id) {
            return mItemsMap.get(id);
        }


        public void remove(BoxListItem listItem) {
            remove(listItem.getIdentifier());
        }

        public synchronized void remove(String key) {
            BoxListItem item = mItemsMap.remove(key);
            if (item != null) {
                boolean success = mListItems.remove(item);
            }
        }

        public void addAll(BoxListItems items) {
            for (BoxItem item : items) {
                if (!mItemsMap.containsKey(item.getId())) {
                    add(new BoxListItem(item, item.getId()));
                } else {
                    // update an existing item if it exists.
                    mItemsMap.get(item.getId()).setBoxItem(item);
                }
            }
        }

        public synchronized void add(BoxListItem listItem) {
            mListItems.add(listItem);
            mItemsMap.put(listItem.getIdentifier(), listItem);
        }

        public void update(String id) {
            BoxListItem item = mItemsMap.get(id);
            if (item != null) {
                int index = mListItems.indexOf(item);
            }
        }

        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }

        @Override
        public boolean isEnabled(int position) {
            return true;
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {

        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {

        }

        @Override
        public int getCount() {
            return mListItems.size();
        }

        @Override
        public Object getItem(int position) {
            return mListItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view = (convertView == null) ? LayoutInflater.from(CategoryBrowseFragment
                    .this.getContext()).inflate(R.layout.category_list_item, parent, false) : convertView;
            String categoryName =  mBoxListItems.get(position).getName();
            ((TextView)view.findViewById(R.id.category_name)).setText(categoryName);
            for (DefaultCategory category : DefaultCategory.values()) {
                if (TextUtils.equals(categoryName, category.name())) {
                    ((ImageView)view.findViewById(R.id.category_icon)).setImageResource(category.iconResId);
                    break;
                }
            }
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    BoxItem item = ((BoxListItem) getItem(position)).getBoxItem();
                    if (item != null) {
                        CategoryBrowseFragment.this.mListener.handleOnItemClick(item);
                    }

                }
            });
            return view;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }



    public static void setAccentColor(Resources res, ProgressBar progressBar) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            int accentColor = res.getColor(R.color.app_blue);
            Drawable drawable = progressBar.getIndeterminateDrawable();
            if (drawable != null) {
                drawable.setColorFilter(accentColor, PorterDuff.Mode.SRC_IN);
                drawable.invalidateSelf();
            }
        }
    }
}