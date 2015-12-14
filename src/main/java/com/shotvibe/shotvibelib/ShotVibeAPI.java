package com.shotvibe.shotvibelib;

import java.util.List;
import java.util.Map;

public class ShotVibeAPI {
    public static final boolean useStagingServer = false;
    public static final String BASE_URL = useStagingServer ? "https://staging-api.shotvibe.com" : "https://api.shotvibe.com";
    public static final String BASE_UPLOAD_URL = useStagingServer ? "https://staging-upload.shotvibe.com" : "https://upload.shotvibe.com";
    // TODO: BASE_UPLOAD_URL is temporary, until we use the urls returned by the server on a "POST /photos/upload_request"

    private final HTTPLib mHttpLib;
    private final NetworkStatusManager mNetworkStatusManager;
    private final AuthData mAuthData;
    private final Map<String, String> mJsonRequestHeaders;

    public ShotVibeAPI(HTTPLib httpLib, NetworkStatusManager networkStatusManager, AuthData authData) {
        if (httpLib == null) {
            throw new IllegalArgumentException("httpLib cannot be null");
        }
        if (networkStatusManager == null) {
            throw new IllegalArgumentException("networkStatusManager cannot be null");
        }
        if (authData == null) {
            throw new IllegalArgumentException("authData cannot be null");
        }

        mHttpLib = httpLib;
        mNetworkStatusManager = networkStatusManager;
        mAuthData = authData;

        mJsonRequestHeaders = new HashMap<String, String>();
        setRequestHeaderContentJSON(mJsonRequestHeaders);
        mJsonRequestHeaders.put("Authorization", "Token " + mAuthData.getAuthToken());
    }

    public AuthData getAuthData() {
        return mAuthData;
    }

    private static DateTime parseDate(JSONObject obj, String field) throws JSONException {
        String input = obj.getString(field);
        DateTime result = DateTime.ParseISO8601(input);
        if (result == null) {
            throw new JSONException("Field `" + field + "` contains an invalid date value: " + input);
        }
        return result;
    }

    private HTTPResponse sendRequest(String method, String url) throws HTTPException {
        return mHttpLib.sendRequest(method, BASE_URL + url, mJsonRequestHeaders, (String) null);
    }

    private HTTPResponse sendRequest(String method, String url, JSONObject jsonObj) throws HTTPException {
        return mHttpLib.sendRequest(method, BASE_URL + url, mJsonRequestHeaders, jsonObj);
    }

    private static void setRequestHeaderContentJSON(Map<String, String> requestHeaders) {
        requestHeaders.put("Content-Type", "application/json");
    }

    private interface NetworkRequestAction<T> {
        static class NetworkRequestResult<T> {
            /**
             * Create a NetworkRequestResult with a single successful HTTPResponse
             *
             * @param result   The result that should be returned to the caller. May be null if the
             *                 action is allowed to return a null result
             * @param response The successful HTTPResponse (that may be logged)
             */
            public NetworkRequestResult(T result, HTTPResponse response) {
                if (response == null) {
                    throw new IllegalArgumentException("response cannot be null");
                }
                mResult = result;
                mResponses = new ArrayList<HTTPResponse>(1);
                mResponses.add(response);
            }

            /**
             * Create a NetworkRequestResult with 2 successful HTTPResponses
             *
             * @param result    The result that should be returned to the caller. May be null if the
             *                  action is allowed to return a null result
             * @param response1 The first successful HTTPResponse (that may be logged)
             * @param response2 The second successful HTTPResponse (that may be logged)
             */
            public NetworkRequestResult(T result, HTTPResponse response1, HTTPResponse response2) {
                if (response1 == null) {
                    throw new IllegalArgumentException("response1 cannot be null");
                }
                if (response2 == null) {
                    throw new IllegalArgumentException("response2 cannot be null");
                }
                mResult = result;
                mResponses = new ArrayList<HTTPResponse>(2);
                mResponses.add(response1);
                mResponses.add(response2);
            }

            public T getResult() {
                return mResult;
            }

            public ArrayList<HTTPResponse> getResponses() {
                return mResponses;
            }

            private T mResult;
            private ArrayList<HTTPResponse> mResponses;
        }

        /**
         * Runs an action that performs a network request, that can later be logged
         *
         * @return Must not return null
         * @throws APIException
         * @throws HTTPException
         */
        NetworkRequestResult<T> runAction() throws APIException, HTTPException;
    }

    private <T> T runAndLogNetworkRequestAction(NetworkRequestAction<T> action) throws APIException {
        try {
            NetworkRequestAction.NetworkRequestResult<T> result = action.runAction();
            if (result == null) {
                throw new IllegalStateException("NetworkRequestAction.runAction returned null: " + action);
            }
            for (HTTPResponse successfulResponse : result.getResponses()) {
                mNetworkStatusManager.logNetworkRequest(successfulResponse);
            }
            return result.getResult();
        } catch (APIException e) {
            mNetworkStatusManager.logNetworkRequestFailure(e);
            throw e;
        } catch (HTTPException e) {
            APIException error = APIException.FromHttpException(e);
            mNetworkStatusManager.logNetworkRequestFailure(error);
            throw error;
        }
    }

    public static final class SMSConfirmationToken {
        public String serialize() {
            JSONObject data = new JSONObject();
            data.put("confirmationKey", confirmationKey);
            data.put("defaultCountry", defaultCountry);
            data.put("deviceDescription", deviceDescription);
            if (customPayload == null) {
                data.putNull("customPayload");
            } else {
                data.put("customPayload", customPayload);
            }

            return data.toString();
        }

