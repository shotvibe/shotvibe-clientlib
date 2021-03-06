package com.shotvibe.shotvibelib;

import java.util.List;

public class UploadSystemDirector {
    public UploadSystemDirector(
            BackgroundUploadSession.Factory<ForAlbumTaskData> backgroundUploadSessionFactory,
            BackgroundUploadSession.Factory<OriginalTaskData> originalUploadSessionFactory,
            UploadStateDB uploadStateDB,
            ShotVibeAPI shotVibeAPI,
            FileSystemManager fileSystemManager,
            PhotoDownloadManager photoDownloadManager,
            String uploadFilesDir,
            BitmapProcessor bitmapProcessor,
            BackgroundTaskManager backgroundTaskManager) {
        mUploadStateDB = uploadStateDB;
        mShotVibeAPI = shotVibeAPI;
        mFileSystemManager = fileSystemManager;
        mBackgroundTaskManager = backgroundTaskManager;

        mUploadingPhotos = loadUploadingPhotosFromDB();
        mUploadManager = new UploadManagerImpl(shotVibeAPI, this, uploadFilesDir, photoDownloadManager, bitmapProcessor, mUploadingPhotos, backgroundTaskManager);

        final BackgroundUploadSession.Listener<ForAlbumTaskData> listener = new BackgroundUploadSession.Listener<ForAlbumTaskData>() {
            @Override
            public void onTaskUploadProgress(ForAlbumTaskData taskData, long bytesSent, long bytesTotal) {
                Log.d("UploadSystem", "onTaskUploadProgress: " + bytesSent + "/" + bytesTotal + " " + taskData.getTmpFile());

                double uploadProgress = (double) bytesSent / (double) bytesTotal;
                long albumId = -1;

                for (UploadingPhoto photo : mUploadingPhotos) {
                    if (taskData.getTmpFile().equals(photo.getTmpFilename())) {
                        albumId = photo.getAlbumId();
                    }
                }
                if (albumId == -1) {
                    throw new IllegalStateException("reportUploadProgress photo not found: " + taskData.getTmpFile());
                }

                mUploadManager.reportUploadProgress(albumId, taskData.getTmpFile(), uploadProgress);
            }

            @Override
            public void onTaskUploadFinished(final BackgroundUploadSession.FinishedTask<ForAlbumTaskData> finishedTask) {
                final boolean successfullyUploaded = finishedTask.completedWithStatusCode()
                        && finishedTask.getStatusCode() < 400;

                Log.d("UploadSystem", "onTaskUploadFinished (" + successfullyUploaded + ") " + finishedTask.getTaskData().getTmpFile());

                final String photoId = finishedTask.getTaskData().getPhotoId();
                final String tmpFile = finishedTask.getTaskData().getTmpFile();

                if (successfullyUploaded) {
                    long albumId = setPhotoUploaded(tmpFile, photoId);

                    mUploadManager.reportUploadComplete(albumId, tmpFile, photoId);
                }

                if (!successfullyUploaded) {
                    ThreadUtil.runInBackgroundThread(new ThreadUtil.Runnable() {
                        @Override
                        public void run() {
                            ThreadUtil.sleep(5000); // TODO Magic Constant

                            Log.d("UploadSystem", "Starting new uploadPlan because of failed: " + tmpFile);
                            mBackgroundUploads.processCurrentTasks(new BackgroundUploadSession.TaskProcessor<ForAlbumTaskData>() {
                                @Override
                                public void processTasks(List<BackgroundUploadSession.Task<ForAlbumTaskData>> currentTasks) {
                                    UploadPlan uploadPlan = getUploadPlan(mUploadingPhotos);
                                    if (uploadPlan != null) {
                                        processUploadPlan(uploadPlan, currentTasks);
                                    }
                                }
                            });
                        }
                    });
                }
            }
        };

        mBackgroundUploads = backgroundUploadSessionFactory.startSession(new ForAlbumTaskDataFactory(), listener);

        final BackgroundUploadSession.Listener<OriginalTaskData> originalsListener = new BackgroundUploadSession.Listener<OriginalTaskData>() {

            @Override
            public void onTaskUploadProgress(OriginalTaskData taskData, long bytesSent, long bytesTotal) {
                // TODO ...
            }

            @Override
            public void onTaskUploadFinished(BackgroundUploadSession.FinishedTask<OriginalTaskData> finishedTask) {
                final boolean successfullyUploaded = finishedTask.completedWithStatusCode()
                        && finishedTask.getStatusCode() < 400;

                final boolean photoWasDeletedFromServer = finishedTask.completedWithStatusCode()
                        && finishedTask.getStatusCode() == 404;

                Log.d("UploadSystem", "original finished (" + finishedTask.completedWithStatusCode() + ") " + finishedTask.getStatusCode());

                final String photoId = finishedTask.getTaskData().getPhotoId();
                final String tmpFile = finishedTask.getTaskData().getTmpFile();

                if (successfullyUploaded || photoWasDeletedFromServer) {
                    long albumId = setPhotoOriginalUploaded(tmpFile, photoId);

                    mFileSystemManager.deleteFile(tmpFile);

                    mUploadManager.reportOriginalUploadComplete(albumId, tmpFile, photoId);
                } else {
                    OriginalTaskData retryTask = new OriginalTaskData(tmpFile, photoId);
                    launchOriginalUpload(retryTask);
                }
            }
        };

        mOriginalUploads = originalUploadSessionFactory.startSession(new OriginalTaskDataFactory(), originalsListener);

        mBackgroundUploads.processCurrentTasks(new BackgroundUploadSession.TaskProcessor<ForAlbumTaskData>() {
            @Override
            public void processTasks(List<BackgroundUploadSession.Task<ForAlbumTaskData>> currentTasks) {
                UploadPlan uploadPlan = getUploadPlan(mUploadingPhotos);
                if (uploadPlan != null) {
                    processUploadPlan(uploadPlan, currentTasks);
                }
            }
        });
    }

