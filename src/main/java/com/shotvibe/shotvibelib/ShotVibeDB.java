package com.shotvibe.shotvibelib;

import java.util.List;
import java.util.Map;

public final class ShotVibeDB {
    private static final String DATABASE_FILENAME = "shotvibe_main.db";
    private static final int DATABASE_VERSION = 9;

    public static class Recipe extends SQLDatabaseRecipe<ShotVibeDB> {
        public Recipe() {
            super(DATABASE_FILENAME, DATABASE_VERSION);
        }

        @Override
        public void populateNewDB(SQLConnection conn) throws SQLException {
            conn.executeSQLScript("create_shotvibe_main.sql");
        }

        @Override
        public void upgradeDB(SQLConnection conn, int oldVersion) throws SQLException {
            // Poor man's migration: just clear the database
            clearDB(conn);
        }

        @Override
        public ShotVibeDB openDB(SQLConnection conn) {
            return new ShotVibeDB(conn);
        }

        /**
         * Clear the database by removing all tables and repopulating it
         * Should be called from within a transaction
         *
         * @param conn
         * @throws SQLException
         */
        private void clearDB(SQLConnection conn) throws SQLException {
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
    }

    private ShotVibeDB(SQLConnection conn) {
        mConn = conn;
    }

    private SQLConnection mConn;

    private static DateTime cursorGetDateTime(SQLCursor cursor, int columnIndex) throws SQLException {
        return DateTime.FromTimeStamp(cursor.getLong(columnIndex));
    }

