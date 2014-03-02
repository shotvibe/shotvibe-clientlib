package com.shotvibe.shotvibelib;

public abstract class HTTPResponse {
    /**
     * @return The HTTP method that was sent during the request
     */
    public abstract String getMethod();

    /**
     * @return The URL that was requested
     */
    public abstract String getUrl();

    /**
     * @return The time in milliseconds that the request took to complete, or 0 if it was not timed
     */
    public abstract long getRequestTime();

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
     * @param headerName Name of the Header. Must be in all lowercase
     * @return The value of the header, or null if doesn't exist
     */
    public abstract String getHeaderValue(String headerName);
}
