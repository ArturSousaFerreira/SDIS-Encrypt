package backend;

import utils.Message;
import utils.Packet;

import java.net.InetAddress;
import java.util.ArrayList;

public class MDRListener implements Runnable{
    private static MDRListener mdrListener = null;

    public ArrayList<ChunkRecord> subscribedChunks;

    private MDRListener() {
        subscribedChunks = new ArrayList<ChunkRecord>();
    }

    public static MDRListener getInstance(){
        if (mdrListener==null){
            mdrListener= new MDRListener();
        }
        return mdrListener;
    }

    @Override
    public void run() {
        System.out.println("MDR listener started");
        InetAddress addr = ConfigManager.getConfigManager().getMdrAddr();
        int port = ConfigManager.getConfigManager().getmMDRport();


        MulticastServer receiver = new MulticastServer(addr , port);
        receiver.join();

        try {
            //TODO: get a way to stop this
            while (true){
                final Packet messagePacket = receiver.receiveMessage();

                Message receivedMessage = new Message(messagePacket.getMessage());

                ConfigManager.getConfigManager().getExecutorService().execute(new MDRHandler(receivedMessage));
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public synchronized void subscribeToChunkData(String fileId, long chunkNo) {
        subscribedChunks.add(new ChunkRecord(fileId, (int) chunkNo));
    }

    public synchronized void unsubscribeToChunkData (String fileId, long chunkNo) {

    }

}