        public static SMSConfirmationToken Deserialize(String serialization) {
            JSONObject obj;
            try {
                obj = JSONObject.Parse(serialization);
                String confirmationKey = obj.getString("confirmationKey");
                String defaultCountry = obj.getString("defaultCountry");
                String deviceDescription = obj.getString("deviceDescription");
                String customPayload;
                if (obj.isNull("customPayload")) {
                    customPayload = null;
                } else {
                    customPayload = obj.getString("customPayload");
                }
                return new SMSConfirmationToken(confirmationKey, defaultCountry, deviceDescription, customPayload);
            } catch (JSONException e) {
                throw new RuntimeException("serialization: " + serialization, e);
            }
        }

        private SMSConfirmationToken(String confirmationKey, String defaultCountry, String deviceDescription, String customPayload) {
            this.confirmationKey = confirmationKey;
            this.defaultCountry = defaultCountry;
            this.deviceDescription = deviceDescription;
            this.customPayload = customPayload;
        }

        private final String confirmationKey;
        private final String defaultCountry;
        private final String deviceDescription;
        private final String customPayload;
    }

    /**
     *
     * @param defaultCountry 2-letter country code
     * @param deviceDescription Manufacturer, Name, Model, Version, etc. of the user's device
     * @param customPayload Query param that was read from the App custom URL scheme. May be null.
     * @return Returns null if the phone number is not a valid phone number.
     * @throws APIException
     */
    public static SMSConfirmationToken authorizePhoneNumber(HTTPLib httpLib, String phoneNumber, String defaultCountry, String deviceDescription, String customPayload) throws APIException {
        if (httpLib == null) {
            throw new IllegalArgumentException("httpLib cannot be null");
        }
        if (phoneNumber == null) {
            throw new IllegalArgumentException("phoneNumber cannot be null");
        }
        if (defaultCountry == null) {
            throw new IllegalArgumentException("defaultCountry cannot be null");
        }
        if (deviceDescription == null) {
            throw new IllegalArgumentException("deviceDescription cannot be null");
        }
        if (defaultCountry.length() != 2) {
            throw new IllegalArgumentException("defaultCountry must be a 2-letter country code");
        }

        JSONObject body = new JSONObject();
        body.put("phone_number", phoneNumber);
        body.put("default_country", defaultCountry);

        Map<String, String> requestHeaders = new HashMap<String, String>();
        setRequestHeaderContentJSON(requestHeaders);
        HTTPResponse response;
        try {
            response = httpLib.sendRequest("POST", BASE_URL + "/auth/authorize_phone_number/", requestHeaders, body);
        } catch (HTTPException e) {
            throw APIException.FromHttpException(e);
        }

        // We want to explicitly check for this error code:
        // It means that the phone number was invalid
        if (response.getStatusCode() == HTTPLib.HTTP_BAD_REQUEST) {
            return null;
        }

        if (response.isError()) {
            throw APIException.ErrorStatusCodeException(response);
        }

        try {
            JSONObject responseObj = response.bodyAsJSONObject();
            String confirmationKey = responseObj.getString("confirmation_key");
            return new SMSConfirmationToken(confirmationKey, defaultCountry, deviceDescription, customPayload);
        } catch (JSONException e) {
            throw APIException.FromJSONException(response, e);
        }
    }

    /**
     *
     * @param smsConfirmationToken Result of {@link ShotVibeAPI#authorizePhoneNumber}
     * @param confirmationCode code that the user entered
     * @return Returns null if the confirmation code that the user entered was incorrect.
     * @throws APIException
     */
    public static AuthData confirmSMSCode(HTTPLib httpLib, SMSConfirmationToken smsConfirmationToken, String confirmationCode) throws APIException {
        if (httpLib == null) {
            throw new IllegalArgumentException("httpLib cannot be null");
        }
        if (smsConfirmationToken == null) {
            throw new IllegalArgumentException("smsConfirmationToken cannot be null");
        }
        if (confirmationCode == null) {
            throw new IllegalArgumentException("confirmationCode cannot be null");
        }

        JSONObject body = new JSONObject();
        body.put("confirmation_code", confirmationCode);
        body.put("device_description", smsConfirmationToken.deviceDescription);

        String endPoint;
        if (smsConfirmationToken.customPayload != null) {
            endPoint = "/auth/confirm_sms_code/" + smsConfirmationToken.confirmationKey + "/?custom_payload=" + smsConfirmationToken.customPayload;
        } else {
            endPoint = "/auth/confirm_sms_code/" + smsConfirmationToken.confirmationKey + "/";
        }

        Map<String, String> requestHeaders = new HashMap<String, String>();
        setRequestHeaderContentJSON(requestHeaders);
        HTTPResponse response;
        try {
            response = httpLib.sendRequest("POST", BASE_URL + endPoint, requestHeaders, body);
        } catch (HTTPException e) {
            throw APIException.FromHttpException(e);
        }

        // We want to explicitly check for this error code:
        // It means that the SMS Code was incorrect
        if (response.getStatusCode() == HTTPLib.HTTP_FORBIDDEN) {
            return null;
        } else if (response.getStatusCode() == HTTPLib.HTTP_GONE) {
            // This error code means that the confirmation_key has expired.

            // TODO We should call authorizePhoneNumber again here and then
            // recursively try calling confirmSMSCode again
            throw new RuntimeException("confirmation_key expired");
        }

        if (response.isError()) {
            throw APIException.ErrorStatusCodeException(response);
        }

        try {
            JSONObject responseObj = response.bodyAsJSONObject();
            long userId = responseObj.getLong("user_id");
            String authToken = responseObj.getString("auth_token");

            return new AuthData(userId, authToken, smsConfirmationToken.defaultCountry);
        } catch (JSONException e) {
            throw APIException.FromJSONException(response, e);
        }
    }

