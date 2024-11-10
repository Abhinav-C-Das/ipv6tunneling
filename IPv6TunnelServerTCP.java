package tcp4;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class IPv6TunnelServerTCP {
    private static final int IPV4_HEADER_LENGTH = 20;
    private static final int IPV6_HEADER_LENGTH = 40;

    public static void main(String[] args) throws Exception {
        final int serverPort = 9999;

        // Create HTTP server
        HttpServer server = HttpServer.create(new InetSocketAddress(serverPort), 0);
        server.createContext("/send", new MessageHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        System.out.println("Server listening on port " + serverPort);
    }

    static class MessageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
          try{
            if ("POST".equals(exchange.getRequestMethod())) {
                InputStream inputStream = exchange.getRequestBody();
                byte[] buffer = inputStream.readAllBytes();
                String jsonMessage = new String(buffer, StandardCharsets.UTF_8);
                String message = extractMessage(jsonMessage);
                
                // For demonstration, we simply print the message
                System.out.println("Received data: " + message);

                // Here, we can simulate the behavior of processing the message, e.g., sending it over TCP.
                // Send response back
                String response = "Message received: " + message;
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            }
          }catch(Exception e){
            System.out.println("error occured"+e.getMessage());
          }
        }

        private String extractMessage(String json) {
            // A basic JSON parser for this example (could be improved with a library)
            int startIndex = json.indexOf(":") + 2; // Assumes format: {"message": "text"}
            int endIndex = json.lastIndexOf("\"");
            return json.substring(startIndex, endIndex);
        }
    }
}

