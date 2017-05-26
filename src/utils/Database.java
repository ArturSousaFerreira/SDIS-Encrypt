package utils;

import backend.Chunk;
import backend.ConfigManager;
import backend.SavedFile;
import protocols.FileDelete;

import java.io.*;
import java.util.*;

/**
 * Created by Duarte on 24-Mar-17.
 */
public class Database implements Serializable {


    public static final String FILE = "metadata.ser";

    private String folder;
    private long maxBackupSize; // Bytes


    private List<Chunk> savedChunks; // chunks from others
    private Map<String, SavedFile> savedFiles; // files from me


    //private Map<String, Integer> mDeletedFiles;

    public Database() {
        maxBackupSize = 40000 * 1000;// 40MB default space
        folder = "";
        savedFiles = new HashMap<String, SavedFile>();
        savedChunks = Collections.synchronizedList(new ArrayList<Chunk>());

        //mDeletedFiles = new HashMap<String, Integer>();
    }

    public List<Chunk> getSavedChunks() {
        return savedChunks;
    }

    public long getMaxBackupSize() {
        return maxBackupSize;
    }

    public synchronized void setAvailSpace(long space) {
        if (space > 0) {
            maxBackupSize = space;
        }

    }

    public void saveDatabase() {
        try {
            FileOutputStream fileOut = new FileOutputStream("metadata.ser");
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(this);
            out.close();
            fileOut.close();

        } catch (IOException i) {

        }
    }

    public Chunk getSavedChunk(String fileId, int chunkNo) {
        Chunk rChunk = null;

        synchronized (savedChunks) {
            for (Chunk chunk : savedChunks) {
                if (chunk.getFileID().equals(fileId)
                        && chunk.getChunkNo() == chunkNo) {
                    rChunk = chunk;
                    break;
                }
            }
        }

        if (rChunk == null) {
            for (SavedFile file : savedFiles.values()) {
                if (file.getFileId().equals(fileId)) {
                    // I have the chunk in my own file
                    rChunk = file.getChunkList().get(chunkNo);
                    break;
                }
            }
        }

        return rChunk;
    }

    public synchronized void incChunkReplication(String fileId, int chunkNo)
            throws ConfigManager.InvalidChunkException {
        SavedFile file = savedFiles.get(fileId);
        Chunk chunk = null;
        if (file != null) {
            // This is the owner machine of chunk's parent file
            file.incChunkReplication(chunkNo);
        } else {
            // It is a chunk saved in a backup operation
            for (Chunk list :
                    savedChunks) {
                if (list.getFileID().equals(fileId)
                        && list.getChunkNo() == chunkNo) {
                    chunk = list;
                    break;
                }
            }
        }
        if (chunk != null) {
            chunk.incCurrentReplication();
        } else {
            throw new ConfigManager.InvalidChunkException();
        }
    }

    public SavedFile getNewSavedFile(String path, int replication) throws
            SavedFile.FileTooLargeException,
            SavedFile.FileDoesNotExistsException,
            ConfigManager.FileAlreadySaved {
        SavedFile file = new SavedFile(path, replication);
        savedFiles.put(file.getFileId(), file);

        return file;
    }

    public void addChunk(Chunk chunk) {
        synchronized (savedChunks) {
            savedChunks.add(chunk);
        }
    }

    public SavedFile getFileByPath(String path) {
        for (SavedFile file : savedFiles.values()) {
            if (file.getFilePath().equals(path)) {
                return file;
            }
        }
        System.out.println("getFileByPath File Not Found: " + path);
        printSavedFiles();
        return null;
    }
    
    public void printSavedFiles(){
    	for (SavedFile file : savedFiles.values()) {
            System.out.println("FileId: " + file.getFileId() + " FilePath: " +file.getFilePath().toString());
        }
    }

    public void print() {

        //print MAX SPACE / USED SPACE
        System.out.println();
        System.out.println("/////////////////////////////////////////");
        System.out.println("Used Space: " + getUsedSpace() / 1000 + " / " + maxBackupSize / 1000 + " KB");
        System.out.println("MY FILES:");
        for (SavedFile file : savedFiles.values()
                ) {
            System.out.println("file = " + file.getFilePath());
            System.out.println("FileId = " + file.getFileId());
            System.out.println("REP DEG = " + file.getWantedReplicationDegree());
            file.showFileChunks();
            System.out.println();
        }
        System.out.println();
        System.out.println("//////////////////////////////");
        System.out.println();
        System.out.println("Chunks Stored:");
        System.out.println();
        for (Chunk chunk : savedChunks
                ) {
            System.out.println("chunk ID = " + chunk.getFileID());
            System.out.println("Chunk NO = " + chunk.getChunkNo());
            System.out.println("chunk size = " + chunk.getSize());
            System.out.println("CurrentReplicationDeg = " + chunk.getCurrentReplicationDegree());
            System.out.println();
        }
    }

    public boolean isChunkBackedUP(String fileID, int chunkNR) {
        for (Chunk chunk : savedChunks
                ) {
            if (chunk.getFileID().equals(fileID) && chunk.getChunkNo() == chunkNR) {
                return true;
            }
        }
        return false;
    }

    public void deleteChunksFile(String fileID) {

        synchronized (savedChunks) {
            Iterator<Chunk> iterator = savedChunks.iterator();

            while (iterator.hasNext()) {
                Chunk chunk = iterator.next();
                if (chunk.getFileID().equals(fileID)) {
                    chunk.removeData();
                    iterator.remove();
                }
            }
        }
        File folder = new File(fileID);
        folder.delete();
    }


    public void removeSavedFile(String fileID,String username) {

        synchronized (savedFiles) {
            Iterator<SavedFile> it = savedFiles.values().iterator();

            while (it.hasNext()) {
                SavedFile file = it.next();
                if (fileID.equals(file.getFileId())) {
                    FileDelete.getInstance().deleteFile(file.getFileId(),username);
                    it.remove();
                }
            }
        }
    }

    public long getUsedSpace() {
        long usedSpace = 0;
        for (Chunk chunk : savedChunks
                ) {
            usedSpace += chunk.getSize();
        }
        return usedSpace;
    }

    public  synchronized void decChunkReplication(String fileId, int chunkNo) {

for (SavedFile file:savedFiles.values()){
    if (file.getFileId().equals(fileId)){
        file.getChunkList().get(chunkNo).decCurrentReplicationDegree();
    }
}

        for (Chunk chunk : savedChunks
                ) {
            if (chunk.getFileID().equals(fileId) && chunk.getChunkNo() == chunkNo) {
                chunk.setCurrentReplicationDegree(chunk.getCurrentReplicationDegree() - 1);
                return;
            }
        }
    }

    public void removeChunk(Chunk deleteChunk) {
        synchronized (savedChunks) {
            Iterator<Chunk> iterator = savedChunks.iterator();

            while (iterator.hasNext()) {
                Chunk chunk = iterator.next();
                if (chunk.getFileID().equals(deleteChunk.getFileID()) && chunk.getChunkNo() == deleteChunk.getChunkNo()) {
                    chunk.removeData();
                    iterator.remove();
                    break;
                }
            }
        }
        File folder = new File(deleteChunk.getFileID());
        folder.delete();
    }
}
