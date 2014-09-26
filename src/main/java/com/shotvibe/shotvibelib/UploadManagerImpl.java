package com.shotvibe.shotvibelib;

import java.util.List;
import java.util.Set;

public class UploadManagerImpl implements UploadManager {
    public UploadManagerImpl(
            ShotVibeAPI shotVibeAPI,
            UploadSystemDirector uploadSystemDirector,
            String uploadFilesDir,
            BitmapProcessor bitmapProcessor,
            List<UploadingPhoto> storedUploads) {
        if (shotVibeAPI == null) {
            throw new IllegalArgumentException("shotVibeAPI cannot be null");
        }
        if (uploadSystemDirector == null) {
            throw new IllegalArgumentException("uploadSystemDirector cannot be null");
        }
        if (uploadFilesDir == null) {
            throw new IllegalArgumentException("uploadFilesDir cannot be null");
        }
        if (bitmapProcessor == null) {
            throw new IllegalArgumentException("bitmapProcessor cannot be null");
        }
        if (storedUploads == null) {
            throw new IllegalArgumentException("storedUploads cannot be null");
        }

        mShotVibeAPI = shotVibeAPI;
        mUploadSystemDirector = uploadSystemDirector;
        mUploadsDir = uploadFilesDir;
        mBitmapProcessor = bitmapProcessor;

        mPreperationExecutor = ThreadUtil.createSingleThreadExecutor();
        mListener = null;

        mUploadingPhotos = new UploadingPhotosContainer();

        initFromStoredUploads(storedUploads);

        for (Long albumId : mUploadingPhotos.getAlbums()) {
            checkAndAddToAlbum(albumId);
        }
    }

