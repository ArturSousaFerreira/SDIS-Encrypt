package backend;

/**
 * Created by Duarte on 27-Mar-17.
 */
public class ChunkData extends Chunk{
    private byte[] data;

    public ChunkData(String fileID, int chunkNR, byte[] data){
        super(fileID,chunkNR,0,data.length);

        this.data = data;
    }
    @Override
    public byte[] getData() {
        return data;
    }


    public void setData(byte[] data) {
        this.data = data;
    }
}
