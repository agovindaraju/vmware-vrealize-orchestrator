package com.vmware.vro.jenkins.plugin.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Created by agovindaraju on 1/9/2016.
 */
public class RestClient {

    private static final String ACCEPT = "Accept";
    private static final String ACCEPT_CONTENT_TYPE = "application/json";
    private static final String AUTHORIZATION = "Authorization";
    private static final String LOCATION_HEADER = "Location";

    //Auth related field
    private static final String USER_NAME = "username";
    private static final String PASSWORD = "password";
    private static final String TENANT = "tenant";
    private static final String ID = "id";

    private final String serverUrl;
    private final String userName;
    private final String password;
    private final String tenant;

    public RestClient(String serverUrl, String userName, String password, String tenant) {
        this.serverUrl = serverUrl;
        this.userName = userName;
        this.password = password;
        this.tenant = tenant;
    }

    /**
     * Performs an HttpGet connection to the server and returns the result a JSON formatted string.
     */
    public String httpGet(String requestUrl)
            throws IOException, URISyntaxException, NoSuchAlgorithmException, KeyStoreException,
            KeyManagementException {

        URL url = new URL(requestUrl);
        HttpGet httpGet = new HttpGet(url.toURI());
        Map<String, String> headers = getRequestHeaders();
        HttpResponse response = executeRequest(httpGet, headers);

        return parseResponse(response);
    }

    /**
     * Performs an HttpPost connection to the server and returns the result a JSON formatted string.
     */
    public String httpPostForLocationHeader(String requestUrl, String payload)
            throws IOException, URISyntaxException, NoSuchAlgorithmException, KeyStoreException,
            KeyManagementException {
        URL url = new URL(requestUrl);
        HttpPost httpPost = new HttpPost(url.toURI());

        if (payload != null) {
            StringEntity postEntity = new StringEntity(payload, StandardCharsets.UTF_8);
            postEntity.setContentType(ACCEPT_CONTENT_TYPE);
            httpPost.setEntity(postEntity);
        }
        Map<String, String> headers = getRequestHeaders();
        HttpResponse response = executeRequest(httpPost, headers);

        if (response != null) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_ACCEPTED) {
                //Get location header
                return parseResponseLocationHeader(response).get(0);
            } else {
                throw new IOException("Server responded with status code " + response.getStatusLine());
            }
        }
        return null;
    }

    public String httpPost(String requestUrl, String payload)
            throws IOException, URISyntaxException, NoSuchAlgorithmException, KeyStoreException,
            KeyManagementException {
        URL url = new URL(requestUrl);
        HttpPost httpPost = new HttpPost(url.toURI());

        if (payload != null) {
            StringEntity postEntity = new StringEntity(payload, StandardCharsets.UTF_8);
            postEntity.setContentType(ACCEPT_CONTENT_TYPE);
            httpPost.setEntity(postEntity);
        }
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(ACCEPT, ACCEPT_CONTENT_TYPE);
        HttpResponse response = executeRequest(httpPost, headers);

        return parseResponse(response);
    }

    /*
     * Executes the http request to the server
     */
    private HttpResponse executeRequest(HttpUriRequest uriRequest, Map<String, String> headers)
            throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
        if (headers != null && headers.size() > 0) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                uriRequest.setHeader(entry.getKey(), entry.getValue());
            }
        }
        return getHttpClient().execute(uriRequest);
    }

    private CloseableHttpClient getHttpClient() throws NoSuchAlgorithmException, KeyStoreException,
            KeyManagementException {
        SSLContextBuilder builder = new SSLContextBuilder();
        builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
        SSLConnectionSocketFactory connectionSocketFactory = new SSLConnectionSocketFactory(builder.build());
        return HttpClients.custom().setSSLSocketFactory(connectionSocketFactory).build();
    }

    private Map<String, String> getRequestHeaders()
            throws IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException,
            URISyntaxException {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(ACCEPT, ACCEPT_CONTENT_TYPE);
        if (StringUtils.isBlank(tenant)) {
            headers.put(AUTHORIZATION, "Basic " + constructAuthorizationHeader());
        } else {
            //Token based auth
            headers.put(AUTHORIZATION, "Bearer " + getAuthToken());
        }
        return headers;
    }

    private String getAuthToken()
            throws URISyntaxException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException,
            IOException {
        String requestUrl = String.format("%s/identity/api/tokens", serverUrl);

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(USER_NAME, userName);
        jsonObject.addProperty(PASSWORD, password);
        jsonObject.addProperty(TENANT, tenant);
        String requestPayload = jsonObject.toString();

        String authResponse = httpPost(requestUrl, requestPayload);
        JsonObject authResponseJson = getJsonObject(authResponse);
        return authResponseJson.get(ID).getAsString();
    }

    private JsonObject getJsonObject(String response) {
        JsonElement responseJson = new JsonParser().parse(response);
        return responseJson.getAsJsonObject();
    }

    private String constructAuthorizationHeader() throws UnsupportedEncodingException {
        String authentication = userName + ':' + password;
        return new String(Base64.encodeBase64(authentication.getBytes("UTF-8")));
    }

    /*
     * Parses the response from the server.
     */
    private String parseResponse(HttpResponse response) throws IOException {
        HttpEntity entity = null;
        String result = null;
        try {
            if (response != null) {
                entity = response.getEntity();
                if (entity.getContent() != null) {
                    StringBuilder sb = new StringBuilder();
                    InputStream stream = entity.getContent();
                    BufferedReader br = null;
                    try {
                        br = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                        }
                    } finally {
                        if (br != null) {
                            br.close();
                        }
                        if (stream != null) {
                            stream.close();
                        }
                    }
                    result = sb.toString();
                }
                return result;
            }
        } finally {
            consumeEntity(entity);
        }
        return result;
    }

    private void consumeEntity(HttpEntity entity) throws IOException {
        if (entity != null) {
            EntityUtils.consume(entity);
        }
    }

    /**
     * Parses the response headers and constructs a list of resource URI form the location header.
     */
    private List<String> parseResponseLocationHeader(HttpResponse response) throws IOException {
        List<String> locationHeaders = new ArrayList<String>();
        try {
            Header[] headers = response.getHeaders(LOCATION_HEADER);
            for (Header header : headers) {
                locationHeaders.add(header.getValue());
            }
        } finally {
            if (response != null) {
                consumeEntity(response.getEntity());
            }
        }
        return locationHeaders;
    }
}
