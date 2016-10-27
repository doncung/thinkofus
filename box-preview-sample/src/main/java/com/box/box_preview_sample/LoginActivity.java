package com.box.box_preview_sample;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.internal.app.WindowDecorActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.box.androidsdk.browse.fragments.BoxBrowseFragment;
import com.box.androidsdk.content.BoxConfig;
import com.box.androidsdk.content.BoxConstants;
import com.box.androidsdk.content.auth.BoxAuthentication;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.utils.SdkUtils;


public class LoginActivity extends AppCompatActivity {

    public static BoxAuthentication.AuthenticationRefreshProvider mRefreshProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BoxConfig.CLIENT_ID = "186mddjxxv7vlyjxli70ur0tmdpkdgsi";
        BoxConfig.CLIENT_SECRET = "psi8wEOuP5s9lDtfoKOUDvchjNcjxaFX";
        BoxSession session = new BoxSession(this, "", mRefreshProvider);

        if (SdkUtils.isBlank(session.getAuthInfo().accessToken())){
            setContentView(R.layout.login);
            final EditText login = (EditText)findViewById(R.id.login);
            final EditText password = (EditText)findViewById(R.id.password);
            Button submit = (Button)findViewById(R.id.submit);
            final Handler handler = new Handler();
            submit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final ProgressDialog dialog = new ProgressDialog(LoginActivity.this);
                    dialog.setTitle("Logging in");
                    dialog.setMessage("Please wait...");
                    dialog.show();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            dialog.dismiss();
                            finish();
                        }
                    }, 1000);
                    String loginString = login.getText() + "/" + password.getText();
                    String encoded = Base64.encodeToString(loginString.getBytes(), Base64.DEFAULT);
                    System.out.println("encoded " + encoded);

                }
            });

//            BoxSession session = new BoxSession(this, "", mRefreshProvider);


        } else {
            // user is already logged in to box
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }

    }



    public static Intent getLaunchIntent(Activity activity, BoxSession mSession) {
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.putExtra(CategoryBrowseFragment.ARG_USER_ID, mSession.getUserId());
        intent.putExtra(CategoryBrowseFragment.ARG_LIMIT, 500);
        return intent;
    }

}
