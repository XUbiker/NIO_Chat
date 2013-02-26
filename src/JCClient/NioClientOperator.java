package JCClient;

import general.MessageTransformer;
import general.SysMessage;
import general.timer.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NioClientOperator implements Runnable, JCTimerSupport {

	private String host;
	private int serverPort, bufferSize = 1024;
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

	private BlockingQueue<String> outQueue;

	//**********************************************************************************************
	public void setStopFlag (boolean newVal) {
		stopFlag = newVal;
	}

	//**********************************************************************************************
	public NioClientOperator(String host, int serverPort) {
		this.host = host;
		this.serverPort = serverPort;
		clientBuf = ByteBuffer.allocateDirect(bufferSize);
		handlerBuf = ByteBuffer.allocateDirect(bufferSize);

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
	public void attachManager(SelectableChannel inChannel, BlockingQueue<String> outQueue) {
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
						try {
							String msg = readByteChannel((ReadableByteChannel) key.channel());
							if (msg != null) {
								sendMessage(clChannel, msg);
							}
						} catch (IOException ex) {
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
						try {
							recieveMessage(channel);
						} catch (IOException | InterruptedException ex) {
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
	private String readByteChannel(ReadableByteChannel channel) throws IOException {
		handlerBuf.clear();
		channel.read(handlerBuf);
		handlerBuf.flip();
		if (handlerBuf.limit() == 0) {
			return null;
		}
		String message = MessageTransformer.ByteBuffer2String(handlerBuf);
		handlerBuf.clear();
		return message;
	}

	//**********************************************************************************************
	private void updateDeepConnection(String msg) {
		try {
			sendMessage(clChannel, msg);
		} catch (IOException ex) {}
		if (deepPackCnt > maxConnectPack) {
			deepConnection = false;
		} else {
			deepPackCnt++;
		}
	}

	//**********************************************************************************************
	private void sendMessage(SocketChannel channel, String message) throws IOException {
		if (!connection) {
			return;
		}
		ByteBuffer buf = MessageTransformer.String2ByteBuffer(message);
		if (buf != null) {
			channel.write(buf);
		}
	}

	//**********************************************************************************************
	private void recieveMessage(SocketChannel channel) throws IOException, InterruptedException {
		if (!connection) {
			return;
		}
		clientBuf.clear();
		channel.read(clientBuf);
		clientBuf.flip();
		if (clientBuf.limit() == 0) {
			return;
		}

		String message = MessageTransformer.ByteBuffer2String(clientBuf);
		clientBuf.clear();
		if (message == null) {
			return;
		}
		if (SysMessage.connectMsg.toString().equals(message)) {
			deepPackCnt = 0;
			deepConnection = true;
		} else {
			if (channel.equals(clChannel)) {
				outQueue.put(message);
	//			System.out.println(message);
			}
		}
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
			updateDeepConnection(SysMessage.connectMsg.toString());
//			System.out.println("connected: " + connectedDeep + " " + connectedPackCnt);
		}
	}
}