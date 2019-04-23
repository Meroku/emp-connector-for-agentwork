package com.salesforce.emp.connector.example;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.util.ajax.JSON;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class oAuthSessionProvider {


    public oAuthSessionProvider(String loginHost, String username,
                                String password, String clientId, String secret) {
        try {
            oAuthSessionProvider(loginHost, username, password, clientId, secret);
        } catch (HttpException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void oAuthSessionProvider(String loginHost, String username,
                                            String password, String clientId, String secret)
            throws HttpException, IOException
    {



        // Set up an HTTP client that makes a connection to REST API.
        DefaultHttpClient client = new DefaultHttpClient();
        HttpParams params = client.getParams();
        HttpClientParams.setCookiePolicy(params, CookiePolicy.RFC_2109);
        params.setParameter(HttpConnectionParams.CONNECTION_TIMEOUT, 30000);

        // Set the SID.
        System.out.println("Logging in as " + username + " in environment " + loginHost);
        String baseUrl = loginHost + "/services/oauth2/token";
        // Send a post request to the OAuth URL.
        HttpPost oauthPost = new HttpPost(baseUrl);
        // The request body must contain these 5 values.
        List<BasicNameValuePair> parametersBody = new ArrayList<BasicNameValuePair>();
        parametersBody.add(new BasicNameValuePair("grant_type", "password"));
        parametersBody.add(new BasicNameValuePair("username", username));
        parametersBody.add(new BasicNameValuePair("password", password));
        parametersBody.add(new BasicNameValuePair("client_id", clientId));
        parametersBody.add(new BasicNameValuePair("client_secret", secret));
        oauthPost.setEntity(new UrlEncodedFormEntity(parametersBody, HTTP.UTF_8));

        // Execute the request.
        System.out.println("POST " + baseUrl + "...\n");
        HttpResponse response = client.execute(oauthPost);
        int code = response.getStatusLine().getStatusCode();
        Map<String, String> oauthLoginResponse = (Map<String, String>)
                JSON.parse(EntityUtils.toString(response.getEntity()));
        System.out.println("OAuth login response");
        for (Map.Entry<String, String> entry : oauthLoginResponse.entrySet())
        {
            System.out.println(String.format("  %s = %s", entry.getKey(), entry.getValue()));
        }
        System.out.println("");

        // Get user info.
        String userIdEndpoint = oauthLoginResponse.get("id");
        String accessToken = oauthLoginResponse.get("access_token");
        List<BasicNameValuePair> qsList = new ArrayList<BasicNameValuePair>();
        qsList.add(new BasicNameValuePair("oauth_token", accessToken));
        String queryString = URLEncodedUtils.format(qsList, HTTP.UTF_8);
        HttpGet userInfoRequest = new HttpGet(userIdEndpoint + "?" + queryString);
        HttpResponse userInfoResponse = client.execute(userInfoRequest);
        Map<String, Object> userInfo = (Map<String, Object>)
                JSON.parse(EntityUtils.toString(userInfoResponse.getEntity()));
        System.out.println("User info response");
        for (Map.Entry<String, Object> entry : userInfo.entrySet())
        {
            System.out.println(String.format("  %s = %s", entry.getKey(), entry.getValue()));
        }
        System.out.println("");

        // Use the user info in interesting ways.
        System.out.println("Username is " + userInfo.get("username"));
        System.out.println("User's email is " + userInfo.get("email"));
        Map<String, String> urls = (Map<String, String>)userInfo.get("urls");
        System.out.println("REST API url is " + urls.get("rest").replace("{version}", "45.0"));
    }

}
