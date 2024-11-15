package server.security;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Properties;

public class AuthenticationManager {
    /*테스트 코드 */
    // private static final String username = "admin";
    // private static final String passwordHash = "5f4dcc3b5aa765d61d8327deb882cf99";

    private String username;
    private String passwordHash;

    public AuthenticationManager() {
        loadCredentials();
    }

    // config/users.properties에서 사용자 자격 증명을 불러오는 메소드
    private void loadCredentials() {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config/users.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find users.properties");
                return;
            }
            properties.load(input);
            this.username = properties.getProperty("username");
            this.passwordHash = properties.getProperty("passwordHash");
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

        // username과 해시화된 password 비교
        String username = credentials[0];
        String password = credentials[1];

        return this.username.equals(username) && this.passwordHash.equals(hashPassword(password));
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
