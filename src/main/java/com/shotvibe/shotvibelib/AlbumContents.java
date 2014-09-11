package com.shotvibe.shotvibelib;

public class AlbumContents extends AlbumBase {

    public AlbumContents(long id, String etag, String name, AlbumUser creator, DateTime dateCreated, DateTime dateUpdated, long numNewPhotos, DateTime lastAccess, ArrayList<AlbumPhoto> photos, ArrayList<AlbumMember> members) {
        super(id, etag, name, creator, dateCreated, dateUpdated, numNewPhotos, lastAccess);

        if (photos == null) {
            throw new IllegalArgumentException("photos cannot be null");
        }
        if (members == null) {
            throw new IllegalArgumentException("members cannot be null");
        }

        mPhotos = photos;
        mMembers = members;
    }

    public ArrayList<AlbumPhoto> getPhotos() {
        return mPhotos;
    }

    public ArrayList<AlbumMember> getMembers() {
        return mMembers;
    }

    private final ArrayList<AlbumPhoto> mPhotos;
    private final ArrayList<AlbumMember> mMembers;

}
