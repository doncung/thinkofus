package com.box.box_preview_sample;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.internal.app.WindowDecorActionBar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

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

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        System.out.println(getSupportActionBar());
        if (getSupportActionBar() != null){
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.app_blue)));
            getSupportActionBar().setTitle("Think of Us");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            if (getSupportActionBar() instanceof WindowDecorActionBar){
                WindowDecorActionBar bar = (WindowDecorActionBar)getSupportActionBar();


            }
        }

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
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

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            // Handle the dashboard action
        } else if (id == R.id.nav_history) {
            // Handle the history action

        } else if (id == R.id.nav_documents) {
            // Handle the documents action

        } else if (id == R.id.nav_settings) {
            // Handle the settings action

        } else if (id == R.id.nav_logout) {
            // Handle the logout action
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
