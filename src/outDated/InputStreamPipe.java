package outDated;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Pipe;
import java.nio.channels.SelectableChannel;
import java.nio.channels.WritableByteChannel;

@Deprecated
public class InputStreamPipe implements Writable2SelectablePipe {

	private Pipe pipe;
	private CopyThread copyThread;

	public InputStreamPipe(InputStream in) throws IOException {
		pipe = Pipe.open();
		copyThread = new CopyThread(in, pipe.sink());
	}

	@Override
	public void start() {
		copyThread.start();
	}

	@Override
	public void stop() {
		copyThread.shutdown();
		System.out.println("stopping pipe");
	}

	@Override
	public WritableByteChannel getSink() {
		return pipe.sink();
	}

	@Override
	public SelectableChannel getSource() {
		try {
			SelectableChannel channel = pipe.source();
			channel.configureBlocking(false);
			return channel;
		} catch (IOException ex) {}
		return null;
	}

}

