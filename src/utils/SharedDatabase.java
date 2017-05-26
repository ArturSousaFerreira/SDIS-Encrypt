package utils;

import backend.FileInfo;
import backend.SavedFile;
import backend.User;
import protocols.ShareClock;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;

public class SharedDatabase implements Serializable {
    private ArrayList<User> users;
    private HashMap<String, ArrayList<FileInfo>> files;
    private long lastChange;
    public static final String FILE = "shareddatabase.ser";


    public SharedDatabase() {
        users = new ArrayList<>();
        files = new HashMap<>();
        lastChange = 0;
    }

    public ArrayList<User> getUsers() {
        return users;
    }

    public long getLastChange() {
        return lastChange;
    }

    private HashMap<String, ArrayList<FileInfo>> getFiles() {
        return files;
    }

    private void updateLastChange() {
        try {
            lastChange = ShareClock.getClock().getTime();
        } catch (ShareClock.notSyncedException e) {
            lastChange = new Date().getTime();
        }
    }

    public User addUser(User user) {
        for (int i = 0; i < users.size(); i++)
            if (users.get(i).getUsername().equals(user.getUsername()))
                return null;

        users.add(user);
        files.put(user.getUsername(), new ArrayList<FileInfo>());
        updateLastChange();
        saveDatabase();
        return user;
    }


    public ArrayList<FileInfo> getUserFiles(String username) {
        return files.get(username);
    }

    public boolean addFile(FileInfo file, String username) {

        ArrayList<FileInfo> userfiles = files.get(username);

        for (FileInfo userfile : userfiles)
            if (userfile.getFileId().equals(file.getFileId()))
                return false; //if exists

        // file not found
        userfiles.add(file);
        saveDatabase();

        return true;
    }

    public FileInfo addFile(SavedFile file, String username) {

        ArrayList<FileInfo> userfiles = files.get(username);

        for (FileInfo userfile : userfiles)
            if (userfile.getFileId().equals(file.getFileId()))
                return null; //if exists

        // file not found
        FileInfo f = new FileInfo(file);
        userfiles.add(f);
        saveDatabase();

        return f;
    }

    public boolean removeFile(FileInfo file, String username) {

        ArrayList<FileInfo> user_files = files.get(username);
        boolean deleted = false;
        Iterator<FileInfo> it;
        for (it = user_files.iterator(); it.hasNext(); ) {
            FileInfo current = it.next();

            if (current.getFileId().equals(file.getFileId())) {
                it.remove();
                saveDatabase();
                deleted = true;
                break;
            }
            it.next();
        }
        return deleted;
    }

    public void saveDatabase() {
        try {
            FileOutputStream fileOut = new FileOutputStream(FILE);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(this);
            out.close();
            fileOut.close();
            updateLastChange();
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    public boolean login(String username, String password) {
        for (int i = 0; i < users.size(); i++) {
            User u = users.get(i);
            if (u.login(username, password))
                return true;
        }

        return false;
    }

    public void print() {

        //print MAX SPACE / USED SPACE
        System.out.println();
        System.out.println("/////////////////////////////////////////");
        System.out.println();
        System.out.println("FILES IN SHARED DATABASE: \n");


        for (Map.Entry<String, ArrayList<FileInfo>> entry : files.entrySet()) {
            String key = entry.getKey();
            ArrayList<FileInfo> value = entry.getValue();
            for (FileInfo file : value) {
                System.out.println("File name : " + file.getFileName());
                System.out.println("Date : " + file.getData());
                System.out.println("FileID : " + file.getFileId());
                System.out.println("Belongs to : " + key);
            }
        }

        System.out.println();
        System.out.println("/////////////////////////////////////////");
        System.out.println();
        System.out.println("Users IN SHARED DATABASE: \n");

        for (User user : users) {
            System.out.println(user.getUsername());
        }

        System.out.println();
        System.out.println("/////////////////////////////////////////");
        System.out.println();
    }

    public SharedDatabase sync(SharedDatabase masterDatabase) {
        long masterLastChange = masterDatabase.getLastChange();
        ArrayList<User> masterUsers = masterDatabase.getUsers();
        HashMap<String, ArrayList<FileInfo>> masterFiles = masterDatabase.getFiles();

        if (lastChange == 0){
            lastChange = masterLastChange;
            users = masterUsers;
            files = masterFiles;
            return this;
        }


        for (User mUser : masterUsers) {
            boolean match = false;
            for (User lUser : users) {
                if (mUser.getUsername().equals(lUser.getUsername())) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                //add new user from master
                users.add(mUser);
            }
        }

        for (String mUserName : masterFiles.keySet()) {
            boolean match = false;
            for (String lUserName : files.keySet()) {
                if (mUserName.equals(lUserName)) {
                    match = true;

                    ArrayList<FileInfo> mFiles = masterFiles.get(mUserName);
                    ArrayList<FileInfo> lFiles = files.get(mUserName);

                    for (FileInfo masterFile : mFiles) {
                        boolean fileFound = false;

                        for (FileInfo lFile : lFiles) {
                            if (masterFile.getFileId().equals(lFile.getFileId())) {
                                fileFound = true;
                                break;
                            }
                        }
                        if (!fileFound) {
                            files.get(mUserName).add(masterFile);
                        }
                    }
                    break;
                }
            }
            if (!match) {
                files.put(mUserName, masterFiles.get(mUserName));
            }
        }
        saveDatabase();
        return this;
    }

    public FileInfo getSavedFile(String username, String fileID) {

        for (FileInfo file : files.get(username)){
            if (file.getFileId().equals(fileID)){
                return file;
            }
        }
        return null;
    }
}
