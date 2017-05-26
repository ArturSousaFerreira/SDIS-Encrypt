package backend;

import protocols.ChunkBackup;
import utils.Message;

import java.util.Random;

/**
 * Created by Duarte on 16-Mar-17.
 */
public class MDBHandler implements Runnable {
    private static final int TIMEOUT = 401;
    private Random random;
    private Message message;

    private static final int MAX_WAIT_TIME = 401;

    private static MDBHandler mInstance = null;

    public MDBHandler(Message receivedMessage) {
        message = receivedMessage;
        random = new Random();
    }

    @Override
    public void run() {
        //TODO: TAKE ME OUT LATER JUST FOR TEST

        String[] header_parts = message.getHeader().split(" ");
        String messageType = header_parts[0].trim();

        if (header_parts[1].trim().equals("1.0")) {
            System.out.println("MDB received a " + messageType);
            switch (messageType) {
                case "PUTCHUNK":
                    //putchunk


                    if (message.getBody().length + ConfigManager.getConfigManager().getUsedSpace() <= ConfigManager.getConfigManager().getMaxSpace()) {
                        final int senderID = Integer.parseInt(header_parts[2].trim());

                        if (senderID == ConfigManager.getConfigManager().getMyID()) {
                            return;
                        }

                        final String fileID = header_parts[3].trim();
                        final int chunkNo = Integer.parseInt(header_parts[4].trim());
                        int wantedReplication = Integer.parseInt(header_parts[5]
                                .trim());

                        Chunk chunkOBJ = ConfigManager.getConfigManager().getSavedChunk(fileID, chunkNo);

                        if (chunkOBJ == null) {
                            Chunk actualChunk = new Chunk(fileID, chunkNo, wantedReplication, 0);

                            synchronized (MCListener.getInstance().pendingChunks) {
                                MCListener.getInstance().pendingChunks
                                        .add(actualChunk);

                            }
                            try {
                                Thread.sleep(random.nextInt(MAX_WAIT_TIME));
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            //if the chunk didnt get enough stored, then im storing it


                            if (actualChunk.getCurrentReplicationDegree() < actualChunk
                                    .getWantedReplicationDegree()) {
                                System.out.println("saving " + actualChunk.getChunkNo() + " chunk with current " + actualChunk.getCurrentReplicationDegree());
                                actualChunk.setSize(message.getBody().length);
                                ChunkBackup.getInstance().storeChunk(actualChunk,
                                        message.getBody());
                            }
                            synchronized (MCListener.getInstance().pendingChunks) {
                                MCListener.getInstance().pendingChunks
                                        .remove(actualChunk);
                            }
                        }
                    } else {
                        System.out.println(("body " + message.getBody().length + " used " + ConfigManager.getConfigManager().getUsedSpace() + " max " + ConfigManager.getConfigManager().getMaxSpace()));
                        System.out.println("Chunk would exceed my space");
                    }

                    break;
                default:
                    System.out.println("Received " + messageType + " message in MDB");
                    break;
            }
        } else System.out.println("MDB: message with different version");
    }
}
