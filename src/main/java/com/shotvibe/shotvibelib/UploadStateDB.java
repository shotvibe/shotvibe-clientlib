package com.shotvibe.shotvibelib;

/**
 * Note: The methods of this class are not thread safe. Callers should take care that all methods
 * are called serially. This was the design choice because the methods of {@link UploadStateAccess}
 * are already synchronized
 */
public final class UploadStateDB {
    private static final String DATABASE_FILENAME = "shotvibe_upload_state.db";
    private static final int DATABASE_VERSION = 1;

    public static class Recipe extends SQLDatabaseRecipe<UploadStateDB> {
        public Recipe() {
            super(DATABASE_FILENAME, DATABASE_VERSION);
        }

        @Override
        public void populateNewDB(SQLConnection conn) throws SQLException {
            conn.executeSQLScript("create_shotvibe_upload_state.sql");
        }

        @Override
        public void upgradeDB(SQLConnection conn, int oldVersion) throws SQLException {
            throw new RuntimeException("upgradeDB not yet supported");
        }

        @Override
        public UploadStateDB openDB(SQLConnection conn) {
            return new UploadStateDB(conn);
        }
    }

    private UploadStateDB(SQLConnection conn) {
        mConn = conn;
    }

    private SQLConnection mConn;

    public ArrayList<UploadingPhoto> getAllUploadingPhotos() throws SQLException {
        SQLCursor cursor = mConn.query(""
                + "SELECT album_id, tmp_filename, upload_strategy, upload_state, photo_id"
                + " FROM uploading_photo"
                + " ORDER BY num ASC");

        try {
            ArrayList<UploadingPhoto> results = new ArrayList<UploadingPhoto>();
            while (cursor.moveToNext()) {
                long albumId = cursor.getLong(0);
                String tmpFilename = cursor.getString(1);
                int uploadStrategyInt = cursor.getInt(2);
                UploadingPhoto.UploadStrategy uploadStrategy = readUploadStrategy(uploadStrategyInt);
                int uploadStateInt = cursor.getInt(3);
                UploadingPhoto.UploadState uploadState = readUploadState(uploadStateInt);
                String photoId;
                if (cursor.isNull(4)) {
                    photoId = null;
                } else {
                    photoId = cursor.getString(4);
                }
                results.add(new UploadingPhoto(
                        albumId,
                        tmpFilename,
                        uploadStrategy,
                        uploadState,
                        photoId));
            }
            return results;
        } finally {
            cursor.close();
        }
    }

    public void insertUploadingPhoto(UploadingPhoto uploadingPhoto) throws SQLException {
        mConn.beginTransaction();
        try {
            String photoId;
            if (uploadingPhoto.getUploadState() == UploadingPhoto.UploadState.Queued) {
                photoId = null;
            } else {
                photoId = uploadingPhoto.getPhotoId();
            }
            mConn.update(""
                            + "INSERT INTO uploading_photo (album_id, tmp_filename, upload_strategy, upload_state, photo_id)"
                            + " VALUES (?, ?, ?, ?, ?)",
                    SQLValues.create()
                            .add(uploadingPhoto.getAlbumId())
                            .add(uploadingPhoto.getTmpFilename())
                            .add(uploadingPhoto.getUploadStrategy().ordinal())
                            .add(uploadingPhoto.getUploadState().ordinal())
                            .addNullable(photoId));
            mConn.setTransactionSuccesful();
        } finally {
            mConn.endTransaction();
        }
    }

    public void setPhotoUploadStrategy(UploadingPhoto uploadingPhoto) throws SQLException {
        mConn.beginTransaction();
        try {
            mConn.update(""
                            + "UPDATE uploading_photo"
                            + " SET upload_strategy=?"
                            + " WHERE tmp_filename=?",
                    SQLValues.create()
                            .add(uploadingPhoto.getUploadStrategy().ordinal())
                            .add(uploadingPhoto.getTmpFilename()));

            mConn.setTransactionSuccesful();
        } finally {
            mConn.endTransaction();
        }
    }

    public void setPhotoUploaded(UploadingPhoto uploadingPhoto) throws SQLException {
        mConn.beginTransaction();
        try {
            mConn.update(""
                            + "UPDATE uploading_photo"
                            + " SET upload_state=?, photo_id=?"
                            + " WHERE tmp_filename=?",
                    SQLValues.create()
                            .add(uploadingPhoto.getUploadState().ordinal())
                            .add(uploadingPhoto.getPhotoId())
                            .add(uploadingPhoto.getTmpFilename()));

            mConn.setTransactionSuccesful();
        } finally {
            mConn.endTransaction();
        }
    }

    public void photosAddedToAlbum(HashSet<String> tmpFilenames) throws SQLException {
        mConn.beginTransaction();
        try {
            for (String tmpFilename : tmpFilenames) {
                // First assume that the UploadStrategy was UploadTwoStage and update the state
                mConn.update(""
                                + "UPDATE uploading_photo"
                                + " SET upload_state=?"
                                + " WHERE tmp_filename=? AND upload_strategy=?",
                        SQLValues.create()
                                .add(UploadingPhoto.UploadState.AddedToAlbum.ordinal())
                                .add(tmpFilename)
                                .add(UploadingPhoto.UploadStrategy.UploadTwoStage.ordinal()));

                if (mConn.changes() == 0) {
                    // The previous update didn't have an effect, so we know that we are dealing
                    // with with an UploadStrategy of UploadOriginalDirectly.
                    //
                    // (Hm... the upload_strategy condition below is redundant...)
                    mConn.update(""
                                    + "DELETE FROM uploading_photo"
                                    + " WHERE tmp_filename=? AND upload_strategy=?",
                            SQLValues.create()
                                    .add(tmpFilename)
                                    .add(UploadingPhoto.UploadStrategy.UploadOriginalDirectly.ordinal()));
                }
            }

            mConn.setTransactionSuccesful();
        } finally {
            mConn.endTransaction();
        }
    }

    private static UploadingPhoto.UploadStrategy readUploadStrategy(int val) {
        if (val == UploadingPhoto.UploadStrategy.Unknown.ordinal()) {
            return UploadingPhoto.UploadStrategy.Unknown;
        }
        if (val == UploadingPhoto.UploadStrategy.UploadOriginalDirectly.ordinal()) {
            return UploadingPhoto.UploadStrategy.UploadOriginalDirectly;
        }
        if (val == UploadingPhoto.UploadStrategy.UploadTwoStage.ordinal()) {
            return UploadingPhoto.UploadStrategy.UploadTwoStage;
        }
        throw new IllegalStateException("Invalid value for UploadStrategy: " + val);
    }

    private static UploadingPhoto.UploadState readUploadState(int val) {
        if (val == UploadingPhoto.UploadState.Queued.ordinal()) {
            return UploadingPhoto.UploadState.Queued;
        }
        if (val == UploadingPhoto.UploadState.Uploaded.ordinal()) {
            return UploadingPhoto.UploadState.Uploaded;
        }
        if (val == UploadingPhoto.UploadState.AddedToAlbum.ordinal()) {
            return UploadingPhoto.UploadState.AddedToAlbum;
        }
        throw new IllegalStateException("Invalid value for UploadState: " + val);
    }

}
