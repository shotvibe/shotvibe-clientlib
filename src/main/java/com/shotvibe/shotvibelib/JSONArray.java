package com.shotvibe.shotvibelib;

public class JSONArray {
    /**
     * Creates an empty JSONArray
     */
    public JSONArray() {
        mArray = new org.json.JSONArray();
    }

    public int length() {
        return mArray.length();
    }

    public boolean isNull(int index) throws JSONException {
        if (index < 0 || index >= length()) {
            throw new JSONException("Index " + index + " out of range [0.." + length() + ")");
        }

        return mArray.isNull(index);
    }

    public boolean getBoolean(int index) throws JSONException {
        try {
            return mArray.getBoolean(index);
        } catch (org.json.JSONException e) {
            throw new JSONException(e);
        }
    }

    public double getDouble(int index) throws JSONException {
        try {
            return mArray.getDouble(index);
        } catch (org.json.JSONException e) {
            throw new JSONException(e);
        }
    }

    public int getInt(int index) throws JSONException {
        try {
            return mArray.getInt(index);
        } catch (org.json.JSONException e) {
            throw new JSONException(e);
        }
    }

    public long getLong(int index) throws JSONException {
        try {
            return mArray.getLong(index);
        } catch (org.json.JSONException e) {
            throw new JSONException(e);
        }
    }

    public String getString(int index) throws JSONException {
        try {
            return mArray.getString(index);
        } catch (org.json.JSONException e) {
            throw new JSONException(e);
        }
    }

    public JSONArray getJSONArray(int index) throws JSONException {
        try {
            org.json.JSONArray array = mArray.getJSONArray(index);
            return new JSONArray(array);
        } catch (org.json.JSONException e) {
            throw new JSONException(e);
        }
    }

    public JSONObject getJSONObject(int index) throws JSONException {
        org.json.JSONObject obj = null;
        try {
            obj = mArray.getJSONObject(index);
            return new JSONObject(obj);
        } catch (org.json.JSONException e) {
            throw new JSONException(e);
        }
    }

    public JSONArray putNull() {
        mArray.put(null);
        return this;
    }

    public JSONArray put(boolean value) {
        mArray.put(value);
        return this;
    }

    public JSONArray put(double value) {
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            throw new IllegalArgumentException("Forbidden numeric value: " + value);
        }

        try {
            mArray.put(value);
        } catch (org.json.JSONException e) {
            throw new RuntimeException("Impossible Happened", e);
        }
        return this;
    }

    public JSONArray put(int value) {
        mArray.put(value);
        return this;
    }

    public JSONArray put(long value) {
        mArray.put(value);
        return this;
    }

    public JSONArray put(String value) {
        if (value == null) {
            throw new IllegalArgumentException("value cannot be null");
        }

        mArray.put(value);
        return this;
    }

    public JSONArray put(JSONObject value) {
        if (value == null) {
            throw new IllegalArgumentException("value cannot be null");
        }

        mArray.put(value.mObj);
        return this;
    }

    public JSONArray put(String key, JSONArray value) {
        if (value == null) {
            throw new IllegalArgumentException("value cannot be null");
        }

        mArray.put(value.mArray);
        return this;
    }


    /**
     * This does not have an Objective-C implementation, and should therefore only be called from
     * native Java code that is not meant to be cross-platform
     *
     * @param data JSON string
     * @return JSONArray
     * @throws JSONException If the string is not valid JSON, or not a JSON array
     */
    public static JSONArray Parse(String data) throws JSONException {
        if (data == null) {
            throw new IllegalArgumentException("data cannot be null");
        }

        try {
            return new JSONArray(new org.json.JSONArray(data));
        } catch (org.json.JSONException e) {
            throw new JSONException(e);
        }
    }

    /**
     * Returns JSON text, in compact format (no whitespace)
     *
     * @return JSON text
     */
    @Override
    public String toString() {
        return mArray.toString();
    }

    /**
     * Package Level visibility. Only needs to be called from JSONObject
     *
     * @param array
     */
    JSONArray(org.json.JSONArray array) {
        mArray = array;
    }

    /**
     * Package Level visibility. Only needs to be accessed from JSONObject
     */
    org.json.JSONArray mArray;
}
