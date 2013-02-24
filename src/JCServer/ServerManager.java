package JCServer;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerManager {

	private int port;
	private Map<String, ClientStruct> clientsMap;
	private JCServerFrame frame;

	public void addClientsToList (ClientStruct client) {
		ArrayList<ClientStruct> list = new ArrayList<>();
		list.add(client);
		frame.addClientsToList(list);
	}
	
	public void addClientsToList (ArrayList<ClientStruct> clients) {
		frame.addClientsToList(clients);
	}

	public void removeClientsFromList (ArrayList<ClientStruct> clients) {
		frame.removeClientsFromList(clients);
	}

	public ServerManager (int port) {
		this.port = port;
		clientsMap = new ConcurrentHashMap<>();
		frame = new JCServerFrame();
		frame.attachServer(this);

		Runnable operator = new NioServerOperator(this.port, this, clientsMap);
		PopulationController popController = new PopulationController(this, clientsMap);

		Thread operatorThread = new Thread(operator);
		operatorThread.start();

		frame.setVisible(true);
	}

	public static void main (String[] args) {
		ServerManager server = new ServerManager(4444);
	}

}
