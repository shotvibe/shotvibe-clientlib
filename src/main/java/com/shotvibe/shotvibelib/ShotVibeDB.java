package com.shotvibe.shotvibelib;

public final class ShotVibeDB {
    private ShotVibeDB(SQLConnection conn) {
        mConn = conn;
    }

    public static final int DATABASE_VERSION = 1;

    /**
     * Should be called after populateNewDB or upgradeDB has been called (if one of them was necessary)
     *
     * @param conn
     * @return
     */
    public static ShotVibeDB open(SQLConnection conn) {
        if (conn == null) {
            throw new IllegalArgumentException("conn cannot be null");
        }

        return new ShotVibeDB(conn);
    }

    /**
     * Should be called from within a transaction
     *
     * @param conn
     * @throws SQLException
     */
    public static void populateNewDB(SQLConnection conn) throws SQLException {
        if (conn == null) {
            throw new IllegalArgumentException("conn cannot be null");
        }

        conn.executeSQLScript("create.sql");
    }

    /**
     * Should be called from within a transaction
     *
     * @param conn
     * @param oldVersion
     * @throws SQLException
     */
    public static void upgradeDB(SQLConnection conn, int oldVersion) throws SQLException {
        if (conn == null) {
            throw new IllegalArgumentException("conn cannot be null");
        }

        // TODO database migration
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

    public HashMap<Long, String> getAlbumListEtagValues() throws SQLException {
        SQLCursor cursor = mConn.query(""
                + "SELECT album_id, last_etag"
                + " FROM album");

        try {
            HashMap<Long, String> results = new HashMap<Long, String>();
            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);

                // etag may be NULL if the full album hasn't been loaded yet
                if (!cursor.isNull(1)) {
                    String last_etag = cursor.getString(1);
                    results.put(id, last_etag);
                }
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

    public void setAlbumList(ArrayList<AlbumSummary> albums) throws SQLException {
        mConn.beginTransaction();
        try {
            // Keep track of all the new albumIds in an efficient data structure
            HashSet<Long> albumIds = new HashSet<Long>();
            for (AlbumSummary album : albums) {
                albumIds.add(album.getId());

                // First try updating an existing row, in order to not erase an existing etag value
                mConn.update(""
                        + "UPDATE album"
                        + " SET album_id=?, name=?, last_updated=?, num_new_photos=?, last_access=?"
                        + " WHERE album_id=?",
                        SQLValues.create()
                                .add(album.getId())
                                .add(album.getName())
                                .add(dateTimeToSQLValue(album.getDateUpdated()))
                                .add(album.getNumNewPhotos())
                                .addNullable(album.getLastAccess() == null ? null : dateTimeToSQLValue(album.getLastAccess()))
                                .add(album.getId()));

                if (mConn.changes() == 0) {
                    // A row didn't exist for the album, this will insert a new row
                    // (while also not failing in the case of a rare race condition
                    // where a row actually was just now added -- if that actually
                    // does happen then we will unfortunately overwrite the etag
                    // with a null value, but that won't cause much harm, it will
                    // just cause the album to be unnecessary refreshed one more time)

                    mConn.update(""
                            + "INSERT OR REPLACE INTO album (album_id, name, last_updated, num_new_photos, last_access)"
                            + " VALUES (?, ?, ?, ?, ?)",
                            SQLValues.create()
                                    .add(album.getId())
                                    .add(album.getName())
                                    .add(dateTimeToSQLValue(album.getDateUpdated()))
                                    .add(album.getNumNewPhotos())
                                    .addNullable(album.getLastAccess() == null ? null : dateTimeToSQLValue(album.getLastAccess())));
                }
            }

            // Delete any old rows in the database that are not in albums:
            SQLCursor cursor = mConn.query(""
                    + "SELECT album_id"
                    + " FROM album");

            try {
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(0);
                    if (!albumIds.contains(id)) {
                        mConn.update(""
                                + "DELETE FROM album"
                                + " WHERE album_id=?",
                                SQLValues.create()
                                        .add(id));
                    }
                }
            } finally {
                cursor.close();
            }

            mConn.setTransactionSuccesful();
        } finally {
            mConn.endTransaction();
        }
    }

    private static long dateTimeToSQLValue(DateTime dateTime) {
        return dateTime.getTimeStamp();
    }
}
