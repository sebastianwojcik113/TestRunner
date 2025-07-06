package com.example.testrunner;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
        //Example:  "Command: WIFI_ADD_NETWORK, SSID: AP1, SEC_TYPE: WPA2, PWD: 12345678"

        Map<String, String> commandParametersMap;
        commandParametersMap = parseCommand(commandToHandle);
        System.out.println("Command after extraction: \"" + commandParametersMap.get("Command") + "\"");
        //Check command type and hrun specific action
        switch(Objects.requireNonNull(commandParametersMap.get("Command"))){
            case "ENABLE_WIFI":
                enableWifi();
                break;
            case "DISABLE_WIFI":
                disableWifi();
                break;
            case "CLOSE_CONNECTION":
                closeClientConnection();
                break;
            case "WIFI_ADD_NETWORK":
                wifiAddNetwork();

                break;
            default:
                Log.e(LOGTAG, "Received unknown command: " + commandToHandle);


        }
    }

    //Method to parse command String and place the parameters inside HashMap
    private Map<String, String> parseCommand(String commandToHandle) {
        // Split command parameters by comma and remove whitespaces
        String[] parameters = commandToHandle.trim().split("\\s*,\\s*");
        System.out.println("Parameters array: " + Arrays.toString(parameters));
        //Create Map to store command parameters in key-value way
        Map<String, String> commandParameters = new HashMap<>();

        //Add parameters to the map
        for(String parameter : parameters){
            //Split key-value by colon and remove whitespaces
            String[] keyValue = parameter.trim().split("\\s*:\\s*");
            System.out.println("KeyValue array: " + "\"" + Arrays.toString(keyValue) + "\"");
            if (keyValue.length == 2){
                commandParameters.put(keyValue[0], keyValue[1]);
            } else {
                Log.e(LOGTAG, "Incorrect value of " + keyValue[0] + "parameter: " + keyValue[1]);
            }

        }
        return commandParameters;
    }

    private void wifiAddNetwork() {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
//        WifiConfiguration wifiConfig;
//        wifiConfig.SSID
//
//        wifiManager.addNetworkPrivileged(wifiConfig)
    }

    private void closeClientConnection() {
        serverSocket.shutdown();
    }

    private void disableWifi() {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if(wifiManager != null) {
            wifiManager.setWifiEnabled(false);
            Log.d(LOGTAG, "Calling action to disable WiFi");
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
