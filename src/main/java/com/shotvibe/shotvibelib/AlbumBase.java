package com.shotvibe.shotvibelib;

public class AlbumBase {
    public AlbumBase(long id, String etag, String name, AlbumUser creator, DateTime dateCreated, DateTime dateUpdated, long numNewPhotos, DateTime lastAccess) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        if (creator == null) {
            throw new IllegalArgumentException("creator cannot be null");
        }
        if (dateCreated == null) {
            throw new IllegalArgumentException("dateCreated cannot be null");
        }
        if (dateUpdated == null) {
            throw new IllegalArgumentException("dateUpdated cannot be null");
        }

        mId = id;
        mEtag = etag;
        mName = name;
        mCreator = creator;
        mDateCreated = dateCreated;
        mDateUpdated = dateUpdated;
        mNumNewPhotos = numNewPhotos;
        mLastAccess = lastAccess;
    }

    public long getId() {
        return mId;
    }

    public String getEtag() {
        return mEtag;
    }

    public String getName() {
        return mName;
    }

    public AlbumUser getCreator() {
        return mCreator;
    }

    public DateTime getDateCreated() {
        return mDateCreated;
    }

    public DateTime getDateUpdated() {
        return mDateUpdated;
    }

    public DateTime getLastAccess() {
        return mLastAccess;
    }

    public long getNumNewPhotos() {
        return mNumNewPhotos;
    }

    private final long mId;
    private final String mEtag;
    private final String mName;
    private final AlbumUser mCreator;
    private final DateTime mDateCreated;
    private final DateTime mDateUpdated;
    private final long mNumNewPhotos;
    private final DateTime mLastAccess;
}
