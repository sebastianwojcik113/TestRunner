package com.example.testrunner;

import android.util.Log;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class TRServerSocket extends Thread{
    private static final String LOGTAG = "TestRunnerServerSocket";
    private final int port;
    private boolean running = true;
    private ServerSocket serverSocket;

    public TRServerSocket(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            Log.d(LOGTAG, "Server started. Listening on port: " + port);
            while (running) {
                Socket socket = serverSocket.accept(); //waiting for incomiong connection
                Log.d(LOGTAG,"Connection request accepted from client: " + socket.getInetAddress().getHostName());
                //Send greetings message to client
                PrintWriter welcomeOutput = new PrintWriter(socket.getOutputStream(), true);
                welcomeOutput.println("Connection confirmed");
                Log.d(LOGTAG, "Greetings message sent to client");
                // Create new instance of TRClientHandler to handle bidirectional communication with client in other Thread
                TRClientHandler handler = new TRClientHandler(socket);
                System.out.println(handler.getId());
                handler.start();
                System.out.println(handler.getId());
                String example = "Example message";
                handler.sendToClient(example);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
