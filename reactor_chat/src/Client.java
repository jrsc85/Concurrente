package reactor_chat.src;

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.io.*;

public class Client {
    private SocketChannel client;
    private String clientName;
    public String palabrAhorcada;

    public void Connect(String host, int port) throws IOException {
        client = SocketChannel.open();
        client.connect(new InetSocketAddress(host, port));

        Thread watchServer = new Thread() {
            public void run() {
                try {
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    while(client.read(buffer) > 0) {
                        buffer.flip();
                        byte[] bytes = new byte[buffer.limit()];
                        buffer.get(bytes);
                        System.out.print("\r" + new String(bytes));
                        System.out.print("\n[" + clientName + "]: ");
                        
                        buffer.clear();
                    }
                    Disconnect();
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }
        };

        Thread watchConsole = new Thread() {
            String Palabra;
            public void run() {
                try {
                    Console console = System.console();
                    clientName = console.readLine("\nEnter username: ");
                    palabrAhorcada =console.readLine("\nIngresa tu palabra");

                    String message;
                    while((message = console.readLine("[" + clientName +"]: ")) != null) {
                        String formatted = "[" + clientName + "]: " + message;
                        String Palabraformat = palabrAhorcada;
                        client.write(ByteBuffer.wrap(Palabraformat.getBytes()));
                        client.write(ByteBuffer.wrap(formatted.getBytes()));
                        if ("./exit".equals(message)) break;
                    }
                    Disconnect();
                } catch(IOException exception) {
                    exception.printStackTrace();
                }
            }
        };

        watchConsole.start();
        watchServer.start();
    }

    public void Disconnect() throws IOException{
        client.close();
        System.out.println("Disconected");
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Client client = new Client();
        client.Connect("127.0.0.1", 7000);
    }
}

