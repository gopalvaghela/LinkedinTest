package com.t.myapplication;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

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


public class LinkedInLogin extends AppCompatActivity {
    /*CONSTANT FOR THE AUTHORIZATION PROCESS*/
    private String fname, lname;
    private String imgurl;
    private String email;
    private String linkedid;
    /****FILL THIS WITH YOUR INFORMATION*********/
    //successfull auth url

//This is the public api key of our application
    private static final String API_KEY = "client_id";
    //This is the private api key of our application
    private static final String SECRET_KEY = "secret_key";
    //This is any string we want to use. This will be used for avoiding CSRF attacks. You can generate one here: http://strongpasswordgenerator.com/
    private static final String STATE = "E3ZYKC1T6H2yP4z";
    //This is the url that LinkedIn Auth process will redirect to. We can put whatever we want that starts with http:// or https:// .
//We use a made up url that we will intercept when redirecting. Avoid Uppercases.
    private static final String REDIRECT_URI = "https://www.latitudetechnolabs.com/auth/linkedin/callback";
    /*********************************************/

//These are constants used for build the urls
    //Replace your client id in this url also
    private static final String AUTHORIZATION_URL = "https://www.linkedin.com/oauth/v2/authorization?response_type=code&client_id=yourclientid&redirect_uri=https://www.latitudetechnolabs.com/auth/linkedin/callback&scope=r_emailaddress%20r_liteprofile%20w_member_social";

    private static final String ACCESS_TOKEN_URL = "https://www.linkedin.com/uas/oauth2/accessToken";

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
        setContentView(R.layout.activity_linked_in_login);
        webView = (WebView) findViewById(R.id.main_activity_web_view);
        //Request focus for the webview
        webView.requestFocus(View.FOCUS_DOWN);

        //Show a progress dialog to the user
        pd = ProgressDialog.show(this, "", "Loading...",true);
        //Set a custom web view client
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                //This method will be executed each time a page finished loading.
                //The only we do is dismiss the progressDialog, in case we are showing any.
                if(pd!=null && pd.isShowing()){
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
        webView.loadUrl(authUrl);

    }



    /**
     * Method that generates the url for get the access token from the Service
     *
     * @return Url
     */
    private static String getAccessTokenUrl(String authorizationToken) {
        return ACCESS_TOKEN_URL
                + QUESTION_MARK
                + GRANT_TYPE_PARAM + EQUALS + GRANT_TYPE
                + AMPERSAND
                + RESPONSE_TYPE_VALUE + EQUALS + authorizationToken
                + AMPERSAND
                + CLIENT_ID_PARAM + EQUALS + API_KEY
                + AMPERSAND
                + REDIRECT_URI_PARAM + EQUALS + REDIRECT_URI
                + AMPERSAND
                + SECRET_KEY_PARAM + EQUALS + SECRET_KEY;
    }

    /**
     * Method that generates the url for get the authorization token from the Service
     *
     * @return Url
     */
    private static String getAuthorizationUrl() {
        return AUTHORIZATION_URL + AMPERSAND + STATE_PARAM + EQUALS + STATE;
    }

