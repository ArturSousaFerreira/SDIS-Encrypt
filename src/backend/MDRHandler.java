package backend;

import protocols.ChunkRestore;
import utils.Message;

import java.util.Random;

public class MDRHandler implements Runnable {
    private static final int TIMEOUT = 401;
    private Random random;
    private Message message;

    private static MDRHandler mInstance = null;

    public MDRHandler(Message receivedMessage) {
        message = receivedMessage;
        random = new Random();
    }

    @Override
    public void run() {
        //TODO: TAKE ME OUT LATER JUST FOR TEST
        System.out.println("MDR received a message");

        String[] header_parts = message.getHeader().split(" ");
        String messageType = header_parts[0].trim();

        if (header_parts[1].trim().equals("1.0")) {
            switch (messageType) {
                case "CHUNK":
                    int senderID = Integer.parseInt(header_parts[2].trim());
                    String fileID = header_parts[3].trim();
                    int chunkNR = Integer.parseInt(header_parts[4].trim());

                    MCListener.getInstance().servedChunk(fileID,chunkNR);
                    synchronized (MDRListener.getInstance().subscribedChunks) {
                        for (ChunkRecord record : MDRListener.getInstance().subscribedChunks
                                ) {
                            if (record.fileId.equals(fileID) && record.chunkNo == chunkNR) {
                                //record.setServed(true);
                                ChunkData wantedChunk = new ChunkData(fileID, chunkNR, message.getBody());
                                ChunkRestore.getInstance().addRequestedChunk(wantedChunk);
                                MDRListener.getInstance().subscribedChunks.remove(record);
                                break;
                            }
                        }
                    }
                    break;
                default:
                    System.out.println("Received " + messageType + " message in MDR");
                    break;
            }
        } else System.out.println("MDR: received message from different version");
    }
}