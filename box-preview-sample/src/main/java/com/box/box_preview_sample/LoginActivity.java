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
import android.text.TextUtils;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.box.androidsdk.browse.fragments.BoxBrowseFragment;
import com.box.androidsdk.content.BoxConfig;
import com.box.androidsdk.content.BoxConstants;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.auth.BoxAuthentication;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxHttpResponse;
import com.box.androidsdk.content.utils.SdkUtils;
import com.eclipsesource.json.JsonObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;


public class LoginActivity extends AppCompatActivity {

    public static BoxAuthentication.AuthenticationRefreshProvider mRefreshProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

            setContentView(R.layout.login);
            final EditText login = (EditText)findViewById(R.id.login);
            final EditText password = (EditText)findViewById(R.id.password);
            Button submit = (Button)findViewById(R.id.submit);
            final Handler handler = new Handler();
            submit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    loginTOU(login.getText().toString(), password.getText().toString());
                }
            });

//            BoxSession session = new BoxSession(this, "", mRefreshProvider);



    }


    private String loginTOU(final String username, final String password){
        final ProgressDialog dialog = new ProgressDialog(LoginActivity.this);
        dialog.setTitle("Logging in");
        dialog.setMessage("Please wait...");
        dialog.show();
        new Thread(){
            @Override
            public void run() {
                String loginString = username + ":" + password;
                String encoded = Base64.encodeToString(loginString.getBytes(), Base64.DEFAULT);
                try{
                    URL requestUrl =  new URL("http://digitallockerwebapi.azurewebsites.net/api/box/authenticate");

                    HttpURLConnection connection = (HttpURLConnection)requestUrl.openConnection();
                    connection.setRequestMethod("GET");
                    connection.addRequestProperty("Authorization", "Basic " +  encoded);

                    BoxHttpResponse response = new BoxHttpResponse(connection);
                    response.open();
                    JsonObject jsonObject = JsonObject.readFrom(response.getStringBody());
                    String userToken = jsonObject.get("userToken").asString();
                    if (!SdkUtils.isBlank(userToken)) {
                        BoxAuthentication.BoxAuthenticationInfo info = new BoxAuthentication.BoxAuthenticationInfo();
                        info.setAccessToken(userToken);
                        BoxAuthentication.getInstance().onAuthenticated(info, LoginActivity.this);
                        finish();
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
                finally{
                    dialog.dismiss();
                }
            }
        }.start();


        return null;
    }


    public static Intent getLaunchIntent(Activity activity, BoxSession mSession) {
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.putExtra(CategoryBrowseFragment.ARG_USER_ID, mSession.getUserId());
        intent.putExtra(CategoryBrowseFragment.ARG_LIMIT, 500);
        return intent;
    }

}
