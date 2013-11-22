package is.ru.tgra.server;

import java.net.Socket;

/******************************************************************************
 * GameServer.java
 * 
 * This the main class of the server, it simply starts a TcpServer and asks it
 * for a new socket, for every socket it spawns a new thread to deal with each
 * client
 *****************************************************************************/


public class GameServer {
	public static void main(String[] args) throws ClassNotFoundException {
		TcpServer tcpServer = new TcpServer(5050);
		Players players = new Players(16);
		World world = new World(50);
		while (true) {
			Socket sock = tcpServer.getNewSocket();
			(new Thread(new ClientThread(sock, players, world))).start();
		}
 	}
}
