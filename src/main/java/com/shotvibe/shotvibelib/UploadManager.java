package com.shotvibe.shotvibelib;

import java.util.List;

public interface UploadManager {
    List<AlbumPhoto> getUploadingPhotos(long albumId);

    void uploadPhotos(long albumId, List<PhotoUploadRequest> photoUploadRequests);

    /**
     * UploadManager may have a single listener. All listener events are sent on the main thread
     * @param listener may be null to remove
     */
    void setListener(Listener listener);

    String getUploadsDir();

    int RESIZED_IMAGE_TARGET_WIDTH = 1920;
    int RESIZED_IMAGE_TARGET_HEIGHT = 1080;

    String RESIZED_FILE_SUFFIX = ".resized.jpg";
    String THUMB_FILE_SUFFIX = ".thumb.jpg";

    public interface Listener {
        void refreshAlbum(long albumId);

        /**
         * @param newAlbumContents The exact result returned from the server. This does not contain
         *                         any remaining AlbumUploadPhotos that may be present in the album,
         *                         so the Listener should manually add them if deemed necessary
         *                         (using {@link UploadManager#getUploadingPhotos}). The Listener
         *                         is free to modify newAlbumContents as it wishes
         */
        void photosAddedToAlbum(long albumId, AlbumContents newAlbumContents);
    }
}
