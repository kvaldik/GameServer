package is.ru.tgra.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/******************************************************************************
 * World.java
 * 
 * This class stores all the information about the world. It loads the world
 * initially from a file and then saves it every 6 seconds (can be changed).
 *****************************************************************************/


public class World {
	// The map variables
	private byte[][][] map;
	private int mapSize;
	private float mapScale;
	
	// Time for saving the map (the map get saved every X seconds)
	private long startTime;
	private long currentTime;

	// Locks for different threads
	private final ReentrantReadWriteLock fLock = new ReentrantReadWriteLock();
	private final Lock fReadLock = fLock.readLock();
	private final Lock fWriteLock = fLock.writeLock();
	
	
	// Constructor
	public World(int size) {
		this.map = new byte[size][size][size];
		this.mapSize = size;
		this.mapScale = 4.0f;
		
		startTime = System.currentTimeMillis();
		
		for (int x = 0; x < this.mapSize; x++)
			for (int y = 0; y < this.mapSize; y++)
				for (int z = 0; z < this.mapSize; z++)
					this.map[x][y][z] = 0;
		
		// Load a map
		this.loadMap(1);
	}
	
	// Get the current map
	public void getMap(byte[][][] newMap) {
		this.fReadLock.lock();
		try {
			for (int x = 0; x < this.mapSize; x++)
				for (int y = 0; y < this.mapSize; y++)
					for (int z = 0; z < this.mapSize; z++)
						newMap[x][y][z] = this.map[x][y][z];
		} finally {
			this.fReadLock.unlock();
		}
	}
	
	// Update a block in the map and check if it has been saved in the last X seconds
	public void updateMap(int x, int y, int z, byte newValue) {
		this.fWriteLock.lock();
		try {
			// Save the map every 6 seconds
			currentTime = System.currentTimeMillis();
			if (startTime+6000 < currentTime) {
				startTime = currentTime;
				this.saveMap(1);
			}
			// Apply the change
			this.map[x][y][z] = newValue;
		} finally {
			this.fWriteLock.unlock();
		}
	}
	
	// This functions takes in a position and returns true if there is a block there
	// Also returns true if the position is outside of the world
	public boolean checkBlock(float posX, float posY, float posZ) {
		this.fReadLock.lock();
		try {
			int currentBlockX = (int)(posX/this.mapScale);
			int currentBlockY = (int)(posY/this.mapScale);
			int currentBlockZ = (int)(posZ/this.mapScale);
			if (currentBlockX < 0 || currentBlockX == this.mapSize ||
				currentBlockY < 0 || currentBlockY == this.mapSize || posY < 0 ||
				currentBlockZ < 0 || currentBlockZ == this.mapSize)
				return true;
			return (this.map[currentBlockX][currentBlockY][currentBlockZ] != 0);
		} finally {
			this.fReadLock.unlock();
		}
	}
	
	/*
	 * Save and load maps
	 */
	private void loadMap(int number) {
	        // The name of the file to open.
	        String fileName = System.getProperty("user.dir") + "\\assets\\maps\\map" + number + ".txt";
	        String fileName_size = System.getProperty("user.dir") + "\\assets\\maps\\map_size" + number + ".txt";
	        String line;
	        
	        // First read the size of the map
	        try {
	            // FileReader reads text files in the default encoding.
	            FileReader fileReader = new FileReader(fileName_size);

	            // Always wrap FileReader in BufferedReader
	            BufferedReader bufferedReader = new BufferedReader(fileReader);
	            
	            // Read the size
	            line = bufferedReader.readLine();
	            this.mapSize = Integer.parseInt(line);

	            // Always close files.
	            bufferedReader.close();
	        }
	        catch(FileNotFoundException ex) {
	            System.out.println("Unable to open file '" +  fileName + "'");				
	        }
	        catch(IOException ex) {
	            System.out.println("Error reading file '" + fileName + "'");
	        }
	        
	        // Second read the blocks from a different file
	        try {
	            // Buffer for the map
	            byte[] buffer = new byte[this.mapSize*this.mapSize*this.mapSize];
	            
	            // Input stream
	            FileInputStream inputStream = new FileInputStream(fileName);
	            
	            // Read the tiles from the file
	            int total = inputStream.read(buffer);
	            
	            // The new tiles
	            //boolean[][] tiles2 = new boolean[this.tile_width][this.tile_height];
	            
	            // Put read buffer into the map array
	            for (int x = 0; x < this.mapSize; x++) {
	            	for (int y = 0; y < this.mapSize; y++) {
		            	for (int z = 0; z < this.mapSize; z++) {
		            		this.map[x][y][z] = buffer[x*this.mapSize*this.mapSize+y*this.mapSize+z];
		            	}
	            	}
	            }

	            // Close the input stream
	            inputStream.close();		
	            
	            // Print status message
	            System.out.println("Read " + total + " bytes");
	        }
	        catch(FileNotFoundException ex) {
	            System.out.println("Unable to open file '" + fileName + "'");				
	        }
	        catch(IOException ex) {
	            System.out.println("Error reading file '" + fileName + "'");
	        }
	    }
	    
	    private void saveMap(int number) {
	        // The name of the file to open.
	        String fileName = System.getProperty("user.dir") + "\\assets\\maps\\map" + number + ".txt";
	        String fileName_size = System.getProperty("user.dir") + "\\assets\\maps\\map_size" + number + ".txt";
	        String writeBuffer;

	        // First save the size of the map
	        try {
	            // Assume default encoding for writing the size of the map
	            FileWriter fileWriter = new FileWriter(fileName_size);

	            // Always wrap FileWriter in BufferedWriter
	            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
	            
	            // Write the size to the file
	            writeBuffer = "" + this.mapSize;
	            bufferedWriter.write(writeBuffer);
	            bufferedWriter.newLine();
	            
	            // Close the file
	            bufferedWriter.close();
	        }
	        catch(IOException ex) {
	            System.out.println("Error writing to file '" + fileName + "'");
	        }
	        
	        // Second save all the blocks in a different file
	        try {
	            // Output stream
	            FileOutputStream outputStream = new FileOutputStream(fileName);
	            
	            // Byte buffer to hold the blocks
	            byte[] byteBuffer = new byte[this.mapSize*this.mapSize*this.mapSize];
	            
	            // Add all the blocks to the byte buffer
	            for (int x = 0; x < this.mapSize; x++) {
	            	for (int y = 0; y < this.mapSize; y++) {
		            	for (int z = 0; z < this.mapSize; z++) {
	            		byteBuffer[x*this.mapSize*this.mapSize+y*this.mapSize+z] = this.map[x][y][z];
		            	}
	            	}
	            }
	            outputStream.write(byteBuffer);

	            // Close the output stream
	            outputStream.close();
	        }
	        catch(IOException ex) {
	            System.out.println("Error writing to file '" + fileName + "'");
	        }
	    }
}
