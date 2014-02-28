package com.shotvibe.shotvibelib;

import java.util.List;
import java.util.Map;

public final class ShotVibeDB {
    private ShotVibeDB(SQLConnection conn) {
        mConn = conn;
    }

    public static final int DATABASE_VERSION = 2;

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

        // Poor man's migration: just clear the database
        clearDB(conn);
    }

    /**
     * Clear the database by removing all tables and repopulating it
     * Should be called from within a transaction
     *
     * @param conn
     * @throws SQLException
     */
    private static void clearDB(SQLConnection conn) throws SQLException {
        Log.d("clearDB", "Clearing old database");
        List<String> tableNames = new ArrayList<String>();
        SQLCursor cursor = conn.query(""
                       + "SELECT tbl_name "
                       + " FROM main.sqlite_master"
                       + " WHERE type='table'");

        try {
            while (cursor.moveToNext()) { // collect all table names
                tableNames.add(cursor.getString(0));
            }
        } finally {
            cursor.close();
        }

        for (String tableName : tableNames) { // drop all tables
            Log.d("clearDB", "Dropping table:" + tableName);
            conn.update(""
                   + "DROP TABLE " + tableName);
        }

        Log.d("clearDB", "Populating new database");
        populateNewDB(conn);

        Log.d("clearDB", "Upgrade complete");
    }


    private SQLConnection mConn;

    private static DateTime cursorGetDateTime(SQLCursor cursor, int columnIndex) throws SQLException {
        return DateTime.FromTimeStamp(cursor.getLong(columnIndex));
    }

    public synchronized ArrayList<AlbumSummary> getAlbumList() throws SQLException {
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

    public synchronized HashMap<Long, String> getAlbumListEtagValues() throws SQLException {
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

    public synchronized void setAlbumList(ArrayList<AlbumSummary> albums) throws SQLException {
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

    public synchronized AlbumContents getAlbumContents(long albumId) throws SQLException {
        mConn.beginTransaction();
        try {
            SQLCursor cursor;
            cursor = mConn.query(""
                    + "SELECT name, last_updated, num_new_photos, last_access"
                    + " FROM album"
                    + " WHERE album_id=?",
                    SQLValues.create()
                            .add(albumId));

            String albumName;
            DateTime albumLastUpdated;
            String albumEtag;
            long albumNumNewPhotos;
            DateTime albumLastAccess;

            try {
                if (!cursor.moveToNext()) {
                    // No cached AlbumContents available
                    return null;
                }

                albumName = cursor.getString(0);
                albumLastUpdated = cursorGetDateTime(cursor, 1);
                albumEtag = null;
                albumNumNewPhotos = cursor.getLong(2);
                albumLastAccess = cursor.isNull(3) ? null : cursorGetDateTime(cursor, 3);
            } finally {
                cursor.close();
            }

            cursor = mConn.query(""
                    + "SELECT photo.photo_id, photo.url, photo.created, user.user_id, user.nickname, user.avatar_url"
                    + " FROM photo"
                    + " LEFT OUTER JOIN user"
                    + " ON photo.author_id = user.user_id"
                    + " WHERE photo.photo_album=?"
                    + " ORDER BY photo.num ASC",
                    SQLValues.create()
                            .add(albumId));

            ArrayList<AlbumPhoto> albumPhotos = new ArrayList<AlbumPhoto>();
            try {
                while (cursor.moveToNext()) {
                    String photoId = cursor.getString(0);
                    String photoUrl = cursor.getString(1);
                    DateTime photoDateAdded = cursorGetDateTime(cursor, 2);
                    long photoAuthorUserId = cursor.getLong(3);
                    String photoAuthorNickname = cursor.getString(4);
                    String photoAuthorAvatarUrl = cursor.getString(5);
                    AlbumUser photoAuthor = new AlbumUser(photoAuthorUserId, photoAuthorNickname, photoAuthorAvatarUrl);
                    albumPhotos.add(new AlbumPhoto(new AlbumServerPhoto(photoId, photoUrl, photoAuthor, photoDateAdded)));
                }
            } finally {
                cursor.close();
            }

            cursor = mConn.query(""
                    + "SELECT album_member.user_id, user.nickname, user.avatar_url"
                    + " FROM album_member"
                    + " LEFT OUTER JOIN user"
                    + " ON album_member.user_id = user.user_id"
                    + " WHERE album_member.album_id=?"
                    + " ORDER BY user.nickname ASC",
                    SQLValues.create()
                            .add(albumId));

            ArrayList<AlbumMember> albumMembers = new ArrayList<AlbumMember>();
            try {
                while (cursor.moveToNext()) {
                    long memberId = cursor.getLong(0);
                    String memberNickname = cursor.getString(1);
                    String memberAvatarUrl = cursor.getString(2);
                    AlbumUser user = new AlbumUser(memberId, memberNickname, memberAvatarUrl);
                    albumMembers.add(new AlbumMember(user, null));
                }
            } finally {
                cursor.close();
            }

            // TODO Add real date for "dateCreated"
            DateTime dummyDateCreated = DateTime.ParseISO8601("2000-01-01T00:00:00.000Z");

            mConn.setTransactionSuccesful();
            return new AlbumContents(albumId, albumEtag, albumName, dummyDateCreated, albumLastUpdated, albumNumNewPhotos, albumLastAccess, albumPhotos, albumMembers);
        } finally {
            mConn.endTransaction();
        }
    }

    /**
     * @param albumId
     * @param albumContents Must contain only photos of type AlbumServerPhoto, no AlbumUploadingPhotos allowed!
     */
    public synchronized void setAlbumContents(long albumId, AlbumContents albumContents) throws SQLException {
        mConn.beginTransaction();
        try {
            mConn.update(""
                    + "INSERT OR REPLACE INTO album (album_id, name, last_updated, last_etag, num_new_photos, last_access)"
                    + " VALUES (?, ?, ?, ?, ?, ?)",
                    SQLValues.create()
                            .add(albumContents.getId())
                            .add(albumContents.getName())
                            .add(dateTimeToSQLValue(albumContents.getDateUpdated()))
                            .add(albumContents.getEtag())
                            .add(albumContents.getNumNewPhotos())
                            .addNullable(albumContents.getLastAccess() == null ? null : dateTimeToSQLValue(albumContents.getLastAccess())));


            // Will be filled with all the users from:
            //  - The authors of all the photos
            //  - The album member list
            // And then will be written to the DB
            HashMap<Long, AlbumUser> allUsers = new HashMap<Long, AlbumUser>();

            // Keep track of all the new photoIds in an efficient data structure
            HashSet<String> photoIds = new HashSet<String>();

            int num = 0;
            for (AlbumPhoto albumPhoto : albumContents.getPhotos()) {
                AlbumServerPhoto photo = albumPhoto.getServerPhoto();
                if (photo == null) {
                    throw new IllegalArgumentException("albumContents is not allowed to contain an AlbumUploadingPhoto: " + albumPhoto.getUploadingPhoto().toString());
                }

                photoIds.add(photo.getId());

                mConn.update(""
                        + "INSERT OR REPLACE INTO photo (photo_album, num, photo_id, url, author_id, created)"
                        + " VALUES (?, ?, ?, ?, ?, ?)",
                        SQLValues.create()
                                .add(albumId)
                                .add(num++)
                                .add(photo.getId())
                                .add(photo.getUrl())
                                .add(photo.getAuthor().getMemberId())
                                .add(dateTimeToSQLValue(photo.getDateAdded())));

                AlbumUser user = photo.getAuthor();
                allUsers.put(user.getMemberId(), user);
            }

            // Delete any old rows in the database that are not in photIds:
            SQLCursor photosCursor = mConn.query(""
                    + "SELECT photo_id"
                    + " FROM photo"
                    + " WHERE photo_album=?",
                    SQLValues.create()
                            .add(albumId));

            try {
                while (photosCursor.moveToNext()) {
                    String id = photosCursor.getString(0);
                    if (!photoIds.contains(id)) {
                        mConn.update(""
                                + "DELETE FROM photo"
                                + " WHERE photo_album=? AND photo_id=?",
                                SQLValues.create()
                                        .add(albumId)
                                        .add(id));
                    }
                }
            } finally {
                photosCursor.close();
            }

            // Keep track of all the new memberIds in an efficient data structure
            HashSet<Long> memberIds = new HashSet<Long>();

            for (AlbumMember member : albumContents.getMembers()) {
                AlbumUser user = member.getUser();

                memberIds.add(user.getMemberId());

                mConn.update(""
                        + "INSERT OR REPLACE INTO album_member (album_id, user_id)"
                        + " VALUES (?, ?)",
                        SQLValues.create()
                                .add(albumId)
                                .add(user.getMemberId()));

                allUsers.put(user.getMemberId(), user);
            }

            // Delete any old rows in the database that are not in memberIds:
            SQLCursor membersCursor = mConn.query(""
                    + "SELECT user_id"
                    + " FROM album_member"
                    + " WHERE album_member.album_id=?",
                    SQLValues.create()
                            .add(albumId));
            try {
                while (membersCursor.moveToNext()) {
                    long id = membersCursor.getLong(0);
                    if (!memberIds.contains(id)) {
                        mConn.update(""
                                + "DELETE FROM album_member"
                                + " WHERE album_member.album_id=? AND user_id=?",
                                SQLValues.create()
                                        .add(albumId)
                                        .add(id));
                    }
                }
            } finally {
                membersCursor.close();
            }

            for (Map.Entry<Long, AlbumUser> entry : allUsers.entrySet()) {
                AlbumUser user = entry.getValue();

                mConn.update(""
                        + "INSERT OR REPLACE INTO user (user_id, nickname, avatar_url)"
                        + "VALUES (?, ?, ?)",
                        SQLValues.create()
                                .add(user.getMemberId())
                                .add(user.getMemberNickname())
                                .add(user.getMemberAvatarUrl()));
            }

            mConn.setTransactionSuccesful();
        } finally {
            mConn.endTransaction();
        }
    }

    public synchronized void setAlbumLastAccess(long albumId, DateTime lastAccess) throws SQLException {
        mConn.beginTransaction();
        try {
            mConn.update(""
                    + "UPDATE album"
                    + " SET num_new_photos=0, last_access=?"
                    + " WHERE album_id=?",
                    SQLValues.create()
                            .add(dateTimeToSQLValue(lastAccess))
                            .add(albumId));

            mConn.setTransactionSuccesful();
        } finally {
            mConn.endTransaction();
        }
    }

    private static long dateTimeToSQLValue(DateTime dateTime) {
        return dateTime == null ? null : dateTime.getTimeStamp();
    }
}
