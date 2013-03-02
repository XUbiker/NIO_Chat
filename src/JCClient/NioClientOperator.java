package JCClient;

import general.Messages.Message;
import general.ChannelProcesser;
import general.Messages.MessageType;
import general.Messages.SysMessage;
import general.timer.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;

public class NioClientOperator implements Runnable, JCTimerSupport {

	private String host;
	private int serverPort;
	private ByteBuffer clientBuf, handlerBuf;
	private Selector selector;
	private SocketChannel clChannel;
	private SelectableChannel inChannel;
	private boolean stopFlag, connection, deepConnection;
	private int maxConnectPack;

	private InetSocketAddress inetSoc;

	private int deepPackCnt;
	private JCTimer connectTimer;
	private int connectTimerIdx;

	private BlockingQueue<Message> outQueue;

	//**********************************************************************************************
	public void setStopFlag (boolean newVal) {
		stopFlag = newVal;
	}

	//**********************************************************************************************
	public NioClientOperator(String host, int serverPort) {
		this.host = host;
		this.serverPort = serverPort;
		clientBuf = ByteBuffer.allocateDirect(ChannelProcesser.getBufferSize());
		handlerBuf = ByteBuffer.allocateDirect(ChannelProcesser.getBufferSize());

		stopFlag = false;
		connection = false;
		deepConnection = false;
		deepPackCnt = 0;
		maxConnectPack = 7;

		inetSoc = new InetSocketAddress(this.host, this.serverPort);
		connectTimer = new JCTimer(this);
		connectTimerIdx = connectTimer.addTask(100, 200);
	}

	//**********************************************************************************************
	public void attachManager(SelectableChannel inChannel, BlockingQueue<Message> outQueue) {
		this.inChannel = inChannel;
		this.outQueue = outQueue;
	}

	//**********************************************************************************************
	private void initializeSelector() {
		try {
			selector = Selector.open();
		} catch (IOException ex) {
			System.err.println("Error opening selector");
			return;
		}
		try {
			inChannel.register(selector, SelectionKey.OP_READ);
		} catch (ClosedChannelException ex) {
			System.err.println("Error registering channels");
		}
		connectSocketChannel();
	}

	//**********************************************************************************************
	private void connectSocketChannel() {
		// previous socketchannel should be closed correctly
		try {
			clChannel = SocketChannel.open();
			clChannel.configureBlocking(false);
			clChannel.connect(inetSoc);
		} catch (IOException ex) {
			System.err.println("Error openning socketChannel");
		}
		try {
			clChannel.register(selector, SelectionKey.OP_CONNECT);
		} catch (ClosedChannelException ex) {
			System.err.println("Error registering channels");
		}
	}

	//**********************************************************************************************
	private void reconnectToServer (SocketChannel lostChannel) {
		try {
			lostChannel.close();
		} catch (IOException ex1) {
			System.err.println("Error closing channel");
		}
		connectSocketChannel();
		connection = false;
	}


	//**********************************************************************************************
	@Override
	public void run() {
		initializeSelector();
		//==========================================================================================
		while (!stopFlag) {
			try {
				if (selector.select() == 0) {
					continue;
				}
			} catch (IOException ex) {
				System.err.println("Error while trying to apply select operation");
			}
			Set<SelectionKey> keys = selector.selectedKeys();
			Iterator<SelectionKey> it = keys.iterator();
			//======================================================================================
			while (it.hasNext() && !stopFlag) {
				SelectionKey key = it.next();
				SelectableChannel activeChannel = key.channel();

				if (activeChannel.equals(inChannel)) { //============================ input activity
					if (connection) {
						if (!processInputActivity((ReadableByteChannel) key.channel())) {
							System.err.println("Error while processing input activity");
						}
					}
				} else if (activeChannel.equals(clChannel)) { //==================== server activity
					SocketChannel channel = (SocketChannel) activeChannel;
					if (key.isValid() && key.isConnectable()) { //....................... connection
						if (channel.isConnectionPending()) {
							try {
								boolean finish = channel.finishConnect();
							} catch (IOException ex) {  // [[ dead connection. start reconnecting ]]
								System.out.println("connection failed. Reconnecting...");
								reconnectToServer(channel);
								break;
							}
						}
						connection = true;
						try {
							System.out.println("Connected to server " + channel.getRemoteAddress());
						} catch (IOException ex) {
							System.err.println("Error getting remote adress");
						}
						key.interestOps(SelectionKey.OP_READ);
					} else if (key.isValid() && key.isReadable()) { //......................... read
						boolean active = recieveMessageFromServer(channel);
						if (!active) {
							System.err.println("Error reading from server");
							reconnectToServer(channel);
							break;
						}
					} else if (key.isValid() && key.isWritable()) { //........................ write
						throw (new UnsupportedOperationException());
					}
				}
			}
			//======================================================================================
			keys.clear();
		}
		//==========================================================================================
	}

	//**********************************************************************************************
	private boolean processInputActivity (ReadableByteChannel channel) {
		if (!connection) {
			return false;
		}
		List<Message> messages = ChannelProcesser.receiveMessagesFromChannel(channel, handlerBuf);
		if (messages == null || messages.isEmpty()) {
			return false;
		}
		for (Message msg : messages) {
			System.out.println(msg.toString());
			ChannelProcesser.sendMessageToChannel(clChannel, msg);
		}
		return true;
	}

	//**********************************************************************************************
	private void updateDeepConnection () {
		String exciterAddr = ChannelProcesser.getLocalAddr(clChannel);
		if (exciterAddr == null) {
			return;
		}
		ChannelProcesser.sendMessageToChannel(clChannel, new Message(MessageType.SYS_MSG, exciterAddr));
		if (deepPackCnt > maxConnectPack) {
			deepConnection = false;
		} else {
			deepPackCnt++;
		}
	}

	//**********************************************************************************************
	private boolean recieveMessageFromServer (SocketChannel channel) {
		if (!connection) {
			return false;
		}
		List<Message> messages = ChannelProcesser.receiveMessagesFromChannel(channel, clientBuf);
		if (messages == null || messages.isEmpty()) {
			return false;
		}
		for (Message msg : messages) {

			if (MessageType.SYS_MSG.equals(msg.getType())) {
				deepPackCnt = 0;
				deepConnection = true;
			} else {
				if (channel.equals(clChannel)) {
					try {
						outQueue.put(msg);
					} catch (InterruptedException ex) {
						return false;
					}
				}
			}
		}
		return true;
	}

	//**********************************************************************************************
	public boolean getConnectionStatus() {
		if (!connection) {
			return false;
		}
		if (!deepConnection) {
			return false;
		}
		return true;
	}

	//**********************************************************************************************
	@Override
	public void timerAction(int timerIdx) {
		if (connectTimerIdx == timerIdx) {
			updateDeepConnection();
		}
	}
}