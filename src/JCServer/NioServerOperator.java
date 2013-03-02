package JCServer;

import general.Messages.Message;
import general.ChannelProcesser;
import general.Messages.MessageType;
import general.Messages.SysMessage;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class NioServerOperator implements Runnable {

	private int port;
	private final ByteBuffer buffer = ByteBuffer.allocate(ChannelProcesser.getBufferSize());
	private Map<String, ClientStruct> clientsMap;
	private ServerManager serverManager;
	private boolean stopFlag;

	//**********************************************************************************************
	public NioServerOperator (int port, ServerManager serverManager, Map<String, ClientStruct> clientsMap) {
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
			while (!stopFlag) {										// look for any activities

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
						Message msg = new Message(MessageType.DATA_MSG, "server", "New user " + sChannel.getRemoteAddress());
						sendGlobalMessage(msg);

					} else if (key.isReadable()) {
						//======================================== process existing connection =====
						SocketChannel sChannel = null;
						sChannel = (SocketChannel) key.channel();
						boolean active = processMessageFromClient(sChannel);
						if (!active) {							// dead connection => remove channel from selector
							key.cancel();
							try {
//								clientsMap.values().remove(sChannel);
								System.out.println("User " + sChannel.getRemoteAddress() + " left chat");
								System.out.println("items in collections: " + clientsMap.values().size());
								Message msg = new Message(MessageType.DATA_MSG, "server", "User " + sChannel.getRemoteAddress() + " left chat");
								sendGlobalMessage(msg);
								sChannel.close();
							} catch (IOException ex2) {
								System.out.println("Error: " + ex2);
							}
							System.out.println("Socket channel closed");
						} else {
							ClientStruct currClient = new ClientStruct(sChannel);
							if (!clientsMap.containsKey(currClient.getKey())) {
								System.out.println("Establishing connect with lost client...");
								clientsMap.put(currClient.getKey(), currClient);
								serverManager.addClientsToList(currClient);
							}
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
	private void sendGlobalMessage(Message message) {
		for (ClientStruct client : clientsMap.values()) {
			ChannelProcesser.sendMessageToChannel(client.getChannel(), message);
		}
	}

	//**********************************************************************************************
	private boolean processMessageFromClient(SocketChannel sChannel) {
		List<Message> messages = ChannelProcesser.receiveMessagesFromChannel(sChannel, buffer);
		if (messages == null || messages.isEmpty()) {
			return false;
		}
		String exciterAddr = ChannelProcesser.getRemoteAddr(sChannel);
		if (exciterAddr == null) {
			return false;
		}

		for (Message msg : messages) {
			System.out.println("Got msg: " + msg.getMessage());
			System.out.println(msg.getType());
			if (MessageType.SYS_MSG.equals(msg.getType())) { //---------------------- system message
				System.out.println("connection msg");
				msg.setExciter(exciterAddr);
				ChannelProcesser.sendMessageToChannel(sChannel, msg);
				for (ClientStruct client : clientsMap.values()) { //-- probably it can be refactored
					if (client.getChannel().equals(sChannel)) {
						client.newConnectionMessage();
					}
				}
			} else if (MessageType.DATA_MSG.equals(msg.getType())) { //---------------- data message
				sendGlobalMessage(msg);
			} else if (MessageType.INPUT_MSG.equals(msg.getType())) { //-------------- input message
				msg.setExciter(exciterAddr);
				sendGlobalMessage(msg);
			}
		}
		return true;
	}
}
