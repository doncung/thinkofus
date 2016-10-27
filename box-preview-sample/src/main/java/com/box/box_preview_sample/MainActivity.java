package com.box.box_preview_sample;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.Toast;

import com.box.androidsdk.browse.activities.BoxBrowseFileActivity;
import com.box.androidsdk.content.BoxConfig;
import com.box.androidsdk.content.BoxConstants;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.auth.BoxAuthentication;
import com.box.androidsdk.content.models.BoxFile;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxList;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.models.BoxUser;
import com.box.androidsdk.preview.BoxPreviewActivity;

/**
 * Sample activity that demonstrates preview functionality for BoxFiles of an authenticated user
 */
public class MainActivity extends AppCompatActivity implements BoxAuthentication.AuthListener {


    BoxSession mSession = null;

    private static final int BROWSE_FILE_REQUEST_CODE = 101;
    private static final int PREVIEW_FILE_REQUEST_CODE = 102;
    private static final String ROOT_FOLDER_ID = "0";

    /** Maintaining state to support browsing back up to selected folders */
    private BoxList<BoxFolder> mPathToRoot;
    private boolean mLoadedRoot;
    private static final String ROOT_IS_LOADED = "Bundle.Current_folder";
    private static final String PATH_TO_ROOT = "Bundle.Path_To_Root";
    BoxAuthentication.AuthenticationRefreshProvider mRefreshProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BoxConfig.IS_LOG_ENABLED = true;
        final Context applicationContext = this;
        mRefreshProvider = new BoxAuthentication.AuthenticationRefreshProvider() {
            @Override
            public BoxAuthentication.BoxAuthenticationInfo refreshAuthenticationInfo(BoxAuthentication.BoxAuthenticationInfo info) throws BoxException {
                System.out.println("*** refreshAuthenticationInfo " + info);

                return info;
            }

            @Override
            public boolean launchAuthUi(String userId, BoxSession session) {
                BoxAuthentication.BoxAuthenticationInfo info = new BoxAuthentication.BoxAuthenticationInfo();
                info.setAccessToken("vEr4S4NeeOXuoSUruuxHVUr2ygIPcSpm");
                info.setRefreshTime(System.currentTimeMillis());
                String json = "{\"type\":\"user\",\"id\":\"406531322\",\"name\":\"enterprise\",\"login\":\"enterprisetestboxer+hack@gmail.com\",\"created_at\":\"2016-10-26T16:52:58-07:00\",\"modified_at\":\"2016-10-26T22:43:45-07:00\",\"language\":\"en\",\"timezone\":\"America\\/Los_Angeles\",\"space_amount\":10737418240,\"space_used\":7917800,\"max_upload_size\":262144000,\"status\":\"active\",\"job_title\":\"\",\"phone\":\"7703627258\",\"address\":\"\",\"avatar_url\":\"https:\\/\\/app.box.com\\/api\\/avatar\\/large\\/406531322\"}";
                BoxUser user = new BoxUser();
                user.createFromJson(json);
                info.setUser(user);
                BoxAuthentication.getInstance().onAuthenticated(info, applicationContext);
                System.out.println("*** launchAuthUi " + userId + " " + session);
                return true;
            }
        };
        BoxAuthentication.getInstance().setRefreshProvider(mRefreshProvider);


   //     BoxConfig.CLIENT_ID = "186mddjxxv7vlyjxli70ur0tmdpkdgsi";
    //    BoxConfig.CLIENT_SECRET = "psi8wEOuP5s9lDtfoKOUDvchjNcjxaFX";


        // needs to match redirect uri in developer settings if set.
        BoxConfig.REDIRECT_URL = "https://app.box.com/static/sync_redirect.html";

        // needs to match redirect uri in developer settings if set.
  //      BoxConfig.REDIRECT_URL = "https://app.box.com/static/sync_redirect.html";

