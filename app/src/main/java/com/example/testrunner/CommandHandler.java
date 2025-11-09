package com.example.testrunner;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextPaint;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

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
        this.wifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);
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
                case "DELAY":
                    delayResponse(commandID);
                    break;
                case "WIFI_CLEAR_ALL":
                    removeAllNetworks(commandID);
                    break;
                case "ENABLE_WIFI":
                    switchWifi(commandID, true);
                    break;
                case "DISABLE_WIFI":
                    switchWifi(commandID, false);
                    break;
                case "CLOSE_CONNECTION":
                    closeClientConnection();
                    break;
                case "WIFI_ADD_NETWORK":
                    addWifiNetworkConfig(commandObj);
                    break;
                case "WIFI_REMOVE_NETWORK":
                    removeWifiNetworkConfig(commandObj);
                    break;
                case "WIFI_CONNECT":
                    wifiConnect(commandObj);
                    break;
                case "WIFI_CHECK_SUPPLICANT_STATE":
                    wifiCheckSupplicantState(commandObj);
                    break;
                case "WAIT_FOR_SUPPLICANT_STATE":
                    waitForSupplicantState(commandObj);
                    break;
                default:
                    unknownCommandResponse(commandID, commandToHandle);


            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void waitForSupplicantState(JSONObject commandObj) {
        int commandID = commandObj.optInt("Command_ID", -1);
        String expectedStateStr;
        int timeoutSeconds = commandObj.optInt("Timeout", 10); // default 10s
        int timeoutMiliseconds = timeoutSeconds * 1000;

        try {
            expectedStateStr = commandObj.getString("Expected_state");
        } catch (JSONException e) {
            serverSocket.sendAck(commandID, ERROR_RESULT, "Missing 'Expected_state' in command!");
            return;
        }

        SupplicantState expectedState;
        try {
            expectedState = SupplicantState.valueOf(expectedStateStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            serverSocket.sendAck(commandID, ERROR_RESULT, "Unknown SupplicantState: " + expectedStateStr);
            return;
        }

        long startTime = System.currentTimeMillis();
        long checkInterval = 250; // ms

        Log.d(LOGTAG, "Waiting for supplicant to reach state: " + expectedState.name());

        while (System.currentTimeMillis() - startTime < timeoutMiliseconds) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            SupplicantState currentState = wifiInfo.getSupplicantState();

            if (currentState == expectedState) {
                Log.d(LOGTAG, "Supplicant state: " + currentState.name() + ", Expected state: " + expectedState.name());
                serverSocket.sendAck(commandID, COMPLETE_RESULT, "Supplicant state: " + currentState.name() + ", Expected state: " + expectedState.name());
                return;
            }

            try {
                Thread.sleep(checkInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                serverSocket.sendAck(commandID, ERROR_RESULT, "Interrupted while waiting for supplicant state");
                return;
            }
        }

        // Timeout
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        SupplicantState finalState = wifiInfo.getSupplicantState();
        Log.w(LOGTAG, "Timeout waiting for supplicant to reach " + expectedState.name() + " (final state: " + finalState.name() + ")");
        serverSocket.sendAck(commandID, ERROR_RESULT, "Timeout waiting for supplicant to reach " + expectedState.name() + " (final: " + finalState.name() + ")");
    }


    private void delayResponse(int commandID) {
        Log.d(LOGTAG, "Received delay command, waiting for next commands...");
        serverSocket.sendAck(commandID, COMPLETE_RESULT, "Received delay command, waiting for next commands...");
    }

    private void unknownCommandResponse(int commandID, String commandToHandle) {
        Log.e(LOGTAG, "Received unknown command: " + commandToHandle);
        serverSocket.sendAck(commandID, ERROR_RESULT, "Received unknown command: " + commandToHandle);
    }

    //NOT WORKING
    private void removeAllNetworks(int commandID) {

        if (wifiManager == null) {
            Log.e(LOGTAG, "WifiManager is null — cannot clear configured networks!");
            serverSocket.sendAck(commandID, ERROR_RESULT, "WifiManager unavailable — cannot clear networks.");
            return;
        }
        //If Wifi is disabled turn it ON for a while
        boolean temporarilyEnabled = false;
        if (!wifiManager.isWifiEnabled()) {
            Log.d(LOGTAG, "Wi-Fi disabled. Enabling temporarily to clear configs...");
            wifiManager.setWifiEnabled(true);

            if (!waitForWifiStateChange(WifiManager.WIFI_STATE_ENABLED, 5000)) {
                Log.e(LOGTAG, "Unable to enable Wi-Fi to clear configurations!");
                serverSocket.sendAck(commandID, ERROR_RESULT, "Wi-Fi couldn't be enabled — clearing configs aborted.");
                return;
            }
            temporarilyEnabled = true;
        }
        // Collect the list of saved networks
        @SuppressLint("MissingPermission")
        List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
        if (configuredNetworks == null || configuredNetworks.isEmpty()) {
            Log.d(LOGTAG, "No configured Wi-Fi networks found — nothing to clear.");
            serverSocket.sendAck(commandID, COMPLETE_RESULT, "No Wi-Fi networks found — nothing to remove.");
        } else {
            int removedCount = 0;
            for (WifiConfiguration config : configuredNetworks) {
                if (config != null && config.networkId != -1) {
                    Log.d(LOGTAG, "Removing network: " + config.SSID + " (id=" + config.networkId + ")");
                    boolean result = wifiManager.removeNetwork(config.networkId);
                    if (result) removedCount++;
                }
            }
            wifiManager.saveConfiguration(); //save config
            Log.d(LOGTAG, "Cleared " + removedCount + " Wi-Fi configurations.");
            serverSocket.sendAck(commandID, COMPLETE_RESULT, "Cleared " + removedCount + " Wi-Fi configurations.");
        }

        // Jeśli Wi-Fi było wcześniej wyłączone, przywróć stan
        if (temporarilyEnabled) {
            Log.d(LOGTAG, "Restoring previous Wi-Fi state (disabling)...");
            wifiManager.setWifiEnabled(false);
        }
    }

    private void removeWifiNetworkConfig(JSONObject jsonCommand) {
        try {
            int netIdToRemove = getNetId(jsonCommand.getString("SSID"));
            Log.d(LOGTAG, "netID to remove: " + netIdToRemove);
            int commandID = Integer.parseInt(jsonCommand.getString("Command_ID"));
            String ssid = jsonCommand.getString("SSID");
            if (netIdToRemove < 0){
                //Check if netId found in current configurations, netIdToRemove should be highher than -1
                Log.e(LOGTAG, "Unable to find Wi-Fi network profile. Configuration may not exist!");
                serverSocket.sendAck(commandID, ERROR_RESULT, "Unable to remove Wi-Fi network profile: " + ssid + " SSID may be incorrect or configuration not exist");
                return;
            }
            if (wifiManager.removeNetwork(netIdToRemove)){
                Log.d(LOGTAG, "Network profile successfully removed: " + ssid);
                serverSocket.sendAck(commandID, COMPLETE_RESULT, "Network profile successfully removed: " + ssid);
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void addWifiNetworkConfig(JSONObject jsonCommand) {
        try {
            int commandID = Integer.parseInt(jsonCommand.getString("Command_ID"));
            String ssid = jsonCommand.optString("SSID", null);
            String securityType = jsonCommand.optString("SECURITY_TYPE", "OPEN");
            String password = jsonCommand.optString("PWD", null);

            // Check if command contain SSID at all
            if (ssid == null){
                Log.e(LOGTAG, "Command " + jsonCommand.getString("Command") + " does not contain any network name(SSID)");
                serverSocket.sendAck(commandID, ERROR_RESULT, "Command " + jsonCommand.getString("Command") + " requires SSID name provided!");
            } else {
                //Check if network is already saved
                if (getNetId(ssid)>0){
                    Log.d(LOGTAG, "Network is already saved. Proceeding with next test steps...");
                    serverSocket.sendAck(commandID, COMPLETE_RESULT, "Network " + ssid + " is already saved");
                    return;
                }

                // Add SSID to the wificonfig
                WifiConfiguration wifiConfig = new WifiConfiguration();
                wifiConfig.SSID = "\"" + ssid + "\"";
                Log.d(LOGTAG, "SSID provided for WIFI_ADD_NETWORK command. Proceeding with adding the network configuration");
                serverSocket.sendMessage("SSID provided for WIFI_ADD_NETWORK command. Proceeding with adding the network configuration");
                //Check security type
                switch (securityType.toUpperCase()) {
                    case "WPA":
                    case "WPA2":
                        if (password == null) {
                            serverSocket.sendAck(commandID, ERROR_RESULT, "WPA/WPA2 secured network requires password to be provided!");
                            return;
                        }
                        wifiConfig.preSharedKey = "\"" + password + "\"";

                        break;
                    case "OPEN":
                        wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                        break;
                    default:
                        serverSocket.sendAck(commandID, ERROR_RESULT, "Unknown security type: " + securityType);
                        return;
                }
                int netId = wifiManager.addNetwork(wifiConfig);
                Log.d(LOGTAG, "NetID assigned to network \"" + ssid+ "\" after addNetwork: " + netId);
                Log.d(LOGTAG, "Connection info after addNetwork: " + wifiManager.getConnectionInfo());
                if (netId < 0){
                    //Check if addNetwork succeded, netId should be highher than -1
                    Log.e(LOGTAG, "Unable to add Wi-Fi network profile. Configuration may be incorrect.");
                    serverSocket.sendAck(commandID, ERROR_RESULT, "Unable to add Wi-Fi network profile: " + ssid + " Configuration may be incorrect.");
                } else {
                    Log.d(LOGTAG, "Network profile successfully added: " + ssid);
                    serverSocket.sendAck(commandID, COMPLETE_RESULT, "Network profile successfully added: " + ssid);
                }
            }
            //TODO dodać obslugę próby dopisania sieci przy wyłączonym wifi, if wifiManager == null?
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
    private void wifiConnect(JSONObject jsonCommand) {
        int netIDToConnect;
        try {
            int commandID = Integer.parseInt(jsonCommand.getString("Command_ID"));
            String ssid = jsonCommand.optString("SSID", null);
            // Check if command contain SSID at all
            if (ssid == null){
                Log.e(LOGTAG, "Command " + jsonCommand.getString("Command") + " does not contain any network name(SSID)");
                serverSocket.sendAck(commandID, ERROR_RESULT, "Command " + jsonCommand.getString("Command") + " requires SSID name provided!");
                return;
            }
//            //Check if DUT is connected currently to the expected network
//            if(isConnectedToSsid(ssid, 5000)){
//                Log.d(LOGTAG, "Already connected to the network: " + ssid + ". Proceeding with next steps...");
//                serverSocket.sendAck(commandID, COMPLETE_RESULT, "Already connected to the network: " + ssid + ". Proceeding with next steps...");
//            }

            netIDToConnect = getNetId(ssid);
            //Check if netID found, if not then netID would stay with value -1
            if (netIDToConnect < 0){
                Log.e(LOGTAG, "WifiConfiguration for SSID " + ssid + " not found!");
//                Log.d(LOGTAG, "Net configs: " + wifiManager.getConnectionInfo())
                serverSocket.sendAck(commandID, ERROR_RESULT, "WifiConfiguration for SSID " + ssid + " not found! Action " + jsonCommand.getString("Command") + " requires WIFI-ADD_NETWORK to be used before");
                return;
            }
            //wifiManager.disconnect();
            //Try to enable network and connect
            if (wifiManager.enableNetwork(netIDToConnect, true)) {
                Log.d(LOGTAG, "Connection info after enableNetwork: " + wifiManager.getConnectionInfo());
                Log.d(LOGTAG, "Network " + ssid + " enabled.");
            } else {
                Log.d(LOGTAG, "Error when trying to enable network");
                serverSocket.sendAck(commandID, ERROR_RESULT, "Error when trying to enable network: " + ssid);
            }
            //Wait for Wi-Fi to be connected
            if (isConnectedToSsid(ssid, 6000)){
                Log.d(LOGTAG, "Connected with network: " + ssid);
                serverSocket.sendAck(commandID, COMPLETE_RESULT, "Connected with network: " + ssid);
            } else {
                Log.d(LOGTAG, "Timeout when trying to connect network!");
                serverSocket.sendAck(commandID, ERROR_RESULT, "TImeout when trying to run commnand " + jsonCommand.getString("Command"));
            }
            //debug prints
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            Log.d(LOGTAG, String.valueOf(wifiInfo.getSupplicantState()));

        } catch (JSONException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    private int getNetId(String ssid) {
        int retries = 20;
        int delayMs = 250;

        for (int i = 0; i < retries; i++) {
            @SuppressLint("MissingPermission")
            List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
            if (configuredNetworks != null) {
                for (WifiConfiguration wifiConfiguration : configuredNetworks) {
                    if (wifiConfiguration.SSID != null) {
                        String configuredSsid = wifiConfiguration.SSID.replace("\"", "");
                        if (configuredSsid.equals(ssid)) {
                            Log.d(LOGTAG, "Found netId for " + ssid + ": " + wifiConfiguration.networkId);
                            return wifiConfiguration.networkId;
                        }
                    }
                }
            }
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException ignored) {}
        }

        Log.w(LOGTAG, "NetId not found for SSID: " + ssid + " after retries.");
        return -1;
    }
//    private int getNetId(String ssid) {
//        //Check if any configured networks found
//        @SuppressLint("MissingPermission") List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
//        if (configuredNetworks == null){
//            Log.e(LOGTAG, "No configured Wi-Fi networks found!");
//            serverSocket.sendMessage("No configured networks found!");
//            return -1;
//        }
//        //Looking for netID of provided SSID
//        int netId = -1;
//        for (WifiConfiguration wifiConfiguration : configuredNetworks){
//            if (wifiConfiguration.SSID != null){
//                String configuredSsid = wifiConfiguration.SSID.replace("\"", "");
//                System.out.println("TestRunner: " + wifiConfiguration);
//                if (configuredSsid.equals(ssid)){
//                    netId = wifiConfiguration.networkId;
//                }
//            }
//        }
//        return netId;
//    }

    private void closeClientConnection() {
        serverSocket.shutdown();
    }
    private void switchWifi(int commandID, boolean switchWifi) {
        int timeout = 5000;

        if (wifiManager == null) {
            Log.e(LOGTAG, "WifiManager is null! Cannot change Wi-Fi state. Possible reasons: context problem or device doesn't support Wi-Fi.");
            serverSocket.sendMessage("Error: Wi-Fi manager unavailable. Please check device capabilities or app permissions.");
            serverSocket.sendAck(commandID, ERROR_RESULT, "Wi-Fi manager is null!");
            return;
        }

        String action = switchWifi ? "enable" : "disable";
        Log.d(LOGTAG, "Current Wi-Fi state: " + parseWifiState(wifiManager.getWifiState()));
        wifiManager.setWifiEnabled(switchWifi);

        int targetState = switchWifi ? WifiManager.WIFI_STATE_ENABLED : WifiManager.WIFI_STATE_DISABLED;

        if (waitForWifiStateChange(targetState, timeout)) {
            String successMessage = switchWifi ? "Wi-Fi enabled successfully" : "Wi-Fi disabled successfully";
            Log.d(LOGTAG, successMessage);
            serverSocket.sendMessage(successMessage);
            serverSocket.sendAck(commandID, COMPLETE_RESULT, "Wi-Fi status: " + parseWifiState(wifiManager.getWifiState()));
        } else {
            String timeoutMessage = "Timeout while trying to " + action + " Wi-Fi";
            Log.w(LOGTAG, timeoutMessage);
            serverSocket.sendMessage("Warning: " + timeoutMessage);
            serverSocket.sendAck(commandID, ERROR_RESULT, timeoutMessage);
        }
    }
    /**
     * Checking the Wi-Fi state every checkingInterval time until it is changed to the
     * expectedState or timeoutMilliseconds time elapsed
     *
     * @param expectedState expected Wi-Fi state
     * @param timeoutMilliseconds timeout time
     * @return true if expectedState is reached, false if timeout time elapsed
     */
    private void wifiCheckSupplicantState(JSONObject commandObj) {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        SupplicantState supplicantstate = wifiInfo.getSupplicantState();
        int commandID = commandObj.optInt("Command_ID", -1);
        try {
            String expectedState = commandObj.getString("Expected_state");
            SupplicantState expected = SupplicantState.valueOf(expectedState.toUpperCase());
            if (supplicantstate.equals(expected)){
                Log.d(LOGTAG, "Current supplicant state: " + supplicantstate.name() + ", Expected state: " + expectedState.toUpperCase());
                serverSocket.sendAck(commandID, COMPLETE_RESULT, "Current supplicant state: " + supplicantstate.name() + ", Expected state: " + expectedState.toUpperCase());
            } else {
                Log.e(LOGTAG, "Current supplicant state: " + supplicantstate.name() + ", Expected state: " + expectedState.toUpperCase());
                serverSocket.sendAck(commandID, ERROR_RESULT, "Current supplicant state: " + supplicantstate.name() + ", Expected state: " + expectedState.toUpperCase());
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
            serverSocket.sendAck(commandID, "ERROR", "Unknown SupplicantState value: " + e.getMessage());
        }
    }
    private boolean waitForWifiStateChange(int expectedState, int timeoutMilliseconds){
        //TODO Zmiana logiki sprawdzania stanu wifi -> BroadcastReceiver?
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
    private String parseWifiState(int state){
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
    private String parseSupplicantState(int state){
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
    private boolean isConnectedToSsid(String expectedSsid, int timeoutMilliseconds) throws InterruptedException {
        int waitTime = 0;
        int step = 200;

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String currentSsid = wifiInfo.getSSID();
        SupplicantState supplicantState = wifiInfo.getSupplicantState();
        //Skip if already connected to the expected SSID
        if (currentSsid.equals(expectedSsid)){
            Log.d(LOGTAG, "Already connected to: " + currentSsid + ", Supplicant state: " + supplicantState);
            return true;
        }
        while (waitTime < timeoutMilliseconds && (currentSsid == null || currentSsid.equals("<unknown ssid>"))) {
            Thread.sleep(step);
            waitTime += step;
            wifiInfo = wifiManager.getConnectionInfo(); // <-- ponowne pobranie stanu
            currentSsid = wifiInfo.getSSID();
            Log.d(LOGTAG, "Waiting for SSID... currently: " + currentSsid + ", Supplicant state: " + supplicantState);
            //TODO dodać sprawdzenie czy supplicantState ma odpowiedni status
            if (currentSsid.replace("\"", "").equals(expectedSsid)){
                return true;
            }
        }
        Log.w(LOGTAG, "Timeout while waiting for connect to SSID: " + expectedSsid);
        return false;
    }
}
