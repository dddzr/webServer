package server.security;

import java.io.FileInputStream;
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
    private Properties userRoles = new Properties();

    public AuthenticationManager() {
        loadCredentials();
    }

    public void loadCredentials() {
        Properties userProperties = new Properties();
        Properties roleProperties = new Properties();

        // 파일에서 유저명과 비밀번호 해시, 역할 정보를 읽어옴
        try (InputStream userInput = new FileInputStream("resources/config/user.properties");
             InputStream roleInput = new FileInputStream("resources/config/role.properties")) {

            if (userInput == null || roleInput == null) {
                System.out.println("Sorry, unable to find user.properties or role.properties");
                return;
            }

            // 유저명과 비밀번호 해시를 읽어 Map에 저장
            userProperties.load(userInput);
            for (String username : userProperties.stringPropertyNames()) {
                String passwordHash = userProperties.getProperty(username);
                userCredentials.put(username, passwordHash);
            }

            // 유저명과 역할을 읽어 Properties에 저장
            roleProperties.load(roleInput);
            userRoles = roleProperties;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

     // username 반환
    public String getUsernameFromAuthHeader(String authHeader) {
        String[] credentials = decodeBase64Credentials(authHeader);
        if (credentials == null || credentials.length != 2) {
            return null;
        }

        return credentials[0];
    }

    // password 인증
    public boolean authenticate(String authHeader) {
        String[] credentials = decodeBase64Credentials(authHeader);
        if (credentials == null || credentials.length != 2) {
            return false;
        }
    
        String username = credentials[0];
        String password = credentials[1];
    
        return authenticate(username, password);
    }

    // Base64 요청 디코딩
    private String[] decodeBase64Credentials(String authHeader) {
        if (authHeader == null || !authHeader.contains(" Basic ")) {
            return null;
        }
        String encodedCredentials = authHeader.split(" ")[2];
        String decoded = new String(Base64.getDecoder().decode(encodedCredentials));
        return decoded.split(":");
    }
    
    
    // 비밀번호가 일치 확인
    public boolean authenticate(String username, String password) {
        String passwordHash = hashPassword(password); // 사용자 입력    
        String storedPasswordHash = userCredentials.get(username);// userCredentials Map
    
        return storedPasswordHash != null && storedPasswordHash.equals(passwordHash);
    }
    
    
    // 인증 실패
    public void sendUnauthorizedResponse(Socket clientSocket) throws IOException {
        OutputStream outputStream = clientSocket.getOutputStream();
        PrintWriter writer = new PrintWriter(outputStream, true);
        writer.println("HTTP/1.1 401 Unauthorized");
        writer.println("Content-Type: text/html");
        writer.println();
        writer.println("<html><body><h1>401 Unauthorized</h1></body></html>");
        clientSocket.close();  // 연결 종료
    }

    //권한 없음
    public void sendForbiddenResponse(Socket clientSocket) throws IOException {
        OutputStream outputStream = clientSocket.getOutputStream();
        PrintWriter writer = new PrintWriter(outputStream, true);
        writer.println("HTTP/1.1 403 Forbidden");
        writer.println("Content-Type: text/html");
        writer.println();
        writer.println("<html><body><h1>403 Forbidden</h1></body></html>");
        clientSocket.close();  // 연결 종료
    }

    // 역할 확인
    public boolean hasRole(String username, String role) {
        String userRole = userRoles.getProperty(username);
        return userRole != null && userRole.equals(role);
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