    public void registerDevicePushAndroid(final String app, final String registrationId) throws APIException {
        runAndLogNetworkRequestAction(new NetworkRequestAction<Void>() {
            @Override
            public NetworkRequestResult<Void> runAction() throws APIException, HTTPException {
                JSONObject body = new JSONObject();
                body.put("type", "gcm");
                body.put("app", app);
                body.put("registration_id", registrationId);

                HTTPResponse response = sendRequest("POST", "/register_device_push/", body);

                if (response.isError()) {
                    throw APIException.ErrorStatusCodeException(response);
                }

                return new NetworkRequestResult<Void>(null, response);
            }
        });
    }

    public void registerDevicePushIOS(final String app, final String deviceToken) throws APIException {
        runAndLogNetworkRequestAction(new NetworkRequestAction<Void>() {
            @Override
            public NetworkRequestResult<Void> runAction() throws APIException, HTTPException {
                JSONObject body = new JSONObject();
                body.put("type", "apns");
                body.put("app", app);
                body.put("device_token", deviceToken);

                HTTPResponse response = sendRequest("POST", "/register_device_push/", body);

                if (response.isError()) {
                    throw APIException.ErrorStatusCodeException(response);
                }

                return new NetworkRequestResult<Void>(null, response);
            }
        });
    }

    public AwsToken getAwsToken() throws APIException {
        return runAndLogNetworkRequestAction(new NetworkRequestAction<AwsToken>() {
            @Override
            public NetworkRequestResult<AwsToken> runAction() throws APIException, HTTPException {
                HTTPResponse response = sendRequest("POST", "/auth/aws_token/");

                if (response.isError()) {
                    throw APIException.ErrorStatusCodeException(response);
                }

                try {
                    JSONObject responseObj = response.bodyAsJSONObject();

                    String awsAccessKey = responseObj.getString("aws_access_key");
                    String awsSecretKey = responseObj.getString("aws_secret_key");
                    String awsSessionToken = responseObj.getString("aws_session_token");
                    DateTime expires = parseDate(responseObj, "expires");

                    AwsToken token = new AwsToken(awsAccessKey, awsSecretKey, awsSessionToken, expires);
                    return new NetworkRequestResult<AwsToken>(token, response);
                } catch (JSONException e) {
                    throw APIException.FromJSONException(response, e);
                }
            }
        });
    }

    public AlbumUser getUserProfile(final long userId) throws APIException {
        return runAndLogNetworkRequestAction(new NetworkRequestAction<AlbumUser>() {
            @Override
            public NetworkRequestResult<AlbumUser> runAction() throws APIException, HTTPException {
                HTTPResponse response = sendRequest("GET", "/users/" + userId + "/");

                if (response.isError()) {
                    throw APIException.ErrorStatusCodeException(response);
                }

                try {
                    JSONObject responseObj = response.bodyAsJSONObject();

                    long memberId = responseObj.getLong("id");
                    String nickname = responseObj.getString("nickname");
                    String avatarUrl = responseObj.getString("avatar_url");

                    AlbumUser user = new AlbumUser(memberId, nickname, avatarUrl);
                    return new NetworkRequestResult<AlbumUser>(user, response);
                } catch (JSONException e) {
                    throw APIException.FromJSONException(response, e);
                }
            }
        });
    }

    public int getUserGlanceScore(final long userId) throws APIException {
        return runAndLogNetworkRequestAction(new NetworkRequestAction<Integer>() {
            @Override
            public NetworkRequestResult<Integer> runAction() throws APIException, HTTPException {
                HTTPResponse response = sendRequest("GET", "/users/" + userId + "/glance_score/");

                if (response.isError()) {
                    throw APIException.ErrorStatusCodeException(response);
                }

                try {
                    JSONObject responseObj = response.bodyAsJSONObject();

                    int glanceScore = responseObj.getInt("user_glance_score");

                    return new NetworkRequestResult<Integer>(glanceScore, response);
                } catch (JSONException e) {
                    throw APIException.FromJSONException(response, e);
                }
            }
        });
    }

    public void setUserNickname(final long userId, final String nickname) throws APIException {
        runAndLogNetworkRequestAction(new NetworkRequestAction<Void>() {
            @Override
            public NetworkRequestResult<Void> runAction() throws APIException, HTTPException {
                JSONObject body = new JSONObject();
                body.put("nickname", nickname);

                HTTPResponse response = sendRequest("PATCH", "/users/" + userId + "/", body);

                if (response.isError()) {
                    throw APIException.ErrorStatusCodeException(response);
                }

                return new NetworkRequestResult<Void>(null, response);
            }
        });
    }

    public void uploadUserAvatar(final long userId, final String filePath) throws APIException {
        runAndLogNetworkRequestAction(new NetworkRequestAction<Void>() {
            @Override
            public NetworkRequestResult<Void> runAction() throws APIException, HTTPException {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/octet-stream");
                headers.put("Authorization", "Token " + mAuthData.getAuthToken());
                HTTPResponse response = mHttpLib.sendRequestFile("PUT", BASE_URL + "/users/" + userId + "/avatar/", headers, filePath);

                if (response.isError()) {
                    throw APIException.ErrorStatusCodeException(response);
                }

                return new NetworkRequestResult<Void>(null, response);
            }
        });
    }

    public static class AlbumMemberPhoneNumber {
        public AlbumMemberPhoneNumber(AlbumUser user, String phoneNumber) {
            if (user == null) {
                throw new IllegalArgumentException("user cannot be null");
            }
            if (phoneNumber == null) {
                throw new IllegalArgumentException("phoneNumber cannot be null");
            }

            mUser = user;
            mPhoneNumber = phoneNumber;
        }

        public AlbumUser getUser() {
            return mUser;
        }

        public String getPhoneNumber() {
            return mPhoneNumber;
        }

        private final AlbumUser mUser;
        private final String mPhoneNumber;
    }

