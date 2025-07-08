package com.example.testrunner;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class TRServerSocket extends Thread{
    private static final String LOGTAG = "TestRunnerServerSocket";
    private final int port;
    private Context context;
    private boolean isServerRunning = true;
    private ServerSocket serverSocket;
    Socket clientSocket;
    private CommandReceiver commandReceiver;
    private CommandHandler commandHandler;
    private MessageSender messageSender;
    //Constructor
    public TRServerSocket(int port, Context context) {
        this.port = port;
        this.context = context;
    }


    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            Log.d(LOGTAG, "Server started. Listening on port: " + port);
            while (isServerRunning) {
                clientSocket = serverSocket.accept(); //waiting for incomiong connection
                Log.d(LOGTAG,"Connection request accepted from client: " + clientSocket.getInetAddress().getHostName());
                // Create input/output streams
                BufferedReader inputCommandStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter outputMessageStream = new PrintWriter(clientSocket.getOutputStream(), true);

                //Send greetings message to client
                outputMessageStream.println("Connection confirmed");
                Log.d(LOGTAG, "Greetings message sent to client");
                //Create instances of classes handling incoming commands and sending messages
                commandHandler = new CommandHandler(context, this);
                commandReceiver = new CommandReceiver(inputCommandStream, commandHandler);
                messageSender = new MessageSender(outputMessageStream);
                //Start separate threads for incoming commands and output messages
                commandReceiver.start();
                Log.d(LOGTAG, "Started separate Thread for commandReceiver with ID: " + commandReceiver.getId());
                messageSender.start();
                Log.d(LOGTAG, "Started separate Thread for messageSender with ID: " + messageSender.getId());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        isServerRunning = false;


        try {
            Log.d(LOGTAG, "Shutting down client connection on request");
            if (clientSocket != null) clientSocket.close();
            Log.d(LOGTAG, "ServerSocket shutdown!");
            if (commandReceiver != null) commandReceiver.shutdown();
            Log.d(LOGTAG, "CommandReceiver shutdown!");
            if (messageSender != null) messageSender.shutdown();
            Log.d(LOGTAG, "MessageReceiver shutdown!");
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(LOGTAG, "Error occurred when trying to close client connection: " + e.getMessage());
        }
    }
    //Test method to check sending messages to client, will be deleted
    public void sendMessage(String msg){
        if(messageSender != null){
            messageSender.sendToClient(msg);
        }
    }
}
