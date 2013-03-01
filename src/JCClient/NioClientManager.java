package JCClient;

import general.Messages.MessageProcesser;
import general.timer.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.concurrent.*;

public final class NioClientManager implements JCTimerSupport, JCClientFrameSupport {

	private NioClientOperator clOperator;
	private JCClientFrame frame;
	private JCTimer timer;

	private Pipe input_pipe;
	private BlockingQueue<String> outQueue;
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
	public void sendMessage(String msg) {
		ByteBuffer buf = MessageProcesser.String2ByteBuffer(msg);
		if (buf != null) {
			try {
				input_pipe.sink().write(buf);
			} catch (IOException ex) {}
		}
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
		String msg = receiveMsg();
		if (!"".equals(msg)) {
			frame.addMsg(msg);
		}
	}

	//**********************************************************************************************
	private String receiveMsg () {
		try {
			if (!outQueue.isEmpty()) {
				return outQueue.take();
			}
			return "";
			}
		catch (InterruptedException ex) {
			return "";
		}
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