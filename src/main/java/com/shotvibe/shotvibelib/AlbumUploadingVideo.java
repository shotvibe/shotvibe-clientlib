package com.shotvibe.shotvibelib;

public class AlbumUploadingVideo {
    public AlbumUploadingVideo(String previewImageFile) {
        if (previewImageFile == null) {
            throw new IllegalArgumentException("previewImageFile cannot be null");
        }

        mPreviewImageFile = previewImageFile;
    }

    public String getPreviewImageFile() {
        return mPreviewImageFile;
    }

    private final String mPreviewImageFile;
}
