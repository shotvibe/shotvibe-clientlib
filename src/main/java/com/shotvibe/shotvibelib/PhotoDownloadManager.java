package com.shotvibe.shotvibelib;

public interface PhotoDownloadManager {
    void queuePhotoForDownload(String photoId, String photoUrl, boolean thumbnail, boolean highPriority);
}