    public synchronized ArrayList<AlbumSummary> getAlbumList() throws SQLException {
        SQLCursor cursor = mConn.query(""
                + "SELECT album_id, name, date_created, last_updated, num_new_photos, last_access, user.user_id, user.nickname, user.avatar_url"
                + " FROM album"
                + " LEFT OUTER JOIN user"
                + " ON album.creator_id = user.user_id"
                + " ORDER BY last_updated DESC");

        try {
            ArrayList<AlbumSummary> results = new ArrayList<AlbumSummary>();
            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                String name = cursor.getString(1);
                DateTime dateCreated = cursorGetDateTime(cursor, 2);
                DateTime lastUpdated = cursorGetDateTime(cursor, 3);
                final String etag = null;
                final int NUM_LATEST_PHOTOS = 2;
                long numNewPhotos = cursor.getLong(4);
                DateTime lastAccess = cursor.isNull(5) ? null : cursorGetDateTime(cursor, 5);
                ArrayList<AlbumPhoto> latestPhotos = getLatestPhotos(id, NUM_LATEST_PHOTOS);

                long creatorAuthorUserId = cursor.getLong(6);
                String creatorAuthorNickname = cursor.getString(7);
                String creatorAuthorAvatarUrl = cursor.getString(8);
                AlbumUser creator = new AlbumUser(creatorAuthorUserId, creatorAuthorNickname, creatorAuthorAvatarUrl);

                AlbumSummary album = new AlbumSummary(id, etag, name, creator, dateCreated, lastUpdated, numNewPhotos, lastAccess, latestPhotos);
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

                // TODO Load real values from DB
                ArrayList<AlbumPhotoGlance> dummy = new ArrayList<AlbumPhotoGlance>();

                // Latest Photos does not contain comments
                ArrayList<AlbumPhotoComment> emptyComments = new ArrayList<AlbumPhotoComment>();

                int globalGlanceScore = 0;
                int myGlanceScoreDelta = 0;

                MediaType mediaType = MediaType.PHOTO;
                AlbumServerVideo video = null;

                AlbumServerPhoto.Params albumServerPhotoParams = new AlbumServerPhoto.Params();
                albumServerPhotoParams.id = photoId;
                albumServerPhotoParams.mediaType = mediaType;
                albumServerPhotoParams.video = video;
                albumServerPhotoParams.url = photoUrl;
                albumServerPhotoParams.author = photoAuthor;
                albumServerPhotoParams.dateAdded = photoDateAdded;
                albumServerPhotoParams.comments = emptyComments;
                albumServerPhotoParams.globalGlanceScore = globalGlanceScore;
                albumServerPhotoParams.myGlanceScoreDelta = myGlanceScoreDelta;
                albumServerPhotoParams.glances = dummy;
                results.add(new AlbumPhoto(new AlbumServerPhoto(albumServerPhotoParams)));
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

                saveUserToDB(mConn, album.getCreator());

                // First try updating an existing row, in order to not erase an existing etag value
                mConn.update(""
                        + "UPDATE album"
                        + " SET album_id=?, name=?, creator_id=?, date_created=?, last_updated=?, num_new_photos=?, last_access=?"
                        + " WHERE album_id=?",
                        SQLValues.create()
                                .add(album.getId())
                                .add(album.getName())
                                .add(album.getCreator().getMemberId())
                                .add(dateTimeToSQLValue(album.getDateCreated()))
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
                                    + "INSERT OR REPLACE INTO album (album_id, name, creator_id, date_created, last_updated, num_new_photos, last_access)"
                                    + " VALUES (?, ?, ?, ?, ?, ?, ?)",
                            SQLValues.create()
                                    .add(album.getId())
                                    .add(album.getName())
                                    .add(album.getCreator().getMemberId())
                                    .add(dateTimeToSQLValue(album.getDateCreated()))
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
                    + "SELECT name, date_created, last_updated, num_new_photos, last_access, user.user_id, user.nickname, user.avatar_url"
                    + " FROM album"
                    + " LEFT OUTER JOIN user"
                    + " ON album.creator_id = user.user_id"
                    + " WHERE album_id=?",
                    SQLValues.create()
                            .add(albumId));

            String albumName;
            DateTime albumDateCreated;
            DateTime albumLastUpdated;
            String albumEtag;
            long albumNumNewPhotos;
            DateTime albumLastAccess;
            AlbumUser albumCreator;

            try {
                if (!cursor.moveToNext()) {
                    // No cached AlbumContents available
                    return null;
                }

                albumName = cursor.getString(0);
                albumDateCreated = cursorGetDateTime(cursor, 1);
                albumLastUpdated = cursorGetDateTime(cursor, 2);
                albumEtag = null;
                albumNumNewPhotos = cursor.getLong(3);
                albumLastAccess = cursor.isNull(4) ? null : cursorGetDateTime(cursor, 4);

                long creatorAuthorUserId = cursor.getLong(5);
                String creatorAuthorNickname = cursor.getString(6);
                String creatorAuthorAvatarUrl = cursor.getString(7);
                albumCreator = new AlbumUser(creatorAuthorUserId, creatorAuthorNickname, creatorAuthorAvatarUrl);
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

                    ArrayList<AlbumPhotoGlance> photoGlances = new ArrayList<AlbumPhotoGlance>();
                    SQLCursor gCursor = mConn.query(""
                            + "SELECT photo_glance.author_id, user.nickname, user.avatar_url, photo_glance.emoticon_name"
                            + " FROM photo_glance"
                            + " LEFT OUTER JOIN user"
                            + " ON photo_glance.author_id = user.user_id"
                            + " WHERE photo_glance.photo_id=?"
                            + " ORDER BY photo_glance.num ASC",
                            SQLValues.create()
                                    .add(photoId));
                    try {
                        while (gCursor.moveToNext()) {
                            long authorId = gCursor.getLong(0);
                            String authorNickname = gCursor.getString(1);
                            String authorAvatarUrl = gCursor.getString(2);
                            String emoticonName = gCursor.getString(3);
                            AlbumUser author = new AlbumUser(authorId, authorNickname, authorAvatarUrl);
                            photoGlances.add(new AlbumPhotoGlance(author, emoticonName));
                        }
                    } finally {
                        gCursor.close();
                    }

                    ArrayList<AlbumPhotoComment> photoComments = readPhotoComments(mConn, photoId);

                    int globalGlanceScore = 0;
                    int myGlanceScoreDelta = 0;

                    MediaType mediaType = MediaType.PHOTO;
                    AlbumServerVideo video = null;

                    AlbumServerPhoto.Params albumServerPhotoParams = new AlbumServerPhoto.Params();
                    albumServerPhotoParams.id = photoId;
                    albumServerPhotoParams.mediaType = mediaType;
                    albumServerPhotoParams.video = video;
                    albumServerPhotoParams.url = photoUrl;
                    albumServerPhotoParams.author = photoAuthor;
                    albumServerPhotoParams.dateAdded = photoDateAdded;
                    albumServerPhotoParams.comments = photoComments;
                    albumServerPhotoParams.globalGlanceScore = globalGlanceScore;
                    albumServerPhotoParams.myGlanceScoreDelta = myGlanceScoreDelta;
                    albumServerPhotoParams.glances = photoGlances;
                    albumPhotos.add(new AlbumPhoto(new AlbumServerPhoto(albumServerPhotoParams)));
                }
            } finally {
                cursor.close();
            }

            cursor = mConn.query(""
                    + "SELECT album_member.user_id, user.nickname, user.avatar_url, album_member.album_admin, album_member.added_by_user_id"
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
                    boolean albumAdmin = cursor.getInt(3) != 0;
                    long addedByUserId = cursor.getLong(4);
                    AlbumUser user = new AlbumUser(memberId, memberNickname, memberAvatarUrl);
                    albumMembers.add(new AlbumMember(user, albumAdmin, addedByUserId, null));
                }
            } finally {
                cursor.close();
            }

