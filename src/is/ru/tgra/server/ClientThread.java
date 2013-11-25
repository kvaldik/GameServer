package is.ru.tgra.server;

import is.ru.tgra.network.TcpPayload;
import is.ru.tgra.network.TcpPayloadInit;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

/******************************************************************************
 * ClientThread.java
 * 
 * This is a thread to communicate with a connected client. It reads input from
 * the client and takes appropriate actions.
 *****************************************************************************/


public class ClientThread extends Thread {
	// Variables available to all threads
	private Socket sock;
	private Players players;
	private World world;
	
	// The input output streams
	private OutputStream oStream;
	private ObjectOutputStream ooStream;
	private InputStream iStream;
	private ObjectInputStream oiStream;
	
	// The current players id
	private int playerId;
	private String playerNickname;
	private boolean alive;
	
	
	// Constructor
	public ClientThread(Socket newSock, Players newPlayers, World newWorld) {
		this.sock = newSock;
		this.players = newPlayers;
		this.world = newWorld;

		// Create the output stream
		try {
			this.oStream = sock.getOutputStream();
			this.ooStream = new ObjectOutputStream(oStream);
		} catch (IOException e) {
			System.out.printf("Error creating the output stream \n");
			e.printStackTrace();
		}
		
		// Create the input stream
		try {
			this.iStream = sock.getInputStream();
			this.oiStream = new ObjectInputStream(iStream);
		} catch (IOException e) {
			System.out.printf("Error creating the input stream \n");
			e.printStackTrace();
		}
		
		this.alive = true;
	}
	
	// The run functions reads changes from the client and takes appropriate actions
	public void run() {
		try {
			// We add ourself to the client thread collection.
			ClientThreads.instance().add(this);
			
			TcpPayload payload;
			TcpPayloadInit payloadInit;
			
			// First the nick must be allocated
			// Also the position of all players and the map must be sent at first
			while (this.alive) {
				payloadInit = new TcpPayloadInit(0);
				payloadInit = (TcpPayloadInit) this.oiStream.readObject();
				// Get the nick from the client and try to allocate an id
				this.playerNickname = payloadInit.message;
				this.playerId = this.players.newPlayer(this.playerNickname);
				// Send the id, map and other connected players
				payloadInit = new TcpPayloadInit(10);
				payloadInit.playerId = this.playerId;
				payloadInit.setMap(world, 50);
				payloadInit.setPlayers(players);
				this.ooStream.writeObject(payloadInit);
				// If the nickname was valid other players must be notified about the new player
				if (this.playerId != -1) {
					System.out.printf("Player nr. %d, %s connected \n", this.playerId, this.playerNickname);
					ClientThreads.instance().broadcast(this, 10, this.playerId, 0, this.playerNickname, 0, 0, 0, (byte)0, 0, 0, 0, 0, 0, 0);
					// Done here continue to regular updates
					break;
				}
			}
			
			while (this.alive) {
				payload = new TcpPayload(0);
				payload = (TcpPayload) this.oiStream.readObject();
				
				// Take appropriate actions depending on the TcpPayload type
				switch (payload.typeOfPayload) {
				case 30: // Player position update
					// Update the server data structure
					this.players.UpdatePlayer(payload.playerId, payload.playerPosX, payload.playerPosY, payload.playerPosZ,
											  payload.playerDirX, payload.playerDirY, payload.playerDirZ);
					// Send this update to all other clients
					ClientThreads.instance().broadcast(this, 30, payload.playerId, 0, null, 0, 0, 0, (byte)0, payload.playerPosX, payload.playerPosY, payload.playerPosZ,
													   payload.playerDirX, payload.playerDirY, payload.playerDirZ);
					break;
				case 40: // Map update (changed block)
					// Update the server data structure
					this.world.updateMap(payload.mapX, payload.mapY, payload.mapZ, payload.mapValue);
					// Send this update to all other clients
					ClientThreads.instance().broadcast(this, 40, payload.playerId, 0, null, payload.mapX, payload.mapY, payload.mapZ, payload.mapValue, 0, 0, 0, 0, 0, 0);
					break;
				case 50: // A bullet was fired from this player
					int hitPlayerId = this.checkBullet(payload.playerPosX, payload.playerPosY, payload.playerPosZ,
													   payload.playerDirX, payload.playerDirY, payload.playerDirZ);
					// If a player was hit, that players id is returned, else -1 is returned
					if (hitPlayerId != -1) {
						// Update the players health
						boolean playerAlive = this.players.takeAHit(hitPlayerId, 25);
						// If the player dies from the hit
						if (!playerAlive) {
							// Tell every client that a player died (update kill score)
							ClientThreads.instance().broadcastToAll(70, payload.playerId, hitPlayerId, null, 0, 0, 0, (byte)0, 0, 0, 0, 0, 0, 0);
							// Print a status message
							System.out.printf("Player nr. %d, %s killed player nr. %d, %s \n", this.playerId, this.playerNickname, hitPlayerId, this.players.getPlayerNick(hitPlayerId));
							this.players.addKill(this.playerId);
							this.players.addDeath(hitPlayerId);
						}
						else {
							// Tell all other clients that a player was hit (although it only affects the one who got hit)
							ClientThreads.instance().broadcast(this, 60, payload.playerId, hitPlayerId, null, 0, 0, 0, (byte)0, 0, 0, 0, 0, 0, 0);
						}
					}
					break;
				case 80: // Chat message
					System.out.printf("Player nr. %d, %s Said: %s \n", this.playerId, this.playerNickname, payload.message);
					// Send this chat message to all other clients
					ClientThreads.instance().broadcast(this, 80, payload.playerId, 0, payload.message, 0, 0, 0, (byte)0, 0, 0, 0, 0, 0, 0);
					break;
				default:
					break;
				}
			}
		}
		catch (ClassNotFoundException e) {
			System.out.printf("ClassNotFound ClientThread, run() \n");
			e.printStackTrace();
			this.dispose();
		}
		catch (SocketException e) {
			this.dispose();
		}
		catch (IOException e) {
			//System.out.printf("IOException ClientThread, run() \n");
			//e.printStackTrace();
			this.dispose();
		}
	}
	
