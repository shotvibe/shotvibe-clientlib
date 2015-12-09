package com.shotvibe.shotvibelib;

import java.util.List;
import java.util.Map;

public class AlbumManager implements UploadManager.Listener {
    public AlbumManager(ShotVibeAPI shotVibeAPI, ShotVibeDB shotVibeDB, PhotoDownloadManager photoDownloadManager, UploadManager uploadManager) {
        if (shotVibeAPI == null) {
            throw new IllegalArgumentException("shotVibeAPI cannot be null");
        }
        if (shotVibeDB == null) {
            throw new IllegalArgumentException("shotVibeDB cannot be null");
        }
        if (uploadManager == null) {
            throw new IllegalArgumentException("uploadManager cannot be null");
        }
        mShotVibeAPI = shotVibeAPI;
        mShotVibeDB = shotVibeDB;
        mPhotoDownloadManager = photoDownloadManager;
        mUploadManager = uploadManager;

        mUploadManager.setListener(this);

        mExecutor = ThreadUtil.createSingleThreadExecutor();
        mAlbumListListeners = new ArrayList<AlbumListListener>();
        mAlbumContentsListeners = new AlbumContentsListenersContainer();
        mAlbumListUserRefreshing = false;
        mUserRefreshingAlbumContents = new HashSet<Long>();
    }

    public ShotVibeAPI getShotVibeAPI() {
        return mShotVibeAPI;
    }

    public ShotVibeDB getShotVibeDB() {
        return mShotVibeDB;
    }

    public interface AlbumListListener {
        void onAlbumListBeginUserRefresh();

        /**
         *
         * @param error Will be null if there was no error
         */
        void onAlbumListEndUserRefresh(APIException error);

        void onAlbumListNewContent(ArrayList<AlbumSummary> albums);
    }

    public interface AlbumContentsListener {
        void onAlbumContentsBeginUserRefresh(long albumId);

        /**
         *
         * @param error Will be null if there was no error
         */
        void onAlbumContentsEndUserRefresh(APIException error);

        void onAlbumContentsNewContent(long albumId, AlbumContents albumContents);

        void onAlbumContentsUploadsProgressed(long albumId);
    }

