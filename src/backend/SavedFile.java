package backend;

import java.io.File;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Duarte on 18-Mar-17.
 */
public class SavedFile implements Serializable {

	public static final long CHUNK_SIZE = 64000;
	public static final long MAX_CHUNK = 1000000;
	public static final long MAX_FILE = CHUNK_SIZE * (MAX_CHUNK - 1);



	private String filePath;
	private String fileId;
	private ArrayList<Chunk> chunkList;
	private long chunkCounter;

	private int wantedReplicationDegree;
	private String fileName;

	public SavedFile(String filePath, int desiredReplicationDegree){
		this.filePath = new File(filePath).getAbsolutePath();
		this.fileName = new File(filePath).getName();
		wantedReplicationDegree = desiredReplicationDegree;

		fileId = generateID(new File(filePath));
		chunkCounter = 0;
		chunkList = new ArrayList<Chunk>();

		generateChunks();
	}

	private void generateChunks() {
		long fileSize = getFileSize();

		for (int i = 0; i < fileSize; i += CHUNK_SIZE) {
			chunkList.add(new Chunk(this, chunkCounter++));
		}

		// If the file size is a multiple of the chunk size, the last chunk has
		// size 0

		if (fileSize % CHUNK_SIZE == 0) {
			chunkList.add(new Chunk(this, chunkCounter++));
		}

	}

	private String generateID(File file) {
		// hash is generated from file name + file data + file size

		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			System.exit(1);
		}

		String file_id = file.getName();
		System.out.println("file.getName() = " + file.getName());
		file_id += file.lastModified();

		file_id += file.length();

		md.update(file_id.getBytes());


		byte[] result = md.digest();


		return bytesToHex(result).toString();
	}

	private static String bytesToHex(byte[] in) {
		final StringBuilder builder = new StringBuilder();
		for(byte b : in) {
			builder.append(String.format("%02x", b));
		}
		return builder.toString();
	}


	public String getFileId() {
		return fileId;
	}

	public long getFileSize() {
		File f = new File(getFilePath());
		return f.length();
	}

	public int getWantedReplicationDegree() {
		return wantedReplicationDegree;
	}

	public String getFileName() {
		return fileName;
	}

	public class FileDoesNotExistsException extends Exception {

	}

	public class FileTooLargeException extends Exception {

	}



	public String getFilePath() {
		return filePath;
	}

	public void showFileChunks() {
		if (chunkCounter > 0 && chunkList.size() > 0) {
			System.out.println();
			for (int i = 0; i < chunkList.size(); i++)
				System.out.println("Chunk ID: " + chunkList.get(i).getFileID() + " Chunk No: " + chunkList.get(i).getChunkNo()
						+ " Replication Degree: " + chunkList.get(i).getCurrentReplicationDegree());
		}
	}
	public ArrayList<Chunk> getChunkList() {
		return chunkList;
	}


	public boolean exists() {
		return new File(filePath).exists();
	}


	public synchronized void incChunkReplication(int chunkNo) throws ConfigManager.InvalidChunkException {
		Chunk chunk = null;

		try {
			chunk = chunkList.get(chunkNo);
		} catch (IndexOutOfBoundsException e) {
			throw new ConfigManager.InvalidChunkException();
		}
		chunk.incCurrentReplication();
	}
}
