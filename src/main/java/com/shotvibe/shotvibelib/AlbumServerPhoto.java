package com.shotvibe.shotvibelib;

public class AlbumServerPhoto {
    public static class Params {
        public String id;
        public MediaType mediaType;
        public AlbumServerVideo video;
        public String url;
        public AlbumUser author;
        public DateTime dateAdded;
        public ArrayList<AlbumPhotoComment> comments;
        public int globalGlanceScore;
        public int myGlanceScoreDelta;
        public ArrayList<AlbumPhotoGlance> glances;
    }

    public AlbumServerPhoto(Params params) {
        if (params.id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
        if (params.mediaType == null) {
            throw new IllegalArgumentException("mediaType cannot be null");
        }
        if (params.mediaType == MediaType.VIDEO) {
            if (params.video == null) {
                throw new IllegalArgumentException("video cannot be null when mediaType is MediaType.VIDEO");
            }
        } else {
            if (params.video != null) {
                throw new IllegalArgumentException("video must be null when mediaType is not MediaType.VIDEO");
            }
        }
        if (params.url == null) {
            throw new IllegalArgumentException("url cannot be null");
        }
        if (params.author == null) {
            throw new IllegalArgumentException("author cannot be null");
        }
        if (params.dateAdded == null) {
            throw new IllegalArgumentException("dateAdded cannot be null");
        }
        if (params.comments == null) {
            throw new IllegalArgumentException("comments cannot be null");
        }
        if (params.glances == null) {
            throw new IllegalArgumentException("glances cannot be null");
        }

        mId = params.id;
        mMediaType = params.mediaType;
        mVideo = params.video;
        mUrl = params.url;
        mAuthor = params.author;
        mDateAdded = params.dateAdded;
        mComments = params.comments;
        mGlobalGlanceScore = params.globalGlanceScore;
        mMyGlanceScoreDelta = params.myGlanceScoreDelta;
        mGlances = params.glances;

        mUploadingOriginal = false;
    }

    public String getId() {
        return mId;
    }

    public MediaType getMediaType() {
        return mMediaType;
    }

    public AlbumServerVideo getVideo() {
        return mVideo;
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

    private final MediaType mMediaType;

    private final AlbumServerVideo mVideo;

    private final String mUrl;

    private final AlbumUser mAuthor;

    private final DateTime mDateAdded;

    private ArrayList<AlbumPhotoComment> mComments;

    private final int mGlobalGlanceScore;

    private final int mMyGlanceScoreDelta;

    private ArrayList<AlbumPhotoGlance> mGlances;

    private boolean mUploadingOriginal;
}
