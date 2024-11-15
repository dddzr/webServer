import server.SimpleServer;
public class App {
    public static void main(String[] args) throws Exception {
        // System.out.println("Hello, World!");
        SimpleServer server = new SimpleServer();
        server.startServer();
    }
}
