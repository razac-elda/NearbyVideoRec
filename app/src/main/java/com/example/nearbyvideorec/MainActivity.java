package com.example.nearbyvideorec;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    /*
     * Singleton implemented with enum, used to save UI elements status when switching fragments.
     * Each fragment uses this singleton to obtain own UI element status, only if needed.
     * Any element status that needs to be monitored during execution must be managed.
     */
    private SavedUIData savedUIData;
    // Select strategy for nearby connection
    private static final Strategy STRATEGY = Strategy.P2P_STAR;

    private Context context;
    private Context activity_context;
    private String SERVICE_ID;
    private HashMap<String, ConnectionInfo> endpoints_connected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Singleton created for the first and only time.
        savedUIData = SavedUIData.INSTANCE;
        BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_client, R.id.navigation_server, R.id.navigation_video)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);
        context = getApplicationContext();
        activity_context = MainActivity.this;
        SERVICE_ID = getPackageName();
        endpoints_connected = new HashMap<>();
    }

    public void requestConnect(String caller) {
        if (caller.equals("CLIENT")) {
            if (!savedUIData.getServer_status_switch()) {
                startDiscovery();
            } else {
                Log.d("Client/Server", "Cannot switch to discovery if you are the server!");
                savedUIData.setClient_status_switch(false);
                // TODO:Find a way to update the UI without switching fragment
            }
        } else {
            // Caller is SERVER
            if (!savedUIData.getClient_status_switch()) {
                startAdvertising();
            } else {
                savedUIData.setClient_status_switch(false);
                // TODO:Request to change the server
            }
        }
    }

    // TODO: Implement a better disconnection? Maybe clear data? Also update switch
    public void requestDisconnect(String caller) {
        if (caller.equals("CLIENT")) {
            Nearby.getConnectionsClient(context).stopDiscovery();
            savedUIData.setClient_status_switch(false);
        } else {
            // Caller is SERVER
            Nearby.getConnectionsClient(context).stopAdvertising();
            Nearby.getConnectionsClient(context).stopAllEndpoints();
            savedUIData.setServer_status_switch(false);
        }
    }

    private String getUserNickname() {
        return Build.MANUFACTURER.toUpperCase() + " " + Build.MODEL;
    }

    private void startAdvertising() {
        AdvertisingOptions advertisingOptions =
                new AdvertisingOptions.Builder().setStrategy(STRATEGY).build();
        Nearby.getConnectionsClient(context)
                .startAdvertising(
                        getUserNickname(), SERVICE_ID, connectionLifecycleCallback, advertisingOptions)
                .addOnSuccessListener(
                        (Void unused) -> {
                            // TODO:We're advertising!
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            // TODO:We were unable to start advertising.
                        });
    }

    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                ConnectionInfo temp_connectionInfo;
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    temp_connectionInfo = connectionInfo;
                    new AlertDialog.Builder(activity_context, R.style.Theme_ConnectionDialog)
                            .setTitle(getString(R.string.accept_connection_to) + " " + connectionInfo.getEndpointName())
                            .setMessage(getString(R.string.confirm_device_code) + " " + connectionInfo.getAuthenticationToken())
                            .setPositiveButton(
                                    android.R.string.yes,
                                    (DialogInterface dialog, int which) ->
                                            // The user confirmed, so we can accept the connection.
                                            Nearby.getConnectionsClient(context)
                                                    .acceptConnection(endpointId, payloadCallback))
                            .setNegativeButton(
                                    android.R.string.no,
                                    (DialogInterface dialog, int which) ->
                                            // The user canceled, so we should reject the connection.
                                            Nearby.getConnectionsClient(context).rejectConnection(endpointId))
                            .setIcon(R.drawable.ic_baseline_warning)
                            .show().setCanceledOnTouchOutside(false);

                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    switch (result.getStatus().getStatusCode()) {
                        case ConnectionsStatusCodes.STATUS_OK:
                            // TODO:We're connected! Can now start sending and receiving data.
                            endpoints_connected.put(endpointId, temp_connectionInfo);
                            break;
                        case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                            // TODO:The connection was rejected by one or both sides.
                            break;
                        case ConnectionsStatusCodes.STATUS_ERROR:
                            // TODO:The connection broke before it was able to be accepted.
                            break;
                        default:
                            // TODO:Unknown status code
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    /* TODO:We've been disconnected from this endpoint. No more data can be
                        sent or received. */
                    endpoints_connected.remove(endpointId);
                }
            };

    // TODO: Understand what this does and complete, called in onConnectionInitiated above
    private PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(@NonNull String s, @NonNull Payload payload) {

        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String s, @NonNull PayloadTransferUpdate payloadTransferUpdate) {

        }
    };

    private void startDiscovery() {
        DiscoveryOptions discoveryOptions =
                new DiscoveryOptions.Builder().setStrategy(STRATEGY).build();
        Nearby.getConnectionsClient(context)
                .startDiscovery(SERVICE_ID, endpointDiscoveryCallback, discoveryOptions)
                .addOnSuccessListener(
                        (Void unused) -> {
                            // TODO:We're discovering!
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            // TODO:We're unable to start discovering.
                        });
    }

    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                    // An endpoint was found. We request a connection to it.
                    Nearby.getConnectionsClient(context)
                            .requestConnection(getUserNickname(), endpointId, connectionLifecycleCallback)
                            .addOnSuccessListener(
                                    (Void unused) -> {
                                        /* TODO:We successfully requested a connection. Now both sides
                                            must accept before the connection is established. */
                                    })
                            .addOnFailureListener(
                                    (Exception e) -> {
                                        // TODO:Nearby Connections failed to request the connection.
                                    });
                }

                @Override
                public void onEndpointLost(String endpointId) {
                    // TODO:A previously discovered endpoint has gone away.
                }
            };


}