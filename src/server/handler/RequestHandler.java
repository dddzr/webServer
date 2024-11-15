package server.handler;

import java.io.*;
import java.nio.file.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;

public class RequestHandler implements Runnable {
    private Socket clientSocket;

    public RequestHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            InputStream inputStream = clientSocket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String requestLine = reader.readLine(); // Read the HTTP request line
            System.out.println("Request: " + requestLine);
            /*
                요청 예시
                - 브라우저 에서
                http://localhost:8080/index.html

                - 실제 요청 형태
                GET /index.html HTTP/1.1
                Host: localhost:8080
                Connection: keep-alive
             */

            // Parse the requested file (assuming the request line is of the form "GET /index.html HTTP/1.1")
            String[] requestParts = requestLine.split(" ");
            String fileRequested = requestParts[1];

            // Set up the output stream for the response
            OutputStream outputStream = clientSocket.getOutputStream();
            PrintWriter writer = new PrintWriter(outputStream, true);

            // Check if the request is for a static file (e.g., .html, .css, .png)
            if (fileRequested.endsWith(".html")) {
                serveStaticFile(writer, fileRequested, "text/html");
            } else if (fileRequested.endsWith(".css")) {
                serveStaticFile(writer, fileRequested, "text/css");
            } else if (fileRequested.endsWith(".png")) {
                serveStaticFile(writer, fileRequested, "image/png");
            } else if (fileRequested.startsWith("/api/")) {
                // For API requests, forward the request to WAS
                String apiResponse = forwardToWAS(fileRequested);
                sendResponse(writer, apiResponse, "application/json"); // Assume API returns JSON
            } else {
                sendNotFoundResponse(writer); // Handle unsupported file types or URLs
            }

            clientSocket.close(); // Close the client connection after sending the response
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Serve static file from webroot
    private void serveStaticFile(PrintWriter writer, String fileRequested, String contentType) {
        try {
            // 요청된 파일이 타입 따라 경로 지정
            String basePath = "resources";
            if (contentType.equals("text/html")) {
                basePath = basePath + "/html" + fileRequested;
            } else if (contentType.equals("text/css")) {
                basePath = basePath + "/css" + fileRequested;
            } else if (contentType.contains("image")) {
                basePath = basePath + "/img" + fileRequested;
            }

            File file = new File(basePath);
            if (file.exists()) {
                writer.println("HTTP/1.1 200 OK");
                writer.println("Content-Type: " + contentType);
                writer.println("Content-Length: " + file.length());
                writer.println(); // End of headers

                // Send the file content to the client
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                clientSocket.getOutputStream().write(fileBytes);
            } else {
                sendNotFoundResponse(writer); // File not found
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Forward request to WAS (e.g., a REST API)
    private String forwardToWAS(String fileRequested) {
        StringBuilder response = new StringBuilder();
        try {
            // Assume the WAS (e.g., Tomcat) is running locally on port 8081
            URL url = new URL("http://localhost:8081" + fileRequested);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response.toString();
    }

    // Send the API response to the client
    private void sendResponse(PrintWriter writer, String response, String contentType) {
        writer.println("HTTP/1.1 200 OK");
        writer.println("Content-Type: " + contentType);
        writer.println();
        writer.println(response); // Send the API response (e.g., JSON) to the client
    }

    // Send a 404 Not Found response
    private void sendNotFoundResponse(PrintWriter writer) {
        writer.println("HTTP/1.1 404 Not Found");
        writer.println("Content-Type: text/html");
        writer.println();
        writer.println("<html><body><h1>404 Not Found</h1></body></html>");
    }
}
