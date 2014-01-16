package com.shotvibe.shotvibelib;

public interface SQLConnection {

    void beginTransaction();

    void setTransactionSuccesful();

    void endTransaction();

    SQLCursor query(String query) throws SQLException;

    SQLCursor query(String query, SQLValues sqlValues) throws SQLException;

    void update(String query) throws SQLException;

    void update(String query, SQLValues sqlValues) throws SQLException;

    void executeSQLScript(String filename) throws SQLException;

    void clearDatabase() throws SQLException;
}
