package com.shotvibe.shotvibelib;

public class AlbumMember {
    public static enum InviteStatus {
        JOINED,
        SMS_SENT,
        INVITATION_VIEWED
    }

    /**
     *
     * @param user
     * @param inviteStatus May be null if status is unknown
     */
    public AlbumMember(AlbumUser user, boolean albumAdmin, long addedByUserId, InviteStatus inviteStatus) {
        if (user == null) {
            throw new IllegalArgumentException("user cannot be null");
        }

        mUser = user;
        mAlbumAdmin = albumAdmin;
        mAddedByUserId = addedByUserId;
        mInviteStatus = inviteStatus;
    }

    public AlbumUser getUser() {
        return mUser;
    }

    public boolean getAlbumAdmin() {
        return mAlbumAdmin;
    }

    public long getAddedByUserId() {
        return mAddedByUserId;
    }

    /**
     *
     * @return May return null if the status is unknown (such as the
     */
    public InviteStatus getInviteStatus() {
        return mInviteStatus;
    }

    private AlbumUser mUser;
    private final boolean mAlbumAdmin;
    private final long mAddedByUserId;
    private InviteStatus mInviteStatus;
}
