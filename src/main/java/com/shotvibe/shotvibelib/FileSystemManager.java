package com.shotvibe.shotvibelib;

public interface FileSystemManager {
    /**
     * Any errors deleting the file will be ignored
     *
     * @param filePath the file that should be deleted
     */
    void deleteFile(String filePath);
}
