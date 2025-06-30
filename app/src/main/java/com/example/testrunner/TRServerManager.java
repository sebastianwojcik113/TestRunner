package com.example.testrunner;

public class TRServerManager {
    private final int port;
    private TRServerSocket serverThread;

    public TRServerManager(int port) {
        this.port = port;
    }

    public void startServer() {
        //create object of TRServerSocket class, pass the port number
        serverThread = new TRServerSocket(port);
        //start() method calls run() which is overrided inside TRServerSocket class
        serverThread.start();
    }

    public void stopServer() {
        if (serverThread != null) {
            serverThread.shutdown();
        }
    }
}