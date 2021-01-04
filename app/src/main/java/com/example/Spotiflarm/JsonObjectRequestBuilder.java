package com.example.Spotiflarm;

import android.util.ArrayMap;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * I found it annoying to manually add headers and params to these requests, so use this class to build them for now.
 * JsonObjectRequest only
 */
public class JsonObjectRequestBuilder {

    private StringBuilder url;
    private int paramCount;
    private Map<String,String> headers;
    private Response.Listener<JSONObject> onResponse;
    private Response.ErrorListener onErrorResponse;

    /**
     * Constructor
     * @param url base url to send HTTP request
     * @param accToken token to be used in all spotify requests
     */
    JsonObjectRequestBuilder(String url, String accToken){

        this.url = new StringBuilder(url);
        this.headers = new ArrayMap<String,String>(3);
        this.paramCount = 0;

        // Standard headers

        addHeader("Accept", "application/json");
        addHeader("Content-Type","application/json");
        addHeader("Authorization", "Bearer " + accToken);
    }

    public void setOnResponse(Response.Listener<JSONObject> onResponse){
        this.onResponse = onResponse;
    }

    public void setOnErrorResponse(Response.ErrorListener onErrorResponse){
        this.onErrorResponse = onErrorResponse;
    }

    /**
     * Adds key:value pair to set of headers for the HTTP request
     * @param key name of header
     * @param val value for this header
     */
    public void addHeader(String key, String val){
        headers.put(key, val);
    }


    /**
     * Adds param key=value pair to end of url
     * @param key name of param
     * @param val value of param
     */
    public void addParam(String key, String val){

        // first param needs ? before key=value
        if(paramCount == 0){
            url = url.append("?");
        }
        // every param afterwards needs & before key=value
        else{
            url = url.append("&");
        }

        url = url.append(key + "=" + val);
        paramCount ++;
    }

    /**
     * @return request to be added to RequestQueue
     */
    public JsonObjectRequest build(){

        return new JsonObjectRequest(url.toString(), null, onResponse, onErrorResponse){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return headers;
            }
        };
    }



}
