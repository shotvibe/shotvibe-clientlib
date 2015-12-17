package com.shotvibe.shotvibelib;

public abstract class NotificationMessage {
    public interface NotificationHandler {
        void Handle(NotificationMessage.TestMessage msg);
        void Handle(NotificationMessage.AlbumListSync msg);
        void Handle(NotificationMessage.AlbumSync msg);
        void Handle(NotificationMessage.PhotosAdded msg);
        void Handle(NotificationMessage.AddedToAlbum msg);
        void Handle(NotificationMessage.PhotoComment msg);
        void Handle(NotificationMessage.PhotoGlanceScoreDelta msg);
        void Handle(NotificationMessage.UserGlanceScoreUpdate msg);
        void Handle(NotificationMessage.PhotoGlance msg);
    }

    public static class ParseException extends Exception {
        public ParseException(String message) {
            super(message);
        }

        public static ParseException FromJSONException(JSONException e) {
            return new ParseException("JSON Parse Error: " + e.getMessage());
        }
    }

    public abstract void handle(NotificationHandler handler);

    public static NotificationMessage parseMessage(JSONObject msg) throws ParseException {
        String type;
        try {
            type = msg.getString("type");
            if (type.equals("test_message")) {
                return TestMessage.parse(msg);
            } else if (type.equals("album_list_sync")) {
                return AlbumListSync.parse(msg);
            } else if (type.equals("album_sync")) {
                return AlbumSync.parse(msg);
            } else if (type.equals("photos_added")) {
                return PhotosAdded.parse(msg);
            } else if (type.equals("added_to_album")) {
                return AddedToAlbum.parse(msg);
            } else if (type.equals("photo_comment")) {
                return PhotoComment.parse(msg);
            } else if (type.equals("photo_glance_score_delta")) {
                return PhotoGlanceScoreDelta.parse(msg);
            } else if (type.equals("user_glance_score_update")) {
                return UserGlanceScoreUpdate.parse(msg);
            } else if (type.equals("photo_glance")) {
                return PhotoGlance.parse(msg);
            } else {
                throw new ParseException("Unknown Message type: " + type);
            }
        } catch (JSONException e) {
            throw ParseException.FromJSONException(e);
        }
    }

    public static final class TestMessage extends NotificationMessage {
        public static TestMessage parse(JSONObject msg) throws ParseException, JSONException {
            String message = msg.getString("message");
            return new TestMessage(message);
        }

        private TestMessage(String message) {
            mMessage = message;
        }

        public String getMessage() {
            return mMessage;
        }

        private final String mMessage;

        @Override
        public void handle(NotificationHandler handler) {
            handler.Handle(this);
        }
    }

    public static final class AlbumListSync extends NotificationMessage {
        public static AlbumListSync parse(JSONObject msg) throws ParseException, JSONException {
            return new AlbumListSync();
        }

        @Override
        public void handle(NotificationHandler handler) {
            handler.Handle(this);
        }
    }

    public static final class AlbumSync extends NotificationMessage {
        public static AlbumSync parse(JSONObject msg) throws ParseException, JSONException {
            long album_id = msg.getLong("album_id");
            return new AlbumSync(album_id);
        }

        private AlbumSync(long albumId) {
            mAlbumId = albumId;
        }

        public long getAlbumId() {
            return mAlbumId;
        }

        private final long mAlbumId;

        @Override
        public void handle(NotificationHandler handler) {
            handler.Handle(this);
        }
    }

    public static final class PhotosAdded extends NotificationMessage {
        public static PhotosAdded parse(JSONObject msg) throws ParseException, JSONException {
            long album_id = msg.getLong("album_id");
            String author_name = msg.getString("author");
            String authorAvatarUrl = msg.getString("author_avatar_url");
            String album_name = msg.getString("album_name");
            int num_photos = msg.getInt("num_photos");

            return new PhotosAdded(album_id, author_name, authorAvatarUrl, album_name, num_photos);
        }

        private PhotosAdded(long albumId, String authorName, String authorAvatarUrl, String albumName, int numPhotos) {
            mAlbumId = albumId;
            mAuthorName = authorName;
            mAuthorAvatarUrl = authorAvatarUrl;
            mAlbumName = albumName;
            mNumPhotos = numPhotos;
        }

        public long getAlbumId() {
            return mAlbumId;
        }

        public String getAuthorName() {
            return mAuthorName;
        }

        public String getAuthorAvatarUrl() {
            return mAuthorAvatarUrl;
        }

        public String getAlbumName() {
            return mAlbumName;
        }

        public int getNumPhotos() {
            return mNumPhotos;
        }

        private final long mAlbumId;
        private final String mAuthorName;
        private final String mAuthorAvatarUrl;
        private final String mAlbumName;
        private final int mNumPhotos;

        @Override
        public void handle(NotificationHandler handler) {
            handler.Handle(this);
        }
    }

    public static final class AddedToAlbum extends NotificationMessage {
        public static AddedToAlbum parse(JSONObject msg) throws ParseException, JSONException {
            long album_id = msg.getLong("album_id");
            String adder_name = msg.getString("adder");
            String adderAvatarUrl = msg.getString("adder_avatar_url");
            String album_name = msg.getString("album_name");

            return new AddedToAlbum(album_id, adder_name, adderAvatarUrl, album_name);
        }

        private AddedToAlbum(long albumId, String adderName, String adderAvatarUrl, String albumName) {
            mAlbumId = albumId;
            mAdderName = adderName;
            mAdderAvatarUrl = adderAvatarUrl;
            mAlbumName = albumName;
        }

        public long getAlbumId() {
            return mAlbumId;
        }

        public String getAdderName() {
            return mAdderName;
        }

