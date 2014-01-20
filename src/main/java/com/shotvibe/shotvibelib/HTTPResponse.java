package com.shotvibe.shotvibelib;

public abstract class HTTPResponse {

    /**
     * HTTP status code
     *
     * @return the HTTP status code returned from the server
     */
    public abstract int getStatusCode();

    /**
     * Check if the server returned an error response
     *
     * @return return true if: status code >= 400
     */
    public final boolean isError() {
        return getStatusCode() >= 400;
    }

    /**
     *
     *
     * @return
     * @throws JSONException
     */
    public abstract JSONObject bodyAsJSONObject() throws JSONException;

    public abstract JSONArray bodyAsJSONArray() throws JSONException;

    public abstract String bodyAsUTF8String();

    /**
     * Get HTTP Header
     *
     * @param headerName Name of the Header
     * @return The value of the header, or null if doesn't exist
     */
    public abstract String getHeaderValue(String headerName);
}
