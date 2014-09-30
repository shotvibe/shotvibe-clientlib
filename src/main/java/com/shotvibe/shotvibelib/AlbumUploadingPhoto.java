package com.shotvibe.shotvibelib;

public final class AlbumUploadingPhoto {
    private AlbumUploadingPhoto(String tmpFile, State state, String photoId, double uploadProgress) {
        mTmpFile = tmpFile;
        mState = state;
        mPhotoId = photoId;
        mUploadProgress = uploadProgress;
    }

    public static AlbumUploadingPhoto NewPreparingFiles(String tmpFile) {
        if (tmpFile == null) {
            throw new IllegalArgumentException("tmpFile cannot be null");
        }
        return new AlbumUploadingPhoto(tmpFile, State.PreparingFiles, null, 0.0);
    }

    public static AlbumUploadingPhoto NewUploading(String tmpFile) {
        if (tmpFile == null) {
            throw new IllegalArgumentException("tmpFile cannot be null");
        }
        return new AlbumUploadingPhoto(tmpFile, State.Uploading, null, 0.0);
    }

    public static AlbumUploadingPhoto NewUploaded(String tmpFile, String photoId) {
        if (tmpFile == null) {
            throw new IllegalArgumentException("tmpFile cannot be null");
        }
        if (photoId == null) {
            throw new IllegalArgumentException("photoId cannot be null");
        }
        return new AlbumUploadingPhoto(tmpFile, State.Uploaded, photoId, 1.0);
    }

    public static AlbumUploadingPhoto NewAddingToAlbum(String tmpFile, String photoId) {
        if (tmpFile == null) {
            throw new IllegalArgumentException("tmpFile cannot be null");
        }
        if (photoId == null) {
            throw new IllegalArgumentException("photoId cannot be null");
        }
        return new AlbumUploadingPhoto(tmpFile, State.AddingToAlbum, photoId, 1.0);
    }

    public enum State {
        PreparingFiles,
        Uploading,
        Uploaded,
        AddingToAlbum
    }

    public synchronized void setUploading() {
        if (mState != State.PreparingFiles) {
            throw new IllegalStateException("Illegal transition from state: " + mState);
        }
        mState = State.Uploading;
    }

    public synchronized void setUploaded(String photoId) {
        if (mState != State.Uploading) {
            throw new IllegalStateException("Illegal transition from state: " + mState);
        }
        if (photoId == null) {
            throw new IllegalArgumentException("photoId cannot be null");
        }
        mState = State.Uploaded;
        mPhotoId = photoId;
        mUploadProgress = 1.0;
    }

    public synchronized void setAddingToAlbum() {
        if (mState != State.Uploaded) {
            throw new IllegalStateException("Illegal transition from state: " + mState);
        }
        mState = State.AddingToAlbum;
    }

    public String getBitmapResizedPath() {
        return mTmpFile + UploadManager.RESIZED_FILE_SUFFIX;
    }

    public String getBitmapThumbPath() {
        return mTmpFile + UploadManager.THUMB_FILE_SUFFIX;
    }

    public String getTmpFile() {
        return mTmpFile;
    }

    public synchronized State getState() {
        return mState;
    }

    public synchronized void setUploadProgress(double progress) {
        if (mState != State.Uploading) {
            throw new IllegalStateException("Illegal setUploadProgress in state: " + mState + " for tmpFile: " + mTmpFile);
        }
        mUploadProgress = progress;
    }

    public synchronized double getUploadProgress() {
        return mUploadProgress;
    }

    /**
     *
     * @return Will return null if the state is @State.PreparingFiles@ or @State.Uploading@
     */
    public synchronized String getPhotoId() {
        return mPhotoId;
    }

    private final String mTmpFile;

    private State mState;
    private String mPhotoId;
    private double mUploadProgress;
}
