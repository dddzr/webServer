package server;
import server.handler.RequestHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimpleServer {
    private ServerSocket serverSocket;
    private ExecutorService executorService; //스레드 풀

    public SimpleServer() {
        try {
            // Server listens on port 8080
            serverSocket = new ServerSocket(8080);
            System.out.println("Server started on port 8080...");

            // Create a thread pool with a fixed size
            executorService = Executors.newFixedThreadPool(10);  // Example: 10 threads
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startServer() {
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept(); // Accept client connection
                System.out.println("New client connected.");
                
                // Handle the client request using thread pool
                executorService.submit(new RequestHandler(clientSocket));  // Submit to thread pool
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
