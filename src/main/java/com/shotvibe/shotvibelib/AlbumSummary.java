package com.shotvibe.shotvibelib;

public class AlbumSummary extends AlbumBase {

    public AlbumSummary(long id, String etag, String name, AlbumUser creator, DateTime dateCreated,
                        DateTime dateUpdated, long numNewPhotos, DateTime lastAccess,
                        ArrayList<AlbumPhoto> latestPhotos) {
        super(id, etag, name, creator, dateCreated, dateUpdated, numNewPhotos, lastAccess);

        if (latestPhotos == null) {
            throw new IllegalArgumentException("latestPhotos cannot be null");
        }

        mLatestPhotos = latestPhotos;
    }

    public ArrayList<AlbumPhoto> getLatestPhotos() {
        return mLatestPhotos;
    }

    private final ArrayList<AlbumPhoto> mLatestPhotos;

    // TODO Last Updated

    // TODO Last Updated User
}
