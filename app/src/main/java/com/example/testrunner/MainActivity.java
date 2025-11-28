package com.example.testrunner;

import android.Manifest;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ToggleButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private static final int PORT = 5557;
    private MessageSender messageSender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Grant required permissions
        //TODO Zaimplementować algorytm nadawania DO po instalacji aplikacji przed jej uruchomieniem,
        // inaczej poniżwsze uprawnienia nie zadziałają (brak DO w momencie uruchamiania kodu)
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminComponent = new ComponentName(this, MyDeviceAdminReceiver.class);
        dpm.setPermissionGrantState(adminComponent, getPackageName(), Manifest.permission.ACCESS_FINE_LOCATION, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
        dpm.setPermissionGrantState(adminComponent, getPackageName(), Manifest.permission.ACCESS_COARSE_LOCATION, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);

        // Enable location - required for some Wi-Fi actions
        dpm.setLocationEnabled(adminComponent, true);

        EdgeToEdge.enable(this);
        setContentView(com.example.testrunner.R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        TRServerSocket serverSocket = new TRServerSocket(PORT, getApplicationContext());
        serverSocket.start();

        //TODO zaimplementować zmianę stanu przycisków gdy wifi jest włączane/połączone z siecią - interfejs?
        ToggleButton wifiIndicator = findViewById(R.id.wifiToggle);
        ToggleButton wifiStateIndicator = findViewById(R.id.wifiStatusToggle);
//        Button button = findViewById(R.id.button);
//        button.setOnClickListener(new View.OnClickListener() {
//                                      public void onClick(View v) {
//                                          //DO SOMETHING! {RUN SOME FUNCTION ... DO CHECKS... ETC}
//                                      }
//                                  });


//        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
//        ComponentName adminComponent = new ComponentName(this, MyDeviceAdminReceiver.class);
//
//        if (dpm.isAdminActive(adminComponent)) {
//            dpm.clearDeviceOwnerApp(getPackageName());
//        }
    }
}