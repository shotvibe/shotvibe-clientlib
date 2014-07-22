package com.shotvibe.shotvibelib;

public abstract class NotificationMessage {
    public interface NotificationHandler {
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
            if (type.equals("photo_glance")) {
                return PhotoGlance.parse(msg);
            } else {
                throw new ParseException("Unknown Message type: " + type);
            }
        } catch (JSONException e) {
            throw ParseException.FromJSONException(e);
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
