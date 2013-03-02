package JCClient;

import general.Messages.Message;
import general.ChannelProcesser;
import general.Messages.MessageType;
import general.timer.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public final class NioClientManager implements JCTimerSupport, JCClientFrameSupport {

	private NioClientOperator clOperator;
	private JCClientFrame frame;
	private JCTimer timer;

	private Pipe input_pipe;
	private BlockingQueue<Message> outQueue;
	private String host;

	private ExecutorService service;
	private int timerConnectEv, timerReadEv;

	public NioClientManager() throws IOException {

		input_pipe = Pipe.open();
		outQueue = new ArrayBlockingQueue<>(1024);

//		host = "25.89.110.78";
		host = "127.0.0.1";
		//host = "25.95.67.46";
		clOperator = new NioClientOperator(host, 4444);
		try {
			SelectableChannel dataChannel = input_pipe.source();
			dataChannel.configureBlocking(false);
			clOperator.attachManager(dataChannel, outQueue);
		} catch (IOException ex) {}

		service = Executors.newFixedThreadPool(1);
		startOperator();

		timer = new JCTimer(this);
		timerConnectEv = timer.addTask(500, 1000);  // on connect timer
		timerReadEv = timer.addTask(500, 250);   // on read timer
	}

	@Override
	public void sendMessage(String str) {
		Message msg = new Message(MessageType.INPUT_MSG, "", str);
		ChannelProcesser.sendMessageToChannel(input_pipe.sink(), msg);
	}

	public void attachFrame (JCClientFrame frame) throws IOException {
		this.frame = frame;
		frame.attachClManager(this);
	}

	@Override
	public void startOperator() {
		service.submit(clOperator);
	}

	private void checkConnection () {
		boolean connected = clOperator.getConnectionStatus();
		if (frame != null) {
			frame.setConnectedLabel(connected);
		}
	}

	@Override
	public void stopOperator() {
		System.out.println("stopping client");
		clOperator.setStopFlag(true);
	}

	private void processReceivedMsg () {
		List<Message> messages = receiveMsg();
		if (!messages.isEmpty()) {
			for (Message msg : messages) {
				frame.addMsg(msg.getExciter() + ":: " + msg.getMessage());
			}
		}
	}

	//**********************************************************************************************
	private List<Message> receiveMsg () {
		ArrayList<Message> messages = new ArrayList<>();
		try {
			while (!outQueue.isEmpty()) {
				messages.add(outQueue.take());
			}
		}
		catch (InterruptedException ex) {}
		return messages;
	}

	//**********************************************************************************************
	@Override
	public void timerAction(int timerIdx) {
		if (timerIdx == timerConnectEv) {
			checkConnection();
		} else if (timerIdx == timerReadEv) {
			processReceivedMsg();
		}
	}
}