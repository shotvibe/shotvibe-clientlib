package com.shotvibe.shotvibelib;

public interface SQLCursor {
    /**
     * Move the cursor to the next row.
     *
     * <p>This method will return false if the cursor is already past the
     * last entry in the result set.
     *
     * @return whether the move succeeded.
     */
    boolean moveToNext();

    int getInt(int columnIndex) throws SQLException;

    long getLong(int columnIndex) throws SQLException;

    double getDouble(int columnIndex) throws SQLException;

    String getString(int columnIndex) throws SQLException;

    boolean isNull(int columnIndex) throws SQLException;

    void close();
}
