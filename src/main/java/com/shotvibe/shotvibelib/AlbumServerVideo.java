package com.shotvibe.shotvibelib;

public class AlbumServerVideo {
    public static enum Status {
        READY,
        PROCESSING,
        INVALID
    }

    public AlbumServerVideo(Status status, String videoUrl, String videoThumbnailUrl, int videoDuration) {
        if (status == null) {
            throw new IllegalArgumentException("status cannot be null");
        }
        if (videoUrl == null) {
            throw new IllegalArgumentException("videoUrl cannot be null");
        }
        if (videoThumbnailUrl == null) {
            throw new IllegalArgumentException("videoThumbnailUrl cannot be null");
        }

        mStatus = status;
        mVideoUrl = videoUrl;
        mVideoThumbnailUrl = videoThumbnailUrl;
        mVideoDuration = videoDuration;
    }

    public Status getStatus() {
        return mStatus;
    }

    public String getVideoUrl() {
        return mVideoUrl;
    }

    public String getVideoThumbnailUrl() {
        return mVideoThumbnailUrl;
    }

    public int getVideoDuration() {
        return mVideoDuration;
    }

    private final Status mStatus;

    private final String mVideoUrl;

    private final String mVideoThumbnailUrl;

    private final int mVideoDuration;
}