            mConn.setTransactionSuccesful();
            return new AlbumContents(albumId, albumEtag, albumName, albumCreator, albumDateCreated, albumLastUpdated, albumNumNewPhotos, albumLastAccess, albumPhotos, albumMembers);
        } finally {
            mConn.endTransaction();
        }
    }

    private static ArrayList<AlbumPhotoComment> readPhotoComments(SQLConnection conn, String photoId) throws SQLException {
        ArrayList<AlbumPhotoComment> photoComments = new ArrayList<AlbumPhotoComment>();
        SQLCursor cursor = conn.query(""
                        + "SELECT photo_comment.author_id, user.nickname, user.avatar_url, photo_comment.date_created, photo_comment.client_msg_id, photo_comment.comment_text"
                        + " FROM photo_comment"
                        + " LEFT OUTER JOIN user"
                        + " ON photo_comment.author_id = user.user_id"
                        + " WHERE photo_comment.photo_id=?"
                        + " ORDER BY photo_comment.date_created ASC",
                SQLValues.create()
                        .add(photoId));
        try {
            while (cursor.moveToNext()) {
                long authorId = cursor.getLong(0);
                String authorNickname = cursor.getString(1);
                String authorAvatarUrl = cursor.getString(2);
                DateTime dateCreated = cursorGetDateTime(cursor, 3);
                long clientMsgId = cursor.getLong(4);
                String commentText = cursor.getString(5);
                AlbumUser author = new AlbumUser(authorId, authorNickname, authorAvatarUrl);
                photoComments.add(new AlbumPhotoComment(author, clientMsgId, dateCreated, commentText));
            }
        } finally {
            cursor.close();
        }

        return photoComments;
    }

    private static void setAlbumRow(SQLConnection conn, AlbumContents albumContents) throws SQLException {
        conn.update(""
                        + "INSERT OR REPLACE INTO album (album_id, name, creator_id, date_created, last_updated, last_etag, num_new_photos, last_access)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                SQLValues.create()
                        .add(albumContents.getId())
                        .add(albumContents.getName())
                        .add(albumContents.getCreator().getMemberId())
                        .add(dateTimeToSQLValue(albumContents.getDateCreated()))
                        .add(dateTimeToSQLValue(albumContents.getDateUpdated()))
                        .add(albumContents.getEtag())
                        .add(albumContents.getNumNewPhotos())
                        .addNullable(albumContents.getLastAccess() == null ? null : dateTimeToSQLValue(albumContents.getLastAccess())));
    }

    private static void saveAlbumPhotos(SQLConnection conn, long albumId, List<AlbumPhoto> photos, HashMap<Long, AlbumUser> allUsers) throws SQLException {
        // Keep track of all the new photoIds in an efficient data structure
        HashSet<String> photoIds = new HashSet<String>();

        int num = 0;
        for (AlbumPhoto albumPhoto : photos) {
            AlbumServerPhoto photo = albumPhoto.getServerPhoto();
            if (photo == null) {
                throw new IllegalArgumentException("albumContents is not allowed to contain an AlbumUploadingPhoto: " + albumPhoto.getUploadingPhoto().toString());
            }

            photoIds.add(photo.getId());

            conn.update(""
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

            // Save Photo Glances:

            // Keep track of all the new authorIds in an efficient data structure
            HashSet<Long> authorIds = new HashSet<Long>();

            int glanceNum = 0;
            for (AlbumPhotoGlance glance : photo.getGlances()) {
                AlbumUser glanceAuthor = glance.getAuthor();

                authorIds.add(glanceAuthor.getMemberId());

                conn.update(""
                                + "INSERT OR REPLACE INTO photo_glance (photo_id, author_id, emoticon_name, num)"
                                + " VALUES (?, ?, ?, ?)",
                        SQLValues.create()
                                .add(photo.getId())
                                .add(glanceAuthor.getMemberId())
                                .add(glance.getEmoticonName())
                                .add(glanceNum));
                glanceNum++;

                allUsers.put(glanceAuthor.getMemberId(), glanceAuthor);
            }

            // Delete any old rows in the database that are not in authorIds:
            SQLCursor glancesCursor = conn.query(""
                            + "SELECT author_id"
                            + " FROM photo_glance"
                            + " WHERE photo_id=?",
                    SQLValues.create()
                            .add(photo.getId()));
            try {
                while (glancesCursor.moveToNext()) {
                    long id = glancesCursor.getLong(0);
                    if (!authorIds.contains(id)) {
                        conn.update(""
                                        + "DELETE FROM photo_glance"
                                        + " WHERE photo_id=? AND author_id=?",
                                SQLValues.create()
                                        .add(photo.getId())
                                        .add(id));
                    }
                }
            } finally {
                glancesCursor.close();
            }

            savePhotoComments(conn, photo.getId(), photo.getComments(), allUsers);
        }

        // Delete any old rows in the database that are not in photIds:
        SQLCursor photosCursor = conn.query(""
                        + "SELECT photo_id"
                        + " FROM photo"
                        + " WHERE photo_album=?",
                SQLValues.create()
                        .add(albumId));

        try {
            while (photosCursor.moveToNext()) {
                String id = photosCursor.getString(0);
                if (!photoIds.contains(id)) {
                    conn.update(""
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
    }

    private static void savePhotoComments(SQLConnection conn, String photoId, List<AlbumPhotoComment> comments, HashMap<Long, AlbumUser> allUsers) throws SQLException {
        for (AlbumPhotoComment comment : comments) {
            conn.update(""
                            + "INSERT OR REPLACE INTO photo_comment (photo_id, date_created, author_id, client_msg_id, comment_text)"
                            + " VALUES (?, ?, ?, ?, ?)",
                    SQLValues.create()
                            .add(photoId)
                            .add(dateTimeToSQLValue(comment.getDateCreated()))
                            .add(comment.getAuthor().getMemberId())
                            .add(comment.getClientMsgId())
                            .add(comment.getCommentText()));

            allUsers.put(comment.getAuthor().getMemberId(), comment.getAuthor());
        }

        // Delete any old comments in the database that are not in comments:
        SQLCursor commentsCursor = conn.query(""
                        + "SELECT author_id, client_msg_id"
                        + " FROM photo_comment"
                        + " WHERE photo_id=?",
                SQLValues.create()
                        .add(photoId));
        try {
            while (commentsCursor.moveToNext()) {
                long authorId = commentsCursor.getLong(0);
                long clientMsgId = commentsCursor.getLong(1);
                // Check if there is a comment matching authorId + clientMsgId
                boolean foundMatch = false;
                for (AlbumPhotoComment c : comments) {
                    if (c.getAuthor().getMemberId() == authorId && c.getClientMsgId() == clientMsgId) {
                        foundMatch = true;
                        break;
                    }
                }
                if (!foundMatch) {
                    conn.update(""
                                    + "DELETE FROM photo_comment"
                                    + " WHERE photo_id=? AND author_id=? AND client_msg_id",
                            SQLValues.create()
                                    .add(photoId)
                                    .add(authorId)
                                    .add(clientMsgId));
                }
            }
        } finally {
            commentsCursor.close();
        }
    }

    private static void saveAlbumMembers(SQLConnection conn, long albumId, List<AlbumMember> albumMembers, HashMap<Long, AlbumUser> allUsers) throws SQLException {
        // Keep track of all the new memberIds in an efficient data structure
        HashSet<Long> memberIds = new HashSet<Long>();

        for (AlbumMember member : albumMembers) {
            AlbumUser user = member.getUser();

            memberIds.add(user.getMemberId());

            conn.update(""
                            + "INSERT OR REPLACE INTO album_member (album_id, user_id, album_admin, added_by_user_id)"
                            + " VALUES (?, ?, ?, ?)",
                    SQLValues.create()
                            .add(albumId)
                            .add(user.getMemberId())
                            .add(member.getAlbumAdmin() ? 1 : 0)
                            .add(member.getAddedByUserId()));

            allUsers.put(user.getMemberId(), user);
        }

        // Delete any old rows in the database that are not in memberIds:
        SQLCursor membersCursor = conn.query(""
                        + "SELECT user_id"
                        + " FROM album_member"
                        + " WHERE album_member.album_id=?",
                SQLValues.create()
                        .add(albumId));
        try {
            while (membersCursor.moveToNext()) {
                long id = membersCursor.getLong(0);
                if (!memberIds.contains(id)) {
                    conn.update(""
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
    }

    /**
     * @param albumId
     * @param albumContents Must contain only photos of type AlbumServerPhoto, no AlbumUploadingPhotos allowed!
     */
    public synchronized void setAlbumContents(long albumId, AlbumContents albumContents) throws SQLException {
        mConn.beginTransaction();
        try {
            saveUserToDB(mConn, albumContents.getCreator());

            setAlbumRow(mConn, albumContents);

            // Will be filled with all the users from:
            //  - The authors of all the photos
            //  - The album member list
            // And then will be written to the DB
            HashMap<Long, AlbumUser> allUsers = new HashMap<Long, AlbumUser>();

            saveAlbumPhotos(mConn, albumId, albumContents.getPhotos(), allUsers);
            saveAlbumMembers(mConn, albumId, albumContents.getMembers(), allUsers);

            for (Map.Entry<Long, AlbumUser> entry : allUsers.entrySet()) {
                AlbumUser user = entry.getValue();

                saveUserToDB(mConn, user);
            }

            mConn.setTransactionSuccesful();
        } finally {
            mConn.endTransaction();
        }
    }

    private static void saveUserToDB(SQLConnection conn, AlbumUser user) throws SQLException {
        conn.update(""
                        + "INSERT OR REPLACE INTO user (user_id, nickname, avatar_url)"
                        + " VALUES (?, ?, ?)",
                SQLValues.create()
                        .add(user.getMemberId())
                        .add(user.getMemberNickname())
                        .add(user.getMemberAvatarUrl()));
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

    /**
     * @return Returns null if no result found
     * @throws SQLException
     */
    public synchronized String getUserNickname(long userId) throws SQLException {
        SQLCursor cursor = mConn.query(""
                + "SELECT nickname"
                + " FROM user"
                + " WHERE user_id=?",
                SQLValues.create()
        .add(userId));

        try {
            if (cursor.moveToNext()) {
                String nickname = cursor.getString(0);
                return nickname;
            } else {
                return null;
            }
        } finally {
            cursor.close();
        }
    }


    public synchronized void addQueriedPhoneContacts(ArrayList<PhoneContactServerResult> contacts) throws SQLException {
        mConn.beginTransaction();
        try {
            for (PhoneContactServerResult contact : contacts) {
                if (contact.getPhoneType() == PhoneContactServerResult.PhoneType.MOBILE) {
                    mConn.update(""
                            + "INSERT OR REPLACE INTO phone_contact (phone_number, last_name, first_name, is_mobile, user_id, avatar_url, canonical_number, query_time)"
                            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                            SQLValues.create()
                                    .add(contact.getPhoneContact().getPhoneNumber())
                                    .add(contact.getPhoneContact().getLastName())
                                    .add(contact.getPhoneContact().getFirstName())
                                    .add(1)
                                    .addNullable(contact.getUserId())
                                    .add(contact.getAvatarUrl())
                                    .add(contact.getCanonicalPhoneNumber())
                                    .add(dateTimeToSQLValue(contact.getQueryTime())));
                } else {
                    mConn.update(""
                            + "INSERT OR REPLACE INTO phone_contact (phone_number, last_name, first_name, is_mobile, user_id, avatar_url, canonical_number, query_time)"
                            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                            SQLValues.create()
                                    .add(contact.getPhoneContact().getPhoneNumber())
                                    .add(contact.getPhoneContact().getLastName())
                                    .add(contact.getPhoneContact().getFirstName())
                                    .add(0)
                                    .addNull()
                                    .addNull()
                                    .addNull()
                                    .add(dateTimeToSQLValue(contact.getQueryTime())));
                }
            }
            mConn.setTransactionSuccesful();
        } finally {
            mConn.endTransaction();
        }
    }

    public synchronized HashMap<PhoneContact, PhoneContactServerResult> getAllCachedPhoneContacts() throws SQLException {
        SQLCursor cursor = mConn.query(""
                + "SELECT phone_number, last_name, first_name, is_mobile, user_id, avatar_url, canonical_number, query_time"
                + " FROM phone_contact");

        try {
            HashMap<PhoneContact, PhoneContactServerResult> results = new HashMap<PhoneContact, PhoneContactServerResult>();
            while (cursor.moveToNext()) {
                String phoneNumber = cursor.getString(0);
                String lastName = cursor.getString(1);
                String firstName = cursor.getString(2);
                boolean isMobile = cursor.getInt(3) != 0;

                DateTime queryTime = cursorGetDateTime(cursor, 7);

                PhoneContact phoneContact = new PhoneContact(phoneNumber, lastName, firstName);

                PhoneContactServerResult serverResult;
                if (isMobile) {
                    Long userId;
                    if (cursor.isNull(4)) {
                        userId = null;
                    } else {
                        userId = cursor.getLong(4);
                    }
                    String avatarUrl = cursor.getString(5);
                    String canonicalPhoneNumber = cursor.getString(6);
                    serverResult = PhoneContactServerResult.createMobileResult(phoneContact, userId, avatarUrl, canonicalPhoneNumber, queryTime);
                } else {
                    // TODO Arbitrarily specifying INVALID is not the cleanest. But the phone type
                    // isn't currently used for anything (other than checking if MOBILE)
                    PhoneContactServerResult.PhoneType phoneType = PhoneContactServerResult.PhoneType.INVALID;

                    serverResult = PhoneContactServerResult.createNonMobileResult(phoneContact, phoneType, queryTime);
                }
                results.put(phoneContact, serverResult);
            }
            return results;
        } finally {
            cursor.close();
        }
    }

    private static long dateTimeToSQLValue(DateTime dateTime) {
        return dateTime == null ? null : dateTime.getTimeStamp();
    }
}
