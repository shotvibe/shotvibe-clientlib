package com.shotvibe.shotvibelib;

public class UploadingPhoto {
    UploadingPhoto(
            long albumId,
            String tmpFilename,
            UploadStrategy uploadStrategy,
            UploadState uploadState,
            String photoId) {
        if (tmpFilename == null) {
            throw new IllegalArgumentException("tmpFilename cannot be null");
        }
        if (uploadStrategy == null) {
            throw new IllegalArgumentException("uploadStrategy cannot be null");
        }
        if (uploadState == null) {
            throw new IllegalArgumentException("uploadState cannot be null");
        }
        if (uploadStrategy == UploadStrategy.Unknown && uploadState != UploadState.Queued) {
            throw new IllegalArgumentException("uploadState must be Queued when UploadStrategy.Unknown");
        }
        if (uploadState == UploadState.Queued && photoId != null) {
            throw new IllegalArgumentException("photoId must be null when UploadState.Queued");
        }
        if (uploadState != UploadState.Queued && photoId == null) {
            throw new IllegalArgumentException("photoId cannot be null when not UploadState.Queued");
        }
        if (uploadState == UploadState.AddedToAlbum && uploadStrategy != UploadStrategy.UploadTwoStage) {
            throw new IllegalArgumentException("uploadState cannot be AddedToAlbum when uploadStrategy is not UploadTwoStage");
        }

        mAlbumId = albumId;
        mTmpFilename = tmpFilename;
        mUploadStrategy = uploadStrategy;
        mUploadState = uploadState;
        mPhotoId = photoId;
    }

    public enum UploadStrategy {
        /**
         * The photo is not yet done processing so the UploadStrategy is not yet known
         */
        Unknown,

        /**
         * Only the original photo will be uploaded, and then it will be added to the album
         */
        UploadOriginalDirectly,

        /**
         * First a resized photo will be uploaded, then it will be added to the album, then the
         * original photo will be uploaded
         */
        UploadTwoStage
    }

    public enum UploadState {
        /**
         * Nothing has been uploaded yet, or the photo is currently being uploaded.
         *
         * PhotoId must be null
         */
        Queued,

        /**
         * The first (or only) photo has been uploaded, but it has not yet been added to the album.
         *
         * PhotoId must not be null
         */
        Uploaded,

        /**
         * The photo is currently being added to an Album. It's albumId should not change.
         *
         * PhotoId must not be null
         */
        AddingToAlbum,

        /**
         * The first photo has been uploaded and added to the album, but the original photo has not
         * yet been uploaded.
         *
         * This is only valid for {@link UploadStrategy.UploadTwoStage}
         *
         * PhotoId must not be null
         */
        AddedToAlbum
    }

    public long getAlbumId() {
        return mAlbumId;
    }

    public String getTmpFilename() {
        return mTmpFilename;
    }

    public UploadStrategy getUploadStrategy() {
        return mUploadStrategy;
    }

    public UploadState getUploadState() {
        return mUploadState;
    }

    public String getPhotoId() {
        if (mUploadState == UploadState.Queued) {
            throw new IllegalArgumentException("getPhotoId cannot be called when UploadState is Queued");
        }
        return mPhotoId;
    }

    private final long mAlbumId;
    private final String mTmpFilename;
    private final UploadStrategy mUploadStrategy;
    private final UploadState mUploadState;
    private final String mPhotoId;
}
