package com.example.testrunner;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandHandler {
    private static final String LOGTAG = "TestRunnerCommandHandler";
    private static final String ERROR_RESULT = "ERROR";
    private static final String COMPLETE_RESULT = "COMPLETE";

    private static Context context;
    private TRServerSocket serverSocket;
    private WifiManager wifiManager;
    //Construktor
    public CommandHandler(Context context, TRServerSocket serverSocket){
        this.context = context;
        this.serverSocket = serverSocket;
    }

    public void handleCommand(String jsonCommandToHandle){
        //Example: "{\"Command_ID\":42, \"Command\":\"WIFI_ADD_NETWORK\", \"SSID\":\"AP1\", \"SECURITY_TYPE\":\"WPA2\", \"PWD\":\"12345678\"}"

        try {
            JSONObject commandObj = new JSONObject(jsonCommandToHandle);
            String commandToHandle = commandObj.getString("Command");
            System.out.println("Command after extraction: \"" + commandObj.getString("Command") + "\"");
            int commandID = commandObj.getInt("Command_ID");
            //Check command type and run specific action
            switch(commandToHandle){
                case "ENABLE_WIFI":
                    enableWifi(commandID);
                    break;
                case "DISABLE_WIFI":
                    disableWifi(commandID);
                    break;
                case "CLOSE_CONNECTION":
                    closeClientConnection();
                    break;
                case "WIFI_ADD_NETWORK":
                    wifiConnectSsid(commandObj);
                    break;
//            case "CONNECT_SSID":
//                wifiConnectSsid(commandParametersMap);
//                break;
                default:
                    Log.e(LOGTAG, "Received unknown command: " + commandToHandle);


            }

        } catch (JSONException e) {
            throw new RuntimeException(e);
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
//    private Map<String, String> parseCommand(JSONObject jsonObj) {
//        // Split command parameters by comma and remove whitespaces
//        String[] parameters = commandToHandle.trim().split("\\s*,\\s*");
//        System.out.println("Parameters array: " + Arrays.toString(parameters));
//        //Create Map to store command parameters in key-value way
//        Map<String, String> commandParameters = new HashMap<>();
//
//        //Add parameters to the map
//        for(String parameter : parameters){
//            //Split key-value by colon and remove whitespaces
//            String[] keyValue = parameter.trim().split("\\s*:\\s*");
//            System.out.println("KeyValue array: " + "\"" + Arrays.toString(keyValue) + "\"");
//            if (keyValue.length == 2){
//                commandParameters.put(keyValue[0], keyValue[1]);
//            } else {
//                Log.e(LOGTAG, "Incorrect value of " + keyValue[0] + "parameter: " + keyValue[1]);
//            }
//
//        }
//        return commandParameters;
//    }

    private void wifiConnectSsid(JSONObject jsonCommand) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiConfiguration wifiConfig = new WifiConfiguration();
        WifiInfo wifiInfo = null;
        int commandID;
        String ssid;

        try {
            commandID = Integer.parseInt(jsonCommand.getString("Command_ID"));
            // Check if command contain SSID at all
            if (!jsonCommand.has("SSID")){
                Log.e(LOGTAG, "Command ADD_NETWORK does not contain any network name(SSID)");
                serverSocket.sendAck(commandID, ERROR_RESULT, "Command " + jsonCommand.getString("Command") + " requires SSID name provided!");
            } else {
                ssid = jsonCommand.getString("SSID");
                // Add SSID to the wificonfig
                wifiConfig.SSID = "\"" + ssid + "\"";
                Log.d(LOGTAG, "SSID provided for WIFI_ADD_NETWORK command. Proceeding with adding the network");
                serverSocket.sendMessage("SSID provided for WIFI_ADD_NETWORK command. Proceeding with adding the network");

                int netId = wifiManager.addNetwork(wifiConfig);
                System.out.println("netID assigned after addNetwork to the wificonfig: " + netId);
                System.out.println("Connection info after addNetwork: " + wifiManager.getConnectionInfo());
                if (netId < 0){
                    //Check if addNetwork succeded, netId should be highher than -1
                    Log.e(LOGTAG, "Unable to add Wi-Fi network profile. Configuration may be incorrect.");
                    serverSocket.sendAck(Integer.parseInt(jsonCommand.getString("Command_ID")), ERROR_RESULT, "Unable to add Wi-Fi network profile: " + jsonCommand.getString("SSID") + " Configuration may be incorrect.");
                } else {
                    Log.d(LOGTAG, "Network profile successfully added: " + jsonCommand.getString("SSID"));
                    serverSocket.sendMessage("Network profile successfully added: " + jsonCommand.getString("SSID"));
                    wifiInfo = wifiManager.getConnectionInfo();
                }

                //wifiManager.disconnect();
                if (wifiManager.enableNetwork(netId, true)) {
                    System.out.println("Connection info after enableNetwork: " + wifiManager.getConnectionInfo());
                    Log.d(LOGTAG, "Network " + jsonCommand.getString("SSID") + " enabled.");
                } else {
                    System.out.println("Error when trying to enable network!");
                    Log.d(LOGTAG, "Error when trying to enable network");
                    serverSocket.sendMessage("Wi-Fi network " + ssid + "enabled");
                }
                if (isConnectedToSsid(ssid, 6000)){
                    Log.d(LOGTAG, "Connected with network: " + ssid);
                    serverSocket.sendAck(commandID, COMPLETE_RESULT, "Connected with network: " + ssid);
                } else {
                    Log.d(LOGTAG, "");
                }


                System.out.println(wifiInfo.getSupplicantState());
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        wifiManager.disconnect();


        //TODO Napisac logikę sprawdzania czy wszystkie wymagane parametry zostały podane, np: w przypadku security type WPA2 musi być haslo
//        wifiConfig.preSharedKey = "7MCE7KPN";
//        if (ssidProvided){
//            if (!commandParametersMap.get("SEC_TYPE").equalsIgnoreCase("open")){
//
//            }
//        }

        //TODO dodać obslugę próby dopisania sieci przy wyłączonym wifi, if wifiManager == null?

    }

    private void closeClientConnection() {
        serverSocket.shutdown();
    }

    private void disableWifi(int commandID) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if(wifiManager != null) {
            wifiManager.setWifiEnabled(false);
            Log.d(LOGTAG, "Calling action to disable WiFi");
            serverSocket.sendMessage("Wi-Fi disabled successfully");
            serverSocket.sendAck(commandID, COMPLETE_RESULT, "Wi-FI status: " + wifiManager.getWifiState());
        } else {
            Log.e(LOGTAG, "WifiManager is null! Cannot disable Wi-Fi. Possible reasons: context problem or device doesn't support Wi-Fi.");
            serverSocket.sendMessage("Error: Wi-Fi manager unavailable. Please check device capabilities or app permissions.");
            serverSocket.sendAck(commandID, ERROR_RESULT, "Wi-Fi manager is null!");
        }
    }

    private void enableWifi(int commandID) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        int timeout = 5000;

        if(wifiManager != null) {
            Log.d(LOGTAG, "Current Wi-Fi state: " + wifiManager.getWifiState());
            wifiManager.setWifiEnabled(true);
            if(waitForWifiStateChange(wifiManager.WIFI_STATE_ENABLED, timeout)){
                //WIFI_STATE_DISABLED -> WIFI_STATE_ENABLING -> WIFI_STATE_ENABLED
                Log.d(LOGTAG, "Calling action to enable WiFi");
                serverSocket.sendMessage("Wi-Fi enabled successfully");
                serverSocket.sendAck(commandID, COMPLETE_RESULT, "Wi-FI status: " + wifiManager.getWifiState());
            }
        } else {
            Log.e(LOGTAG, "WifiManager is null! Cannot enable Wi-Fi. Possible reasons: context problem or device doesn't support Wi-Fi.");
            serverSocket.sendMessage("Error: Wi-Fi manager unavailable. Please check device capabilities or app permissions.");
            serverSocket.sendAck(commandID, ERROR_RESULT, "Wi-Fi manager is null!");
        }
    }
    private String parseWifiState(int state){
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        HashMap<Integer, String> wifiStateMap = new  HashMap<Integer, String>();
        wifiStateMap.put(0, "WIFI_STATE_DISABLING");
        wifiStateMap.put(1, "WIFI_STATE_DISABLED");
        wifiStateMap.put(2, "WIFI_STATE_ENABLING");
        wifiStateMap.put(3, "WIFI_STATE_ENABLED");
        wifiStateMap.put(4, "WIFI_STATE_UNKNOWN");
        String wifiState = "";

        if (wifiStateMap.containsKey(state)){
            wifiState = wifiStateMap.get(state);
        } else {
            System.out.println("Unable to match Wi-Fi state key to value. Check if following Wi-Fi state is in HashMap: " + state);
        }
        return wifiState;
    }
    /**
     * Checking the Wi-Fi state every checkingInterval time until it is changed to the
     * expectedState or timeoutMilliseconds time elapsed
     *
     * @param expectedState expected Wi-Fi state
     * @param timeoutMilliseconds timeout time
     * @return true if expectedState is reached, false if timeout time elapsed
     */
    private boolean waitForWifiStateChange(int expectedState, int timeoutMilliseconds){
        //TODO Zmiana logiki sprawdzania stanu wifi -> BroadcastReceiver?
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        int elapsedTime = 0;
        int checkingInterval = 100; //checking wifi state every 50ms

        while(elapsedTime < timeoutMilliseconds){
            int state = wifiManager.getWifiState();
            System.out.println("Wi-Fi state: " + parseWifiState(state));
            if (state == expectedState){
                return true;
            }
            elapsedTime += checkingInterval;
            try {
                Thread.sleep(checkingInterval);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        Log.d(LOGTAG, "Action waitForWifiStateChange timed out!");
        return false;
    }
    private boolean isConnectedToSsid(String expectedSsid, int timeoutMilliseconds) throws InterruptedException {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String currentSsid = "<unknown ssid>";
        int waited = 0;
        int step = 200;

        while (waited < timeoutMilliseconds && (currentSsid == null || currentSsid.equals("<unknown ssid>"))) {
            Thread.sleep(step);
            waited += step;
            wifiInfo = wifiManager.getConnectionInfo(); // <-- ponowne pobranie stanu
            currentSsid = wifiInfo.getSSID();
            Log.d(LOGTAG, "Waiting for SSID... currently: " + currentSsid);
            //TODO dodać sprawdzenie czy supplicantState ma odpowiedni status
            if (currentSsid.replace("\"", "").equals(expectedSsid)){
                return true;
            }
        }
        return false;
    }

}
