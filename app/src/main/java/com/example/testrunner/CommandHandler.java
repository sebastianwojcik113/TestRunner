package com.example.testrunner;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

public class CommandHandler {
    private static final String LOGTAG = "TestRunnerCommandHandler";
    private static Context context;
    private TRServerSocket serverSocket;
    //Construktor
    public CommandHandler(Context context, TRServerSocket serverSocket){
        this.context = context;
        this.serverSocket = serverSocket;
    }

    public void handleCommand(String commandToHandle){
        switch(commandToHandle){
            case "ENABLE_WIFI":
                enableWifi();
                break;
            case "DISABLE_WIFI":
                disableWifi();
                break;
            case "CLOSE_CONNECTION":
                closeClientConnection();
                break;
            default:
                Log.d(LOGTAG, "Received unknown command: " + commandToHandle);


        }
    }

    private void closeClientConnection() {
        serverSocket.shutdown();
    }

    private void disableWifi() {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if(wifiManager != null) {
            wifiManager.setWifiEnabled(false);
        }
    }

    private void enableWifi() {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        Log.d(LOGTAG, "Current WifiManager: " + wifiManager.getWifiState());
        if(wifiManager != null) {
            wifiManager.setWifiEnabled(true);
            Log.d(LOGTAG, "Calling action to enable WiFi");
        }
    }
}
