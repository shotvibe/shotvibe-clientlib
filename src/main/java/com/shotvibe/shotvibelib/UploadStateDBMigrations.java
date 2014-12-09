package com.shotvibe.shotvibelib;

public final class UploadStateDBMigrations {
    static void upgradeFromVersion_1_to_2(SQLConnection conn) throws SQLException {
        Log.d("UploadState", "Migrating DB from version 1 to 2");

        // The new value "UploadingPhoto.UploadStrategy.Unknown" was added to the enum with ordinal value 0
        conn.update(""
                + "UPDATE uploading_photo"
                + " SET upload_strategy = upload_strategy + 1");

        // The new value "UploadingPhoto.UploadState.AddingToAlbum" was added to the enum with ordinal value 2
        conn.update(""
                + "UPDATE uploading_photo"
                + " SET upload_state = upload_state + 1"
                + " WHERE upload_state >= 2");
    }

    static void upgradeFromVersion_2_to_3(SQLConnection conn) throws SQLException {
        // This is a drastic measure: Server issues during 2014-12-06 may have caused some
        // inconsistencies with uploads so we are resetting the upload system in this app update
        // by deleting all in-progress uploads
        conn.update("DELETE FROM uploading_photo");
    }

    private UploadStateDBMigrations() {
        // Not used
    }
}
