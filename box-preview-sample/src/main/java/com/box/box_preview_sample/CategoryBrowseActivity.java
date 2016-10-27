package com.box.box_preview_sample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;

import com.box.androidsdk.browse.fragments.BoxBrowseFragment;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxSession;


public class CategoryBrowseActivity extends NavigationDrawerActivity implements BoxBrowseFragment.OnFragmentInteractionListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.category_activity);
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        CategoryBrowseFragment fragment = new CategoryBrowseFragment();
        fragment.setArguments(getIntent().getExtras());
        tx.replace(R.id.fragment_container, fragment);
        tx.commit();
    }



    public static Intent getLaunchIntent(Activity activity, BoxSession mSession) {
        Intent intent = new Intent(activity, CategoryBrowseActivity.class);
        intent.putExtra(CategoryBrowseFragment.ARG_USER_ID, mSession.getUserId());
        intent.putExtra(CategoryBrowseFragment.ARG_LIMIT, 500);
        return intent;
    }

    @Override
    public boolean handleOnItemClick(BoxItem item) {
        BoxSession session = new BoxSession(this, getIntent().getStringExtra(CategoryBrowseFragment.ARG_USER_ID));

        Intent intent = CustomBrowseFileActivity.getLaunchIntent(this, (BoxFolder)item, session);
        startActivity(intent);
        return false;
    }
}
