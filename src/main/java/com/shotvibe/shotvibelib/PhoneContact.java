package com.shotvibe.shotvibelib;

public class PhoneContact {
    public PhoneContact(String phoneNumber, String lastName, String firstName) {
        if (phoneNumber == null) {
            throw new IllegalArgumentException("phoneNumber cannot be null");
        }
        if (lastName == null) {
            throw new IllegalArgumentException("lastName cannot be null");
        }
        if (firstName == null) {
            throw new IllegalArgumentException("firstName cannot be null");
        }
        if (lastName.length() == 0 && firstName.length() == 0) {
            throw new IllegalArgumentException("contact must have a non-empty name");
        }

        mPhoneNumber = phoneNumber;
        mLastName = lastName;
        mFirstName = firstName;
    }

    /**
     * For devices that don't split the name into first + last.
     *
     * The "last name" field will take the value of fullName.
     *
     * The "first name" field will be empty.
     *
     * @param fullName
     * @param phoneNumber
     */
    public PhoneContact(String phoneNumber, String fullName) {
        if (phoneNumber == null) {
            throw new IllegalArgumentException("phoneNumber cannot be null");
        }
        if (fullName == null) {
            throw new IllegalArgumentException("fullName cannot be null");
        }
        if (fullName.length() == 0) {
            throw new IllegalArgumentException("contact must have a non-empty name");
        }

        mPhoneNumber = phoneNumber;
        mLastName = fullName;
        mFirstName = "";
    }

    public String getPhoneNumber() {
        return mPhoneNumber;
    }

    public String getLastName() {
        return mLastName;
    }

    public String getFirstName() {
        return mFirstName;
    }

    public String getFullName() {
        if (mFirstName.length() > 0) {
            return mFirstName + " " + mLastName;
        } else {
            return mLastName;
        }
    }

    @Override
    public String toString() {
        return "[" + getFullName() + ": " + getPhoneNumber() + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }

        PhoneContact that = (PhoneContact) o;

        if (!mFirstName.equals(that.mFirstName)) {
            return false;
        }
        if (!mLastName.equals(that.mLastName)) {
            return false;
        }
        if (!mPhoneNumber.equals(that.mPhoneNumber)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = mPhoneNumber.hashCode();
        result = 31 * result + mLastName.hashCode();
        result = 31 * result + mFirstName.hashCode();
        return result;
    }

    private final String mPhoneNumber;
    private final String mLastName;
    private final String mFirstName;
}
