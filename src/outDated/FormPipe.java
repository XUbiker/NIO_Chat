package outDated;

import java.io.IOException;
import java.nio.channels.Pipe;
import java.nio.channels.SelectableChannel;
import java.nio.channels.WritableByteChannel;

@Deprecated
public class FormPipe implements Writable2SelectablePipe {

	private Pipe pipe;

	public FormPipe() throws IOException {
		pipe = Pipe.open();
	}

	@Override
	public WritableByteChannel getSink () {
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

	@Override
	public void stop() {}

	@Override
	public void start() {}

}