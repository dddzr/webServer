package server.handler;

import java.io.*;
import java.nio.file.*;

import server.security.AuthenticationManager;

import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;

public class RequestHandler implements Runnable {
    private Socket clientSocket;
    private AuthenticationManager authenticationManager;

    public RequestHandler(Socket socket) {
        this.clientSocket = socket;
        this.authenticationManager = new AuthenticationManager();
    }

    @Override
    public void run() {
        try {
            InputStream inputStream = clientSocket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String requestLine = reader.readLine(); // Read Header

            /* 
                the HTTP request 
                요청 예시
                - 브라우저 에서
                http://localhost:8080/index.html

                - 실제 요청 형태
                GET /index.html HTTP/1.1
                Host: localhost:8080
                Connection: keep-alive

                - 인증 포함
                GET /protected/resource HTTP/1.1
                Host: example.com
                Authorization: Bearer abcdefgh1234567890 (JWT토큰 이용) 또는 Basic YWRtaW46cGFzc3dvcmQ=
                Connection: keep-alive

             */
            System.out.println("Request: " + requestLine);

            // 인증 체크: 요청이 API나 보호된 리소스에 대한 것이라면 인증을 확인
            // if (isProtectedResource(requestLine)) {
                String authHeader = getAuthorizationHeader(reader);
                if (!authenticationManager.authenticate(authHeader)) {
                    authenticationManager.sendUnauthorizedResponse(clientSocket); // 인증 실패 응답
                    return; // 인증 실패 시 요청 처리 중단
                }
            // }
            
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
                sendResponse(writer, apiResponse, "application/json");
            } else {
                sendNotFoundResponse(writer);
            }

            clientSocket.close(); // Close the client connection after sending the response
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Check if the requested resource requires authentication
    private boolean isProtectedResource(String requestLine) {
        return requestLine.contains("/api/") || requestLine.contains("/admin");
    }

    // Extract the Authorization header from the request
    private String getAuthorizationHeader(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            // "Authorization: Bearer <token>" 형식이라면
            if (line.startsWith("Authorization:")) {
                return line.split(" ")[1]; // 두 번째 부분인 "Bearer"나 토큰을 반환
            }
        }
        return null; // No Authorization header found
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

    // Forward request to WAS
    // 리버스 프록시 역할
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
