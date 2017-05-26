package backend;

import protocols.Election;
import protocols.ShareClock;
import utils.Database;
import utils.SharedDatabase;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConfigManager {
    private static final String VERSION = "1.0";
    private MCListener mcListener;


    private SharedDatabase sharedDatabase;
    private int myID;
    private MDRListener mdrListener;
    private MDBListener mdbListener;
    private ExecutorService mExecutorService = null;

    private String RMI_Object_Name = null;

    private long upTime;
    private boolean isDatabaseLoaded = false;
    private Database database = null;
    private Random random;
    private InetAddress mcAddr = null, mdbAddr = null, mdrAddr = null;
    private int mMCport = 0, mMDBport = 0, mMDRport = 0;


    private long startupTime = 0;
    // static
    private static ConfigManager iConfigManager = null;
    private InetAddress myIP;


    private ConfigManager() {
        mcListener = null;
        mdrListener = null;
        mdbListener = null;
        mExecutorService = Executors.newFixedThreadPool(60);
        isDatabaseLoaded = loadDatabase();
        sharedDatabase = new SharedDatabase();
        random = new Random();

    }

    public static ConfigManager getConfigManager() {
        if (iConfigManager == null) {
            iConfigManager = new ConfigManager();
        }
        return iConfigManager;
    }

    private boolean loadDatabase() {
        try {
            FileInputStream fileIn = new FileInputStream(Database.FILE);
            ObjectInputStream in = new ObjectInputStream(fileIn);

            try {
                database = (Database) in.readObject();
            } catch (ClassNotFoundException e) {

                System.out.println("Starting new DB");
                in.close();
                fileIn.close();
                database = new Database();
                return false;
            }
            System.out.println("== DB already exists ==");
            System.out.println("===== Saved Files =====");
            database.printSavedFiles();
            System.out.println("=======================");
            in.close();
            fileIn.close();

        } catch (FileNotFoundException e) {

            System.out.println("Starting new DB");
            database = new Database();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }


    public void setRMI_Object_Name(String nome) {
        RMI_Object_Name = nome;
    }

    public boolean setAdresses(String mcIP, int mcPort, String mdbIP, int mdbPort, String mdrIP, int mdrPort) {

        try {
            this.mcAddr = InetAddress.getByName(mcIP);
            this.mdbAddr = InetAddress.getByName(mdbIP);
            this.mdrAddr = InetAddress.getByName(mdrIP);
            System.out.println("mcAddr = " + mdrAddr);
            this.mMCport = mcPort;
            this.mMDBport = mdbPort;
            this.mMDRport = mdrPort;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void startupListeners() {
        if (mcListener == null) {
            mcListener = MCListener.getInstance();
            mExecutorService.execute(mcListener);
        }
        if (mdbListener == null) {
            mdbListener = MDBListener.getInstance();
            mExecutorService.execute(mdbListener);
        }
        if (mdrListener == null) {
            mdrListener = MDRListener.getInstance();
            mExecutorService.execute(mdrListener);
        }
    }

    public InetAddress getMcAddr() {
        return mcAddr;
    }

    public InetAddress getMdbAddr() {
        return mdbAddr;
    }

    public InetAddress getMdrAddr() {
        return mdrAddr;
    }

    public int getmMCport() {
        return mMCport;
    }

    public int getmMDBport() {
        return mMDBport;
    }

    public int getmMDRport() {
        return mMDRport;
    }

    public Executor getExecutorService() {
        return mExecutorService;
    }

    public void addSavedChunk(Chunk chunk) {
        database.addChunk(chunk);
        saveDB();
    }

    public void terminate() {
        mExecutorService.shutdown();
    }

    public int getMyID() {
        return myID;
    }

    public void setMyID(int myID) {
        this.myID = myID;
    }

    public Chunk getSavedChunk(String fileId, int chunkNo) {
        return database.getSavedChunk(fileId, chunkNo);
    }

    public synchronized void incChunkReplication(String fileId, int chunkNo)
            throws InvalidChunkException {
        database.incChunkReplication(fileId, chunkNo);
        database.saveDatabase();
    }

    public synchronized void decChunkReplication(String fileId, int chunkNo) {
        database.decChunkReplication(fileId, chunkNo);
        database.saveDatabase();
    }

    public void removeChunk(Chunk chunk) {
        database.removeChunk(chunk);
        saveDB();
    }

    public void saveDB() {
        database.saveDatabase();
    }

    public SavedFile getNewSavedFile(String path, int replication)
            throws SavedFile.FileTooLargeException, FileAlreadySaved, SavedFile.FileDoesNotExistsException {
        return database.getNewSavedFile(path, replication);
    }

    public SavedFile getFileByPath(String path) {
        return database.getFileByPath(path);
    }


    public String getRMI_Object_Name() {
        return RMI_Object_Name;
    }

    public void printState() {

        database.print();
    }

    public void deleteFile(String fileID) {
        database.deleteChunksFile(fileID);
        saveDB();
    }

    public void removeFile(String fileID, String username) {
        database.removeSavedFile(fileID,username);
        saveDB();
    }

    public long getUsedSpace() {
        return database.getUsedSpace();
    }

    public long getMaxSpace() {
        return database.getMaxBackupSize();
    }

    public void setMaxSpace(long maxSpace) {

        database.setAvailSpace(maxSpace);
        saveDB();
    }

    public Chunk getNextRemovableChunk() {
        List<Chunk> savedChunks = database.getSavedChunks();

        for (Chunk chunk : savedChunks
                ) {
            if (chunk.getCurrentReplicationDegree() > chunk.getWantedReplicationDegree()) {
                return chunk;
            }
        }

        Chunk randomChunk = null;

        do {
            randomChunk = savedChunks.get(random.nextInt(savedChunks.size()));
        } while (randomChunk.getCurrentReplicationDegree() <= 0);

        return randomChunk;
    }

    public long getUpTime() {
        Date d = new Date();
        if (startupTime > 0) {
            System.out.println("getTime " + d.getTime() + " - " + startupTime);
            return d.getTime() - startupTime;
        } else {
            return 0;
        }
    }

    public void startupTime() {
        this.startupTime = new Date().getTime();
    }

    public InetAddress getMyIP() {
        return myIP;
    }

    public void setMyIP(String myIP) {
        try {
            this.myIP = InetAddress.getByName(myIP);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

    }

    public void startClocks() {
        startupTime();
        if (!Election.getElectionInstance().isMaster()) {
            ShareClock.getClock().startSync();
        }
    }

    public SharedDatabase getSharedDatabase() {
        return sharedDatabase;
    }

    public void setMasterDB(SharedDatabase masterDB) {
        this.sharedDatabase= masterDB;
        sharedDatabase.saveDatabase();
    }



    public static class ConfigurationsNotInitializedException extends Exception {
    }

    public static class InvalidFolderException extends Exception {

    }

    public static class InvalidBackupSizeException extends Exception {

    }

    public static class InvalidChunkException extends Exception {


    }

    public static class FileAlreadySaved extends Exception {

    }
}
