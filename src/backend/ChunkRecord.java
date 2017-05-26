package backend;

public class ChunkRecord {
	String fileId;
	int chunkNo;
	boolean isServed;

	public ChunkRecord(String fileId, int chunkNo) {
		this.fileId = fileId;
		this.chunkNo = chunkNo;
		this.isServed = false;
	}

    public void setServed(boolean b) {
		isServed=b;
    }
}