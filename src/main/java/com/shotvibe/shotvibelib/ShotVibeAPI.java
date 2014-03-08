package com.shotvibe.shotvibelib;

import java.util.List;
import java.util.Map;

public class ShotVibeAPI {
    public static final boolean useStagingServer = false;
    public static final String BASE_URL = useStagingServer ? "https://staging-api.shotvibe.com" : "https://api.shotvibe.com";
    public static final String BASE_UPLOAD_URL = useStagingServer ? "https://staging-upload.shotvibe.com" : "https://api.shotvibe.com";
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
        String nullBody = null;
        return mHttpLib.sendRequest(method, BASE_URL + url, mJsonRequestHeaders, nullBody);
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

    private ArrayList<AlbumPhoto> parsePhotoList(JSONArray photos_array) throws JSONException {
        ArrayList<AlbumPhoto> result = new ArrayList<AlbumPhoto>();
        for (int i = 0; i < photos_array.length(); ++i) {
            JSONObject photo_obj = photos_array.getJSONObject(i);
            String photo_id = photo_obj.getString("photo_id");
            String photo_url = photo_obj.getString("photo_url");
            DateTime photo_date_created = parseDate(photo_obj, "date_created");

            JSONObject author_obj = photo_obj.getJSONObject("author");

            long authorId = author_obj.getLong("id");
            String authorNickname = author_obj.getString("nickname");
            String authorAvatarUrl = author_obj.getString("avatar_url");
            AlbumUser author = new AlbumUser(authorId, authorNickname, authorAvatarUrl);

            result.add(new AlbumPhoto(new AlbumServerPhoto(photo_id, photo_url, author, photo_date_created)));
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
                        DateTime date_updated = parseDate(albumObj, "last_updated");
                        ArrayList<AlbumPhoto> latestPhotos = parsePhotoList(albumObj.getJSONArray("latest_photos"));
                        long num_new_photos = albumObj.getLong("num_new_photos");
                        DateTime last_access = null;
                        if (!albumObj.isNull("last_access")) {
                            last_access = parseDate(albumObj, "last_access");
                        }
                        DateTime dummyDateCreated = DateTime.ParseISO8601("2000-01-01T00:00:00.000Z");
                        AlbumSummary newAlbum = new AlbumSummary(id, etag, name, dummyDateCreated, date_updated, num_new_photos, last_access, latestPhotos);
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

    public AlbumContents getAlbumContents(long albumId) throws APIException {
        try {
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

                return parseAlbumContents(json, etagValue);
            } catch (JSONException e) {
                throw APIException.FromJSONException(response, e);
            }
        } catch (HTTPException e) {
            throw APIException.FromHttpException(e);
        }
    }

    /**
     * @param obj
     * @param etag ETag value from HTTP header, may be null
     * @return AlbumContents object
     * @throws JSONException
     */
    private AlbumContents parseAlbumContents(JSONObject obj, String etag) throws JSONException {
        long id = obj.getLong("id");

        String name = obj.getString("name");

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
            members.add(new AlbumMember(user, inviteStatus));
        }

        return new AlbumContents(id, etag, name, date_created, date_updated, num_new_photos, last_access, photos, members);
    }

    public AlbumContents createNewBlankAlbum(String albumName) throws APIException {
        try {
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
                return parseAlbumContents(json, etagValue);
            } catch (JSONException e) {
                throw APIException.FromJSONException(response, e);
            }
        } catch (HTTPException e) {
            throw APIException.FromHttpException(e);
        }
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

            try {
                JSONArray responseArray = null;
                responseArray = response.bodyAsJSONArray();
                for (int i = 0; i < responseArray.length(); ++i) {
                    JSONObject photoUploadRequestObj = responseArray.getJSONObject(i);
                    String photoId = photoUploadRequestObj.getString("photo_id");
                    result.add(photoId);
                }
            } catch (JSONException e) {
                throw APIException.FromJSONException(response, e);
            }

            return result;
        } catch (HTTPException e) {
            throw APIException.FromHttpException(e);
        }
    }

    public AlbumContents albumAddPhotos(long albumId, Iterable<String> photoIds) throws APIException {
        try {
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

                return parseAlbumContents(json, etagValue);
            } catch (JSONException e) {
                throw APIException.FromJSONException(response, e);
            }
        } catch (HTTPException e) {
            throw APIException.FromHttpException(e);
        }
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

    public ArrayList<MemberAddFailure> albumAddMembers(long albumId, List<MemberAddRequest> memberAddRequests, String defaultCountry) throws APIException {
        if (memberAddRequests.isEmpty()) {
            throw new IllegalArgumentException("memberAddRequests cannot be empty");
        }

        try {
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

                return result;
            } catch (JSONException e) {
                throw APIException.FromJSONException(response, e);
            }

        } catch (HTTPException e) {
            throw APIException.FromHttpException(e);
        }
    }

    public void markAlbumAsViewed(long albumId, DateTime lastAccess) throws APIException {
        try {
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
        } catch (HTTPException e) {
            throw APIException.FromHttpException(e);
        }
    }

    public void leaveAlbum(long albumId) throws APIException {
        try {
            HTTPResponse response = sendRequest("POST", "/albums/" + albumId + "/leave/");

            if (response.isError()) {
                throw APIException.ErrorStatusCodeException(response);
            }
        } catch (HTTPException e) {
            throw APIException.FromHttpException(e);
        }
    }

    public void deletePhotos(Iterable<String> photoIds) throws APIException {
        try {
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
        } catch (HTTPException e) {
            throw APIException.FromHttpException(e);
        }
    }

    public ArrayList<PhoneContactServerResult> queryPhoneNumbers(ArrayList<PhoneContact> phoneContacts, String defaultCountry) throws APIException {
        try {
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
                        serverResult = PhoneContactServerResult.createNonMobileResult(inputPhoneContact, phoneType);
                    } else {
                        Long userId;
                        if (obj.isNull("user_id")) {
                            userId = null;
                        } else {
                            userId = obj.getLong("user_id");
                        }
                        String avatarUrl = obj.getString("avatar_url");
                        String canonicalPhoneNumber = obj.getString("phone_number");
                        serverResult = PhoneContactServerResult.createMobileResult(inputPhoneContact, userId, avatarUrl, canonicalPhoneNumber);
                    }
                    results.add(serverResult);
                }

                return results;

            } catch (JSONException e) {
                throw APIException.FromJSONException(response, e);
            }
        } catch (HTTPException e) {
            throw APIException.FromHttpException(e);
        }
    }
}