    public AlbumMemberPhoneNumber getAlbumMemberPhoneNumber(final long albumId, final long userId) throws APIException {
        return runAndLogNetworkRequestAction(new NetworkRequestAction<AlbumMemberPhoneNumber>() {
            @Override
            public NetworkRequestResult<AlbumMemberPhoneNumber> runAction() throws APIException, HTTPException {
                HTTPResponse response = sendRequest("GET", "/albums/" + albumId + "/members/" + userId + "/phone_number/");

                if (response.isError()) {
                    throw APIException.ErrorStatusCodeException(response);
                }

                try {
                    JSONObject responseObj = response.bodyAsJSONObject();

                    AlbumUser user = parseAlbumUser(responseObj.getJSONObject("user"));
                    String phoneNumber = responseObj.getString("phone_number");

                    AlbumMemberPhoneNumber result = new AlbumMemberPhoneNumber(user, phoneNumber);
                    return new NetworkRequestResult<AlbumMemberPhoneNumber>(result, response);
                } catch (JSONException e) {
                    throw APIException.FromJSONException(response, e);
                }
            }
        });
    }

    private static AlbumUser parseAlbumUser(JSONObject userObj) throws JSONException {
        long id = userObj.getLong("id");
        String nickname = userObj.getString("nickname");
        String avatarUrl = userObj.getString("avatar_url");

        return new AlbumUser(id, nickname, avatarUrl);
    }

    private ArrayList<AlbumPhoto> parsePhotoList(JSONArray photos_array) throws JSONException {
        ArrayList<AlbumPhoto> result = new ArrayList<AlbumPhoto>();
        for (int i = 0; i < photos_array.length(); ++i) {
            JSONObject photo_obj = photos_array.getJSONObject(i);
            String photo_id = photo_obj.getString("photo_id");

            String mediaTypeStr = photo_obj.getString("media_type");
            MediaType mediaType;
            if (mediaTypeStr == "photo") {
                mediaType = MediaType.PHOTO;
            } else if (mediaTypeStr == "video") {
                mediaType = MediaType.VIDEO;
            } else {
                throw new JSONException("Invalid `media_type` value: " + mediaTypeStr);
            }

            AlbumServerVideo video = null;
            if (mediaType == MediaType.VIDEO) {
                String statusStr = photo_obj.getString("video_status");
                AlbumServerVideo.Status status;
                if (statusStr == "ready") {
                    status = AlbumServerVideo.Status.READY;
                } else if (statusStr == "processing") {
                    status = AlbumServerVideo.Status.PROCESSING;
                } else if (statusStr == "invalid") {
                    status = AlbumServerVideo.Status.INVALID;
                } else {
                    throw new JSONException("Invalid `video_status` value: " + statusStr);
                }

                String videoUrl = photo_obj.getString("video_url");
                String videoThumbnailUrl = photo_obj.getString("video_thumbnail_url");
                int videoDuration = photo_obj.getInt("video_duration");

                video = new AlbumServerVideo(status, videoUrl, videoThumbnailUrl, videoDuration);
            }

            String clientUploadId = null;
            if (photo_obj.has("client_upload_id")) {
                clientUploadId = photo_obj.getString("client_upload_id");
            }

            String photo_url = photo_obj.getString("photo_url");
            DateTime photo_date_created = parseDate(photo_obj, "date_created");

            AlbumUser author = parseAlbumUser(photo_obj.getJSONObject("author"));

            int globalGlanceScore = photo_obj.getInt("global_glance_score");
            int myGlanceScoreDelta = photo_obj.getInt("my_glance_score_delta");

            JSONArray glancesArray = photo_obj.getJSONArray("glances");
            ArrayList<AlbumPhotoGlance> glances = new ArrayList<AlbumPhotoGlance>();
            for (int j = 0; j < glancesArray.length(); ++j) {
                JSONObject glanceObj = glancesArray.getJSONObject(j);
                String emoticonName = glanceObj.getString("emoticon_name");
                AlbumUser glanceAuthor = parseAlbumUser(glanceObj.getJSONObject("author"));

                glances.add(new AlbumPhotoGlance(glanceAuthor, emoticonName));
            }

            JSONArray commentsArray = photo_obj.getJSONArray("comments");
            ArrayList<AlbumPhotoComment> comments = new ArrayList<AlbumPhotoComment>();
            for (int j = 0; j < commentsArray.length(); ++j) {
                JSONObject commentObj = commentsArray.getJSONObject(j);
                long clientMsgId = commentObj.getLong("client_msg_id");
                AlbumUser commentAuthor = parseAlbumUser(commentObj.getJSONObject("author"));
                DateTime dateCreated = parseDate(commentObj, "date_created");
                String commentText = commentObj.getString("comment");

                comments.add(new AlbumPhotoComment(commentAuthor, clientMsgId, dateCreated, commentText));
            }

            AlbumServerPhoto.Params albumServerPhotoParams = new AlbumServerPhoto.Params();
            albumServerPhotoParams.id = photo_id;
            albumServerPhotoParams.mediaType = mediaType;
            albumServerPhotoParams.video = video;
            albumServerPhotoParams.clientUploadId = clientUploadId;
            albumServerPhotoParams.url = photo_url;
            albumServerPhotoParams.author = author;
            albumServerPhotoParams.dateAdded = photo_date_created;
            albumServerPhotoParams.comments = comments;
            albumServerPhotoParams.globalGlanceScore = globalGlanceScore;
            albumServerPhotoParams.myGlanceScoreDelta = myGlanceScoreDelta;
            albumServerPhotoParams.glances = glances;
            result.add(new AlbumPhoto(new AlbumServerPhoto(albumServerPhotoParams)));
        }
        return result;
    }

