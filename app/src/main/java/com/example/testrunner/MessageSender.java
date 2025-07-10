package com.example.testrunner;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MessageSender extends Thread{
    private static final String LOGTAG = "TestRunnerClientHandler";
    private static final String SERVERTOCLIENT = "[<--] ";
    private PrintWriter outputMessageStream;
    private boolean isRunning = true;
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

    //Constructor
    public MessageSender(PrintWriter outputMessageStream){
        this.outputMessageStream = outputMessageStream;
    }

    //Override run() method of superclass Thread as it is required (documentation)
    @Override
    public void run() {
        try {
            while (isRunning) {
                String message = messageQueue.take();
                outputMessageStream.println(message);
                Log.d(LOGTAG, "Message sent to client: " + message);
            }
        } catch (InterruptedException e) {
            Log.d(LOGTAG, "Error occurred when trying to send message to client: " + e.getMessage());
            shutdown();

            }
    }

    public void sendToClient(String msg){
        messageQueue.offer(SERVERTOCLIENT + msg);
        Log.d(LOGTAG, "Output message added to queue: " + SERVERTOCLIENT + msg);
    }

    public void shutdown() {
        isRunning = false;
        //Close output stream
        outputMessageStream.close();
        //Interrupt the MessageSender Thread
        this.interrupt();
        Log.d(LOGTAG, "Client socket shutdown!");
    }
}
