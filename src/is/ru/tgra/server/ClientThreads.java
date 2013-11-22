package is.ru.tgra.server;

import java.util.LinkedList;
import java.util.List;

/******************************************************************************
 * ClientThreads.java
 * 
 * This is a class that keeps track of all the connected client threads and has
 * functions to broadcast to them.
 *****************************************************************************/


public class ClientThreads {
	private static ClientThreads instance;
	List<ClientThread> clientThreadsList;
	
	// Constructor
	private ClientThreads(){
		this.clientThreadsList = new LinkedList<ClientThread>();
	}
	
	public static ClientThreads instance() {
		if(instance == null)
			instance = new ClientThreads();
		return instance;
	}
	
	public void add(ClientThread t) {
		this.clientThreadsList.add(t);
	}
	
	public void broadcast(ClientThread from, int typeOfPayload, int playerId, int playerId2, String message, int mapX, int mapY, int mapZ, byte mapValue,
						  float playerPosX, float playerPosY, float playerPosZ, float playerDirX, float playerDirY, float playerDirZ) {
		for(ClientThread c : this.clientThreadsList){
			if(c.getPlayerId() == (from.getPlayerId()))
				continue;
			c.sendPayload(typeOfPayload, playerId, playerId2, message, mapX, mapY, mapZ, mapValue,
						  playerPosX, playerPosY, playerPosZ, playerDirX, playerDirY, playerDirZ);
		}
	}
	
	public void broadcastToAll(int typeOfPayload, int playerId, int playerId2, String message, int mapX, int mapY, int mapZ, byte mapValue,
						  float playerPosX, float playerPosY, float playerPosZ, float playerDirX, float playerDirY, float playerDirZ) {
		for(ClientThread c : this.clientThreadsList){
			c.sendPayload(typeOfPayload, playerId, playerId2, message, mapX, mapY, mapZ, mapValue,
						  playerPosX, playerPosY, playerPosZ, playerDirX, playerDirY, playerDirZ);
		}
	}

	public void remove(ClientThread clientThread) {
		this.clientThreadsList.remove(clientThread);
		
		// Notify all connected users that he has left.
		for(ClientThread c : this.clientThreadsList){
			if(c.getPlayerId() == (clientThread.getPlayerId()))
				continue;
			c.sendPayload(20, clientThread.getPlayerId(), 0, null, 0, 0, 0, (byte)0, 0, 0, 0, 0, 0, 0);
		}
	}
}
