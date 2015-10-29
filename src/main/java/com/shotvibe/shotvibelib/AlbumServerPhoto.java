package com.shotvibe.shotvibelib;

public class AlbumServerPhoto {
    public AlbumServerPhoto(String id, String url, AlbumUser author, DateTime dateAdded, ArrayList<AlbumPhotoComment> comments, int globalGlanceScore, int myGlanceScoreDelta, ArrayList<AlbumPhotoGlance> glances) {
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
        if (comments == null) {
            throw new IllegalArgumentException("comments cannot be null");
        }
        if (glances == null) {
            throw new IllegalArgumentException("glances cannot be null");
        }

        mId = id;
        mUrl = url;
        mAuthor = author;
        mDateAdded = dateAdded;
        mComments = comments;
        mGlobalGlanceScore = globalGlanceScore;
        mMyGlanceScoreDelta = myGlanceScoreDelta;
        mGlances = glances;

        mUploadingOriginal = false;
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

    public ArrayList<AlbumPhotoComment> getComments() {
        return mComments;
    }

    public int getGlobalGlanceScore() {
        return mGlobalGlanceScore;
    }

    public int getMyGlanceScoreDelta() {
        return mMyGlanceScoreDelta;
    }

    public ArrayList<AlbumPhotoGlance> getGlances() {
        return mGlances;
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

    public void setUploadingOriginal() {
        mUploadingOriginal = true;
    }

    public boolean getUploadingOriginal() {
        return mUploadingOriginal;
    }

    private final String mId;

    private final String mUrl;

    private final AlbumUser mAuthor;

    private final DateTime mDateAdded;

    private ArrayList<AlbumPhotoComment> mComments;

    private final int mGlobalGlanceScore;

    private final int mMyGlanceScoreDelta;

    private ArrayList<AlbumPhotoGlance> mGlances;

    private boolean mUploadingOriginal;
}
