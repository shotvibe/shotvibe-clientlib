package com.shotvibe.shotvibelib;

public class AlbumUser {
    private final long mMemberId;
    private final String mMemberNickname;
    private final DateTime mLastOnline;
    private final String mMemberAvatarUrl;

    public AlbumUser(long memberId, String memberNickname, DateTime lastOnline, String memberAvatarUrl) {
        if (memberNickname == null) {
            throw new IllegalArgumentException("memberNickname cannot be null");
        }
        if (memberAvatarUrl == null) {
            throw new IllegalArgumentException("memberAvatarUrl cannot be null");
        }

        mMemberId = memberId;
        mMemberNickname = memberNickname;
        mLastOnline = lastOnline;
        mMemberAvatarUrl = memberAvatarUrl;
    }

    public long getMemberId() {
        return mMemberId;
    }

    public String getMemberNickname() {
        return mMemberNickname;
    }

    // May return null
    public DateTime getLastOnline() {
        return mLastOnline;
    }

    public String getMemberAvatarUrl() {
        return mMemberAvatarUrl;
    }
}
