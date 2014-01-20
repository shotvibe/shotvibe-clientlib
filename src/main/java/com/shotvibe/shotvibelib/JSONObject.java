package com.shotvibe.shotvibelib;

public final class JSONObject {
    /**
     * Creates an empty JSONObject
     */
    public JSONObject() {
        mObj = new org.json.JSONObject();
    }

    public boolean isNull(String key) throws JSONException {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }

        if (!mObj.has(key)) {
            throw new JSONException("Missing key \"" + key + "\"");
        }
        return mObj.isNull(key);
    }

    public boolean getBoolean(String key) throws JSONException {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }

        try {
            return mObj.getBoolean(key);
        } catch (org.json.JSONException e) {
            throw new JSONException(e);
        }
    }

    public double getDouble(String key) throws JSONException {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }

        try {
            return mObj.getDouble(key);
        } catch (org.json.JSONException e) {
            throw new JSONException(e);
        }
    }

    public int getInt(String key) throws JSONException {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }

        try {
            return mObj.getInt(key);
        } catch (org.json.JSONException e) {
            throw new JSONException(e);
        }
    }

    public long getLong(String key) throws JSONException {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }

        try {
            return mObj.getLong(key);
        } catch (org.json.JSONException e) {
            throw new JSONException(e);
        }
    }

    public String getString(String key) throws JSONException {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }

        try {
            return mObj.getString(key);
        } catch (org.json.JSONException e) {
            throw new JSONException(e);
        }
    }

    public JSONArray getJSONArray(String key) throws JSONException {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }

        try {
            org.json.JSONArray array = mObj.getJSONArray(key);
            return new JSONArray(array);
        } catch (org.json.JSONException e) {
            throw new JSONException(e);
        }
    }

    public JSONObject getJSONObject(String key) throws JSONException {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }

        try {
            org.json.JSONObject obj = mObj.getJSONObject(key);
            return new JSONObject(obj);
        } catch (org.json.JSONException e) {
            throw new JSONException(e);
        }
    }

    public JSONObject putNull(String key) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }

        try {
            mObj.putOpt(key, null);
        } catch (org.json.JSONException e) {
            throw new RuntimeException("Impossible Happened", e);
        }

        return this;
    }

    public JSONObject put(String key, boolean value) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }

        try {
            mObj.put(key, value);
        } catch (org.json.JSONException e) {
            throw new RuntimeException("Impossible Happened", e);
        }

        return this;
    }

    public JSONObject put(String key, double value) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }

        if (Double.isInfinite(value) || Double.isNaN(value)) {
            throw new IllegalArgumentException("Forbidden numeric value: " + value);
        }

        try {
            mObj.put(key, value);
        } catch (org.json.JSONException e) {
            throw new RuntimeException("Impossible Happened", e);
        }

        return this;
    }

    public JSONObject put(String key, int value) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }

        try {
            mObj.put(key, value);
        } catch (org.json.JSONException e) {
            throw new RuntimeException("Impossible Happened", e);
        }

        return this;
    }

    public JSONObject put(String key, long value) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }

        try {
            mObj.put(key, value);
        } catch (org.json.JSONException e) {
            throw new RuntimeException("Impossible Happened", e);
        }

        return this;
    }

    public JSONObject put(String key, String value) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }

        try {
            mObj.put(key, value);
        } catch (org.json.JSONException e) {
            throw new RuntimeException("Impossible Happened", e);
        }

        return this;
    }

    public JSONObject put(String key, JSONObject value) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }

        try {
            mObj.put(key, value.mObj);
        } catch (org.json.JSONException e) {
            throw new RuntimeException("Impossible Happened", e);
        }

        return this;
    }

    public JSONObject put(String key, JSONArray value) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }

        try {
            mObj.put(key, value.mArray);
        } catch (org.json.JSONException e) {
            throw new RuntimeException("Impossible Happened", e);
        }

        return this;
    }

    /**
     * This does not have an Objective-C implementation, and should therefore only be called from
     * native Java code that is not meant to be cross-platform
     *
     * @param data JSON string
     * @return JSONObject
     * @throws JSONException If the string is not valid JSON, or not a JSON object
     */
    public static JSONObject Parse(String data) throws JSONException {
        if (data == null) {
            throw new IllegalArgumentException("data cannot be null");
        }

        try {
            return new JSONObject(new org.json.JSONObject(data));
        } catch (org.json.JSONException e) {
            throw new JSONException(e);
        }
    }

    /**
     * Package Level visibility. Only needs to be called from JSONArray
     *
     * @param obj
     */
    JSONObject(org.json.JSONObject obj) {
        mObj = obj;
    }

    /**
     * Package Level visibility. Only needs to be accessed from JSONArray
     */
    org.json.JSONObject mObj;
}
