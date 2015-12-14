package com.shotvibe.shotvibelib;

public class AlbumUploadingMedia {
    public AlbumUploadingMedia(MediaType mediaType, AlbumUploadingVideo video, AlbumUploadingMediaPhoto photo, float progress) {
        if (mediaType == null) {
            throw new IllegalArgumentException("mediaType cannot be null");
        }

        if (mediaType == MediaType.VIDEO) {
            if (video == null) {
                throw new IllegalArgumentException("video cannot be null when mediaType is MediaType.VIDEO");
            }
        } else {
            if (video != null) {
                throw new IllegalArgumentException("video must be null when mediaType is not MediaType.VIDEO");
            }
        }

        if (mediaType == MediaType.PHOTO) {
            if (photo == null) {
                throw new IllegalArgumentException("photo cannot be null when mediaType is MediaType.PHOTO");
            }
        } else {
            if (photo != null) {
                throw new IllegalArgumentException("photo must be null when mediaType is not MediaType.PHOTO");
            }
        }

        mMediaType = mediaType;
        mVideo = video;
        mPhoto = photo;
        mProgress = progress;
    }

    public MediaType getMediaType() {
        return mMediaType;
    }

    public AlbumUploadingVideo getVideo() {
        if (mMediaType != MediaType.VIDEO) {
            throw new IllegalStateException("mediaType is not MediaType.VIDEO");
        }

        return mVideo;
    }

    public AlbumUploadingMediaPhoto getPhoto() {
        if (mMediaType != MediaType.PHOTO) {
            throw new IllegalStateException("mediaType is not MediaType.PHOTO");
        }

        return mPhoto;
    }

    public float getProgress() {
        return mProgress;
    }

    public void setProgress(float progress) {
        mProgress = progress;
    }

    private final MediaType mMediaType;
    private final AlbumUploadingVideo mVideo;
    private final AlbumUploadingMediaPhoto mPhoto;

    private float mProgress;
}
