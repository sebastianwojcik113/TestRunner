package com.example.testrunner;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;

public class CommandReceiver extends Thread{
    private static final String LOGTAG = "TestRunnerCommandReceiver";
    private BufferedReader incomingCommand;
    private CommandHandler commandHandler;
    private boolean isServerRunning = true;
    // Constructor
    public CommandReceiver(BufferedReader incomingCommand, CommandHandler commandHandler) throws IOException {
        this.incomingCommand = incomingCommand;
        this.commandHandler = commandHandler;
    }
    public void shutdown(){
        isServerRunning = false;
    }
    @Override
    public void run(){
        String receivedCommand;
        try {
            while (isServerRunning && (receivedCommand = incomingCommand.readLine()) != null) {
                Log.d(LOGTAG, "Command from client received: " + receivedCommand);
                commandHandler.handleCommand(receivedCommand);
            }
        } catch (Exception e) {
            if (isServerRunning) e.printStackTrace();
        }
    }


}

