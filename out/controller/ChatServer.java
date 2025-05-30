/*
package com.example.lms.chat;

import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static Set<ClientHandler> clientHandlers = Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) {
        int port = 5555;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Chat server started on port " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket);
                clientHandlers.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void broadcast(String message, ClientHandler excludeUser) {
        synchronized (clientHandlers) {
            for (ClientHandler client : clientHandlers) {
                if (client != excludeUser) {
                    client.sendMessage(message);
                }
            }
        }
    }

    static void removeClient(ClientHandler client) {
        clientHandlers.remove(client);
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private String userName;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try (
                    InputStream input = socket.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                    OutputStream output = socket.getOutputStream();
            ) {
                out = new PrintWriter(output, true);

                out.println("Enter your name:");
                userName = reader.readLine();
                ChatServer.broadcast(userName + " has joined the chat.", this);

                String clientMessage;
                while ((clientMessage = reader.readLine()) != null) {
                    if (clientMessage.equalsIgnoreCase("bye")) {
                        break;
                    }
                    String message = "[" + userName + "]: " + clientMessage;
                    ChatServer.broadcast(message, this);
                }

                socket.close();
                ChatServer.removeClient(this);
                ChatServer.broadcast(userName + " has left the chat.", this);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void sendMessage(String message) {
            out.println(message);
        }
    }
}
*/
