package com.shotvibe.shotvibelib;

public interface BitmapProcessor {
    int RESIZED_PHOTO_WIDTH = 1920;
    int RESIZED_PHOTO_HEIGHT = 1080;

    public class ResizedResult {
        public ResizedResult(
                boolean success,
                int originalWidth,
                int originalHeight,
                int resizedWidth,
                int resizedHeight) {
            this.success = success;
            this.originalWidth = originalWidth;
            this.originalHeight = originalHeight;
            this.resizedWidth = resizedWidth;
            this.resizedHeight = resizedHeight;
        }

        public final boolean success;
        public final int originalWidth;
        public final int originalHeight;
        public final int resizedWidth;
        public final int resizedHeight;
    }

    /**
     * The size for the resized image should be the constants
     * UploadManager.RESIZED_IMAGE_TARGET_WIDTH and UploadManager.RESIZED_IMAGE_TARGET_HEIGHT and
     * should use the "boxFitWithRotationOnlyShrink" algorithm.
     *
     * The size for the thumbnail image should be an appropriate size suitable for the device.
     *
     * @param originalPath
     * @param resizedSavePath
     * @param thumbSavePath
     * @return
     */
    ResizedResult createResizedAndThumbnail(String originalPath, String resizedSavePath, String thumbSavePath);
}
