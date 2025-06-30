package com.example.testrunner;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TRClientHandler extends Thread{
    private static final String LOGTAG = "TestRunnerClientHandler";
    private final Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean isRunning = true;
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

    //Constructor
    public TRClientHandler(Socket socket){
        this.socket = socket;
    }

    //Override run() method of superclass Thread as it is required (documentation)
    @Override
    public void run(){
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            //start sender Thread before receiving loop as it can block the thread until client send first message
            Thread listenThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    sendingLoop();
                }
            });
            listenThread.start();

            //Loop for receiving message
            // readLine() will block the thread until the message comes in
            String line;
            while (isRunning && (line = in.readLine()) != null) {
                Log.d(LOGTAG, "Message received from client: " + line);
                System.out.println("From client: " + line);
                // Możesz odpowiedzieć lub zignorować
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendingLoop() {
        try {
            while (isRunning) {
                // waiting as message to send appears in messageQUeue
                String msg = messageQueue.take();
                System.out.println("Message taken from Queue: " + msg);
                // sending taken message to client
                out.println(msg);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void sendToClient(String msg){
        messageQueue.offer(msg);
        Log.d(LOGTAG, "Message sent to client: " + msg);
    }

    public void shutdown() {
        isRunning = false;
        try {
            socket.close();
            out.close();
            in.close();
            Log.d(LOGTAG, "Client socket shutdown");
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(LOGTAG, "Error when trying to shutdown client socket: " + e.getMessage());
        }
    }
}
