package server;
import server.handler.RequestHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SimpleServer {
    private ServerSocket serverSocket;

    public SimpleServer() {
        try {
            // Server listens on port 8080
            serverSocket = new ServerSocket(8080);
            System.out.println("Server started on port 8080...");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startServer() {
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept(); // Accept client connection
                System.out.println("New client connected.");
                
                // Handle the client request in a separate thread
                RequestHandler requestHandler = new RequestHandler(clientSocket);
                requestHandler.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
