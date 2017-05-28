package backend;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.util.Base64;

/**
 * Created by Duarte on 18-Mar-17.
 */
public class Chunk implements Serializable {

    public static int MAX_CHUNK_SIZE = 64000;

    private SavedFile file;
    private String fileID;
    private long chunkNo;
    private int currentReplicationDegree;
    private int size = 0;


    private int wantedReplicationDegree;

    public boolean isMyFile() {
        return isMyFile;
    }

    private boolean isMyFile;

    public Chunk(SavedFile tFile, long tChunkno) {
        file = tFile;
        chunkNo = tChunkno;
        fileID = tFile.getFileId();
        currentReplicationDegree = 0;
        wantedReplicationDegree = tFile.getWantedReplicationDegree();

        isMyFile = true;

    }

    public Chunk(String fileId, int chunkNo, int wantedReplication, int size) {
        this.file = null;
        this.chunkNo = chunkNo;
        this.fileID = fileId;
        this.size = size;
        currentReplicationDegree = 0;
        this.wantedReplicationDegree = wantedReplication;
        isMyFile = false;
    }

    public long getChunkNo() {
        return chunkNo;
    }

    public void setChunkNo(long chunkNo) {
        this.chunkNo = chunkNo;
    }

    public synchronized int getCurrentReplicationDegree() {
        return currentReplicationDegree;
    }

    public int getWantedReplicationDegree() {
        return wantedReplicationDegree;
    }

    public void setCurrentReplicationDegree(int currentReplicationDegree) {
        this.currentReplicationDegree = currentReplicationDegree;
    }


    public String getFileID() {
        return fileID;
    }

    public void setFileID(String fileID) {
        this.fileID = fileID;
    }

    public int getSize() {
        return size;
    }

    public byte[] getData() {

        if (isMyFile) {
            if (file.exists()) {
                int offset = (int) (SavedFile.CHUNK_SIZE * chunkNo);

                int chunkSize = (int) Math.min(SavedFile.CHUNK_SIZE,
                        file.getFileSize() - offset);


                byte[] chunk = new byte[chunkSize];
                FileInputStream in = null;

                try {
                    in = new FileInputStream(file.getFilePath());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                try {
                    in.skip(offset);

                    in.read(chunk, 0, chunkSize);

                    //Log.log("Lenght chunk" + chunkSize);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return chunk;
            } else {
                return null;
            }
        } else {
            File newchunkfile = new File(fileID + "/" + String.valueOf(chunkNo));
            FileInputStream in = null;
            try {
                in = new FileInputStream(newchunkfile);
                byte[] buffer = new byte[(int) newchunkfile.length()];
                int i = in.read(buffer);

                in.close();
                return buffer;
            } catch (FileNotFoundException e) {
                return null;
            } catch (IOException e) {
                return null;
            }
        }
    }

    public boolean saveToFile(byte[] data) {
        FileOutputStream out = null;
        try {
            File f = new File(fileID + "/" + chunkNo);
            f.getParentFile().mkdir();
            out = new FileOutputStream(f);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        try {
            out.write(data);
        } catch (IOException e) {

            e.printStackTrace();
            try {
                out.close();
            } catch (IOException e1) {

                e1.printStackTrace();
            }
            return false;
        }
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public synchronized void incCurrentReplication() {
        ++currentReplicationDegree;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void removeData() {
        File chunk_file = new File(fileID + "/" + chunkNo);
        chunk_file.delete();
    }

    //Encrypt
    public byte[] encrypt(byte[] datatoencrypt) {

        byte[] key = "sdisssssssssssss".getBytes();
        byte[] encryptedData = new byte[0];
        Cipher c = null;

        try {
            c = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec k = new SecretKeySpec(key, "AES");
            c.init(Cipher.ENCRYPT_MODE, k);
            encryptedData = c.doFinal(datatoencrypt);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return encryptedData;
    }

    // Decrypt
    public byte[] decrypt(byte[] encryptedData) {

        byte[] iv = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        IvParameterSpec ivspec = new IvParameterSpec(iv);
        byte[] key = "sdisssssssssssss".getBytes();
        byte[] data = new byte[encryptedData.length];
        Cipher c = null;

        byte[] asB64 = new byte[0];
        try {
            c = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec k = new SecretKeySpec(key, "AES");
            c.init(Cipher.DECRYPT_MODE, k, ivspec);
            data = c.doFinal(encryptedData);
            asB64 = Base64.getEncoder().encodeToString(data).getBytes("utf-8");


        } catch (Exception e) {
            e.printStackTrace();
        }


        return asB64;

    }

    public synchronized void decCurrentReplicationDegree() {
        --currentReplicationDegree;
    }
}
