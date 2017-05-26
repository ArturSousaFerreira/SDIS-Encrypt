package backend;

import utils.Message;
import utils.Packet;

import java.net.InetAddress;

/**
 * Created by Duarte on 16-Mar-17.
 */
public class MDBListener implements Runnable{
    private static MDBListener mdbListener = null;


    private MDBListener() {
    }

    public static MDBListener getInstance(){
        if (mdbListener==null){
            mdbListener= new MDBListener();
        }
        return mdbListener;
    }

    @Override
    public void run() {
        System.out.println("MDB listener started");
        InetAddress addr = ConfigManager.getConfigManager().getMdbAddr();
        int port = ConfigManager.getConfigManager().getmMDBport();


        MulticastServer receiver = new MulticastServer(addr , port);
        receiver.join();


        try {
            //TODO: get a way to stop this
            while (true){
                final Packet messagePacket = receiver.receiveMessage();

                Message receivedMessage = new Message(messagePacket.getMessage());

                ConfigManager.getConfigManager().getExecutorService().execute(new MDBHandler(receivedMessage));
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}
