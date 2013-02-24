package JCClient;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientApp {
	public static void main (String[] args) {
		try {
			NioClientManager client = new NioClientManager();
			JCClientFrame frame = new JCClientFrame();
			client.attachFrame(frame);
			frame.setVisible(true);
		} catch (IOException ex) {
			Logger.getLogger(ClientApp.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
