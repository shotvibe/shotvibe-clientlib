package com.shotvibe.shotvibelib;

public final class ShotVibeDB {
    public ShotVibeDB(SQLConnection conn) {
        mConn = conn;
    }

    private SQLConnection mConn;

    private static DateTime cursorGetDateTime(SQLCursor cursor, int columnIndex) throws SQLException {
        return DateTime.FromTimeStamp(cursor.getLong(columnIndex));
    }

    public ArrayList<AlbumSummary> getAlbumList() throws SQLException {
        SQLCursor cursor = mConn.query(""
                + "SELECT album_id, name, last_updated, num_new_photos, last_access"
                + " FROM album"
                + " ORDER BY last_updated DESC");

         try {
            ArrayList<AlbumSummary> results = new ArrayList<AlbumSummary>();
            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                String name = cursor.getString(1);
                DateTime lastUpdated = cursorGetDateTime(cursor, 2);
                final String etag = null;
                // TODO Add real date for "dateCreated"
                DateTime dummyDateCreated = DateTime.ParseISO8601("2000-01-01T00:00:00.000Z");
                final int NUM_LATEST_PHOTOS = 2;
                long numNewPhotos = cursor.getLong(3);
                DateTime lastAccess = cursor.isNull(2) ? null : cursorGetDateTime(cursor, 2);
                ArrayList<AlbumPhoto> latestPhotos = getLatestPhotos(id, NUM_LATEST_PHOTOS);
                AlbumSummary album = new AlbumSummary(id, etag, name, dummyDateCreated, lastUpdated, numNewPhotos, lastAccess, latestPhotos);
                results.add(album);
            }
            return results;
        } finally {
            cursor.close();
        }
    }

    private ArrayList<AlbumPhoto> getLatestPhotos(long albumId, int numPhotos) throws SQLException {
        SQLCursor cursor = mConn.query(""
                + "SELECT photo.photo_id, photo.url, photo.created, user.user_id, user.nickname, user.avatar_url"
                + " FROM photo"
                + " LEFT OUTER JOIN user"
                + " ON photo.author_id = user.user_id"
                + " WHERE photo.photo_album=?"
                + " ORDER BY photo.num DESC"
                + " LIMIT ?",
                SQLValues.create()
                        .add(albumId)
                        .add(numPhotos));
        try {
            ArrayList<AlbumPhoto> results = new ArrayList<AlbumPhoto>();
            while (cursor.moveToNext()) {
                String photoId = cursor.getString(0);
                String photoUrl = cursor.getString(1);
                DateTime photoDateAdded = cursorGetDateTime(cursor, 2);
                long photoAuthorUserId = cursor.getLong(3);
                String photoAuthorNickname = cursor.getString(4);
                String photoAuthorAvatarUrl = cursor.getString(5);
                AlbumUser photoAuthor = new AlbumUser(photoAuthorUserId, photoAuthorNickname, photoAuthorAvatarUrl);
                results.add(new AlbumPhoto(new AlbumServerPhoto(photoId, photoUrl, photoAuthor, photoDateAdded)));
            }
            return results;
        } finally {
            cursor.close();
        }
    }
}
