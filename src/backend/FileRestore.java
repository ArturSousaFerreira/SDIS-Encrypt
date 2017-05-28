package backend;

import protocols.ChunkRestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Duarte on 27-Mar-17.
 */
public class FileRestore {

    private static FileRestore fInstance = null;

    private FileRestore() {
    }

    public static FileRestore getInstance() {
        if (fInstance == null) {
            fInstance = new FileRestore();
        }
        return fInstance;
    }

    public boolean restoreFile (SavedFile file){
        ArrayList<ChunkData> receivedChunks = new ArrayList<ChunkData>();

        for (Chunk chunk:file.getChunkList()) {
            ChunkData chunkData = ChunkRestore.getInstance().requestChunk(chunk.getFileID(),chunk.getChunkNo());
            if(chunkData != null){
                receivedChunks.add(chunkData);
            }
        }
        if (receivedChunks.size() == file.getChunkList().size()){
            System.out.println("GOT ALL THE CHUNKS, REBUILDING");
            return rebuildFile(file.getFilePath(),receivedChunks);
        }
        else return false;
    }

    private boolean rebuildFile(String path, ArrayList<ChunkData> chunks) {
        File directory = new File("restore");
        if (! directory.exists()){
            directory.mkdir();
        }

        for (ChunkData chunk:chunks
                ) {
            if (!writeToFile(chunk.getData(), "restore\\"+path)){
                return false;
            }
        }
        System.out.println("REBUILD DONE");
        return true;
    }

    private boolean writeToFile(byte[] data, String filePath) {

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(new File(filePath), true);

            out.write(data);
            out.close();
            return true;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean restoreFile(FileInfo fileToRestore) {
        ArrayList<ChunkData> receivedChunks = new ArrayList<ChunkData>();

        for (int i = 0; i<fileToRestore.getChunkNR();i++) {
            ChunkData chunkData = ChunkRestore.getInstance().requestChunk(fileToRestore.getFileId(),i);
            if(chunkData != null){
                //TODo: passar chunk data pelo decrypt
                chunkData.setData(chunkData.decrypt(chunkData.getData()));
                receivedChunks.add(chunkData);
            }
        }
        if (receivedChunks.size() == fileToRestore.getChunkNR()){

            System.out.println("GOT ALL THE CHUNKS, REBUILDING");
            return rebuildFile(fileToRestore.getFileName(),receivedChunks);
        }
        else return false;
    }
}
