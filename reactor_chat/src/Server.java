package reactor_chat.src;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public static class ReactorData {
        Selector selector;
        ExecutorService service;
        ServerSocketChannel serverSocketChannel;

        ReactorData(int port) throws IOException {
            service = Executors.newFixedThreadPool(12);
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().bind(new InetSocketAddress(port));

            System.out.println("Server running on port " + port);

            serverSocketChannel.configureBlocking(false);
            selector = Selector.open();

            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT, new ConnectionAcceptor(this));

            while (true) {
                selector.select();
                // for (SelectionKey selKey : selector.selectedKeys()) {
                //     Runnable runnable = (Runnable) selKey.attachment();
                //     runnable.run();
                // }

                Iterator iter = selector.selectedKeys().iterator();
                
                while(iter.hasNext()) {
                    SelectionKey selKey = (SelectionKey) iter.next();
                    iter.remove();
                    Runnable runnable = (Runnable) selKey.attachment();
                    runnable.run();
                }
            }
        }
    }

    public static class ConnectionAcceptor implements Runnable {
        ReactorData reactor;

        ConnectionAcceptor(ReactorData _reactor) {
            reactor = _reactor;
        }

        @Override
        public void run() {
            try {
                SocketChannel client = reactor.serverSocketChannel.accept();
                
                System.out.println("Client connected from " + client.getRemoteAddress());
                new ConnectionHandler(client, reactor);
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    }

    public static class ConnectionHandler implements Runnable {
        SocketChannel client;
        ReactorData reactor;
        SelectionKey key;
        public String palabra;
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        ConnectionHandler(SocketChannel _client, ReactorData _reactor) throws IOException {
            client = _client;
            reactor = _reactor;

            client.configureBlocking(false);
            key = client.register(reactor.selector, SelectionKey.OP_READ, this);
        }

        @Override
        public void run() {
            try {
                buffer.clear();
                client.read(buffer);
                reactor.service.execute(new Runnable(){
                    @Override
                    public void run() {
                        try {
                            read();
                        } catch (IOException exception) {
                            exception.printStackTrace();
                        }
                    }
                });
                
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }

        void read() throws IOException {
            buffer.flip();
            byte[] bytes = new byte[buffer.limit()];
            buffer.get(bytes);
            String message = new String(bytes);
        
            System.out.println("Message:  \"" + message + "\" from: " + client.getRemoteAddress());
            
            for (SelectionKey sKey : reactor.selector.keys()) {
                if (sKey.channel() instanceof SocketChannel) {
                    SocketChannel c = (SocketChannel) sKey.channel();
                    if (!client.getRemoteAddress().toString().equals(c.getRemoteAddress().toString())) {
                        c.write(ByteBuffer.wrap(message.getBytes()));
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new ReactorData(7000);
    }
}
