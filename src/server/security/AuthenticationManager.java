package server.security;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class AuthenticationManager {
    private Map<String, String> userCredentials = new HashMap<>(); // 유저명과 해시된 비밀번호를 저장

    public void loadCredentials() {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config/users.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find users.properties");
                return;
            }
            properties.load(input);

            // 파일에서 유저명과 비밀번호 해시를 읽어와 Map에 저장
            for (String username : properties.stringPropertyNames()) {
                String passwordHash = properties.getProperty(username);
                userCredentials.put(username, passwordHash);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public boolean authenticate(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return false;
        }
    
        // Base64로 인코딩된 자격 증명 디코딩
        String encodedCredentials = authHeader.split(" ")[1];
        String decoded = new String(Base64.getDecoder().decode(encodedCredentials));
        String[] credentials = decoded.split(":");
    
        if (credentials.length != 2) {
            return false;  // username과 password가 올바르게 제공되지 않은 경우
        }
    
        // username과 password 추출
        String username = credentials[0];
        String password = credentials[1];
    
        return authenticate(username, password);
    }
    
    public boolean authenticate(String username, String password) {
        // 사용자가 입력한 비밀번호의 해시값을 구함
        String passwordHash = hashPassword(password);
    
        // userCredentials Map에서 해당 유저의 해시된 비밀번호를 가져와 비교
        String storedPasswordHash = userCredentials.get(username);
    
        // 비밀번호가 일치하는지 확인
        return storedPasswordHash != null && storedPasswordHash.equals(passwordHash);
    }
    
    
    // Send Unauthorized response when authentication fails
    public void sendUnauthorizedResponse(Socket clientSocket) throws IOException {
        OutputStream outputStream = clientSocket.getOutputStream();
        PrintWriter writer = new PrintWriter(outputStream, true);
        writer.println("HTTP/1.1 401 Unauthorized");
        writer.println("Content-Type: text/html");
        writer.println();
        writer.println("<html><body><h1>401 Unauthorized</h1></body></html>");
    }

    // 비밀번호를 해시화하는 메소드 (단순화된 예시, 실제 환경에서는 더 안전한 해시 알고리즘 사용)
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}
