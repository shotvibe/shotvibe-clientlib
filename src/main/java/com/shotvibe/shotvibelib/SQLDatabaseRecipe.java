package com.shotvibe.shotvibelib;

public abstract class SQLDatabaseRecipe<T> {
    /**
     *
     * @param databaseFilename Should be a simple filename such as "mydatabase.db". The actual
     *                         location where it will be stored is system dependant.
     * @param databaseVersion The version of the database that the code expects to work with
     *                        (starting at 1). If the saved database has an older version than
     *                        @upgradeDB@ will be called.
     */
    public SQLDatabaseRecipe(String databaseFilename, int databaseVersion) {
        if (databaseFilename == null) {
            throw new IllegalArgumentException("databaseFilename cannot be null");
        }
        mDatabaseFilename = databaseFilename;
        mDatabaseVersion = databaseVersion;
    }

    private final int mDatabaseVersion;
    private final String mDatabaseFilename;

    public int getDatabaseVersion() {
        return mDatabaseVersion;
    }

    public String getDatabaseFilename() {
        return mDatabaseFilename;
    }

    /**
     * Will be called before openDB is called if no database file exists and one needs to be
     * created. Will be called inside a transaction and should NOT call any transaction commands.
     *
     * @param conn
     * @throws SQLException
     */
    public abstract void populateNewDB(SQLConnection conn) throws SQLException;

    /**
     * Will be called before openDB is called if a database file exists but needs to be upgraded.
     * Will be called inside a transaction and should NOT call any transaction commands.
     *
     * @param conn
     * @param oldVersion
     * @throws SQLException
     */
    public abstract void upgradeDB(SQLConnection conn, int oldVersion) throws SQLException;

    /**
     * Will always be called. Should create and return an object that interfaces with the database.
     *
     * @param conn
     * @return
     */
    public abstract T openDB(SQLConnection conn);
}