    public ArrayList<AlbumSummary> getAlbums() throws APIException {
        return runAndLogNetworkRequestAction(new NetworkRequestAction<ArrayList<AlbumSummary>>() {
            @Override
            public NetworkRequestResult<ArrayList<AlbumSummary>> runAction() throws APIException, HTTPException {
                ArrayList<AlbumSummary> result = new ArrayList<AlbumSummary>();

                HTTPResponse response = sendRequest("GET", "/albums/");

                if (response.isError()) {
                    throw APIException.ErrorStatusCodeException(response);
                }

                try {
                    JSONArray response_array = response.bodyAsJSONArray();
                    for (int i = 0; i < response_array.length(); ++i) {
                        JSONObject albumObj = response_array.getJSONObject(i);

                        String etag = albumObj.getString("etag");
                        long id = albumObj.getLong("id");
                        String name = albumObj.getString("name");
                        AlbumUser creator = parseAlbumUser(albumObj.getJSONObject("creator"));
                        DateTime date_created = parseDate(albumObj, "date_created");
                        DateTime date_updated = parseDate(albumObj, "last_updated");
                        ArrayList<AlbumPhoto> latestPhotos = parsePhotoList(albumObj.getJSONArray("latest_photos"));
                        long num_new_photos = albumObj.getLong("num_new_photos");
                        DateTime last_access = null;
                        if (!albumObj.isNull("last_access")) {
                            last_access = parseDate(albumObj, "last_access");
                        }
                        AlbumSummary newAlbum = new AlbumSummary(id, etag, name, creator, date_created, date_updated, num_new_photos, last_access, latestPhotos);
                        result.add(newAlbum);
                    }
                } catch (JSONException e) {
                    throw APIException.FromJSONException(response, e);
                }

                return new NetworkRequestResult<ArrayList<AlbumSummary>>(result, response);
            }
        });
    }

