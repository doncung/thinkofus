package com.box.box_preview_sample;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.LayoutRes;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.box.androidsdk.browse.activities.BoxBrowseFileActivity;
import com.box.androidsdk.browse.fragments.BoxBrowseFolderFragment;
import com.box.androidsdk.browse.fragments.BoxBrowseFragment;
import com.box.androidsdk.content.BoxApiFile;
import com.box.androidsdk.content.BoxFutureTask;
import com.box.androidsdk.content.auth.BoxAuthentication;
import com.box.androidsdk.content.models.BoxFile;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestBatch;
import com.box.androidsdk.content.requests.BoxRequestsFile;
import com.box.androidsdk.content.requests.BoxResponse;
import com.box.androidsdk.content.utils.SdkUtils;
import com.box.androidsdk.preview.BoxPreviewActivity;
import com.box.box_preview_sample.fragments.BigBrowseFolderFragment;
import com.box.box_preview_sample.fragments.CreatePhotoHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Extend BoxBrowseFileActivity to add custom menu items for demoing a few auth options
 */
public class CustomBrowseFileActivity extends BoxBrowseFileActivity {

    static final int RESULT_LOGIN = 2;
    static final int RESULT_LOGOUT = 3;
    static final int REQUEST_CAMERA = 5;

    private Uri mMediaUri;


    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(R.layout.activity_files);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void initToolbar() {
        Toolbar actionBar = (Toolbar) findViewById(com.box.androidsdk.browse.R.id.box_action_bar);
        setSupportActionBar(actionBar);
//        actionBar.setNavigationIcon(com.box.androidsdk.browse.R.drawable.ic_box_browsesdk_arrow_back_grey_24dp);
        actionBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragManager = getSupportFragmentManager();
                if (fragManager != null && fragManager.getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                } else {
                    finish();
                }
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorPrimary)));
        actionBar.setTitleTextColor(Color.WHITE);
        actionBar.setSubtitleTextColor(Color.WHITE);


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.myfab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                mMediaUri = CreatePhotoHelper.getNewPhotoUri(CustomBrowseFileActivity.this);
                startActivityForResult(CreatePhotoHelper.getPhotoIntent(mMediaUri), REQUEST_CAMERA);

            }
        });
    }


    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        outState.putParcelable("MEDIA_URI", mMediaUri);
        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null){
            mMediaUri = savedInstanceState.getParcelable("MEDIA_URI");
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK){
            switch(requestCode){
                case REQUEST_CAMERA:
                    if (mMediaUri != null) {
                        File file = new File(mMediaUri.getPath());
                        boolean isFile = file.isFile();

                        if (!isFile) {
                            // check to see we have an actual file from the camera application.
                            Toast.makeText(this,"No file found", Toast.LENGTH_LONG).show();
                            return;
                        }
                        CreatePhotoHelper.deleteCameraCopyOf(getApplicationContext(), file);
                        uploadWithExternalUri(file);
                    }

            }
        }
    }


    public void uploadWithExternalUri(final File file){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Upload file?");
        final EditText editText = new EditText(this);
        builder.setView(editText);
        editText.setText("health_2.jpg");
        editText.setGravity(Gravity.CENTER_HORIZONTAL);
        builder.setMessage("Upload file with given name? ");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                BigBrowseFolderFragment fragment = (BigBrowseFolderFragment)getTopBrowseFragment();
                uploadWithName(file, editText.getText().toString(), fragment.getFolder().getId());
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                file.delete();

            }
        });
        builder.create().show();

    }


    private void uploadWithName(final File file, String fileName, String folderId){
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setTitle("Uploading...");
        dialog.setMessage("Please wait...");
        dialog.show();
        BoxApiFile mFileApi = new BoxApiFile(mSession);
        BoxRequestsFile.UploadFile request = mFileApi.getUploadRequest(file, folderId).setFileName(fileName);
        BoxFutureTask<BoxFile> task = request.toTask();
        task.addOnCompletedListener(new BoxFutureTask.OnCompletedListener<BoxFile>() {
            @Override
            public void onCompleted(BoxResponse<BoxFile> response) {
                dialog.dismiss();
                BigBrowseFolderFragment fragment = (BigBrowseFolderFragment)getTopBrowseFragment();
                getApiExecutor(CustomBrowseFileActivity.this.getApplication()).execute(fragment.fetchInfo());
                file.delete();
            }
        });

        getApiExecutor(this.getApplication()).execute(task);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch(id) {
            case R.id.login:
                setResult(RESULT_LOGIN);
                finish();
                return true;
            case R.id.logout:
                setResult(RESULT_LOGOUT);
                finish();
                return true;
             default:
                 return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public boolean handleOnItemClick(BoxItem item) {
        if (item instanceof BoxFile){
            BigBrowseFolderFragment fragment = (BigBrowseFolderFragment)getTopBrowseFragment();
            Intent intent = BoxPreviewActivity.createIntentBuilder(this, mSession, (BoxFile)item).setBoxFolder(fragment.getFolder()).createIntent();
            startActivity(intent);
            return false;
        }
        return super.handleOnItemClick(item);
    }

    @Override
    protected void handleBoxFolderClicked(final BoxFolder boxFolder) {
        FragmentTransaction trans = getSupportFragmentManager().beginTransaction();

        // All fragments will always navigate into folders
        BoxBrowseFolderFragment browseFolderFragment = createBrowseFolderFragment(boxFolder, mSession);
        trans.replace(com.box.androidsdk.browse.R.id.box_browsesdk_fragment_container, browseFolderFragment);
        if (getSupportFragmentManager().getBackStackEntryCount() > 0 || getSupportFragmentManager().getFragments() != null) {
            trans.addToBackStack(BoxBrowseFragment.TAG);
        }
        trans.commit();
    }

    @Override
    protected BoxBrowseFolderFragment createBrowseFolderFragment(final BoxItem folder, final BoxSession session) {
        return BigBrowseFolderFragment.newInstance((BoxFolder) folder, mSession);
    }
    public static Intent getLaunchIntent(Context context, BoxFolder folder, BoxSession session) {
        if(folder != null && !SdkUtils.isBlank(folder.getId())) {
            if(session != null && session.getUser() != null && !SdkUtils.isBlank(session.getUser().getId())) {
                Intent intent = new Intent(context, CustomBrowseFileActivity.class);
                intent.putExtra("extraItem", folder);
                intent.putExtra("extraUserId", session.getUser().getId());
                return intent;
            } else {
                throw new IllegalArgumentException("A valid user must be provided to browse");
            }
        } else {
            throw new IllegalArgumentException("A valid folder must be provided to browse");
        }
    }

    @Override
    public void onBackPressed() {
        Log.d("custom browse", "back");
        super.onBackPressed();
    }




}
