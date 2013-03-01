package JCServer;

import general.timer.JCTimer;
import general.timer.JCTimerSupport;
import java.util.ArrayList;
import java.util.Map;

public class PopulationController implements JCTimerSupport {

	private Map<String, ClientStruct> clientsMap;
	private ArrayList<ClientStruct> clientsToBeDeleted;
	private ServerManager serverManager;

	//**********************************************************************************************
	public PopulationController (ServerManager serverManager, Map<String, ClientStruct> clientsMap) {
		this.serverManager = serverManager;
		this.clientsMap = clientsMap;
		clientsToBeDeleted = new ArrayList<>();
		JCTimer timer = new JCTimer(this);
		timer.addTask(500, 2000);
	}

	//**********************************************************************************************
	@Override
	public void timerAction(int timerIdx) {
		System.out.println("Population controller invoked");
		if (clientsMap.isEmpty()) {
			return;
		}
		clientsToBeDeleted.clear();
		for (ClientStruct client : clientsMap.values()) {
			client.timerConnectionCheck();
			boolean connected = client.getConnectionStatus();
			if (!connected) {
				System.out.println("going to remove:" + client.getKey());
				clientsToBeDeleted.add(client);
			}
		}
		for (int i = 0; i < clientsToBeDeleted.size(); i++) {
			System.out.println("removing client");
			clientsMap.remove(clientsToBeDeleted.get(i).getKey());
		}
		System.out.println("items in collection: " + clientsMap.size());
		serverManager.removeClientsFromList(new ArrayList<>(clientsToBeDeleted));
		clientsToBeDeleted.clear();
	}

}
