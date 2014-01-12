package com.shotvibe.shotvibelib;

public class AlbumServerPhoto {
    public AlbumServerPhoto(String id, String url, AlbumUser author, DateTime dateAdded) {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
        if (url == null) {
            throw new IllegalArgumentException("url cannot be null");
        }
        if (author == null) {
            throw new IllegalArgumentException("author cannot be null");
        }
        if (dateAdded == null) {
            throw new IllegalArgumentException("dateAdded cannot be null");
        }

        mId = id;
        mUrl = url;
        mAuthor = author;
        mDateAdded = dateAdded;
    }

    public String getId() {
        return mId;
    }

    public String getUrl() {
        return mUrl;
    }

    public AlbumUser getAuthor() {
        return mAuthor;
    }

    public DateTime getDateAdded() {
        return mDateAdded;
    }

    public boolean isNew(DateTime lastAccess, long userId) {
        if (mAuthor.getMemberId() == userId) {
            return false;
        }

        if (lastAccess == null) {
            return true;
        }

        return (lastAccess.getTimeStamp() < mDateAdded.getTimeStamp());
    }

    private final String mId;

    private final String mUrl;

    private final AlbumUser mAuthor;

    private final DateTime mDateAdded;
}
