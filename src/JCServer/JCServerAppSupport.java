package JCServer;

import java.util.ArrayList;

public interface JCServerAppSupport {
	void attachServer (ServerManager server);
	void updateClientList ();
	void addClientsToList (ArrayList<ClientStruct> clients);
	void removeClientsFromList (ArrayList<ClientStruct> clients);

}
