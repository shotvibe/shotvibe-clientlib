package com.shotvibe.shotvibelib;

import java.util.Map;

public class ShotVibeAPI {
    public static final String BASE_URL = "https://api.shotvibe.com";

    private final HTTPLib mHttpLib;
    private final AuthData mAuthData;
    private final Map<String, String> mJsonRequestHeaders;

    public ShotVibeAPI(HTTPLib httpLib, AuthData authData) {
        if (httpLib == null) {
            throw new IllegalArgumentException("httpLib cannot be null");
        }
        if (authData == null) {
            throw new IllegalArgumentException("authData cannot be null");
        }

        mHttpLib = httpLib;
        mAuthData = authData;

        mJsonRequestHeaders = new HashMap<String, String>();
        setRequestHeaderContentJSON(mJsonRequestHeaders);
        mJsonRequestHeaders.put("Authorization", "Token " + mAuthData.getAuthToken());
    }

    private HTTPResponse sendRequest(String method, String url) throws HTTPException {
        String nullBody = null;
        return mHttpLib.sendRequest(method, BASE_URL + url, mJsonRequestHeaders, nullBody);
    }

    private static void setRequestHeaderContentJSON(Map<String, String> requestHeaders) {
        requestHeaders.put("Content-Type", "application/json");
    }

    public ArrayList<String> photosUploadRequest(int numPhotos) throws APIException {
        if (numPhotos < 1) {
            throw new IllegalArgumentException("numPhotos must be at least 1: " + numPhotos);
        }

        try {
            HTTPResponse response = sendRequest("POST", "/photos/upload_request/?num_photos=" + numPhotos);

            if (response.isError()) {
                throw APIException.ErrorStatusCodeException(response);
            }

            ArrayList<String> result = new ArrayList<String>();

            JSONArray responseArray = null;
            responseArray = response.bodyAsJSONArray();
            for (int i = 0; i < responseArray.length(); ++i) {
                JSONObject photoUploadRequestObj = responseArray.getJSONObject(i);
                String photoId = photoUploadRequestObj.getString("photo_id");
                result.add(photoId);
            }

            return result;
        } catch (JSONException e) {
            throw new APIException(e);
        } catch (HTTPException e) {
            throw new APIException(e);
        }
    }
}
