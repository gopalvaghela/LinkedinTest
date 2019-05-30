package com.t.myapplication;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.TextHttpResponseHandler;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {

    /*CONSTANT FOR THE AUTHORIZATION PROCESS*/
    String fname, lname;
    private String linkedin_fname, linkedin_lname;
    private String linkedin_imgurl;
    private String linkedin_emailid;
    private String linkedin_id;
    /****FILL THIS WITH YOUR INFORMATION*********/
    //successfull auth url
    // private static final String AUTHORIZATION_URL="https://www.linkedin.com/oauth/v2/authorization?response_type=code&client_id=815v14d0fga13f&redirect_uri=https://www.latitudetechnolabs.com/auth/linkedin/callback&scope=r_emailaddress%20r_liteprofile%20w_member_social";
//This is the public api key of our application
    private static final String API_KEY = "815v14d0fga13f";
    //This is the private api key of our application
    private static final String SECRET_KEY = "zKRtJlXcjg4KDH5f";
    //This is any string we want to use. This will be used for avoiding CSRF attacks. You can generate one here: http://strongpasswordgenerator.com/
    private static final String STATE = "E3ZYKC1T6H2yP4z";
    //This is the url that LinkedIn Auth process will redirect to. We can put whatever we want that starts with http:// or https:// .
//We use a made up url that we will intercept when redirecting. Avoid Uppercases.
    private static final String REDIRECT_URI = "https://www.latitudetechnolabs.com/auth/linkedin/callback";
    /*********************************************/

//These are constants used for build the urls
    //  private static final String AUTHORIZATION_URL = "https://www.linkedin.com/uas/oauth2/authorization?scope=r_liteprofile";
    private static final String AUTHORIZATION_URL = "https://www.linkedin.com/oauth/v2/authorization?response_type=code&client_id=815v14d0fga13f&redirect_uri=https://www.latitudetechnolabs.com/auth/linkedin/callback&scope=r_emailaddress%20r_liteprofile%20w_member_social";
    //  private static final String AUTHORIZATION_URL="https://www.linkedin.com/oauth/v2/authorization?response_type=code&client_id=815v14d0fga13f&redirect_uri=https://www.latitudetechnolabs.com/auth/linkedin/callback&state=asldj@$@#$sdjsldfjsd&scope=r_liteprofile%20r_emailaddress%20";
    private static final String ACCESS_TOKEN_URL = "https://www.linkedin.com/uas/oauth2/accessToken";
    // private static final String ACCESS_TOKEN_URL ="https://www.linkedin.com/oauth/v2/accessToken?grant_type=authorization_code&code={authorization_code_from_step2_response}&redirect_uri=https://www.latitudetechnolabs.com/auth/linkedin/callback&client_id={815v14d0fga13f}&client_secret={zKRtJlXcjg4KDH5f}";
    private static final String SECRET_KEY_PARAM = "client_secret";
    private static final String RESPONSE_TYPE_PARAM = "response_type";
    private static final String GRANT_TYPE_PARAM = "grant_type";
    private static final String GRANT_TYPE = "authorization_code";
    private static final String RESPONSE_TYPE_VALUE = "code";
    private static final String CLIENT_ID_PARAM = "client_id";
    private static final String STATE_PARAM = "state";
    private static final String REDIRECT_URI_PARAM = "redirect_uri";
    /*---------------------------------------*/
    private static final String QUESTION_MARK = "?";
    private static final String AMPERSAND = "&";
    private static final String EQUALS = "=";

    String emailurlv2="https://api.linkedin.com/v2/emailAddress?q=members&projection=(elements*(handle~))";

    private WebView webView;
    private ProgressDialog pd;
    Button btnlogin;
    String accessToken;
    TextView txtdata,txtemail;
    ImageView imgview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnlogin = findViewById(R.id.btnlogin);
        //get the webView from the layout
      //  webView = (WebView) findViewById(R.id.main_activity_web_view);
        txtdata=findViewById(R.id.txtdata);
        txtemail=findViewById(R.id.txtEMAIL);
        imgview=findViewById(R.id.imgview);
      //  webView.setVisibility(View.VISIBLE);
        pd = new ProgressDialog(this);
        pd.setMessage("Loading...");
      //  pd.dismiss();
        btnlogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startActivityForResult(new Intent(MainActivity.this, LinkedInLogin.class), 110);

               /*// pd = ProgressDialog.show(getApplicationContext(), "", "this.getString(R.string.loading)", true);
                pd.show();

                //Request focus for the webview
                webView.requestFocus(View.FOCUS_DOWN);

                //Show a progress dialog to the user

                //Set a custom web view client
                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        //This method will be executed each time a page finished loading.
                        //The only we do is dismiss the progressDialog, in case we are showing any.
                        if (pd != null && pd.isShowing()) {
                            pd.dismiss();
                        }
                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String authorizationUrl) {
                        //This method will be called when the Auth proccess redirect to our RedirectUri.
                        //We will check the url looking for our RedirectUri.
                        if (authorizationUrl.startsWith(REDIRECT_URI)) {
                            Log.i("Authorize", "");
                            Uri uri = Uri.parse(authorizationUrl);
                            //We take from the url the authorizationToken and the state token. We have to check that the state token returned by the Service is the same we sent.
                            //If not, that means the request may be a result of CSRF and must be rejected.
                            String stateToken = uri.getQueryParameter(STATE_PARAM);
                            if (stateToken == null || !stateToken.equals(STATE)) {
                                Log.e("Authorize", "State token doesn't match");
                                return true;
                            }

                            //If the user doesn't allow authorization to our application, the authorizationToken Will be null.
                            String authorizationToken = uri.getQueryParameter(RESPONSE_TYPE_VALUE);
                            if (authorizationToken == null) {
                                Log.i("Authorize", "The user doesn't allow authorization.");
                                return true;
                            }
                            Log.i("Authorize", "Auth token received: " + authorizationToken);

                            //Generate URL for requesting Access Token
                            String accessTokenUrl = getAccessTokenUrl(authorizationToken);
                            //We make the request in a AsyncTask
                            new PostRequestAsyncTask().execute(accessTokenUrl);

                        } else {
                            //Default behaviour
                            Log.i("Authorize", "Redirecting to: " + authorizationUrl);
                            webView.loadUrl(authorizationUrl);
                        }
                        return true;
                    }
                });

                //Get the authorization Url
                String authUrl = getAuthorizationUrl();
                Log.i("Authorize", "Loading Auth Url: " + authUrl);
                //Load the authorization URL into the webView
                webView.loadUrl(authUrl);*/

            }
        });
    }

    @Override
    protected void onActivityResult(final int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 110) {
            linkedin_fname = data.getStringExtra("firstname");
            linkedin_lname = data.getStringExtra("lastname");
            linkedin_id = data.getStringExtra("id");
            linkedin_imgurl = data.getStringExtra("imageurl");
            linkedin_emailid = data.getStringExtra("email");

            Glide.with(MainActivity.this).load(linkedin_imgurl).into(imgview);
            txtdata.setText("Firstname :"+linkedin_fname+"\nLastname :"+linkedin_lname);
            txtemail.setText("Email "+linkedin_emailid);
        }

//        LISessionManager.getInstance(getApplicationContext())
//                .onActivityResult(this,
//                        requestCode, resultCode, data);


    }

}
