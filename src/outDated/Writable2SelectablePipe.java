package outDated;

import java.nio.channels.SelectableChannel;
import java.nio.channels.WritableByteChannel;

@Deprecated
public interface Writable2SelectablePipe {

	public WritableByteChannel getSink ();
	public SelectableChannel getSource();
	public void start();
	public void stop();
}
