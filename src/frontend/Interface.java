package frontend;

import backend.*;
import protocols.Election;
import protocols.FileDelete;
import utils.SharedDatabase;
import protocols.SpaceReclaim;
import utils.RMI_Interface;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

/**
 * Created by Duarte on 16-Mar-17.
 */
public class Interface implements RMI_Interface {

    private static Interface instance = null;
    private String accessPoint;

    private Interface() {
    }

    public static Interface getInstance() {
        if (instance == null) {
            instance = new Interface();
        }
        return instance;
    }

    public void startUp() {
        startRMI(accessPoint);
        ConfigManager.getConfigManager().startupListeners();
        ConfigManager.getConfigManager().saveDB();
        ConfigManager.getConfigManager().startupTime();
        Election.getElectionInstance().startUp();

    }


    private void startRMI(String acessPoint) {
        try {
            RMI_Interface stub = (RMI_Interface) UnicastRemoteObject.exportObject(this, 0);
            Registry reg = null;
            try {
                reg = LocateRegistry.createRegistry(RMI_Interface.RMI_PORT);
            } catch (RemoteException e) {
                System.out.println("RMI registry already running");
                reg = LocateRegistry.getRegistry(RMI_Interface.RMI_PORT);
            }
            reg.rebind(accessPoint, stub);
        } catch (RemoteException e) {
            System.out.println("RMI problem");
            e.printStackTrace();
        }
    }

    public String sayHello() throws RemoteException {
        return "Hello!";
    }

    public boolean backupFile(String filePath, int replication, String username) {

        if (validateFile(filePath)) {
            System.out.println("===============  validateFile  ==================");
            SavedFile file = null;
            try {
                file = ConfigManager.getConfigManager().getNewSavedFile(filePath, replication);
                ConfigManager.getConfigManager().saveDB();
            } catch (SavedFile.FileTooLargeException e) {
                e.printStackTrace();
            } catch (ConfigManager.FileAlreadySaved fileAlreadySaved) {
                System.out.println("file Already Saved");
                //fileAlreadySaved.printStackTrace();
            } catch (SavedFile.FileDoesNotExistsException e) {
                System.out.println("File Does Not Exists");
                //e.printStackTrace();
            }
            // file.showFileChunks();

            // TODO: if fileSize + database.availableSpace > Max space, cancel
            // file.showFileChunks();

            if (FileBackup.getInstance().saveFile(file)) {

                FileInfo info = ConfigManager.getConfigManager().getSharedDatabase().addFile(file, username);
                InetAddress multCtrlAddr = ConfigManager.getConfigManager().getMcAddr();
                int multCtrlPort = ConfigManager.getConfigManager().getmMCport();

                MulticastServer sender = new MulticastServer(multCtrlAddr, multCtrlPort);
                String msg = "";

                msg = "ADDFILE" + " " + "1.0" + " " + ConfigManager.getConfigManager().getMyID() + " " + username + " " + file.getFileId() + " " + file.getFileName() + " " + file.getChunkList().size() + " " + info.getData() + MulticastServer.CRLF + MulticastServer.CRLF;

                try {
                    sender.sendMessage(msg.getBytes(MulticastServer.ASCII_CODE));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }


                return true;
            } else
                return false;

        } else {

            return false;

        }
    }

    private boolean validateFile(String path) {
        // exists
        File f = new File(path);
        if (f.exists()) {
            if (f.length() < SavedFile.MAX_FILE) {
                return true;
            }
        }
        return false;
    }

    public void setAccessPoint(String accessPoint) {
        this.accessPoint = accessPoint;
    }

    /*
        public boolean restoreFile(String filePath) {

            SavedFile fileToRestore = ConfigManager.getConfigManager().getFileByPath(filePath);
            if (fileToRestore != null) {
                return FileRestore.getInstance().restoreFile(fileToRestore);
            }
            return false;
        }
    */
    public boolean restoreFile(String username, String fileID) {
        FileInfo fileToRestore = ConfigManager.getConfigManager().getSharedDatabase().getSavedFile(username, fileID);
        if (fileToRestore != null) {
            return FileRestore.getInstance().restoreFile(fileToRestore);
        }
        return false;
    }


    public void statemydatabase() {
        ConfigManager.getConfigManager().printState();
    }

    public boolean deleteFile(String fileId, String username) throws RemoteException {

        boolean response = false;
        String filePath = null;
        ArrayList<FileInfo> files = ConfigManager.getConfigManager().getSharedDatabase().getUserFiles(username);

        for (FileInfo file : files)
            if (file.getFileId().equals(fileId)) {

                response = ConfigManager.getConfigManager().getSharedDatabase().removeFile(file, username);
                if (response) {
                    break;
                }
            }

        if (response)
            ConfigManager.getConfigManager().removeFile(fileId,username);
            ConfigManager.getConfigManager().deleteFile(fileId);
            FileDelete.getInstance().deleteFile(fileId,username);

        return response;

    }

    public void stateSharedDatabase() {
        ConfigManager.getConfigManager().getSharedDatabase().print();
    }

    public boolean spaceReclaim(int newSpace) {

        ConfigManager.getConfigManager().setMaxSpace(newSpace * 1000);

        while (ConfigManager.getConfigManager().getMaxSpace() <= ConfigManager.getConfigManager().getUsedSpace()) {
            // get next reclaim//
            Chunk deletedChunk = ConfigManager.getConfigManager().getNextRemovableChunk();
            SpaceReclaim.getInstance().reclaim(deletedChunk);
            ConfigManager.getConfigManager().removeChunk(deletedChunk);
        }
        return true;
    }

    public boolean createUser(String username, String password) {

        User user = ConfigManager.getConfigManager().getSharedDatabase().addUser(new User(username, password, false));
        if (user != null) {

            InetAddress multCtrlAddr = ConfigManager.getConfigManager().getMcAddr();
            int multCtrlPort = ConfigManager.getConfigManager().getmMCport();

            MulticastServer sender = new MulticastServer(multCtrlAddr, multCtrlPort);
            String msg = "";

            msg = "ADDUSER" + " " + "1.0" + " " + ConfigManager.getConfigManager().getMyID() + " " + username + " " + user.getPassword() + MulticastServer.CRLF + MulticastServer.CRLF;

            try {
                sender.sendMessage(msg.getBytes(MulticastServer.ASCII_CODE));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            return true;
        }
        return false;
    }

    public boolean login(String username, String password) {
        return ConfigManager.getConfigManager().getSharedDatabase().login(username, password);
    }

    public ArrayList<FileInfo> getUserFiles(String username) {
        return ConfigManager.getConfigManager().getSharedDatabase().getUserFiles(username);
    }
}