package backend;

import utils.Packet;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;

public class MulticastServer {

	private static int TTL = 2;

    public static final String ASCII_CODE = "US-ASCII";
	private static int MAX_PACKET_SIZE  = 65000;

	public static final String CRLF = "\r\n";

	// Server Port for Multicast
	public int multicastPort;
	// IP address for Multicast
	public InetAddress multicastAddress;

	// Server Socket for communication
	public MulticastSocket multicastSocket;



	public MulticastServer(InetAddress multicastAddress, int multicastPort){

		this.multicastPort = multicastPort;
		this.multicastAddress = multicastAddress;
		this.multicastSocket = null;
	}

	public void join() {
		try {
			multicastSocket = new MulticastSocket(multicastPort);
			//might solve interface problems
			multicastSocket.setInterface(ConfigManager.getConfigManager().getMyIP());
			multicastSocket.setTimeToLive(TTL);

		} catch (IOException e) {
			System.out.println("Could not create MulticastSocket.");
			e.printStackTrace();
			System.exit(1);
		}

		try {
			multicastSocket.joinGroup(multicastAddress);

		} catch (IOException e) {
			System.out.println("Could not join multicast group.");
			e.printStackTrace();
			System.exit(1);

		}
		
	}
	
	public boolean sendMessage(byte[] messg) {

		try {
			multicastSocket = new MulticastSocket();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		DatagramPacket packet = new DatagramPacket(messg, messg.length,
				multicastAddress, multicastPort);
		
		try {
			multicastSocket.setLoopbackMode(false);
		} catch (SocketException e1) {

			System.out.println("Failed loopback");
			e1.printStackTrace();
		}

		try {
			multicastSocket.send(packet);
		} catch (IOException e) {
			System.err.println("Failed to send packet");
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public Packet receiveMessage() throws Exception {

		if (multicastSocket == null) {
			throw new Exception();
		}

		byte[] buffer = new byte[MAX_PACKET_SIZE];

		DatagramPacket packet = new DatagramPacket(buffer, MAX_PACKET_SIZE);

		try {
			multicastSocket.receive(packet);
		} catch (IOException e) {
			System.err.println("Failed to send packet");
			e.printStackTrace();
			return null;
		}
		
		byte[] message = new byte[packet.getLength()];
		
		System.arraycopy(buffer, 0, message, 0, packet.getLength());


		return new Packet(message,(packet.getAddress()).getHostAddress());
	}

}