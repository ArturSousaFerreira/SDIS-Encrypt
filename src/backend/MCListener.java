package backend;

import utils.Message;
import utils.Packet;

import java.net.InetAddress;
import java.util.ArrayList;

/**
 * Created by Duarte on 16-Mar-17.
 */
public class MCListener implements Runnable{


    public ArrayList<Chunk> pendingChunks;
    public ArrayList<ChunkRecord> watchedChunk;

    private static MCListener mcListener = null;


    private MCListener() {

        pendingChunks = new ArrayList<Chunk>();
        watchedChunk = new ArrayList<ChunkRecord>();
    }

    public static MCListener getInstance(){
        if (mcListener==null){
            mcListener= new MCListener();
        }
        return mcListener;
    }

    @Override
    public void run() {
        System.out.println("MMC listener started");
        InetAddress addr = ConfigManager.getConfigManager().getMcAddr();
        int port = ConfigManager.getConfigManager().getmMCport();


        MulticastServer receiver = new MulticastServer(addr , port);
        receiver.join();

        try {
            //TODO: get a way to stop this
            while (true){
                final Packet messagePacket = receiver.receiveMessage();

                Message receivedMessage = new Message(messagePacket.getMessage());

                ConfigManager.getConfigManager().getExecutorService().execute(new MCHandler(receivedMessage,messagePacket.getIp()));
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public synchronized void servedChunk (String fileID , int chunkNR){
        for (ChunkRecord record:watchedChunk
                ) {
            if (record.fileId.equals(fileID) && record.chunkNo == chunkNR){
                record.isServed = true;
            }
        }
    }
}
