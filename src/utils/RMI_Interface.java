package utils;

import backend.FileInfo;
import backend.SavedFile;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 * Created by Duarte on 25-Mar-17.
 */
public interface RMI_Interface extends Remote{

    int RMI_PORT = 1900;
    String RMI_HOST = "localhost";

    String sayHello()throws RemoteException;
    boolean backupFile(String filePath, int replication, String username) throws RemoteException;
    boolean restoreFile(String username, String fileID)throws RemoteException;
    void statemydatabase() throws RemoteException;
    void stateSharedDatabase() throws RemoteException;
    boolean deleteFile(String fileId, String username) throws RemoteException;
    boolean spaceReclaim(int newSpace) throws RemoteException;
    boolean createUser(String username, String password)throws RemoteException;
    boolean login(String username, String password)throws RemoteException;
    ArrayList<FileInfo> getUserFiles(String username)throws RemoteException;

    }
