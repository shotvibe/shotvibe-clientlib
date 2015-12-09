package com.shotvibe.shotvibelib;

public class AlbumPhoto {
    public AlbumPhoto(AlbumServerPhoto photo) {
        if (photo == null) {
            throw new IllegalArgumentException("photo cannot be null");
        }

        mServerPhoto = photo;
        mUploadingPhoto = null;
        mUploadingMedia = null;
    }

    public AlbumPhoto(AlbumUploadingPhoto uploadingPhoto) {
        if (uploadingPhoto == null) {
            throw new IllegalArgumentException("uploadingPhoto cannot be null");
        }

        mUploadingPhoto = uploadingPhoto;
        mServerPhoto = null;
        mUploadingMedia = null;
    }

    public AlbumPhoto(AlbumUploadingMedia uploadingMedia) {
        if (uploadingMedia == null) {
            throw new IllegalArgumentException("uploadingMedia cannot be null");
        }

        mUploadingMedia = uploadingMedia;
        mServerPhoto = null;
        mUploadingPhoto = null;
    }

    public AlbumServerPhoto getServerPhoto() {
        return mServerPhoto;
    }

    public AlbumUploadingPhoto getUploadingPhoto() {
        return mUploadingPhoto;
    }

    public AlbumUploadingMedia getUploadingMedia() {
        return mUploadingMedia;
    }

    public String getAuthorNickname() {
        if (mServerPhoto != null) {
            return mServerPhoto.getAuthor().getMemberNickname();
        } else {
            // TODO return the nickname of the local user
            return "me";
        }
    }

    private final AlbumServerPhoto mServerPhoto;
    private final AlbumUploadingPhoto mUploadingPhoto;
    private final AlbumUploadingMedia mUploadingMedia;
}
