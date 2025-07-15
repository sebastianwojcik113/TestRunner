package com.example.testrunner;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MessageSender extends Thread{
    private static final String LOGTAG = "TestRunnerClientHandler";
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

    public void sendMsgToClient(String msg){
        try {
            JSONObject jsonMessage = new JSONObject();
            jsonMessage.put("Message", msg);
            messageQueue.offer(jsonMessage.toString());
            Log.d(LOGTAG, "Output message added to queue: " + jsonMessage);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
    public void sendAckToClient(int commandId, String result, String msg){
        try {
            JSONObject jsonMessage = new JSONObject();
            jsonMessage.put("Ack_ID", commandId);
            jsonMessage.put("Result", result);
            jsonMessage.put("Message", msg);
            messageQueue.offer(jsonMessage.toString());
            Log.d(LOGTAG, "Output message added to queue: " + jsonMessage);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }


    //TODO Przemyslec jak zaimplementowac ACK zmapowane z Command_ID i czy potrzebne?

    public void shutdown() {
        isRunning = false;
        //Close output stream
        outputMessageStream.close();
        //Interrupt the MessageSender Thread
        this.interrupt();
        Log.d(LOGTAG, "Client socket shutdown!");
    }
}
