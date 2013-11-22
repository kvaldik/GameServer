package is.ru.tgra.server;

import java.io.IOException;

/******************************************************************************
 * TcpServer.java
 * 
 * This is simple tcp server that returns a new socket if a client connects
 *****************************************************************************/
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;


public class TcpServer {
	private ServerSocket serverSocket;
	
	public TcpServer(int port) {
		try {
			this.serverSocket = new java.net.ServerSocket(port);
			assert this.serverSocket.isBound();
			if (this.serverSocket.isBound()) {
				System.out.println("SERVER inbound data port " +
					this.serverSocket.getLocalPort() +
					" is ready and waiting for client to connect...");
			}
		}
		catch (SocketException se) {
			System.err.println("Unable to create socket.");
			se.printStackTrace();
		}
		catch (IOException ioe) {
			System.err.println("Unable to read data from an open socket.");
			ioe.printStackTrace();
		}
	}
	
	public Socket getNewSocket() {
		try {
			return this.serverSocket.accept();
		} catch (IOException e) {
			System.out.printf("Error occured when waiting for a connection \n");
			e.printStackTrace();
			return null;
		}
	}
}