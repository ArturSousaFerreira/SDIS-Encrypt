package backend;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by Duarte on 19-May-17.
 */
public class FileInfo  implements Serializable{

    private String fileId;
    private int chunkNR;
    private String fileName;
    private long dataAdd;




    public FileInfo(String fileID, String fileName, long dataAdd, int chunkNR){
        this.fileId=fileID;
        this.fileName = fileName;
        this.chunkNR= chunkNR;
        this.dataAdd = dataAdd;
    }

    public FileInfo(SavedFile file) {
        this.fileId=file.getFileId();
        this.fileName = file.getFileName();
        this.chunkNR= file.getChunkList().size();
        this.dataAdd = new Date().getTime();

    }

    public String getFileId() {
        return fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public long getData() {
        return dataAdd;
    }
    public int getChunkNR() {
        return chunkNR;
    }
}
