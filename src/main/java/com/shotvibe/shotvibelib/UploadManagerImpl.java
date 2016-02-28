package com.shotvibe.shotvibelib;

import java.util.List;
import java.util.Set;

public class UploadManagerImpl implements UploadManager {
    public UploadManagerImpl(
            ShotVibeAPI shotVibeAPI,
            UploadSystemDirector uploadSystemDirector,
            String uploadFilesDir,
            PhotoDownloadManager photoDownloadManager,
            BitmapProcessor bitmapProcessor,
            List<UploadingPhoto> storedUploads,
            BackgroundTaskManager backgroundTaskManager) {
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
        mPhotoDownloadManager = photoDownloadManager;
        mBitmapProcessor = bitmapProcessor;
        mBackgroundTaskManager = backgroundTaskManager;

        mListener = null;

        mUploadingPhotos = new UploadingPhotosContainer();

        initFromStoredUploads(storedUploads);

        ThreadUtil.runInBackgroundThread(new ThreadUtil.Runnable() {
            @Override
            public void run() {
                while (true) {
                    processNextJob();
                }
            }
        });

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

        mJobsConditionVar.lock();
        try {
            mNumProcessedAndReady--;
            if (mNumProcessedAndReady < 0) {
                throw new IllegalStateException("Impossible Happened");
            }
        } finally {
            mJobsConditionVar.unlock();
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

    public void reportOriginalUploadComplete(final long albumId, String tmpFile, String photoId) {
        synchronized (mUploadingOriginalPhotoIds) {
            mUploadingOriginalPhotoIds.remove(photoId);
        }

        ThreadUtil.runInMainThread(new ThreadUtil.Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.photoUploadedOriginal(albumId);
                }
            }
        });
    }

    public void reportNewOriginalUploads(final long albumId, ArrayList<String> newUploadOriginalPhotoIds) {
        synchronized (mUploadingOriginalPhotoIds) {
            mUploadingOriginalPhotoIds.addAll(newUploadOriginalPhotoIds);
        }

        ThreadUtil.runInMainThread(new ThreadUtil.Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.photoUploadedOriginal(albumId);
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

    private void addPhotosToAlbum(final long albumId, final ArrayList<AlbumUploadingPhoto> photos) {
        BackgroundTaskManager.BackgroundTask addToAlbumBackgroundTask = mBackgroundTaskManager.beginBackgroundTask(new BackgroundTaskManager.ExpirationHandler() {
            @Override
            public void onAppWillTerminate() {
                // TODO ...
            }
        });

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

        mUploadSystemDirector.reportPhotosAddedToAlbum(albumId, tmpFiles);

        // This is a nice enhancement. Instead of the app later needing to download the thumbnails
        // from the server, we use the thumbnails that we already have
        for (AlbumUploadingPhoto addedPhoto : photos) {
            String thumbFile = addedPhoto.getTmpFile() + THUMB_FILE_SUFFIX;
            mPhotoDownloadManager.takePhotoThumbnailFile(thumbFile, addedPhoto.getPhotoId());

            String fullFile = addedPhoto.getTmpFile() + RESIZED_FILE_SUFFIX;
            mPhotoDownloadManager.takePhotoFileFull(fullFile, addedPhoto.getPhotoId());
        }

        addToAlbumBackgroundTask.reportFinished();

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
        if (!storedUploads.isEmpty()) {
            mJobsConditionVar.lock();
        }

        for (UploadingPhoto photo : storedUploads) {
            if (photo.getUploadState() == UploadingPhoto.UploadState.Queued) {
                AlbumUploadingPhoto albumUploadingPhoto;

                if (photo.getUploadStrategy() == UploadingPhoto.UploadStrategy.Unknown) {
                    albumUploadingPhoto = AlbumUploadingPhoto.NewPreparingFiles(photo.getTmpFilename());
                    mPhotoProcessJobQueue.add(new PhotoProcessJob(albumUploadingPhoto, photo.getAlbumId()));

                    startBackgroundActivity();
                } else {
                    albumUploadingPhoto = AlbumUploadingPhoto.NewUploading(photo.getTmpFilename());

                    mNumProcessedAndReady++;
                }

                mUploadingPhotos.addUploadingPhoto(photo.getAlbumId(), albumUploadingPhoto);
            } else if (photo.getUploadState() == UploadingPhoto.UploadState.Uploaded) {
                AlbumUploadingPhoto albumUploadingPhoto = AlbumUploadingPhoto.NewUploaded(photo.getTmpFilename(), photo.getPhotoId());
                mUploadingPhotos.addUploadingPhoto(photo.getAlbumId(), albumUploadingPhoto);
            } else if (photo.getUploadState() == UploadingPhoto.UploadState.AddedToAlbum) {
                synchronized (mUploadingOriginalPhotoIds) {
                    mUploadingOriginalPhotoIds.add(photo.getPhotoId());
                }
            }
        }

        if (!storedUploads.isEmpty()) {
            mJobsConditionVar.unlock();
        }
    }

    private final String mUploadsDir;
    private final UploadSystemDirector mUploadSystemDirector;
    private final ShotVibeAPI mShotVibeAPI;
    private final PhotoDownloadManager mPhotoDownloadManager;
    private final BitmapProcessor mBitmapProcessor;
    private final BackgroundTaskManager mBackgroundTaskManager;
    private Listener mListener;

    private final Object mBackgroundTaskLock = new Object();
    private BackgroundTaskManager.BackgroundTask mBackgroundTask = null;

    private void startBackgroundActivity() {
        synchronized (mBackgroundTaskLock) {
            if (mBackgroundTask != null) {
                return;
            }

            mBackgroundTask = mBackgroundTaskManager.beginBackgroundTask(new BackgroundTaskManager.ExpirationHandler() {
                @Override
                public void onAppWillTerminate() {
                    reportAppWillTerminate();
                }
            });
        }
    }

    public void reportAllUploadsLaunched() {
        mJobsConditionVar.lock();
        try {
            if (mPhotoSaveJobQueue.isEmpty() && mPhotoProcessJobQueue.isEmpty()) {
                synchronized (mBackgroundTaskLock) {
                    if (mBackgroundTask != null) {
                        mBackgroundTask.reportFinished();
                    }
                }
            }
        } finally {
            mJobsConditionVar.unlock();
        }
    }


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
    public List<String> getUploadingOriginalPhotoIds() {
        ArrayList<String> result = new ArrayList<String>();

        synchronized (mUploadingOriginalPhotoIds) {
            result.addAll(mUploadingOriginalPhotoIds);
        }

        return result;
    }

    public void reportAppWillTerminate() {
        mJobsConditionVar.lock();
        try {
            if (!mPhotoSaveJobQueue.isEmpty()) {
                mBackgroundTaskManager.showNotificationMessage(BackgroundTaskManager.NotificationMessage.UPLOADS_STILL_SAVING);
            } else if (!mPhotoProcessJobQueue.isEmpty()) {
                mBackgroundTaskManager.showNotificationMessage(BackgroundTaskManager.NotificationMessage.UPLOADS_STILL_PROCESSING);
            }
        } finally {
            mJobsConditionVar.unlock();
        }
    }

    private static class PhotoSaveJob {
        public PhotoSaveJob(AlbumUploadingPhoto albumUploadingPhoto, String tmpFile, PhotoUploadRequest photoUploadRequest, long albumId) {
            if (albumUploadingPhoto == null) {
                throw new IllegalArgumentException("albumUploadingPhoto cannot be null");
            }
            if (tmpFile == null) {
                throw new IllegalArgumentException("tmpFile cannot be null");
            }
            if (photoUploadRequest == null) {
                throw new IllegalArgumentException("photoUploadRequest cannot be null");
            }
            this.AlbumUploadingPhoto = albumUploadingPhoto;
            this.TmpFile = tmpFile;
            this.PhotoUploadRequest = photoUploadRequest;
            this.AlbumId = albumId;
        }
        public final AlbumUploadingPhoto AlbumUploadingPhoto;
        public final String TmpFile;
        public final PhotoUploadRequest PhotoUploadRequest;
        public final long AlbumId;
    }

    private static class PhotoProcessJob {
        public PhotoProcessJob(AlbumUploadingPhoto albumUploadingPhoto, long albumId) {
            if (albumUploadingPhoto == null) {
                throw new IllegalArgumentException("albumUploadingPhoto cannot be null");
            }
            this.AlbumUploadingPhoto = albumUploadingPhoto;
            this.AlbumId = albumId;
        }
        public final AlbumUploadingPhoto AlbumUploadingPhoto;
        public final long AlbumId;
    }

    private final ConditionVar mJobsConditionVar = new ConditionVar();

    // The following must be accessed only when mJobsConditionVar is locked:
    private final ArrayList<PhotoSaveJob> mPhotoSaveJobQueue = new ArrayList<PhotoSaveJob>();
    private final ArrayList<PhotoProcessJob> mPhotoProcessJobQueue = new ArrayList<PhotoProcessJob>();
    private int mNumProcessedAndReady = 0;


//    @Override
//    public void uploadYouTube(long albumId, String youtube_id) {
//
//        if (!ThreadUtil.isMainThread()) {
//            throw new IllegalStateException("Must be called from the Main Thread");
//        }
//
//
////        Log.d("UploadManager", "Uploading photos: " + photoUploadRequests.size());
//
////        startBackgroundActivity();
//
//
//    }

    @Override
    public void uploadPhotos(final long albumId, List<PhotoUploadRequest> photoUploadRequests) {
        if (!ThreadUtil.isMainThread()) {
            throw new IllegalStateException("Must be called from the Main Thread");
        }

        Log.d("UploadManager", "Uploading photos: " + photoUploadRequests.size());

        startBackgroundActivity();

        // TODO Set some sort of lock on this album so that `addPhotosToAlbum` is not yet called
        // until the last photo in `photoUploadRequests` has been prepared. Otherwise, a very fast
        // internet connection might upload and add individual photos as they are prepared one by
        // one, instead of adding the entire batch at once

        for (PhotoUploadRequest photoUploadRequest : photoUploadRequests) {
            final String tmpFile = mUploadsDir + TmpNameGenerator.newTmpName();

            final AlbumUploadingPhoto albumUploadingPhoto = AlbumUploadingPhoto.NewSaving(tmpFile);

            synchronized (mUploadingPhotos) {
                mUploadingPhotos.addUploadingPhoto(albumId, albumUploadingPhoto);
            }

            mJobsConditionVar.lock();
            try {
                mPhotoSaveJobQueue.add(new PhotoSaveJob(albumUploadingPhoto, tmpFile, photoUploadRequest, albumId));
                mJobsConditionVar.signal();
            } finally {
                mJobsConditionVar.unlock();
            }
        }
    }

    private void processNextJob() {
        // We want to have at least this number of available photos processed and ready for
        // uploading, before we do any more save jobs. Should be equal to the number
        // of concurrent uploads + 1
        final int PROCESS_BUFFER = 3;

        PhotoSaveJob photoSaveJob = null;
        PhotoProcessJob photoProcessJob = null;

        while (true) {
            mJobsConditionVar.lock();
            try {
                // First priority is to handle the save jobs (but only if we don't urgently need
                // more process jobs handled due to a low buffer)
                if (!mPhotoSaveJobQueue.isEmpty() && (mNumProcessedAndReady >= PROCESS_BUFFER || mPhotoProcessJobQueue.isEmpty())) {
                    photoSaveJob = mPhotoSaveJobQueue.get(0);
                    mPhotoSaveJobQueue.remove(0);
                    break;
                }

                // Otherwise, handle any process jobs
                if (!mPhotoProcessJobQueue.isEmpty()) {
                    photoProcessJob = mPhotoProcessJobQueue.get(0);
                    mPhotoProcessJobQueue.remove(0);
                    break;
                }

                // No jobs at all available, wait for one to be added to a queue
                mJobsConditionVar.await();
            } finally {
                mJobsConditionVar.unlock();
            }
        }

        if (photoSaveJob != null) {
            processPhotoSaveJob(photoSaveJob);
        } else if (photoProcessJob != null) {
            processPhotoProcessJob(photoProcessJob);
        } else {
            throw new IllegalStateException("Impossible happened");
        }
    }

    private void processPhotoSaveJob(final PhotoSaveJob photoSaveJob) {
        Log.d("UploadManager", "saving file: " + photoSaveJob.TmpFile);
        photoSaveJob.PhotoUploadRequest.saveToFile(photoSaveJob.TmpFile);
        Log.d("UploadManager", "saved file: " + photoSaveJob.TmpFile);

        photoSaveJob.AlbumUploadingPhoto.setPreparingFiles();
        mUploadSystemDirector.reportNewUploadingPhoto(photoSaveJob.AlbumId, photoSaveJob.TmpFile);

        mJobsConditionVar.lock();
        try {
            mPhotoProcessJobQueue.add(new PhotoProcessJob(photoSaveJob.AlbumUploadingPhoto, photoSaveJob.AlbumId));
        } finally {
            mJobsConditionVar.unlock();
        }

        ThreadUtil.runInMainThread(new ThreadUtil.Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.refreshAlbum(photoSaveJob.AlbumId);
                }
            }
        });
    }

    private void processPhotoProcessJob(final PhotoProcessJob photoProcessJob) {
        final String tmpFile = photoProcessJob.AlbumUploadingPhoto.getTmpFile();
        final String resizedPath = tmpFile + UploadManager.RESIZED_FILE_SUFFIX;
        final String thumbPath = tmpFile + UploadManager.THUMB_FILE_SUFFIX;

        Log.d("UploadManager", "processing file: " + tmpFile);
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

        photoProcessJob.AlbumUploadingPhoto.setUploading();
        mUploadSystemDirector.reportUploadingPhotoReady(tmpFile, uploadStrategy);

        mJobsConditionVar.lock();
        try {
            mNumProcessedAndReady++;
        } finally {
            mJobsConditionVar.unlock();
        }

        ThreadUtil.runInMainThread(new ThreadUtil.Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.refreshAlbum(photoProcessJob.AlbumId);
                }
            }
        });
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

    private final ArrayList<String> mUploadingOriginalPhotoIds = new ArrayList<String>();

    // Must be accessed only from the main thread
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