    /**
     * Parse a quoted ETag value
     *
     * @param value Exact string as it appears in the HTTP header
     * @return null on failure
     */
    private static String ParseETagValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("value cannot be null");
        }

        if (value.length() < 2
            || value.charAt(0) != '"'
            || value.charAt(value.length() - 1) != '"') {
            // Malformed ETag header
            return null;
        }

        // Remove the quote('"') characters from the beginning and end of the string.
        // TODO To be really correct, should properly unescape the string
        return value.substring(1, value.length() - 1);
    }

    public AlbumContents getAlbumContents(final long albumId) throws APIException {
        return runAndLogNetworkRequestAction(new NetworkRequestAction<AlbumContents>() {
            @Override
            public NetworkRequestResult<AlbumContents> runAction() throws APIException, HTTPException {
                HTTPResponse response = sendRequest("GET", "/albums/" + albumId + "/");

                if (response.isError()) {
                    throw APIException.ErrorStatusCodeException(response);
                }

                try {
                    JSONObject json = response.bodyAsJSONObject();
                    String etagValue = response.getHeaderValue("etag");
                    if (etagValue != null) {
                        etagValue = ParseETagValue(etagValue);
                    }

                    AlbumContents result = parseAlbumContents(json, etagValue);
                    return new NetworkRequestResult<AlbumContents>(result, response);
                } catch (JSONException e) {
                    throw APIException.FromJSONException(response, e);
                }
            }
        });
    }

    /**
     * @param etag ETag value from HTTP header, may be null
     * @return AlbumContents object
     * @throws JSONException
     */
    private AlbumContents parseAlbumContents(JSONObject obj, String etag) throws JSONException {
        long id = obj.getLong("id");

        String name = obj.getString("name");
        AlbumUser creator = parseAlbumUser(obj.getJSONObject("creator"));
        DateTime date_created = parseDate(obj, "date_created");
        DateTime date_updated = parseDate(obj, "last_updated");

        long num_new_photos = obj.has("num_new_photos") ? obj.getLong("num_new_photos") : 0;
        DateTime last_access = null;
        if (obj.has("last_access") && !obj.isNull("last_access")) {
            last_access = parseDate(obj, "last_access");
        }

        JSONArray members_array = obj.getJSONArray("members");

        ArrayList<AlbumPhoto> photos = parsePhotoList(obj.getJSONArray("photos"));

        ArrayList<AlbumMember> members = new ArrayList<AlbumMember>();
        for (int i = 0; i < members_array.length(); ++i) {
            JSONObject member_obj = members_array.getJSONObject(i);
            long member_id = member_obj.getLong("id");
            String member_nickname = member_obj.getString("nickname");
            String member_avatar_url = member_obj.getString("avatar_url");
            boolean memberAlbumAdmin = member_obj.getBoolean("album_admin");
            long addedByUserId = member_obj.getLong("added_by_user_id");
            String inviteStatusStr = member_obj.getString("invite_status");
            AlbumMember.InviteStatus inviteStatus;
            if (inviteStatusStr.equals("joined")) {
                inviteStatus = AlbumMember.InviteStatus.JOINED;
            } else if (inviteStatusStr.equals("sms_sent")) {
                inviteStatus = AlbumMember.InviteStatus.SMS_SENT;
            } else if (inviteStatusStr.equals("invitation_viewed")) {
                inviteStatus = AlbumMember.InviteStatus.INVITATION_VIEWED;
            } else {
                throw new JSONException("Invalid `invite_status` value: " + inviteStatusStr);
            }

            AlbumUser user = new AlbumUser(member_id, member_nickname, member_avatar_url);
            members.add(new AlbumMember(user, memberAlbumAdmin, addedByUserId, inviteStatus));
        }

        return new AlbumContents(id, etag, name, creator, date_created, date_updated, num_new_photos, last_access, photos, members);
    }

    public long getPublicAlbumId() throws APIException {
        return runAndLogNetworkRequestAction(new NetworkRequestAction<Long>() {
            @Override
            public NetworkRequestResult<Long> runAction() throws APIException, HTTPException {
                HTTPResponse response = sendRequest("GET", "/albums/public/");

                if (response.isError()) {
                    throw APIException.ErrorStatusCodeException(response);
                }

                try {
                    JSONObject responseObj = response.bodyAsJSONObject();

                    long albumId = responseObj.getLong("album_id");

                    return new NetworkRequestResult<Long>(albumId, response);
                } catch (JSONException e) {
                    throw APIException.FromJSONException(response, e);
                }
            }
        });
    }

    public AlbumContents getPublicAlbumContents() throws APIException {
        long albumId = getPublicAlbumId();
        return getAlbumContents(albumId);
    }

    public AlbumContents createNewBlankAlbum(final String albumName) throws APIException {
        return runAndLogNetworkRequestAction(new NetworkRequestAction<AlbumContents>() {
            @Override
            public NetworkRequestResult<AlbumContents> runAction() throws APIException, HTTPException {
                JSONObject data = new JSONObject();
                data.put("album_name", albumName);
                data.put("photos", new JSONArray());
                data.put("members", new JSONArray());

                HTTPResponse response = sendRequest("POST", "/albums/", data);

                if (response.isError()) {
                    throw APIException.ErrorStatusCodeException(response);
                }

                String etagValue = response.getHeaderValue("etag");
                if (etagValue != null) {
                    etagValue = ParseETagValue(etagValue);
                }

                try {
                    JSONObject json = response.bodyAsJSONObject();
                    AlbumContents result = parseAlbumContents(json, etagValue);
                    return new NetworkRequestResult<AlbumContents>(result, response);
                } catch (JSONException e) {
                    throw APIException.FromJSONException(response, e);
                }
            }
        });
    }

    /**
     * Change the name of an album
     *
     * @return 'true' if album name successfully changed. 'false' if the name cannot be changed
     * because the album was created by a different user
     * @throws APIException
     */
    public boolean albumChangeName(final long albumId, final String newAlbumName) throws APIException {
        return runAndLogNetworkRequestAction(new NetworkRequestAction<Boolean>() {
            @Override
            public NetworkRequestResult<Boolean> runAction() throws APIException, HTTPException {
                JSONObject data = new JSONObject();
                data.put("name", newAlbumName);

                HTTPResponse response = sendRequest("PUT", "/albums/" + albumId + "/name/", data);

                final int HTTP_FORBIDDEN = 403;
                if (response.getStatusCode() == HTTP_FORBIDDEN) {
                    return new NetworkRequestResult<Boolean>(false, response);
                }

                if (response.isError()) {
                    throw APIException.ErrorStatusCodeException(response);
                }
                return new NetworkRequestResult<Boolean>(true, response);
            }
        });
    }

    public ArrayList<String> photosUploadRequest(final int numPhotos) throws APIException {
        if (numPhotos < 1) {
            throw new IllegalArgumentException("numPhotos must be at least 1: " + numPhotos);
        }

        return runAndLogNetworkRequestAction(new NetworkRequestAction<ArrayList<String>>() {
            @Override
            public NetworkRequestResult<ArrayList<String>> runAction() throws APIException, HTTPException {
                HTTPResponse response = sendRequest("POST", "/photos/upload_request/?num_photos=" + numPhotos);

                if (response.isError()) {
                    throw APIException.ErrorStatusCodeException(response);
                }

                ArrayList<String> result = new ArrayList<String>();

                try {
                    JSONArray responseArray = response.bodyAsJSONArray();
                    for (int i = 0; i < responseArray.length(); ++i) {
                        JSONObject photoUploadRequestObj = responseArray.getJSONObject(i);
                        String photoId = photoUploadRequestObj.getString("photo_id");
                        //Log.d("ShotVibeAPI", photoUploadRequestObj.getString("upload_url"));
                        result.add(photoId);
                    }
                } catch (JSONException e) {
                    throw APIException.FromJSONException(response, e);
                }

                return new NetworkRequestResult<ArrayList<String>>(result, response);
            }
        });
    }

    public AlbumContents albumAddPhotos(final long albumId, final Iterable<String> photoIds) throws APIException {
        return runAndLogNetworkRequestAction(new NetworkRequestAction<AlbumContents>() {
            @Override
            public NetworkRequestResult<AlbumContents> runAction() throws APIException, HTTPException {
                JSONObject data = new JSONObject();

                JSONArray photosArray = new JSONArray();
                for (String photoId : photoIds) {
                    JSONObject photoObj = new JSONObject();
                    if (photoId == null) {
                        throw new IllegalArgumentException("photoIds must not contain any null values");
                    }
                    photoObj.put("photo_id", photoId);
                    photosArray.put(photoObj);
                }

                data.put("add_photos", photosArray);

                HTTPResponse response = sendRequest("POST", "/albums/" + albumId + "/", data);

                if (response.isError()) {
                    throw APIException.ErrorStatusCodeException(response);
                }

                String etagValue = response.getHeaderValue("etag");
                if (etagValue != null) {
                    etagValue = ParseETagValue(etagValue);
                }

                try {
                    JSONObject json = response.bodyAsJSONObject();

                    AlbumContents result = parseAlbumContents(json, etagValue);
                    return new NetworkRequestResult<AlbumContents>(result, response);
                } catch (JSONException e) {
                    throw APIException.FromJSONException(response, e);
                }
            }
        });
    }

    public AlbumContents albumCopyPhotos(final long albumId, final Iterable<String> photoIds) throws APIException {
        return runAndLogNetworkRequestAction(new NetworkRequestAction<AlbumContents>() {
            @Override
            public NetworkRequestResult<AlbumContents> runAction() throws APIException, HTTPException {
                JSONObject data = new JSONObject();

                JSONArray photosArray = new JSONArray();
                for (String photoId : photoIds) {
                    JSONObject photoObj = new JSONObject();
                    if (photoId == null) {
                        throw new IllegalArgumentException("photoIds must not contain any null values");
                    }
                    photoObj.put("photo_id", photoId);
                    photosArray.put(photoObj);
                }

                data.put("copy_photos", photosArray);

                HTTPResponse response = sendRequest("POST", "/albums/" + albumId + "/", data);

                if (response.isError()) {
                    throw APIException.ErrorStatusCodeException(response);
                }

                String etagValue = response.getHeaderValue("etag");
                if (etagValue != null) {
                    etagValue = ParseETagValue(etagValue);
                }

                try {
                    JSONObject json = response.bodyAsJSONObject();

                    AlbumContents result = parseAlbumContents(json, etagValue);
                    return new NetworkRequestResult<AlbumContents>(result, response);
                } catch (JSONException e) {
                    throw APIException.FromJSONException(response, e);
                }
            }
        });
    }

    public static class MemberAddRequest {
        public MemberAddRequest(long userId) {
            mUserId = userId;
            mNickname = null;
            mPhoneNumber = null;
        }

        public MemberAddRequest(String nickname, String phoneNumber) {
            mNickname = nickname;
            mPhoneNumber = phoneNumber;
        }

        public boolean isUserIdRequest() {
            return mNickname == null;
        }

        public long getUserId() {
            if (!isUserIdRequest()) {
                throw new IllegalArgumentException("cannot getUserId for non isUserIdRequest request");
            }
            return mUserId;
        }

        public String getNickname() {
            if (isUserIdRequest()) {
                throw new IllegalArgumentException("cannot getNickname for isUserIdRequest request");
            }
            return mNickname;
        }

        public String getPhoneNumber() {
            if (isUserIdRequest()) {
                throw new IllegalArgumentException("cannot getPhoneNumber for isUserIdRequest request");
            }
            return mPhoneNumber;
        }

        private long mUserId;
        private String mNickname;
        private String mPhoneNumber;
    }

    public static class MemberAddFailure {
        public MemberAddFailure(MemberAddRequest memberAddRequest) {
            mMemberAddRequest = memberAddRequest;
        }

        public MemberAddRequest getMemberAddRequest() {
            return mMemberAddRequest;
        }

        private MemberAddRequest mMemberAddRequest;

        // TODO Add Failure reason enum
    }

    public ArrayList<MemberAddFailure> albumAddMembers(final long albumId, final List<MemberAddRequest> memberAddRequests, final String defaultCountry) throws APIException {
        if (memberAddRequests.isEmpty()) {
            throw new IllegalArgumentException("memberAddRequests cannot be empty");
        }

        return runAndLogNetworkRequestAction(new NetworkRequestAction<ArrayList<MemberAddFailure>>() {
            @Override
            public NetworkRequestResult<ArrayList<MemberAddFailure>> runAction() throws APIException, HTTPException {
                JSONObject requestBody = new JSONObject();

                JSONArray membersArray = new JSONArray();

                for (MemberAddRequest memberAddRequest : memberAddRequests) {
                    if (memberAddRequest.isUserIdRequest()) {
                        JSONObject memberObj = new JSONObject();
                        memberObj.put("user_id", memberAddRequest.getUserId());
                        membersArray.put(memberObj);
                    } else {
                        JSONObject memberObj = new JSONObject();
                        memberObj.put("contact_nickname", memberAddRequest.getNickname());
                        memberObj.put("phone_number", memberAddRequest.getPhoneNumber());
                        memberObj.put("default_country", defaultCountry);
                        membersArray.put(memberObj);
                    }
                }

                requestBody.put("members", membersArray);

                HTTPResponse response = sendRequest("POST", "/albums/" + albumId + "/members/", requestBody);

                if (response.isError()) {
                    throw APIException.ErrorStatusCodeException(response);
                }

                try {
                    JSONArray responseJson = response.bodyAsJSONArray();

                    ArrayList<MemberAddFailure> result = new ArrayList<MemberAddFailure>();
                    for (int i = 0; i < responseJson.length(); ++i) {
                        JSONObject obj = responseJson.getJSONObject(i);
                        if (!obj.getBoolean("success")) {
                            MemberAddFailure elem = new MemberAddFailure(memberAddRequests.get(i));
                            result.add(elem);
                        }
                    }

                    return new NetworkRequestResult<ArrayList<MemberAddFailure>>(result, response);
                } catch (JSONException e) {
                    throw APIException.FromJSONException(response, e);
                }
            }
        });
    }

    public void markAlbumAsViewed(final long albumId, final DateTime lastAccess) throws APIException {
        runAndLogNetworkRequestAction(new NetworkRequestAction<Void>() {
            @Override
            public NetworkRequestResult<Void> runAction() throws APIException, HTTPException {
                JSONObject data = new JSONObject();
                if (lastAccess == null) {
                    data.putNull("timestamp");
                } else {
                    data.put("timestamp", lastAccess.formatISO8601());
                }

                HTTPResponse response = sendRequest("POST", "/albums/" + albumId + "/view/", data);

                if (response.isError()) {
                    throw APIException.ErrorStatusCodeException(response);
                }
                return new NetworkRequestResult<Void>(null, response);
            }
        });
    }

    public void leaveAlbum(final long albumId) throws APIException {
        runAndLogNetworkRequestAction(new NetworkRequestAction<Object>() {
            @Override
            public NetworkRequestResult<Object> runAction() throws APIException, HTTPException {
                HTTPResponse response = sendRequest("POST", "/albums/" + albumId + "/leave/");

                if (response.isError()) {
                    throw APIException.ErrorStatusCodeException(response);
                }

                return new NetworkRequestResult<Object>(null, response);
            }
        });
    }

    public void glancePhoto(final String photoId, final String emoticonName) throws APIException {
        runAndLogNetworkRequestAction(new NetworkRequestAction<Void>() {
            @Override
            public NetworkRequestResult<Void> runAction() throws APIException, HTTPException {
                JSONObject requestBody = new JSONObject();

                requestBody.put("emoticon_name", emoticonName);

                HTTPResponse response = sendRequest("PUT", "/photos/" + photoId + "/glance/", requestBody);

                if (response.isError()) {
                    throw APIException.ErrorStatusCodeException(response);
                }

                return new NetworkRequestResult<Void>(null, response);
            }
        });
    }

    public void deletePhotos(final Iterable<String> photoIds) throws APIException {
        runAndLogNetworkRequestAction(new NetworkRequestAction<Object>() {
            @Override
            public NetworkRequestResult<Object> runAction() throws APIException, HTTPException {
                JSONObject requestBody = new JSONObject();

                JSONArray photosArray = new JSONArray();

                for (String photoId : photoIds) {
                    JSONObject memberObj = new JSONObject();
                    memberObj.put("photo_id", photoId);
                    photosArray.put(memberObj);
                }

                requestBody.put("photos", photosArray);

                HTTPResponse response = sendRequest("POST", "/photos/delete/", requestBody);

                if (response.isError()) {
                    throw APIException.ErrorStatusCodeException(response);
                }

                return new NetworkRequestResult<Object>(null, response);
            }
        });
    }

    public void postPhotoComment(final String photoId, final String commentText, final long clientMsgId) throws APIException {
        runAndLogNetworkRequestAction(new NetworkRequestAction<Object>() {
            @Override
            public NetworkRequestResult<Object> runAction() throws APIException, HTTPException {
                JSONObject requestBody = new JSONObject();
                requestBody.put("comment", commentText);

                long authorId = mAuthData.getUserId();

                HTTPResponse response = sendRequest("PUT", "/photos/" + photoId + "/comments/" + authorId + "/" + clientMsgId + "/", requestBody);

                if (response.isError()) {
                    throw APIException.ErrorStatusCodeException(response);
                }

                return new NetworkRequestResult<Object>(null, response);
            }
        });
    }

    public void setPhotoMyGlanceScoreDelta(final String photoId, final int myGlanceScoreDelta) throws APIException {
        runAndLogNetworkRequestAction(new NetworkRequestAction<Object>() {
            @Override
            public NetworkRequestResult<Object> runAction() throws APIException, HTTPException {
                JSONObject requestBody = new JSONObject();
                requestBody.put("score_delta", myGlanceScoreDelta);

                long authorId = mAuthData.getUserId();

                HTTPResponse response = sendRequest("PUT", "/photos/" + photoId + "/glance_scores/" + authorId + "/", requestBody);

                if (response.isError()) {
                    throw APIException.ErrorStatusCodeException(response);
                }

                return new NetworkRequestResult<Object>(null, response);
            }
        });
    }

    public ArrayList<PhoneContactServerResult> queryPhoneNumbers(final ArrayList<PhoneContact> phoneContacts, final String defaultCountry) throws APIException {
        return runAndLogNetworkRequestAction(new NetworkRequestAction<ArrayList<PhoneContactServerResult>>() {
            @Override
            public NetworkRequestResult<ArrayList<PhoneContactServerResult>> runAction() throws APIException, HTTPException {
                JSONArray phoneNumbers = new JSONArray();
                for (PhoneContact p : phoneContacts) {
                    JSONObject entry = new JSONObject();
                    entry.put("phone_number", p.getPhoneNumber());
                    entry.put("contact_nickname", p.getFullName());
                    phoneNumbers.put(entry);
                }

                JSONObject requestBody = new JSONObject();
                requestBody.put("default_country", defaultCountry);
                requestBody.put("phone_numbers", phoneNumbers);

                HTTPResponse response = sendRequest("POST", "/query_phone_numbers/", requestBody);

                if (response.isError()) {
                    throw APIException.ErrorStatusCodeException(response);
                }

                DateTime now = DateTime.NowUTC();

                try {
                    ArrayList<PhoneContactServerResult> results = new ArrayList<PhoneContactServerResult>();
                    JSONObject responseObj = response.bodyAsJSONObject();
                    JSONArray phoneNumberDetails = responseObj.getJSONArray("phone_number_details");
                    for (int i = 0; i < phoneNumberDetails.length(); ++i) {
                        JSONObject obj = phoneNumberDetails.getJSONObject(i);
                        String phoneTypeStr = obj.getString("phone_type");
                        PhoneContactServerResult.PhoneType phoneType;
                        if (phoneTypeStr.equals("invalid")) {
                            phoneType = PhoneContactServerResult.PhoneType.INVALID;
                        } else if (phoneTypeStr.equals("mobile")) {
                            phoneType = PhoneContactServerResult.PhoneType.MOBILE;
                        } else if (phoneTypeStr.equals("landline")) {
                            phoneType = PhoneContactServerResult.PhoneType.LANDLINE;
                        } else {
                            throw new JSONException("Invalid `phone_type` value: " + phoneTypeStr);
                        }

                        PhoneContact inputPhoneContact = phoneContacts.get(i);

                        PhoneContactServerResult serverResult;
                        if (phoneType != PhoneContactServerResult.PhoneType.MOBILE) {
                            serverResult = PhoneContactServerResult.createNonMobileResult(inputPhoneContact, phoneType, now);
                        } else {
                            Long userId;
                            if (obj.isNull("user_id")) {
                                userId = null;
                            } else {
                                userId = obj.getLong("user_id");
                            }
                            String avatarUrl = obj.getString("avatar_url");
                            String canonicalPhoneNumber = obj.getString("phone_number");
                            serverResult = PhoneContactServerResult.createMobileResult(inputPhoneContact, userId, avatarUrl, canonicalPhoneNumber, now);
                        }
                        results.add(serverResult);
                    }

                    return new NetworkRequestResult<ArrayList<PhoneContactServerResult>>(results, response);
                } catch (JSONException e) {
                    throw APIException.FromJSONException(response, e);
                }
            }
        });
    }
}
