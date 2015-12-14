package com.shotvibe.shotvibelib;

public class AlbumUploadingMediaPhoto {
    public AlbumUploadingMediaPhoto(String previewImageFile) {
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
