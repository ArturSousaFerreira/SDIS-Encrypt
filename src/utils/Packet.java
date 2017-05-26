package utils;

/**
 * Created by Duarte on 27-Mar-17.
 */
public class Packet {

    private byte[] message;
    private String ip;

    public Packet(byte[] info, String ip){
        this.message=info;
        this.ip=ip;
    }

    public byte[] getMessage() {
        return message;
    }

    public String getIp() {
        return ip;
    }

}