        if (savedInstanceState != null) {
            mPathToRoot = (BoxList<BoxFolder>) savedInstanceState.getSerializable(PATH_TO_ROOT);
            mLoadedRoot =  savedInstanceState.getBoolean(ROOT_IS_LOADED);
        }
        initialize(false);
    }

    /**
     * Open the box preview activity for previewing this file
     *
     * @param file
     */
    private void launchPreview(BoxFile file) {
        mPathToRoot = file.getPathCollection();
        BoxFolder parentFolder = file.getParent() == null ? BoxFolder.createFromId("0") : file.getParent();
        BoxPreviewActivity.IntentBuilder builder = BoxPreviewActivity.createIntentBuilder(this, mSession, file)
                .setBoxFolder(parentFolder);
        //set the box folder, so the sdk can page through other files in the directory for images, audio or video
        startActivityForResult(builder.createIntent(), PREVIEW_FILE_REQUEST_CODE);
    }

    /**
     * Browsing files in the folder using the box browse sdk.
     * @param folder BoxFolder to browse
     */
    private void browseFolder(BoxFolder folder) {
        startActivityForResult(TextUtils.equals(folder.getId(), BoxConstants.ROOT_FOLDER_ID) ?
                CategoryBrowseActivity.getLaunchIntent(this, mSession) :
                CustomBrowseFileActivity.
                        getLaunchIntent(MainActivity.this, folder, mSession),
                BROWSE_FILE_REQUEST_CODE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //Response of the box browse api
        if (requestCode == BROWSE_FILE_REQUEST_CODE) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    if (data.getSerializableExtra(BoxBrowseFileActivity.EXTRA_BOX_FILE) instanceof BoxFile) {
                    launchPreview((BoxFile) data.getSerializableExtra(BoxBrowseFileActivity.EXTRA_BOX_FILE));
                } else {
                    Toast.makeText(this, com.box.androidsdk.preview.R.string
                            .box_previewsdk_unable_to_access_file, Toast.LENGTH_LONG).show();
                }
                break;
                case Activity.RESULT_CANCELED:
                    up();
                    break;
                case CustomBrowseFileActivity.RESULT_LOGOUT:
                    if (mSession == null) {
                        return;
                    }
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                mSession.logout().get();
                                mLoadedRoot = false;
                                initialize(true);
                            } catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    }.start();
                    break;
            }


        } else if (requestCode == PREVIEW_FILE_REQUEST_CODE) { //Preview api response
            up();
        }
    }

    //Browse the parent folder or exit if we are already at the root
    private void up() {
        if (mPathToRoot == null || mPathToRoot.isEmpty()) {
            finish();
        } else {
            BoxFolder folder = mPathToRoot.get(mPathToRoot.size() - 1);
            mPathToRoot.remove(folder);
            browseFolder(folder);

        }
    }

    /**
     * Authenticate using the box sdk.
     */
    private void initialize(boolean showUserPicker) {
        mSession = showUserPicker ? new BoxSession(this, null) : new BoxSession(this);
        mSession.setSessionAuthListener(this);
        mSession.authenticate();
    }


    @Override
    public void onRefreshed(BoxAuthentication.BoxAuthenticationInfo info) {
    }

    @Override
    public void onAuthCreated(BoxAuthentication.BoxAuthenticationInfo info) {
        if (!mLoadedRoot) {
            loadRootFolder();
        }
    }

    @Override
    public void onAuthFailure(BoxAuthentication.BoxAuthenticationInfo info, Exception ex) {
        if (ex != null) {
            mLoadedRoot = false;
            initialize(false);
        } else {
            finish();
        }
    }

    @Override
    public void onLoggedOut(BoxAuthentication.BoxAuthenticationInfo info, Exception ex) {
        mLoadedRoot = false;
    }


    private void loadRootFolder() {
        mLoadedRoot = true;
        browseFolder(BoxFolder.createFromId(ROOT_FOLDER_ID));
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(PATH_TO_ROOT, mPathToRoot);
        outState.putBoolean(ROOT_IS_LOADED, mLoadedRoot);
    }


}