        public String getAdderAvatarUrl() {
            return mAdderAvatarUrl;
        }

        public String getAlbumName() {
            return mAlbumName;
        }

        private final long mAlbumId;
        private final String mAdderName;
        private final String mAdderAvatarUrl;
        private final String mAlbumName;

        @Override
        public void handle(NotificationHandler handler) {
            handler.Handle(this);
        }
    }

    public static final class PhotoComment extends NotificationMessage {
        public static PhotoComment parse(JSONObject msg) throws ParseException, JSONException {
            long albumId = msg.getLong("album_id");
            String photoId = msg.getString("photo_id");
            String albumName = msg.getString("album_name");
            String commentAuthorNickname = msg.getString("comment_author_nickname");
            String commentAuthorAvatarUrl = msg.getString("comment_author_avatar_url");
            String commentText = msg.getString("comment_text");

            return new PhotoComment(albumId, photoId, albumName, commentAuthorNickname, commentAuthorAvatarUrl, commentText);
        }

        private PhotoComment(long albumId, String photoId, String albumName, String commentAuthorNickname, String commentAuthorAvatarUrl, String commentText) {
            mAlbumId = albumId;
            mPhotoId = photoId;
            mAlbumName = albumName;
            mCommentAuthorNickname = commentAuthorNickname;
            mCommentAuthorAvatarUrl = commentAuthorAvatarUrl;
            mCommentText = commentText;
        }

        public long getAlbumId() {
            return mAlbumId;
        }

        public String getPhotoId() {
            return mPhotoId;
        }

        public String getAlbumName() {
            return mAlbumName;
        }

        public String getCommentAuthorNickname() {
            return mCommentAuthorNickname;
        }

        public String getCommentAuthorAvatarUrl() {
            return mCommentAuthorAvatarUrl;
        }

        public String getCommentText() {
            return mCommentText;
        }

        private final long mAlbumId;
        private final String mPhotoId;
        private final String mAlbumName;
        private final String mCommentAuthorNickname;
        private final String mCommentAuthorAvatarUrl;
        private final String mCommentText;

        @Override
        public void handle(NotificationHandler handler) {
            handler.Handle(this);
        }
    }

    public static final class PhotoGlanceScoreDelta extends NotificationMessage {
        public static PhotoGlanceScoreDelta parse(JSONObject msg) throws ParseException, JSONException {
            long albumId = msg.getLong("album_id");
            String photoId = msg.getString("photo_id");
            String albumName = msg.getString("album_name");
            String glanceAuthorNickname = msg.getString("glance_author_nickname");
            String glanceAuthorAvatarUrl = msg.getString("glance_author_avatar_url");
            int scoreDelta = msg.getInt("score_delta");

            return new PhotoGlanceScoreDelta(albumId, photoId, albumName, glanceAuthorNickname, glanceAuthorAvatarUrl, scoreDelta);
        }

        private PhotoGlanceScoreDelta(long albumId, String photoId, String albumName, String glanceAuthorNickname, String glanceAuthorAvatarUrl, int scoreDelta) {
            mAlbumId = albumId;
            mPhotoId = photoId;
            mAlbumName = albumName;
            mGlanceAuthorNickname = glanceAuthorNickname;
            mGlanceAuthorAvatarUrl = glanceAuthorAvatarUrl;
            mScoreDelta = scoreDelta;
        }

        public long getAlbumId() {
            return mAlbumId;
        }

        public String getPhotoId() {
            return mPhotoId;
        }

        public String getAlbumName() {
            return mAlbumName;
        }

        public String getGlanceAuthorNickname() {
            return mGlanceAuthorNickname;
        }

        public String getGlanceAuthorAvatarUrl() {
            return mGlanceAuthorAvatarUrl;
        }

        public int getScoreDelta() {
            return mScoreDelta;
        }

        private final long mAlbumId;
        private final String mPhotoId;
        private final String mAlbumName;
        private final String mGlanceAuthorNickname;
        private final String mGlanceAuthorAvatarUrl;
        private final int mScoreDelta;

        @Override
        public void handle(NotificationHandler handler) {
            handler.Handle(this);
        }
    }

    public static final class UserGlanceScoreUpdate extends NotificationMessage {
        public static UserGlanceScoreUpdate parse(JSONObject msg) throws ParseException, JSONException {
            int userGlanceScore = msg.getInt("user_glance_score");

            return new UserGlanceScoreUpdate(userGlanceScore);
        }

        private UserGlanceScoreUpdate(int userGlanceScore) {
            mUserGlanceScore = userGlanceScore;
        }

        public int getUserGlanceScore() {
            return mUserGlanceScore;
        }

        private final int mUserGlanceScore;

        @Override
        public void handle(NotificationHandler handler) {
            handler.Handle(this);
        }
    }

    public static final class PhotoGlance extends NotificationMessage {
        public static PhotoGlance parse(JSONObject msg) throws ParseException, JSONException {
            long albumId = msg.getLong("album_id");
            String albumName = msg.getString("album_name");
            String userNickname = msg.getString("user_nickname");

            return new PhotoGlance(albumId, albumName, userNickname);
        }

        private PhotoGlance(long albumId, String albumName, String userNickname) {
            mAlbumId = albumId;
            mAlbumName = albumName;
            mUserNickname = userNickname;
        }

        public long getAlbumId() {
            return mAlbumId;
        }

        public String getAlbumName() {
            return mAlbumName;
        }

        public String getUserNickname() {
            return mUserNickname;
        }

        private final long mAlbumId;
        private final String mAlbumName;
        private final String mUserNickname;

        @Override
        public void handle(NotificationHandler handler) {
            handler.Handle(this);
        }
    }
}
