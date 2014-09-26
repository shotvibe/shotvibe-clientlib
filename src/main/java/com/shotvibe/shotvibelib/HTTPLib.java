package com.shotvibe.shotvibelib;

import java.util.Map;

public interface HTTPLib {
    int HTTP_BAD_REQUEST = 400;
    int HTTP_FORBIDDEN = 403;
    int HTTP_GONE = 410;

    /**
     * Send an HTTP Request
     *
     * @param httpMethod The HTTP method
     * @param url The URL
     * @param requestHeaders Any request headers that should be added. May be null
     * @param body The request body. May be null
     * @return The HTTP response from the server
     * @throws HTTPException If there is a networking error and an HTTPResponse cannot be returned
     */
    HTTPResponse sendRequest(String httpMethod, String url, Map<String, String> requestHeaders, String body) throws HTTPException;

    HTTPResponse sendRequest(String httpMethod, String url, Map<String, String> requestHeaders, JSONObject body) throws HTTPException;

    HTTPResponse sendRequest(String httpMethod, String url, Map<String, String> requestHeaders, JSONArray body) throws HTTPException;

    HTTPResponse sendRequestFile(String httpMethod, String url, Map<String, String> requestHeaders, String filePath) throws HTTPException;
}
