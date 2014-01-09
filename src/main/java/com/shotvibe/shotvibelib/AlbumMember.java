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
    public AlbumMember(AlbumUser user, InviteStatus inviteStatus) {
        if (user == null) {
            throw new IllegalArgumentException("user cannot be null");
        }

        mUser = user;
        mInviteStatus = inviteStatus;
    }

    public AlbumUser getUser() {
        return mUser;
    }

    /**
     *
     * @return May return null if the status is unknown (such as the
     */
    public InviteStatus getInviteStatus() {
        return mInviteStatus;
    }

    private AlbumUser mUser;
    private InviteStatus mInviteStatus;
}
