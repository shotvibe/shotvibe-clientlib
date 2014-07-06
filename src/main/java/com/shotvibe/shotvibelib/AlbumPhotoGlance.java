package com.shotvibe.shotvibelib;

public class AlbumPhotoGlance {
    public AlbumPhotoGlance(AlbumUser author, String emoticonName) {
        if (author == null) {
            throw new IllegalArgumentException("author cannot be null");
        }
        if (emoticonName == null) {
            throw new IllegalArgumentException("emoticonName cannot be null");
        }

        mAuthor = author;
        mEmoticonName = emoticonName;
    }

    public AlbumUser getAuthor() {
        return mAuthor;
    }

    public String getEmoticonName() {
        return mEmoticonName;
    }

    private final AlbumUser mAuthor;
    private final String mEmoticonName;
}
