package JCServer;

import general.MessageTransformer;
import general.SysMessage;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class NioServerOperator implements Runnable {

	private int port;
	private final ByteBuffer buffer = ByteBuffer.allocate(16384);   // buffer for encrypting data
	private Map<String, ClientStruct> clientsMap;
	private ServerManager serverManager;
	private boolean stopFlag;

	//**********************************************************************************************
	public NioServerOperator(int port, ServerManager serverManager, Map<String, ClientStruct> clientsMap) {
		this.port = port;
		this.serverManager = serverManager;
		this.clientsMap = clientsMap;
		stopFlag = false;
	}

	//**********************************************************************************************
	@Override
	public void run() {
		System.out.println("Starting server operator...");
		try {
			ServerSocketChannel ssCh = ServerSocketChannel.open();	// create a ServerSocketChannel
			ssCh.configureBlocking(false);							// make it non-blocking
			ssCh.bind(new InetSocketAddress(port));					// bind it to listening port

			Selector selector = Selector.open();					// create Selector
			ssCh.register(selector, SelectionKey.OP_ACCEPT);		// register ServerSocketChannel
			System.out.println("Listening on port " + port);

			//--------------------------------------------------------------------------------------
			while (!stopFlag) {											// look for any activities

				if (selector.select() == 0) {
					continue;
				}
				Set<SelectionKey> keys = selector.selectedKeys();	// get keys of all activities
				Iterator<SelectionKey> it = keys.iterator();

				//----------------------------------------------------------------------------------
				while (it.hasNext()) {
					SelectionKey key = it.next();

					if (key.isAcceptable()) {
						//=========================================== establish new connection =====
						SocketChannel sChannel = ssCh.accept();
						System.out.println("Connection with" + sChannel.getRemoteAddress());
						ClientStruct newClient = new ClientStruct(sChannel);
						clientsMap.put(newClient.getKey(), newClient);
						serverManager.addClientsToList(newClient);
						System.out.println("items in collections: " + clientsMap.values().size());
						sChannel.configureBlocking(false);
						sChannel.register(selector, SelectionKey.OP_READ);
						sendGlobalServerMessage("New user " + sChannel.getRemoteAddress());

					} else if (key.isReadable()) {
						//======================================== process existing connection =====
						SocketChannel sChannel = null;
						try {
							sChannel = (SocketChannel) key.channel();
							boolean active = receiveMessage(sChannel);
							if (!active) {							// dead connection
								System.out.println("dead connection");
								key.cancel();
							}
							ClientStruct currClient = new ClientStruct(sChannel);
							if (!clientsMap.containsKey(currClient.getKey())) {
								System.out.println("Establishing connect with lost client...");
								clientsMap.put(currClient.getKey(), currClient);
								serverManager.addClientsToList(currClient);
							}
						} catch (IOException ex) {					// remove channel from selector
							key.cancel();
							try {
//								clientsMap.values().remove(sChannel);
								System.out.println("User " + sChannel.getRemoteAddress() + " left chat");
								System.out.println("items in collections: " + clientsMap.values().size());
								sendGlobalServerMessage("User " + sChannel.getRemoteAddress() + " left chat");
								sChannel.close();
							} catch (IOException ex2) {
								System.out.println("Error: " + ex2);
							}
							System.out.println("Socket channel closed");
						}
					}
				} //--------------------------------------------------------------------------------
				keys.clear();										// remove all used keys
			} //------------------------------------------------------------------------------------
		} catch (IOException ex) {
			System.out.println("Error: " + ex);
		}
	}

	//**********************************************************************************************
	private void sendMessage (SocketChannel sChannel, String message) throws IOException {
		ByteBuffer clientBuf = MessageTransformer.String2ByteBuffer(message);
		if (clientBuf != null) {
			sChannel.write(clientBuf);
		}
	}

	//**********************************************************************************************
	private void sendGlobalChatMessage(SocketChannel exciter, String message) throws IOException {
		System.out.println("Global ");
		for (ClientStruct client : clientsMap.values()) {
//			if (!chan.equals(exciter)) {
				sendMessage(client.getChannel(), exciter.getRemoteAddress() + ":: " + message);
//			}
		}
	}

	//**********************************************************************************************
	private void sendGlobalServerMessage(String message) throws IOException {
		for (ClientStruct client : clientsMap.values()) {
			sendMessage(client.getChannel(), "Server:: " + message);
		}
	}

	//**********************************************************************************************
	private boolean receiveMessage (SocketChannel sChannel) throws IOException {
		buffer.clear();
		sChannel.read(buffer);
		buffer.flip();
		if (buffer.limit() == 0) {
			return false;
		}
		String message = MessageTransformer.ByteBuffer2String(buffer);
		buffer.clear();
		if (message != null) {
			System.out.println("Got msg: " + message);
			if (SysMessage.connectMsg.toString().equals(message)) {
				for (ClientStruct client : clientsMap.values()) { //-- probably it can be refactored
					if (client.getChannel().equals(sChannel)) {
						client.newConnectionMessage();
					}
				}
				sendMessage(sChannel, message);
			} else {
				sendGlobalChatMessage(sChannel, message);
			}
			return true;
		}
		return false;
	}
}
