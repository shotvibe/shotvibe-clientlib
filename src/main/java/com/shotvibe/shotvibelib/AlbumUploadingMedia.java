package com.shotvibe.shotvibelib;

public class AlbumUploadingMedia {
    public AlbumUploadingMedia(MediaType mediaType, AlbumUploadingVideo video, float progress) {
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

        mMediaType = mediaType;
        mVideo = video;
        mProgress = progress;
    }

    public MediaType getMediaType() {
        return mMediaType;
    }

    public AlbumUploadingVideo getVideo() {
        return mVideo;
    }

    public float getProgress() {
        return mProgress;
    }

    public void setProgress(float progress) {
        mProgress = progress;
    }

    private final MediaType mMediaType;
    private final AlbumUploadingVideo mVideo;

    private float mProgress;
}