    public ArrayList<AlbumSummary> getCachedAlbums() {
        try {
            return mShotVibeDB.getAlbumList();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public ArrayList<AlbumContents> getCachedAlbumContents() {
        if (!ThreadUtil.isMainThread()) {
            throw new IllegalStateException("Must be called from the Main Thread");
        }

        try {
            ArrayList<AlbumContents> result = new ArrayList<AlbumContents>();
            ArrayList<AlbumSummary> albumSummaries = mShotVibeDB.getAlbumList();
            for (AlbumSummary a : albumSummaries) {
                AlbumContents albumContents = mShotVibeDB.getAlbumContents(a.getId());
                if (albumContents != null) {
                    result.add(albumContents);
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public ArrayList<AlbumSummary> addAlbumListListener(final AlbumListListener listener) {
        if (!ThreadUtil.isMainThread()) {
            throw new IllegalStateException("Must be called from the Main Thread");
        }

        if (listContainsListener(mAlbumListListeners, listener)) {
            throw new IllegalArgumentException("Tried to add an AlbumListListener that is already registered in the AlbumManager");
        }

        mAlbumListListeners.add(listener);

        if (mAlbumListUserRefreshing) {
            listener.onAlbumListBeginUserRefresh();
        }

        try {
            return mShotVibeDB.getAlbumList();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeAlbumListListener(AlbumListListener listener) {
        if (!ThreadUtil.isMainThread()) {
            throw new IllegalStateException("Must be called from the Main Thread");
        }

        listRemoveListener(mAlbumListListeners, listener);
    }

    public void refreshAlbumList(boolean userRefresh) {
        if (userRefresh && !ThreadUtil.isMainThread()) {
            throw new IllegalStateException("Must be called from the Main Thread when userRefresh set");
        }

        mRefreshAlbumList.trigger(mExecutor);

        if (userRefresh && !mAlbumListUserRefreshing) {
            mAlbumListUserRefreshing = true;
            for (AlbumListListener listener : mAlbumListListeners) {
                listener.onAlbumListBeginUserRefresh();
            }
        }
    }

    public AlbumContents addAlbumContentsListener(final long albumId, final AlbumContentsListener listener) {
        if (!ThreadUtil.isMainThread()) {
            throw new IllegalStateException("Must be called from the Main Thread");
        }

        mAlbumContentsListeners.addAlbumContentsListener(albumId, listener);

        if (mUserRefreshingAlbumContents.contains(albumId)) {
            listener.onAlbumContentsBeginUserRefresh(albumId);
        }

        AlbumContents cachedAlbumContents;
        try {
            cachedAlbumContents = mShotVibeDB.getAlbumContents(albumId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        if (cachedAlbumContents != null) {
            // Add the Uploading photos to the end of album:
            addUploadingPhotosToAlbumContents(cachedAlbumContents, mUploadManager.getUploadingPhotos(albumId), mUploadManager.getUploadingOriginalPhotoIds());
        }

        return cachedAlbumContents;
    }

    public void removeAlbumContentsListener(long albumId, AlbumContentsListener listener) {
        if (!ThreadUtil.isMainThread()) {
            throw new IllegalStateException("Must be called from the Main Thread");
        }

        mAlbumContentsListeners.removeAlbumContentsListener(albumId, listener);
    }

    public void refreshAlbumContents(final long albumId, boolean userRefresh) {
        if (userRefresh && !ThreadUtil.isMainThread()) {
            throw new IllegalStateException("Must be called from the Main Thread when userRefresh set");
        }

        ThreadUtil.runInMainThread(new ThreadUtil.Runnable() {
            @Override
            public void run() {
                RefreshAlbumContentsTrigerrableAction action = mRefreshAlbumContentsActions.get(albumId);
                if (action == null) {
                    action = new RefreshAlbumContentsTrigerrableAction(albumId);
                    mRefreshAlbumContentsActions.put(albumId, action);
                }
                action.trigger(mExecutor);
            }
        });

        if (userRefresh && !mUserRefreshingAlbumContents.contains(albumId)) {
            mUserRefreshingAlbumContents.add(albumId);
            for (AlbumContentsListener listener : mAlbumContentsListeners.getAlbumContentsListeners(albumId)) {
                listener.onAlbumContentsBeginUserRefresh(albumId);
            }
        }
    }

    private final RefreshAlbumListTrigerrableAction mRefreshAlbumList = new RefreshAlbumListTrigerrableAction();

    private static class RefreshAlbumListResult {
        public ArrayList<AlbumSummary> albumsList = null;
        public APIException error = null;
    }

    //@WeakOuter
    private class RefreshAlbumListTrigerrableAction extends TriggerableAction<RefreshAlbumListResult> {
        @Override
        public RefreshAlbumListResult runAction() {
            RefreshAlbumListResult result = new RefreshAlbumListResult();
            try {
                ArrayList<AlbumSummary> albumsList = mShotVibeAPI.getAlbums();

                CollectionUtils.sortArrayList(albumsList, new CollectionUtils.Comparator<AlbumSummary>() {
                    @Override
                    public int compare(AlbumSummary lhs, AlbumSummary rhs) {
                        int compareTimestamps = new Long(lhs.getDateUpdated().getTimeStamp()).compareTo(rhs.getDateUpdated().getTimeStamp());
                        return -compareTimestamps;
                    }
                });

                result.albumsList = albumsList;
                return result;
            } catch (APIException e) {
                result.error = e;
                return result;
            }
        }

        @Override
        public void actionComplete(final RefreshAlbumListResult result) {
            if (result.albumsList != null) {
                try {
                    mShotVibeDB.setAlbumList(result.albumsList);

                    // Loop over the new albumsList, and refresh any albums that have
                    // an updated etag value:
                    // TODO Right now all of these refresh requests happen in parallel, they should run in sequence
                    Map<Long, String> albumEtags = mShotVibeDB.getAlbumListEtagValues();
                    for (AlbumSummary a : result.albumsList) {
                        String newEtag = a.getEtag();
                        String oldEtag = albumEtags.get(a.getId());
                        if (!newEtag.equals(oldEtag)) {
                            refreshAlbumContents(a.getId(), false);
                        }
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

            ThreadUtil.runInMainThread(new ThreadUtil.Runnable() {
                @Override
                public void run() {
                    for (AlbumListListener listener : mAlbumListListeners) {
                        if (result.albumsList != null) {
                            listener.onAlbumListNewContent(result.albumsList);
                        }
                        if (mAlbumListUserRefreshing) {
                            listener.onAlbumListEndUserRefresh(result.error);
                        }
                    }
                    mAlbumListUserRefreshing = false;
                }
            });
        }
    }

    private static class RefreshAlbumContentsResult {
        public AlbumContents albumContents = null;
        public APIException error = null;
    }

    //@WeakOuter
    private class RefreshAlbumContentsTrigerrableAction extends TriggerableAction<RefreshAlbumContentsResult> {
        public RefreshAlbumContentsTrigerrableAction(long albumId) {
            mAlbumId = albumId;
        }

        long mAlbumId;

        @Override
        public RefreshAlbumContentsResult runAction() {
            RefreshAlbumContentsResult result = new RefreshAlbumContentsResult();
            try {
                AlbumContents albumContents = mShotVibeAPI.getAlbumContents(mAlbumId);

                CollectionUtils.sortArrayList(albumContents.getMembers(), new CollectionUtils.Comparator<AlbumMember>() {
                    @Override
                    public int compare(AlbumMember lhs, AlbumMember rhs) {
                        String nick1 = lhs.getUser().getMemberNickname();
                        String nick2 = rhs.getUser().getMemberNickname();
                        return nick1.compareTo(nick2);
                    }
                });

                result.albumContents = albumContents;
                return result;
            } catch (APIException e) {
                result.error = e;
                return result;
            }
        }

        @Override
        public void actionComplete(final RefreshAlbumContentsResult result) {
            if (result.albumContents != null) {
                try {
                    mShotVibeDB.setAlbumContents(mAlbumId, result.albumContents);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                // Start downloading the photos in the background
                for (AlbumPhoto p : result.albumContents.getPhotos()) {
                    String photoId = p.getServerPhoto().getId();
                    String photoUrl = p.getServerPhoto().getUrl();
                    mPhotoDownloadManager.queuePhotoForDownload(photoId, photoUrl, true, true);
                    mPhotoDownloadManager.queuePhotoForDownload(photoId, photoUrl, false, false);
                }
            }

            ThreadUtil.runInMainThread(new ThreadUtil.Runnable() {
                @Override
                public void run() {
                    List<AlbumContentsListener> listeners = mAlbumContentsListeners.getAlbumContentsListeners(mAlbumId);
                    if (result.albumContents != null && !listeners.isEmpty()) {
                        addUploadingPhotosToAlbumContents(result.albumContents, mUploadManager.getUploadingPhotos(mAlbumId), mUploadManager.getUploadingOriginalPhotoIds());
                    }

                    for (AlbumContentsListener listener : listeners) {
                        if (result.albumContents != null) {
                            listener.onAlbumContentsNewContent(mAlbumId, result.albumContents);
                        }
                        if (mUserRefreshingAlbumContents.contains(mAlbumId)) {
                            listener.onAlbumContentsEndUserRefresh(result.error);
                        }
                    }
                    mUserRefreshingAlbumContents.remove(mAlbumId);

                    mRefreshAlbumContentsActions.remove(mAlbumId);
                }
            });
        }
    }

    public void reportAlbumUpdate(final long albumId) {
        ThreadUtil.runInMainThread(new ThreadUtil.Runnable() {
            @Override
            public void run() {
                // If there are any albumListListeners then refresh the albumList
                if (!mAlbumListListeners.isEmpty()) {
                    refreshAlbumList(false);
                }

                if (!mAlbumContentsListeners.getAlbumContentsListeners(albumId).isEmpty()) {
                    refreshAlbumContents(albumId, false);
                }
            }
        });
    }

    public void updateLastAccess(final long albumId, final DateTime lastAccess) {
        try {
            mShotVibeDB.setAlbumLastAccess(albumId, lastAccess);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        ThreadUtil.runInBackgroundThread(new ThreadUtil.Runnable() {
            @Override
            public void run() {
                try {
                    mShotVibeAPI.markAlbumAsViewed(albumId, lastAccess);
                } catch (APIException e) {
                    // Ignore error. It is not critical if this event is lost
                }
            }
        });
    }

    public void uploadPhotos(long albumId, List<PhotoUploadRequest> photoUploadRequests) {
        if (!ThreadUtil.isMainThread()) {
            throw new IllegalStateException("Must be called from the Main Thread");
        }

        mUploadManager.uploadPhotos(albumId, photoUploadRequests);

        List<AlbumContentsListener> listeners = mAlbumContentsListeners.getAlbumContentsListeners(albumId);
        if (!listeners.isEmpty()) {
            AlbumContents albumContents;
            try {
                albumContents = mShotVibeDB.getAlbumContents(albumId);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            addUploadingPhotosToAlbumContents(albumContents, mUploadManager.getUploadingPhotos(albumId), mUploadManager.getUploadingOriginalPhotoIds());

            for (AlbumContentsListener listener : listeners) {
                listener.onAlbumContentsNewContent(albumId, albumContents);
            }
        }
    }

    private static void addUploadingPhotosToAlbumContents(AlbumContents albumContents, List<AlbumPhoto> uploadingPhotos, List<String> uploadingOriginalPhotoIds) {
        // Go over all the existing AlbumServerPhotos, and mark any that are currently uploading original
        for (AlbumPhoto p : albumContents.getPhotos()) {
            if (p.getServerPhoto() != null) {
                if (uploadingOriginalPhotoIds.contains(p.getServerPhoto().getId())) {
                    p.getServerPhoto().setUploadingOriginal();
                }
            }
        }

        // Bail out early if there are no uploadingPhotos
        if (uploadingPhotos.size() == 0) {
            // TODO This is temporary
            // This adds a hard-coded uploading photo, for testing
            MediaType mediaType = MediaType.VIDEO;
            AlbumUploadingVideo uploadingVideo = new AlbumUploadingVideo();
            float progress = 0.75f;
            AlbumPhoto testUploadingVideo = new AlbumPhoto(new AlbumUploadingMedia(mediaType, uploadingVideo, progress));
            albumContents.getPhotos().add(testUploadingVideo);

            return;
        }

        // In rare cases it is possible for uploaded photos to be added to the
        // server and contained in albumContents, before the client has
        // received the acknowledgment and so they will have not yet been
        // removed from uploadingPhotos. We must make sure to not show such duplicates

        // But first an optimization: if none of the uploadingPhotos are
        // being isAddingToAlbum, then there can be no duplicates, so just
        // add them all
        boolean foundAddingToAlbum = false;
        for (AlbumPhoto u : uploadingPhotos) {
            if (u.getUploadingPhoto().getState() == AlbumUploadingPhoto.State.AddingToAlbum) {
                foundAddingToAlbum = true;
            }
        }
        if (!foundAddingToAlbum) {
            albumContents.getPhotos().addAll(uploadingPhotos);
            return;
        }

        // Keep track of all the server photo ids in an appropriate efficient data
        // structure, so that duplicates can be found:
        HashSet<String> serverPhotoIds = new HashSet<String>();
        for (AlbumPhoto p : albumContents.getPhotos()) {
            if (p.getServerPhoto() != null) {
                serverPhotoIds.add(p.getServerPhoto().getId());
            }
        }

        // Add only the uploading photos that don't appear in the server photos
        for (AlbumPhoto u : uploadingPhotos) {
            AlbumUploadingPhoto uploadingPhoto = (AlbumUploadingPhoto) u.getUploadingPhoto();
            if (uploadingPhoto.getState() != AlbumUploadingPhoto.State.AddingToAlbum
                    || !serverPhotoIds.contains(uploadingPhoto.getPhotoId())) {
                albumContents.getPhotos().add(u);
            }
        }
    }

    @Override
    public void refreshAlbum(long albumId) {
        Log.d("AlbumManager", "refreshAlbum: " + albumId);
        for (AlbumContentsListener listener : mAlbumContentsListeners.getAlbumContentsListeners(albumId)) {
            listener.onAlbumContentsUploadsProgressed(albumId);
        }
    }

    @Override
    public void photoUploadedOriginal(long albumId) {
        List<AlbumContentsListener> listeners = mAlbumContentsListeners.getAlbumContentsListeners(albumId);

        if (listeners.isEmpty()) {
            return;
        }

        AlbumContents albumContents;
        try {
            albumContents = mShotVibeDB.getAlbumContents(albumId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        for (AlbumContentsListener listener : mAlbumContentsListeners.getAlbumContentsListeners(albumId)) {
            listener.onAlbumContentsUploadsProgressed(albumId);
        }

        addUploadingPhotosToAlbumContents(albumContents, mUploadManager.getUploadingPhotos(albumId), mUploadManager.getUploadingOriginalPhotoIds());

        for (AlbumContentsListener listener : listeners) {
            listener.onAlbumContentsNewContent(albumId, albumContents);
        }
    }

    @Override
    public void photosAddedToAlbum(long albumId, AlbumContents newAlbumContents) {
        try {
            mShotVibeDB.setAlbumContents(albumId, newAlbumContents);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        List<AlbumContentsListener> listeners = mAlbumContentsListeners.getAlbumContentsListeners(albumId);
        if (!listeners.isEmpty()) {
            addUploadingPhotosToAlbumContents(newAlbumContents, mUploadManager.getUploadingPhotos(albumId), mUploadManager.getUploadingOriginalPhotoIds());
        }

        for (AlbumContentsListener listener : listeners) {
            listener.onAlbumContentsNewContent(albumId, newAlbumContents);
        }

        // Trigger a refresh just in case there was a race condition in which the AlbumContents
        // that was returned from the upload arrived after a requested refresh that was sent earlier
        // arrived
        refreshAlbumContents(albumId, false);
    }

    private static <T> boolean listContainsListener(ArrayList<T> list, T listener) {
        for (T l : list) {
            if (l == listener) {
                return true;
            }
        }

        return false;
    }

    private static <T> void listRemoveListener(ArrayList<T> list, T listener) {
        for (int i = 0; i < list.size(); ++i) {
            if (list.get(i) == listener) {
                list.remove(i);
                return;
            }
        }
        throw new IllegalStateException("Listener does not exist");
    }

    private final ShotVibeAPI mShotVibeAPI;
    private final ShotVibeDB mShotVibeDB;
    private final PhotoDownloadManager mPhotoDownloadManager;
    private final UploadManager mUploadManager;
    private final ThreadUtil.Executor mExecutor;

    // All of the following must only be touched on the main thread:
    private final ArrayList<AlbumListListener> mAlbumListListeners;
    boolean mAlbumListUserRefreshing;
    private final AlbumContentsListenersContainer mAlbumContentsListeners;
    private final HashSet<Long> mUserRefreshingAlbumContents;
    private final HashMap<Long, RefreshAlbumContentsTrigerrableAction> mRefreshAlbumContentsActions = new HashMap<Long, RefreshAlbumContentsTrigerrableAction>();

    private static class AlbumContentsListenersContainer {
        public void addAlbumContentsListener(long albumId, AlbumContentsListener listener) {
            ArrayList<AlbumContentsListener> list = mMap.get(albumId);
            if (list != null) {
                if (listContainsListener(list, listener)) {
                    throw new IllegalArgumentException("Tried to add an AlbumContentsListListener that is already registered in the AlbumManager for this albumId");
                }
                list.add(listener);
            } else {
                ArrayList<AlbumContentsListener> newList = new ArrayList<AlbumContentsListener>();
                newList.add(listener);
                mMap.put(albumId, newList);
            }
        }

        public void removeAlbumContentsListener(long albumId, AlbumContentsListener listener) {
            ArrayList<AlbumContentsListener> list = mMap.get(albumId);
            if (list == null) {
                throw new IllegalStateException("Tried to remove an AlbumContentsListener that isn't registered");
            }
            listRemoveListener(list, listener);
        }

        public List<AlbumContentsListener> getAlbumContentsListeners(long albumId) {
            ArrayList<AlbumContentsListener> list = mMap.get(albumId);
            if (list == null) {
                return mEmptyList;
            } else {
                return list;
            }
        }

        private HashMap<Long, ArrayList<AlbumContentsListener>> mMap = new HashMap<Long, ArrayList<AlbumContentsListener>>();
        private final ArrayList<AlbumContentsListener> mEmptyList = new ArrayList<AlbumContentsListener>();
    }
}
