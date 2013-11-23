package is.ru.tgra.server;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/******************************************************************************
 * Players.java
 * 
 * This class stores information about all connected players.
 *****************************************************************************/


public class Players {
	// Information about connected players
	private float[] playersPosX;
	private float[] playersPosY;
	private float[] playersPosZ;
	private float[] playersDirX;
	private float[] playersDirY;
	private float[] playersDirZ;
	private String[] playersNicks;
	private int[] playersDeaths;
	private int[] playersKills;
	private int[] playersHealth;
	private int nrPlayers;
	
	// Locks for different threads
	private final ReentrantReadWriteLock fLock = new ReentrantReadWriteLock();
	private final Lock fReadLock = fLock.readLock();
	private final Lock fWriteLock = fLock.writeLock();
	
	
	// Constructor
	public Players(int newNrPlayers) {
		this.playersPosX = new float[newNrPlayers];
		this.playersPosY = new float[newNrPlayers];
		this.playersPosZ = new float[newNrPlayers];
		this.playersDirX = new float[newNrPlayers];
		this.playersDirY = new float[newNrPlayers];
		this.playersDirZ = new float[newNrPlayers];
		this.playersNicks = new String[newNrPlayers];
		this.playersKills = new int[newNrPlayers];
		this.playersDeaths = new int[newNrPlayers];
		this.playersHealth = new int[newNrPlayers];
		this.nrPlayers = newNrPlayers;
	}
	
	// Try to allocate space for a new player, -1 is returned if no space or nick in use
	public int newPlayer(String newNickname) {
		this.fWriteLock.lock();
		try {
			// Check if nick is in use
			for (int i = 0; i < this.nrPlayers; i++) {
				if (this.playersNicks[i] != null) {
					if (newNickname.equals(this.playersNicks[i])) {
						return -1;
					}
				}
			}
			// Find available id
			for (int i = 0; i < this.nrPlayers; i++) {
				if (this.playersPosY[i] == 0) {
					this.playersPosY[i] = 1;
					this.playersNicks[i] = newNickname;
					this.playersHealth[i] = 100;
					return i;
				}
			}
			return -1;
		} finally {
			this.fWriteLock.unlock();
		}
	}
	
	// Update the position of a player
	public void UpdatePlayer(int playerId, float posX, float posY, float posZ, float dirX, float dirY, float dirZ) {
		this.fWriteLock.lock();
		try {
			this.playersPosX[playerId] = posX;
			this.playersPosY[playerId] = posY;
			this.playersPosZ[playerId] = posZ;
			this.playersDirX[playerId] = dirX;
			this.playersDirY[playerId] = dirY;
			this.playersDirZ[playerId] = dirZ;
		} finally {
			this.fWriteLock.unlock();
		}
	}
	
	// The takeAHit functions returns true if the player can take the hit, false if it kills him
	public boolean takeAHit(int playerId, int hitPoints) {
		this.fWriteLock.lock();
		try {
			this.playersHealth[playerId] -= hitPoints;
			// Check if the hit is fatal
			if (this.playersHealth[playerId] <= 0) {
				this.playersHealth[playerId] = 100;
				return false;
			}
			else
				return true;
		} finally {
			this.fWriteLock.unlock();
		}
	}
	
	// Check if a bullet hits a player
	public int checkBullet(float posX, float posY, float posZ) {
		this.fReadLock.lock();
		try {
			for (int i = 0; i < this.nrPlayers; i++) {
				// Check if the x coordinate matches
				if (posX < this.playersPosX[i]+1.3f && posX > this.playersPosX[i]-1.3f)
					// Check if the y coordinate matches
					if (posY < this.playersPosY[i] && posY > this.playersPosY[i]-6.0f)
						// Check if the z coordinate matches
						if (posZ < this.playersPosZ[i]+1.3f && posZ > this.playersPosZ[i]-1.3f)
							return i;
			}
			return -1;
		} finally {
			this.fReadLock.unlock();
		}
	}
	
	// The addKill functions adds a kill score the given player id
	public void addKill(int playerId) {
		this.fWriteLock.lock();
		try {
			this.playersKills[playerId]++;
		} finally {
			this.fWriteLock.unlock();
		}
	}
	
	// The addDeath functions adds a death score the given player id
	public void addDeath(int playerId) {
		this.fWriteLock.lock();
		try {
			this.playersDeaths[playerId]++;
		} finally {
			this.fWriteLock.unlock();
		}
	}
	
	/*
	 * Get and set
	 */
	public void getPlayers(float[] newPlayersPosX, float[] newPlayersPosY, float[] newPlayersPosZ, float[] newPlayersDirX, float[] newPlayersDirY, float[] newPlayersDirZ,
						   String[] newPlayersNicks, int[] newPlayersKills, int[] newPlayersDeaths) {
		this.fReadLock.lock();
		try {
			for (int i = 0; i < this.nrPlayers; i++) {
				newPlayersPosX[i] = this.playersPosX[i];
				newPlayersPosY[i] = this.playersPosY[i];
				newPlayersPosZ[i] = this.playersPosZ[i];
				newPlayersDirX[i] = this.playersDirX[i];
				newPlayersDirY[i] = this.playersDirY[i];
				newPlayersDirZ[i] = this.playersDirZ[i];
				newPlayersNicks[i] = this.playersNicks[i];
				newPlayersKills[i] = this.playersKills[i];
				newPlayersDeaths[i] = this.playersDeaths[i];
				newPlayersKills[i] = this.playersKills[i];
			}
		} finally {
			this.fReadLock.unlock();
		}
	}
	
	public int getNrPlayers() {
		this.fReadLock.lock();
		try {
			return this.nrPlayers;
		} finally {
			this.fReadLock.unlock();
		}
	}
	
	public void setKills(int newPlayerId, int newValue) {
		this.fWriteLock.lock();
		try {
			this.playersKills[newPlayerId] = newValue;
		} finally {
			this.fWriteLock.unlock();
		}
	}
	
	public void setDeaths(int newPlayerId, int newValue) {
		this.fWriteLock.lock();
		try {
			this.playersDeaths[newPlayerId] = newValue;
		} finally {
			this.fWriteLock.unlock();
		}
	}
	
	public void setNickname(int newPlayerId, String newNickname) {
		this.fWriteLock.lock();
		try {
			this.playersNicks[newPlayerId] = newNickname;
		} finally {
			this.fWriteLock.unlock();
		}
	}
	
	public String getPlayerNick(int playerId) {
		this.fReadLock.lock();
		try {
			return this.playersNicks[playerId];
		} finally {
			this.fReadLock.unlock();
		}
	}
}