    @SuppressLint("StaticFieldLeak")
    private class PostRequestAsyncTask extends AsyncTask<String, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            pd = ProgressDialog.show(LinkedInLogin.this, "","Loading...",true);

        }

        @Override
        protected Boolean doInBackground(String... urls) {
            if (urls.length > 0) {
                String url = urls[0];
                HttpClient httpClient = new DefaultHttpClient();
                HttpPost httpost = new HttpPost(url);
                try {
                    HttpResponse response = httpClient.execute(httpost);
                    if (response != null) {
                        //If status is OK 200
                        if (response.getStatusLine().getStatusCode() == 200) {
                            String result = EntityUtils.toString(response.getEntity());
                            //Convert the string result to a JSON Object
                            JSONObject resultJson = new JSONObject(result);
                            //Extract data from JSON Response
                            int expiresIn = resultJson.has("expires_in") ? resultJson.getInt("expires_in") : 0;

                            accessToken = resultJson.has("access_token") ? resultJson.getString("access_token") : null;
                            Log.e("Tokenm", "" + accessToken);
                            if (expiresIn > 0 && accessToken != null) {
                                Log.i("Authorize", "This is the access Token: " + accessToken + ". It will expires in " + expiresIn + " secs");

                                //Calculate date of expiration
                                Calendar calendar = Calendar.getInstance();
                                calendar.add(Calendar.SECOND, expiresIn);
                                long expireDate = calendar.getTimeInMillis();

                                ////Store both expires in and access token in shared preferences
                                SharedPreferences preferences = LinkedInLogin.this.getSharedPreferences("user_info", 0);
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putLong("expires", expireDate);
                                editor.putString("accessToken", accessToken);
                                editor.commit();

                                return true;
                            }
                        }
                    }
                } catch (IOException e) {
                    Log.e("Authorize", "Error Http response " + e.getLocalizedMessage());
                } catch (ParseException e) {
                    Log.e("Authorize", "Error Parsing Http response " + e.getLocalizedMessage());
                } catch (JSONException e) {
                    Log.e("Authorize", "Error Parsing Http response " + e.getLocalizedMessage());
                }
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean status) {
            if(pd!=null && pd.isShowing()){
                pd.dismiss();
            }
            if (status) {

                getClientTokenFromServer();
                //If everything went Ok, change to another activity.
                //  Intent startProfileActivity = new Intent(MainActivity.this, ProfileActivity.class);
                // MainActivity.this.startActivity(startProfileActivity);
            }
        }

    }



    private void getClientTokenFromServer() {
        AsyncHttpClient androidClient = new AsyncHttpClient();
        androidClient.addHeader("Authorization", "Bearer " + accessToken);
        androidClient.get("https://api.linkedin.com/v2/me", new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Log.d("TAG", "failed" + responseString);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseToken) {
                //Log.d("TAG", "Client token: " + responseToken);
                Uri uri = Uri.parse(responseToken);
                try {
                    JSONObject jsonObject = new JSONObject(responseToken);
                    Log.d("TAG", "Client token: " + responseToken);
                    fname = jsonObject.getString("localizedFirstName");
                    lname = jsonObject.getString("localizedLastName");
                    linkedid=jsonObject.getString("id");
                   // txtdata.setText("Firstname :"+fname+"\nLastname :"+lname);
                    getProfileLinkedin();

                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }
        });
    }
    private void getProfileLinkedin() {
        AsyncHttpClient androidClient = new AsyncHttpClient();
        androidClient.addHeader("Authorization", "Bearer " + accessToken);
        androidClient.get("https://api.linkedin.com/v2/me?projection=(id,profilePicture(displayImage~:playableStreams))", new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Log.d("TAG", "failed" + responseString);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseToken) {
                //Log.d("TAG", "Client token: " + responseToken);
                Uri uri = Uri.parse(responseToken);
                try {
                    JSONObject jsonObject = new JSONObject(responseToken);
                    Log.d("TAG", "Client token: " + responseToken);

                    String s=jsonObject.getString("profilePicture");
                    JSONObject jsonObject1=new JSONObject(s);
                    String s1= jsonObject1.getString("displayImage~");
                    JSONObject jsonObject2=new JSONObject(s1);
                    String element=jsonObject2.getString("elements");
                    JSONArray jsonObject3=new JSONArray(element);
                    String identifiers=jsonObject3.getString(3);
                    JSONObject jsonObject4=new JSONObject(identifiers);
                    String img=jsonObject4.getString("identifiers");
                    JSONArray jsonObject5=new JSONArray(img);
                    String str5=jsonObject5.getString(0);
                    JSONObject jsonObject6=new JSONObject(str5);
                     imgurl=jsonObject6.getString("identifier");
                   // Glide.with(MainActivity.this).load(imgurl).into(imgview);

                    Log.d("TAG", "Client token: " + responseToken);
                    webView.setVisibility(View.GONE);
                    getEmailId();




                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }
        });
    }
    private void getEmailId() {
        AsyncHttpClient androidClient = new AsyncHttpClient();
        androidClient.addHeader("Authorization", "Bearer " + accessToken);
        androidClient.get(emailurlv2, new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Log.d("TAG", "failed" + responseString);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseToken) {
                //Log.d("TAG", "Client token: " + responseToken);
                Uri uri = Uri.parse(responseToken);
                try {
                    JSONObject jsonObject = new JSONObject(responseToken);
                    Log.d("TAG", "Client token: " + responseToken);

                    String s=jsonObject.getString("elements");
                    JSONArray jsonObject1=new JSONArray(s);
                    String s1= jsonObject1.getString(0);
                    JSONObject jsonObject2=new JSONObject(s1);
                    String element=jsonObject2.getString("handle~");
                    JSONObject jsonObject3=new JSONObject(element);
                     email=jsonObject3.getString("emailAddress");
                  //  txtemail.setText("Email "+email);

                    // webView.setVisibility(View.GONE);
                    Intent data = new Intent();
                    data.putExtra("firstname",fname);
                    data.putExtra("lastname",lname);
                    data.putExtra("id",linkedid);
                    data.putExtra("imageurl",imgurl);
                    data.putExtra("email",email);


                    setResult(110, data);

                    finish();



                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }
        });
    }



}