    private final UploadStateDB mUploadStateDB;
    private final ArrayList<UploadingPhoto> mUploadingPhotos;

    private final ShotVibeAPI mShotVibeAPI;
    private final FileSystemManager mFileSystemManager;
    private final BackgroundTaskManager mBackgroundTaskManager;

    private final BackgroundUploadSession<ForAlbumTaskData> mBackgroundUploads;
    private final BackgroundUploadSession<OriginalTaskData> mOriginalUploads;

    public UploadManager getUploadManager() {
        return mUploadManager;
    }

    private final UploadManagerImpl mUploadManager;

    private final ArrayList<String> mAvailablePhotoIds = new ArrayList<String>();

    private ArrayList<UploadingPhoto> loadUploadingPhotosFromDB() {
        try {
            return mUploadStateDB.getAllUploadingPhotos();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void fetchMorePhotoIds() {
        ArrayList<String> newPhotoIds = null;
        while (newPhotoIds == null) {
            try {
                newPhotoIds = mShotVibeAPI.photosUploadRequest(10); // TODO Magic constant
            } catch (APIException e) {
                Log.d("UploadSystem", e.getTechnicalMessage());
                ThreadUtil.sleep(5000); // TODO Magic constant
            }
        }

        synchronized (mAvailablePhotoIds) {
            mAvailablePhotoIds.addAll(newPhotoIds);
        }

        mBackgroundUploads.processCurrentTasks(new BackgroundUploadSession.TaskProcessor<ForAlbumTaskData>() {
            @Override
            public void processTasks(List<BackgroundUploadSession.Task<ForAlbumTaskData>> currentTasks) {
                UploadPlan uploadPlan = getUploadPlan(mUploadingPhotos);
                if (uploadPlan != null) {
                    Log.d("UploadSystem", "fetchMorePhotoIds: processUploadPlan");
                    processUploadPlan(uploadPlan, currentTasks);
                }
            }
        });
    }

    private void processUploadPlan(final UploadPlan uploadPlan, List<BackgroundUploadSession.Task<ForAlbumTaskData>> currentTasks) {
        if (uploadPlan.uploadForAlbum != null) {
            cancelAllOriginalUploads();

            int index = 0;

            HashSet<String> currentTaskFiles = new HashSet<String>();
            HashSet<String> newItemFiles = new HashSet<String>();
            for (BackgroundUploadSession.Task<ForAlbumTaskData> task : currentTasks) {
                currentTaskFiles.add(task.getTaskData().getTmpFile());
            }
            for (UploadPlan.ForAlbum item : uploadPlan.uploadForAlbum) {
                newItemFiles.add(item.uploadFile);
            }

            // Cancel currently active tasks that are not part of the new upload plan:

            for (BackgroundUploadSession.Task<ForAlbumTaskData> task : currentTasks) {
                if (!newItemFiles.contains(task.getTaskData().getTmpFile())) {
                    mBackgroundUploads.cancelTask(task);
                }
            }

            // Add any new tasks:

            synchronized (mAvailablePhotoIds) {
                for (UploadPlan.ForAlbum forAlbum : uploadPlan.uploadForAlbum) {
                    if (!currentTaskFiles.contains(forAlbum.uploadFile)) {
                        if (mAvailablePhotoIds.isEmpty()) {
                            ThreadUtil.runInBackgroundThread(new ThreadUtil.Runnable() {
                                @Override
                                public void run() {
                                    fetchMorePhotoIds();
                                }
                            });
                            return;
                        }

                        String photoId = mAvailablePhotoIds.get(0);
                        mAvailablePhotoIds.remove(0);

                        launchForAlbumUpload(forAlbum, photoId);
                    }
                }

                // If we got here then all queued photos have been launched and are now uploading
                // in the background
                mUploadManager.reportAllUploadsLaunched();
            }
        } else if (uploadPlan.uploadOriginals != null) {
            Log.d("UPLOADSYSTEM", "Uploading original photos: " + uploadPlan.uploadOriginals.size());


            mOriginalUploads.processCurrentTasks(new BackgroundUploadSession.TaskProcessor<OriginalTaskData>() {
                @Override
                public void processTasks(List<BackgroundUploadSession.Task<OriginalTaskData>> currentTasks) {
                    HashSet<String> currentTaskFiles = new HashSet<String>();
                    HashSet<String> newItemFiles = new HashSet<String>();
                    for (BackgroundUploadSession.Task<OriginalTaskData> task : currentTasks) {
                        currentTaskFiles.add(task.getTaskData().getTmpFile());
                    }
                    for (UploadPlan.Original item : uploadPlan.uploadOriginals) {
                        newItemFiles.add(item.uploadFile);
                    }

                    // Cancel currently active tasks that are not part of the new upload plan:

                    for (BackgroundUploadSession.Task<OriginalTaskData> task : currentTasks) {
                        if (!newItemFiles.contains(task.getTaskData().getTmpFile())) {
                            mOriginalUploads.cancelTask(task);
                        }
                    }

                    // Add any new tasks:
                    for (UploadPlan.Original original : uploadPlan.uploadOriginals) {
                        if (!currentTaskFiles.contains(original.uploadFile)) {
                            OriginalTaskData taskData = new OriginalTaskData(original.uploadFile, original.photoId);
                            launchOriginalUpload(taskData);
                        }
                    }
                }
            });
        }
    }

    private void cancelAllOriginalUploads() {
        mOriginalUploads.processCurrentTasks(new BackgroundUploadSession.TaskProcessor<OriginalTaskData>() {
            @Override
            public void processTasks(List<BackgroundUploadSession.Task<OriginalTaskData>> currentTasks) {
                for (BackgroundUploadSession.Task<OriginalTaskData> task : currentTasks) {
                    mOriginalUploads.cancelTask(task);
                }
            }
        });
    }

    private void launchOriginalUpload(OriginalTaskData taskData) {
        String url = ShotVibeAPI.BASE_UPLOAD_URL + "/photos/upload/" + taskData.getPhotoId() + "/original/";
        String realUploadFile = taskData.getTmpFile();

        mOriginalUploads.startUploadTask(taskData, url, realUploadFile);
    }

    private void launchForAlbumUpload(UploadPlan.ForAlbum forAlbum, String photoId) {
        String url;
        String realUploadFile;
        switch (forAlbum.uploadStrategy) {
            case UploadTwoStage:
                url = ShotVibeAPI.BASE_UPLOAD_URL + "/photos/upload/" + photoId + "/";
                realUploadFile = forAlbum.uploadFile + UploadManager.RESIZED_FILE_SUFFIX;
                break;
            case UploadOriginalDirectly:
                url = ShotVibeAPI.BASE_UPLOAD_URL + "/photos/upload/" + photoId + "/original/";
                realUploadFile = forAlbum.uploadFile;
                break;
            default:
                throw new IllegalStateException("Unknown UploadStrategy: " + forAlbum.uploadStrategy);
        }

        ForAlbumTaskData taskData = new ForAlbumTaskData(forAlbum.uploadFile, photoId, forAlbum.uploadStrategy);

        mBackgroundUploads.startUploadTask(taskData, url, realUploadFile);
    }

    public static final class UploadPlan {
        public abstract static class Item {
            public Item(String uploadFile) {
                this.uploadFile = uploadFile;
            }

            public final String uploadFile;
        }
        public static UploadPlan CreateUploadPlanForAlbum(ArrayList<ForAlbum> uploadForAlbum) {
            if (uploadForAlbum == null) {
                throw new IllegalArgumentException("uploadForAlbum cannot be null");
            }
            return new UploadPlan(uploadForAlbum, null);
        }

        public static UploadPlan CreateUploadPlanOriginals(ArrayList<Original> uploadOriginals) {
            if (uploadOriginals == null) {
                throw new IllegalArgumentException("uploadOriginals cannot be null");
            }
            return new UploadPlan(null, uploadOriginals);
        }

        public static class ForAlbum extends Item {
            public ForAlbum(String uploadFile, UploadingPhoto.UploadStrategy uploadStrategy) {
                super(uploadFile);
                if (uploadStrategy == null) {
                    throw new IllegalArgumentException("uploadStrategy cannot be null");
                }

                this.uploadStrategy = uploadStrategy;
            }

            /**
             * Should be used to determine which URL endpoint to upload to
             */
            public final UploadingPhoto.UploadStrategy uploadStrategy;
        }

        public static class Original extends Item {
            public Original(String uploadFile, String photoId) {
                super(uploadFile);
                this.photoId = photoId;
            }

            public final String photoId;
        }

        public final ArrayList<ForAlbum> uploadForAlbum;
        public final ArrayList<Original> uploadOriginals;

        private UploadPlan(ArrayList<ForAlbum> uploadForAlbum, ArrayList<Original> uploadOriginals) {
            this.uploadForAlbum = uploadForAlbum;
            this.uploadOriginals = uploadOriginals;
        }
    }

    /**
     * @return Returns null if there is nothing to do
     */
    private static UploadPlan getUploadPlan(List<UploadingPhoto> uploadingPhotos) {
        ArrayList<UploadPlan.ForAlbum> uploadForAlbum = new ArrayList<UploadPlan.ForAlbum>();
        ArrayList<UploadPlan.Original> uploadOriginals = new ArrayList<UploadPlan.Original>();

        boolean allAddedToAlbum = true;

        for (UploadingPhoto photo : uploadingPhotos) {
            if (photo.getUploadState() == UploadingPhoto.UploadState.Queued) {
                allAddedToAlbum = false;
                if (photo.getUploadStrategy() != UploadingPhoto.UploadStrategy.Unknown) {
                    uploadForAlbum.add(new UploadPlan.ForAlbum(photo.getTmpFilename(), photo.getUploadStrategy()));
                }
            } else if (allAddedToAlbum && photo.getUploadState() == UploadingPhoto.UploadState.AddedToAlbum) {
                uploadOriginals.add(new UploadPlan.Original(photo.getTmpFilename(), photo.getPhotoId()));
            } else {
                allAddedToAlbum = false;
            }
        }

        if (allAddedToAlbum) {
            if (!uploadOriginals.isEmpty()) {
                return UploadPlan.CreateUploadPlanOriginals(uploadOriginals);
            } else {
                return null;
            }
        } else {
            if (!uploadForAlbum.isEmpty()) {
                return UploadPlan.CreateUploadPlanForAlbum(uploadForAlbum);
            } else {
                return null;
            }
        }
    }

    /**
     * Report that a new photo is queued to be uploaded. The new photo is not yet done processing
     */
    public void reportNewUploadingPhoto(long albumId, String tmpFilename) {
        final UploadingPhoto newUploadingPhoto = new UploadingPhoto(
                albumId,
                tmpFilename,
                UploadingPhoto.UploadStrategy.Unknown,
                UploadingPhoto.UploadState.Queued,
                null);

        mBackgroundUploads.processCurrentTasks(new BackgroundUploadSession.TaskProcessor<ForAlbumTaskData>() {
            @Override
            public void processTasks(List<BackgroundUploadSession.Task<ForAlbumTaskData>> currentTasks) {
                mUploadingPhotos.add(newUploadingPhoto);
                try {
                    mUploadStateDB.insertUploadingPhoto(newUploadingPhoto);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public void reportUploadingPhotoReady(final String tmpFilename, final UploadingPhoto.UploadStrategy uploadStrategy) {
        mBackgroundUploads.processCurrentTasks(new BackgroundUploadSession.TaskProcessor<ForAlbumTaskData>() {
            @Override
            public void processTasks(List<BackgroundUploadSession.Task<ForAlbumTaskData>> currentTasks) {
                UploadingPhoto oldUploadingPhoto = null;
                int index = -1;
                for (int i = 0; i < mUploadingPhotos.size(); ++i) {
                    UploadingPhoto photo = mUploadingPhotos.get(i);
                    if (photo.getTmpFilename().equals(tmpFilename)) {
                        oldUploadingPhoto = photo;
                        index = i;
                        break;
                    }
                }

                if (index == -1) {
                    throw new IllegalStateException("tmpFile not found: " + tmpFilename);
                }
                if (oldUploadingPhoto.getUploadState() != UploadingPhoto.UploadState.Queued) {
                    throw new IllegalStateException("reportUploadingPhotoReady with state: " + oldUploadingPhoto.getUploadState());
                }
                if (oldUploadingPhoto.getUploadStrategy() != UploadingPhoto.UploadStrategy.Unknown) {
                    throw new IllegalStateException("reportUploadingPhotoReady with strategy: " + oldUploadingPhoto.getUploadStrategy());
                }

                UploadingPhoto updatedUploadingPhoto = new UploadingPhoto(
                        oldUploadingPhoto.getAlbumId(),
                        tmpFilename,
                        uploadStrategy,
                        UploadingPhoto.UploadState.Queued,
                        null);

                mUploadingPhotos.set(index, updatedUploadingPhoto);

                try {
                    mUploadStateDB.setPhotoUploadStrategy(updatedUploadingPhoto);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                UploadPlan uploadPlan = getUploadPlan(mUploadingPhotos);
                if (uploadPlan == null) {
                    throw new IllegalStateException("The Impossible Happened");
                }
                Log.d("UploadSystem", "addUploadingPhoto: processUploadPlan");
                processUploadPlan(uploadPlan, currentTasks);

                // TODO End background task
            }
        });
    }

    private long setPhotoUploaded(String tmpFilename, String photoId) {
        UploadingPhoto oldUploadingPhoto = null;
        int index = -1;
        for (int i = 0; i < mUploadingPhotos.size(); ++i) {
            UploadingPhoto photo = mUploadingPhotos.get(i);
            if (photo.getTmpFilename().equals(tmpFilename)) {
                oldUploadingPhoto = photo;
                index = i;
                break;
            }
        }

        // TODO: We should actually allow this to be called for a missing tmpFilename and simply
        // ignore the call. Due to a race the client can't
        if (index == -1) {
            throw new IllegalStateException("setPhotoUploaded not found");
        }
        if (oldUploadingPhoto.getUploadState() == UploadingPhoto.UploadState.AddedToAlbum) {
            throw new IllegalStateException("setPhotoUploaded with photo that was already AddedToAlbum");
        }

        UploadingPhoto updatedUploadingPhoto = new UploadingPhoto(
                oldUploadingPhoto.getAlbumId(),
                tmpFilename,
                oldUploadingPhoto.getUploadStrategy(),
                UploadingPhoto.UploadState.Uploaded,
                photoId);

        mUploadingPhotos.set(index, updatedUploadingPhoto);

        try {
            mUploadStateDB.setPhotoUploaded(updatedUploadingPhoto);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return updatedUploadingPhoto.getAlbumId();
    }

    private long setPhotoOriginalUploaded(String tmpFilename, String photoId) {
        UploadingPhoto oldUploadingPhoto = null;
        int index = -1;
        for (int i = 0; i < mUploadingPhotos.size(); ++i) {
            UploadingPhoto photo = mUploadingPhotos.get(i);
            if (photo.getTmpFilename().equals(tmpFilename)) {
                oldUploadingPhoto = photo;
                index = i;
                break;
            }
        }

        if (index == -1) {
            throw new IllegalStateException("setPhotoOriginalUploaded not found");
        }
        if (oldUploadingPhoto.getUploadState() != UploadingPhoto.UploadState.AddedToAlbum) {
            throw new IllegalStateException("setPhotoOriginalUploaded with photo that was not AddedToAlbum");
        }

        mUploadingPhotos.remove(index);

        try {
            mUploadStateDB.setPhotoOriginalUploaded(oldUploadingPhoto);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return oldUploadingPhoto.getAlbumId();
    }

    public void reportPhotosAddedToAlbum(final long albumId, final HashSet<String> tmpFiles) {
        mBackgroundUploads.processCurrentTasks(new BackgroundUploadSession.TaskProcessor<ForAlbumTaskData>() {
            @Override
            public void processTasks(List<BackgroundUploadSession.Task<ForAlbumTaskData>> currentTasks) {

                // Go through all the photoIds
                // Delete the rows that have strategy UploadOriginalDirectly
                // Set state to AddedToAlbum for rows that have strategy UploadTwoStage

                ArrayList<UploadingPhoto> newUploadingPhotos = new ArrayList<UploadingPhoto>();

                ArrayList<String> newUploadOriginalPhotoIds = new ArrayList<String>();

                for (UploadingPhoto photo : mUploadingPhotos) {
                    if (tmpFiles.contains(photo.getTmpFilename())) {
                        // Delete the RESIZED file, it is not longer needed
                        //mFileSystemManager.deleteFile(photo.getTmpFilename() + UploadManager.RESIZED_FILE_SUFFIX);
                        // Note: The THUMB file is not removed, UploadManagerImpl handles it

                        if (photo.getUploadStrategy() == UploadingPhoto.UploadStrategy.UploadTwoStage) {
                            newUploadingPhotos.add(new UploadingPhoto(
                                    photo.getAlbumId(),
                                    photo.getTmpFilename(),
                                    photo.getUploadStrategy(),
                                    UploadingPhoto.UploadState.AddedToAlbum,
                                    photo.getPhotoId()));

                            newUploadOriginalPhotoIds.add(photo.getPhotoId());
                        } else {
                            // The original file was directly uploaded, so it can be deleted now
                            mFileSystemManager.deleteFile(photo.getTmpFilename());
                        }
                    } else {
                        newUploadingPhotos.add(photo);
                    }
                }

                mUploadingPhotos.clear();
                mUploadingPhotos.addAll(newUploadingPhotos);

                try {
                    mUploadStateDB.photosAddedToAlbum(tmpFiles);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                if (!newUploadOriginalPhotoIds.isEmpty()) {
                    mUploadManager.reportNewOriginalUploads(albumId, newUploadOriginalPhotoIds);
                }

                UploadPlan uploadPlan = getUploadPlan(mUploadingPhotos);
                if (uploadPlan != null) {
                    processUploadPlan(uploadPlan, currentTasks);
                } else {
                    // TODO This is the place to indicate to iOS that we are done processing background tasks
                }
            }
        });
    }

    public static class ForAlbumTaskDataFactory implements BackgroundUploadSession.TaskDataFactory<ForAlbumTaskData> {
        @Override
        public String serialize(ForAlbumTaskData taskData) {
            JSONObject obj = new JSONObject();
            obj.put("tmpFile", taskData.getTmpFile());
            obj.put("photoId", taskData.getPhotoId());
            obj.put("uploadStrategy", taskData.getUploadStrategy().ordinal());
            return obj.toString();
        }

        @Override
        public ForAlbumTaskData deserialize(String s) {
            try {
                JSONObject obj = JSONObject.Parse(s);
                String tmpFile = obj.getString("tmpFile");
                String photoId = obj.getString("photoId");
                int uploadStrategyInt = obj.getInt("uploadStrategy");
                UploadingPhoto.UploadStrategy uploadStrategy;
                if (uploadStrategyInt == UploadingPhoto.UploadStrategy.UploadOriginalDirectly.ordinal()) {
                    uploadStrategy = UploadingPhoto.UploadStrategy.UploadOriginalDirectly;
                } else if (uploadStrategyInt == UploadingPhoto.UploadStrategy.UploadTwoStage.ordinal()) {
                    uploadStrategy = UploadingPhoto.UploadStrategy.UploadTwoStage;
                } else {
                    throw new JSONException("Invalid value for \"uploadStrategy\": " + uploadStrategyInt);
                }
                return new ForAlbumTaskData(tmpFile, photoId, uploadStrategy);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class ForAlbumTaskData {
        public ForAlbumTaskData(String tmpFile, String photoId, UploadingPhoto.UploadStrategy uploadStrategy) {
            if (tmpFile == null) {
                throw new IllegalArgumentException("tmpFile cannot be null");
            }
            if (photoId == null) {
                throw new IllegalArgumentException("photoId cannot be null");
            }
            if (uploadStrategy == null) {
                throw new IllegalArgumentException("uploadStrategy cannot be null");
            }
            mTmpFile = tmpFile;
            mPhotoId = photoId;
            mUploadStrategy = uploadStrategy;
        }

        public String getTmpFile() {
            return mTmpFile;
        }

        public String getPhotoId() {
            return mPhotoId;
        }

        public UploadingPhoto.UploadStrategy getUploadStrategy() {
            return mUploadStrategy;
        }

        private final String mTmpFile;
        private final String mPhotoId;
        private final UploadingPhoto.UploadStrategy mUploadStrategy;
    }

    public static class OriginalTaskDataFactory implements BackgroundUploadSession.TaskDataFactory<OriginalTaskData> {
        @Override
        public String serialize(OriginalTaskData taskData) {
            JSONObject obj = new JSONObject();
            obj.put("tmpFile", taskData.getTmpFile());
            obj.put("photoId", taskData.getPhotoId());
            return obj.toString();
        }

        @Override
        public OriginalTaskData deserialize(String s) {
            try {
                JSONObject obj = JSONObject.Parse(s);
                String tmpFile = obj.getString("tmpFile");
                String photoId = obj.getString("photoId");
                return new OriginalTaskData(tmpFile, photoId);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class OriginalTaskData {
        public OriginalTaskData(String tmpFile, String photoId) {
            if (tmpFile == null) {
                throw new IllegalArgumentException("tmpFile cannot be null");
            }
            if (photoId == null) {
                throw new IllegalArgumentException("photoId cannot be null");
            }
            mTmpFile = tmpFile;
            mPhotoId = photoId;
        }

        public String getTmpFile() {
            return mTmpFile;
        }

        public String getPhotoId() {
            return mPhotoId;
        }

        private final String mTmpFile;
        private final String mPhotoId;
    }
}
