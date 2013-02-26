package JCServer;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public class ClientStruct {
	private SocketChannel channel;
	private String key;
	private int connectionMsgCnt;

	public ClientStruct (SocketChannel channel) {
		this.channel = channel;
		try {
			key = channel.getRemoteAddress().toString();
		} catch (IOException ex) {
			key = "";
		}
		connectionMsgCnt = 0;

	}

	public String getKey () {
		return key;
	}

	public static String generateKey (SocketChannel channel) {
		String result = "";
		try {
			result = channel.getRemoteAddress().toString();
		} catch (IOException ex) {}
		return result;
		
	}

	public SocketChannel getChannel () {
		return channel;
	}

	public synchronized void newConnectionMessage () {
		connectionMsgCnt = 0;
	}

	public synchronized void timerConnectionCheck () {
		connectionMsgCnt--;
	}

	public synchronized boolean getConnectionStatus () {
		if (connectionMsgCnt < -2) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return key;
	}

}