	// Check if a bullet hits someone
	// Returns the player id if it hit someone, else -1
	private int checkBullet(float posX, float posY, float posZ, float dirX, float dirY, float dirZ) {
		float totalDistance = 0;
		float length = (float)Math.sqrt(dirX*dirX+dirY*dirY+dirZ*dirZ);
		int playerId;
		boolean block;
		while (totalDistance < 90.0f) {
			playerId = this.players.checkBullet(posX, posY, posZ);
			block = this.world.checkBlock(posX, posY, posZ);
			// Check if the bullet hit a block
			if (block)
				return -1;
			// Check if the bullet hit a player
			if (playerId != -1 && this.playerId != playerId)
				return playerId;
			posX += dirX;
			posY += dirY;
			posZ += dirZ;
			totalDistance += length;
		}
		return -1;
	}
	
	// Send a TcpPayload to the client
	public synchronized void sendPayload(int typeOfPayload, int playerId, int playerId2, String message, int mapX, int mapY, int mapZ, byte mapValue,
							float playerPosX, float playerPosY, float playerPosZ, float playerDirX, float playerDirY, float playerDirZ) {
		TcpPayload payload = new TcpPayload(typeOfPayload);
		payload.playerId = playerId;
		payload.playerId2 = playerId2;
		payload.message = message;
		payload.mapX = mapX;
		payload.mapY = mapY;
		payload.mapZ = mapZ;
		payload.mapValue = mapValue;
		payload.playerPosX = playerPosX;
		payload.playerPosY = playerPosY;
		payload.playerPosZ = playerPosZ;
		payload.playerDirX = playerDirX;
		payload.playerDirY = playerDirY;
		payload.playerDirZ = playerDirZ;
		try {
			this.ooStream.writeObject(payload);
	    	this.ooStream.flush();
		} catch (IOException e) {
			System.out.printf("IOException ClientThread, SendPayload() \n");
			this.dispose();
			e.printStackTrace();
		}
	}
	
	public void dispose() {
		System.out.printf("Player nr. %d, %s disconected \n", this.playerId, this.playerNickname);
		ClientThreads.instance().remove(this);
		if (this.playerId != -1) {
			this.players.UpdatePlayer(this.playerId, 0, 0, 0, 0, 0, 0);
			this.players.setNickname(this.playerId, null);
			this.players.setKills(this.playerId, 0);
			this.players.setDeaths(this.playerId, 0);
			this.alive = false;
		}
	}

	/*
	 * Get and set
	 */
	public String getNickname() {
		return this.playerNickname;
	}
	public int getPlayerId() {
		return this.playerId;
	}
}