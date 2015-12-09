package com.shotvibe.shotvibelib;

import java.util.List;

public interface MediaUploader {
    List<AlbumUploadingMedia> getUploadingMedia(long albumId);

    void setListener(Listener listener);

    public interface Listener {
        void onMediaUploadProgress(long albumId);
        void onMediaUploadObjectsChanged(long albumId);
    }
}