    public void reportUploadProgress(final long albumId, String tmpFile, double uploadProgress) {
        AlbumUploadingPhoto albumUploadingPhoto;
        synchronized (mUploadingPhotos) {
            albumUploadingPhoto = mUploadingPhotos.findUploadingPhoto(albumId, tmpFile);
        }
        albumUploadingPhoto.setUploadProgress(uploadProgress);
        ThreadUtil.runInMainThread(new ThreadUtil.Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.refreshAlbum(albumId);
                }
            }
        });
    }

    public void reportUploadComplete(final long albumId, String tmpFile, String photoId) {
        AlbumUploadingPhoto albumUploadingPhoto;
        synchronized (mUploadingPhotos) {
            albumUploadingPhoto = mUploadingPhotos.findUploadingPhoto(albumId, tmpFile);
        }

        albumUploadingPhoto.setUploaded(photoId);

        checkAndAddToAlbum(albumId);

        ThreadUtil.runInMainThread(new ThreadUtil.Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.refreshAlbum(albumId);
                }
            }
        });
    }

    public void checkAndAddToAlbum(final long albumId) {
        ArrayList<AlbumUploadingPhoto> toAddToAlbum = null;
        synchronized (mUploadingPhotos) {
            if (mUploadingPhotos.allReadyToAddToAlbum(albumId)) {
                toAddToAlbum = new ArrayList<AlbumUploadingPhoto>();
                for (AlbumUploadingPhoto photo : mUploadingPhotos.getAlbumUploadingPhotos(albumId)) {
                    if (photo.getState() == AlbumUploadingPhoto.State.Uploaded) {
                        photo.setAddingToAlbum();
                        toAddToAlbum.add(photo);
                    }
                }
            }
        }

        if (toAddToAlbum != null) {
            final ArrayList<AlbumUploadingPhoto> finalToAddToAlbum = toAddToAlbum;
            ThreadUtil.runInBackgroundThread(new ThreadUtil.Runnable() {
                @Override
                public void run() {
                    addPhotosToAlbum(albumId, finalToAddToAlbum);
                }
            });
        }
    }

    private static final int ADD_PHOTOS_ERROR_RETRY_TIME = 5000;

    private void addPhotosToAlbum(final long albumId, ArrayList<AlbumUploadingPhoto> photos) {
        ArrayList<String> photoIds = new ArrayList<String>(photos.size());
        HashSet<String> tmpFiles = new HashSet<String>(photos.size());
        for (AlbumUploadingPhoto p : photos) {
            photoIds.add(p.getPhotoId());
            tmpFiles.add(p.getTmpFile());
        }

        AlbumContents newAlbumContents = null;
        while (newAlbumContents == null) {
            try {
                Log.d("UploadSystem", "Adding photos to album...");
                newAlbumContents = mShotVibeAPI.albumAddPhotos(albumId, photoIds);
            } catch (APIException e) {
                Log.d("UploadSystem", "Error Adding photos to album: " + e);
                ThreadUtil.sleep(ADD_PHOTOS_ERROR_RETRY_TIME);
            }
        }
        Log.d("UploadSystem", "Added photos to album");

        synchronized (mUploadingPhotos) {
            mUploadingPhotos.remove(albumId, photos);
        }

        mUploadSystemDirector.reportPhotosAddedToAlbum(tmpFiles);

        // TODO Neat optimization: move tmp thumb file into PhotoFilesManager for instant load

        // TODO Delete uploaded tmp files

        final AlbumContents finalNewAlbumContents = newAlbumContents;
        ThreadUtil.runInMainThread(new ThreadUtil.Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.photosAddedToAlbum(albumId, finalNewAlbumContents);
                }
            }
        });
    }

    private void initFromStoredUploads(List<UploadingPhoto> storedUploads) {
        for (UploadingPhoto photo : storedUploads) {
            if (photo.getUploadState() == UploadingPhoto.UploadState.Queued) {
                AlbumUploadingPhoto albumUploadingPhoto = AlbumUploadingPhoto.NewUploading(photo.getTmpFilename());
                mUploadingPhotos.addUploadingPhoto(photo.getAlbumId(), albumUploadingPhoto);
            } else if (photo.getUploadState() == UploadingPhoto.UploadState.Uploaded) {
                AlbumUploadingPhoto albumUploadingPhoto = AlbumUploadingPhoto.NewUploaded(photo.getTmpFilename(), photo.getPhotoId());
                mUploadingPhotos.addUploadingPhoto(photo.getAlbumId(), albumUploadingPhoto);
            }
        }
    }

    private final String mUploadsDir;
    private final UploadSystemDirector mUploadSystemDirector;
    private final ShotVibeAPI mShotVibeAPI;
    private final BitmapProcessor mBitmapProcessor;
    private Listener mListener;

    @Override
    public List<AlbumPhoto> getUploadingPhotos(long albumId) {
        if (!ThreadUtil.isMainThread()) {
            throw new IllegalStateException("Must be called from the Main Thread");
        }

        ArrayList<AlbumPhoto> result = new ArrayList<AlbumPhoto>();
        synchronized (mUploadingPhotos) {
            for (AlbumUploadingPhoto albumUploadingPhoto : mUploadingPhotos.getAlbumUploadingPhotos(albumId)) {
                result.add(new AlbumPhoto(albumUploadingPhoto));
            }
        }

        return result;
    }

    @Override
    public void uploadPhotos(final long albumId, List<PhotoUploadRequest> photoUploadRequests) {
        if (!ThreadUtil.isMainThread()) {
            throw new IllegalStateException("Must be called from the Main Thread");
        }

        // TODO Set some sort of lock on this album so that `addPhotosToAlbum` is not yet called
        // until the last photo in `photoUploadRequests` has been prepared. Otherwise, a very fast
        // internet connection might upload and add individual photos as they are prepared one by
        // one, instead of adding the entire batch at once

        for (PhotoUploadRequest photoUploadRequest : photoUploadRequests) {
            final String tmpFile = mUploadsDir + TmpNameGenerator.newTmpName();
            final AlbumUploadingPhoto newUploadingPhoto = AlbumUploadingPhoto.NewPreparingFiles(tmpFile);
            mUploadingPhotos.addUploadingPhoto(albumId, newUploadingPhoto);

            final PhotoUploadRequest currentPhotoUploadRequest = photoUploadRequest;

            mPreperationExecutor.execute(new ThreadUtil.Runnable() {
                @Override
                public void run() {
                    String resizedPath = tmpFile + UploadManager.RESIZED_FILE_SUFFIX;
                    String thumbPath = tmpFile + UploadManager.THUMB_FILE_SUFFIX;

                    currentPhotoUploadRequest.saveToFile(tmpFile);

                    BitmapProcessor.ResizedResult result = mBitmapProcessor.createResizedAndThumbnail(tmpFile, resizedPath, thumbPath);
                    if (!result.success) {
                        // TODO ! Uh oh...
                        // Could be an invalid image file or something...
                        return;
                    }
                    Log.d("UploadManager", "processed file: " + tmpFile);

                    boolean shouldUploadOriginalDirectly = shouldUploadOriginalDirectly(result.originalWidth, result.originalHeight, result.resizedWidth, result.resizedHeight);

                    final UploadingPhoto.UploadStrategy uploadStrategy = shouldUploadOriginalDirectly
                            ? UploadingPhoto.UploadStrategy.UploadOriginalDirectly
                            : UploadingPhoto.UploadStrategy.UploadTwoStage;

                    newUploadingPhoto.setUploading();

                    mUploadSystemDirector.addUploadingPhoto(albumId, tmpFile, uploadStrategy);

                    ThreadUtil.runInMainThread(new ThreadUtil.Runnable() {
                        @Override
                        public void run() {
                            if (mListener != null) {
                                mListener.refreshAlbum(albumId);
                            }
                        }
                    });
                }
            });
        }
    }

    private static class TmpNameGenerator {
        /**
         * Not thread safe! Must be called from the main thread
         * @return
         */
        public static String newTmpName() {
            counter++;
            return "uploading_photo_" + launchTimeStamp + "_" + counter + ".data";
        }

        private static long counter = 0;
        private static final long launchTimeStamp = DateTime.NowUTC().getTimeStamp();
    }

    @Override
    public void setListener(Listener listener) {
        if (!ThreadUtil.isMainThread()) {
            throw new IllegalStateException("Must be called from the Main Thread");
        }
        mListener = listener;
    }

    @Override
    public String getUploadsDir() {
        return mUploadsDir;
    }

    private static boolean shouldUploadOriginalDirectly(int originalWidth, int originalHeight, int resizedWidth, int resizedHeight) {
        long originalArea = (long) originalWidth * (long) originalHeight;
        long resizedArea = (long) resizedWidth * (long) resizedHeight;

        if (originalArea <= resizedArea) {
            return true;
        }

        final long THRESHOLD_PERCENT = 85;
        return 100 * resizedArea / originalArea >= THRESHOLD_PERCENT;
    }

    private final ThreadUtil.Executor mPreperationExecutor;

    private final UploadingPhotosContainer mUploadingPhotos;

    private static class UploadingPhotosContainer {
        public Set<Long> getAlbums() {
            return mMap.keySet();
        }

        public List<AlbumUploadingPhoto> getAlbumUploadingPhotos(long albumId) {
            ArrayList<AlbumUploadingPhoto> x = mMap.get(albumId);
            if (x == null) {
                return mEmptyList;
            } else {
                return x;
            }
        }

        public void addUploadingPhoto(long albumId, AlbumUploadingPhoto uploadingPhoto) {
            ArrayList<AlbumUploadingPhoto> list = mMap.get(albumId);
            if (list != null) {
                list.add(uploadingPhoto);
            } else {
                ArrayList<AlbumUploadingPhoto> newList = new ArrayList<AlbumUploadingPhoto>();
                newList.add(uploadingPhoto);
                mMap.put(albumId, newList);
            }
        }

        public AlbumUploadingPhoto findUploadingPhoto(long albumId, String tmpFile) {
            for (AlbumUploadingPhoto photo : mMap.get(albumId)) {
                if (tmpFile.equals(photo.getTmpFile())) {
                    return photo;
                }
            }
            throw new IllegalStateException("Could not find photo: " + tmpFile);
        }

        public boolean allReadyToAddToAlbum(long albumId) {
            for (AlbumUploadingPhoto photo : getAlbumUploadingPhotos(albumId)) {
                AlbumUploadingPhoto.State state = photo.getState();
                if (!(state == AlbumUploadingPhoto.State.Uploaded
                        || state == AlbumUploadingPhoto.State.AddingToAlbum)) {
                    return false;
                }
            }

            return true;
        }

        public void remove(long albumId, List<AlbumUploadingPhoto> toRemove) {
            if (toRemove.size() == 0) {
                return;
            }

            HashSet<String> toRemoveTmpFiles = new HashSet<String>();
            for (AlbumUploadingPhoto photo : toRemove) {
                toRemoveTmpFiles.add(photo.getTmpFile());
            }

            ArrayList<AlbumUploadingPhoto> toKeep = new ArrayList<AlbumUploadingPhoto>();
            for (AlbumUploadingPhoto photo : mMap.get(albumId)) {
                if (!toRemoveTmpFiles.contains(photo.getTmpFile())) {
                    toKeep.add(photo);
                }
            }

            // Replace the old list with the new one we created
            mMap.put(albumId, toKeep);
        }

        private HashMap<Long, ArrayList<AlbumUploadingPhoto>> mMap = new HashMap<Long, ArrayList<AlbumUploadingPhoto>>();
        private final ArrayList<AlbumUploadingPhoto> mEmptyList = new ArrayList<AlbumUploadingPhoto>();
    }
}
