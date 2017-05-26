package protocols;

import backend.Chunk;
import backend.ConfigManager;
import backend.MulticastServer;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;

/**
 * Created by Duarte on 07-Apr-17.
 */
public class SpaceReclaim {

    private static SpaceReclaim sInstance = null;

    public static SpaceReclaim getInstance() {

        if (sInstance == null) {
            sInstance = new SpaceReclaim();
        }
        return sInstance;
    }

    private SpaceReclaim() {
    }

    public void reclaim(Chunk chunk) {

        String message = "";

        message += "REMOVED" + " " + "1.0" + " " + ConfigManager.getConfigManager().getMyID() + " " + chunk.getFileID() + " " +
                chunk.getChunkNo() + MulticastServer.CRLF + MulticastServer.CRLF;

        InetAddress mcAddr = ConfigManager.getConfigManager().getMcAddr();
        int mcPort = ConfigManager.getConfigManager().getmMCport();

        MulticastServer sender = new MulticastServer(mcAddr,
                mcPort);

        try {
            sender.sendMessage(message
                    .getBytes(MulticastServer.ASCII_CODE));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
