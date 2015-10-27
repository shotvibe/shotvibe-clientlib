package com.shotvibe.shotvibelib;

public class AlbumPhotoComment {
    public AlbumPhotoComment(AlbumUser author, long clientMsgId, DateTime dateCreated, String commentText) {
        if (author == null) {
            throw new IllegalArgumentException("author cannot be null");
        }
        if (dateCreated == null) {
            throw new IllegalArgumentException("dateCreated cannot be null");
        }
        if (commentText == null) {
            throw new IllegalArgumentException("commentText cannot be null");
        }

        mAuthor = author;
        mClientMsgId = clientMsgId;
        mDateCreated = dateCreated;
        mCommentText = commentText;
    }

    public AlbumUser getAuthor() {
        return mAuthor;
    }

    public long getClientMsgId() {
        return mClientMsgId;
    }

    public DateTime getDateCreated() {
        return mDateCreated;
    }

    public String getCommentText() {
        return mCommentText;
    }

    private final AlbumUser mAuthor;
    private final long mClientMsgId;
    private final DateTime mDateCreated;
    private final String mCommentText;
}
