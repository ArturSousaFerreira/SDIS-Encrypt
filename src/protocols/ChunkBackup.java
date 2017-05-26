package protocols;

import backend.Chunk;
import backend.ConfigManager;
import backend.MulticastServer;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;

public class ChunkBackup {

	public static final String PUT_COMMAND = "PUTCHUNK";
	public static final String STORED_COMMAND = "STORED";
	private static final int PUT_TIME_INTERVAL = 1000;
	private static final int MAX_RETRIES = 5;
	private static ChunkBackup sInstance = null;

	public static ChunkBackup getInstance() {

		if (sInstance == null) {
			sInstance = new ChunkBackup();
		}
		return sInstance;
	}

	private ChunkBackup() {
	}

	public boolean putChunk(Chunk chunk) {

		String header = "";

		header += PUT_COMMAND + " " + "1.0"+" "+ ConfigManager.getConfigManager().getMyID()+" " + chunk.getFileID() + " " + chunk.getChunkNo() + " "
				+ chunk.getWantedReplicationDegree()
				+ MulticastServer.CRLF + MulticastServer.CRLF;

		byte[] data = chunk.getData();

		byte[] message = new byte[header.length() + data.length];
		System.out.println("messageCB = " + message.length);

		try {
			System.arraycopy(header.getBytes(MulticastServer.ASCII_CODE), 0, message, 0, header.length());
			System.out.println("message.lengthCB2 = " + message.length);
		} catch (UnsupportedEncodingException e1) {

			e1.printStackTrace();
		}
		System.arraycopy(data, 0, message, header.length(), data.length);
		System.out.println("messagecb3 = " + message.length);

		InetAddress multDBAddr = ConfigManager.getConfigManager().getMdbAddr();
		int multDBPort = ConfigManager.getConfigManager().getmMDBport();

		MulticastServer sender = new MulticastServer(multDBAddr, multDBPort);

		int counter = 0;

		do {
			try {
				sender.sendMessage(message);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			try {
				System.out.println("WAITING : " + PUT_TIME_INTERVAL * (int) Math.pow(2, counter));
				Thread.sleep(PUT_TIME_INTERVAL * (int) Math.pow(2, counter));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			counter++;
			System.out.println("REP DEG: " + chunk.getChunkNo() + " " + chunk.getCurrentReplicationDegree());
		} while (chunk.getWantedReplicationDegree() > chunk.getCurrentReplicationDegree() && counter < MAX_RETRIES);

		if (counter == MAX_RETRIES) {

			System.out.println("Did not reach wanted replication");

			return false;
		} else {

			System.out.println("Sent successfully");
			return true;
		}

	}

	public boolean storeChunk(Chunk chunk, byte[] data) {


		InetAddress multCtrlAddr = ConfigManager.getConfigManager().getMcAddr();
		int multCtrlPort = ConfigManager.getConfigManager().getmMCport();

		MulticastServer sender = new MulticastServer(multCtrlAddr, multCtrlPort);

		// save chunk in file
		chunk.saveToFile(data);

		//chunk.incCurrentReplication();

		// add chunk to database
		ConfigManager.getConfigManager().addSavedChunk(chunk);

		String message = null;

		message = STORED_COMMAND + " " + "1.0" + " " + ConfigManager.getConfigManager().getMyID()+ " "+ chunk.getFileID() + " " + String.valueOf(chunk.getChunkNo())
				+ MulticastServer.CRLF + MulticastServer.CRLF;

		try {
			sender.sendMessage(message.getBytes(MulticastServer.ASCII_CODE));
		} catch (Exception e) {
			System.out.println("FAILS");
			e.printStackTrace();
		}
		System.out.println("Sent STORED command for chunk of file " + chunk.getFileID() + " no "
				+ chunk.getChunkNo() + " with " + data.length + " bytes");
		return true;
	}
}