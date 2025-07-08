package com.example.testrunner;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CommandHandler {
    private static final String LOGTAG = "TestRunnerCommandHandler";
    private static Context context;
    private TRServerSocket serverSocket;
    private WifiManager wifiManager;
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
                wifiConnectSsid(commandParametersMap);
                break;
//            case "CONNECT_SSID":
//                wifiConnectSsid(commandParametersMap);
//                break;
            default:
                Log.e(LOGTAG, "Received unknown command: " + commandToHandle);


        }
    }
    //TODO Sprawdzic czy rozdzielenie akcji addProfile i Connect jest w ogole potrzebne
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private void wifiConnectSsid_copy(Map<String, String> commandParametersMap) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        String ssid = commandParametersMap.get("SSID");
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
        Log.d(LOGTAG, configuredNetworks.toString());
        //TODO Dodać usuwanie wszystkich sieci wifi na poczatku uruchamiania apki lub przy wywołaniu wifiConnectSsid


//        wifiManager.disconnect();
//        wifiManager.enableNetwork(netId, true);
//        wifiManager.reconnect();
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

    private void wifiConnectSsid(Map<String, String> commandParametersMap) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiConfiguration wifiConfig = new WifiConfiguration();
        boolean ssidProvided = false;

        if (commandParametersMap.get("SSID") == null){
            Log.e(LOGTAG, "Command ADD_NETWORK does not contain any network name(SSID)");
            ssidProvided = false;
        } else {
            wifiConfig.SSID = "\"" + commandParametersMap.get("SSID") + "\"";
            ssidProvided = true;
            Log.d(LOGTAG, "SSID provided for WIFI_ADD_NETWORK command. Proceeding with adding the network");
        }


        //TODO Napisac logikę sprawdzania czy wszystkie wymagane parametry zostały podane, np: w przypadku security type WPA2 musi być haslo
//        wifiConfig.preSharedKey = "7MCE7KPN";
//        if (ssidProvided){
//            if (!commandParametersMap.get("SEC_TYPE").equalsIgnoreCase("open")){
//
//            }
//        }

        //TODO dodać obslugę próby dopisania sieci przy wyłączonym wifi, pewnie if wifiManager == null
        int netId = wifiManager.addNetwork(wifiConfig);
        System.out.println("netID: " + netId);
        if (netId < 0){
            Log.e(LOGTAG, "Unable to add Wi-Fi network profile. Configuration may be incorrect.");
            serverSocket.sendMessage("Unable to add network profile: " + commandParametersMap.get("SSID") + " Configuration may be incorrect.");
        } else {
            Log.d(LOGTAG, "Network profile successfully added: " + commandParametersMap.get("SSID"));
        }
        wifiManager.disconnect();
        if (wifiManager.enableNetwork(netId, true)) {
            Log.d(LOGTAG, "Network " + commandParametersMap.get("SSID") + " enabled.");

        }
    }

    private void closeClientConnection() {
        serverSocket.shutdown();
    }

    private void disableWifi() {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if(wifiManager != null) {
            wifiManager.setWifiEnabled(false);
            Log.d(LOGTAG, "Calling action to disable WiFi");
            serverSocket.sendMessage("Wi-Fi disabled successfully");
        }
    }

    private void enableWifi() {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        Log.d(LOGTAG, "Current WifiManager: " + wifiManager.getWifiState());
        if(wifiManager != null) {
            wifiManager.setWifiEnabled(true);
            Log.d(LOGTAG, "Calling action to enable WiFi");
            serverSocket.sendMessage("Wi-Fi enabled successfully");
        }
    }
}
