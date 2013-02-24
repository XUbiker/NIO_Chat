package outDated;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

@Deprecated
public class CopyThread extends Thread {
	private boolean keepRunning;
	private byte[] bytes;
	private ByteBuffer buffer;
	private InputStream input;
	private WritableByteChannel output;

	CopyThread(InputStream input, WritableByteChannel output) {
		keepRunning = true;
		bytes = new byte[128];
		buffer = ByteBuffer.wrap(bytes);
		this.input = input;
		this.output = output;
		this.setDaemon(true);
	}

	public void shutdown() {
		keepRunning = false;
		this.interrupt();
		System.out.println("CopyThread stoped");
	}

	@Override
	public void run() {
		// this could be improved
		try {
			while (keepRunning) {
				int count = input.read(bytes);
				if (count < 0) {
					break;
				}
				buffer.clear().limit(count);
				output.write(buffer);
			}
			output.close();
		} catch (IOException e) {
			System.out.println("Error #1");
		}
	}
}