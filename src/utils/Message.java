package utils;

import backend.MulticastServer;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * Created by Duarte on 16-Mar-17.
 */
public class Message {

	private String header;
	private byte[] body;
	public Message(byte[] msg) {
		//System.out.println("msg = " + msg.length);
		byte[] headerEnd = null;


		try {
			headerEnd = new String(MulticastServer.CRLF + MulticastServer.CRLF).getBytes(MulticastServer.ASCII_CODE);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		int headerEndIndex = findIndexEndofLine(msg, headerEnd);

		header = new String(Arrays.copyOfRange(msg, 0, headerEndIndex));

		try {
			body = Arrays.copyOfRange(msg, headerEndIndex + 4, msg.length);
		//	System.out.println("body = " + body.length);
		} catch (ArrayIndexOutOfBoundsException e) {
			// there is no body
			body = null;
		}
	}

	public String getHeader() {
		return header;
	}

	public void setHeader(String header) {
		this.header = header;
	}

	public byte[] getBody() {
		return body;
	}

	public void setBody(byte[] body) {
		this.body = body;
	}

	private static int findIndexEndofLine(byte[] original, byte[] separator) {
		for (int i = 0; i < original.length; i++) {
			for (int j = 0; j < separator.length; j++) {
				if (separator[j] != original[i + j]) {
					break;
				} else {
					if (j == separator.length - 1) {
						return i;
					}
				}
			}
		}
		// did not find index
		return -1;
	}
}
