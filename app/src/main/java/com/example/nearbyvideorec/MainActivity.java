package com.example.nearbyvideorec;

import android.content.Context;
import android.content.DialogInterface;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import com.example.nearbyvideorec.ui.server.ServerFragment;
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

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private NavController navController;
    /*
     * Singleton implemented with enum, used to save UI elements status when switching fragments.
     * Each fragment uses this singleton to obtain own UI element status, only if needed.
     * Any element status that needs to be monitored during execution must be managed.
     */
    private SavedUIData savedUIData;
    // Select strategy for nearby connection
    private static final Strategy STRATEGY = Strategy.P2P_STAR;
    private String deviceRole;

    private Context context;
    private Context activity_context;
    private String SERVICE_ID;
    private Boolean legacy;
    private HashMap<String, ConnectionInfo> connectedEndpoints;
    private ArrayList<String> selectedVideoNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getApplicationContext();
        activity_context = MainActivity.this;
        SERVICE_ID = getPackageName();
        connectedEndpoints = new HashMap<>();

        setContentView(R.layout.activity_main);

        // Check camera API level
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        legacy = false;
        try {
            // Cycle through all cameras
            for (String cameraId : manager.getCameraIdList()) {

                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);

                // If back camera API support is LEGACY we mark it as "legacy" to avoid using Camera2 API
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    if (characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) ==
                            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
                        legacy = true;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        // Singleton created for the first and only time.
        savedUIData = SavedUIData.INSTANCE;
        BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_client, R.id.navigation_server, R.id.navigation_video)
                .build();

        // Later we use the navController to refresh the fragment
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);



    }

    public Boolean getLegacy() {
        return legacy;
    }

    public HashMap<String, ConnectionInfo> getConnectedEndpoints() {
        return connectedEndpoints;
    }

    public void requestConnect(String caller) {

        if (caller.equals("CLIENT")) {

            if (!savedUIData.getServer_status_switch()) {
                deviceRole = "Client";
                startDiscovery();
            } else {

                new AlertDialog.Builder(activity_context, R.style.Theme_ConnectionDialog)
                        .setTitle(getString(R.string.error))
                        .setMessage(getString(R.string.error_discovery_server))
                        .setPositiveButton(getString(R.string.ok), null)
                        .setIcon(R.drawable.ic_baseline_warning)
                        .show();
                savedUIData.setClient_status_switch(false);
            }
            navController.navigate(R.id.navigation_client);

        } else {

            // Caller is SERVER
            if (!savedUIData.getClient_status_switch()) {
                deviceRole = "Server";
                startAdvertising();
            } else {
                // Caller is client and want to become a Server
                String endpointId = null;

                // Get Server ID, only one entry on connectedEndpoints when acting as Client
                for (String endpoint : connectedEndpoints.keySet())
                    endpointId = endpoint;

                /*
                 * TODO:Only working with two devices, need to update for multi device.
                 * Idea:Send to other devices a request to switch off-on discovery without
                 * disconnecting. Maybe send new Server endpointId and connect with requestConnection
                 * Unknown:Connection needs to authenticate again?
                 */
                sendMessage(endpointId, "Change");
                startAdvertising();
                Nearby.getConnectionsClient(context).stopDiscovery();
                savedUIData.setClient_status_switch(false);
                savedUIData.setServer_status_switch(true);
            }
            navController.navigate(R.id.navigation_server);
        }
    }

    public void requestDisconnect(String caller) {

        if (caller.equals("CLIENT")) {

            Nearby.getConnectionsClient(context).stopDiscovery();
            savedUIData.setClient_status_switch(false);
            navController.navigate(R.id.navigation_client);
        } else {

            // Caller is SERVER
            Nearby.getConnectionsClient(context).stopAdvertising();
            savedUIData.setServer_status_switch(false);
            navController.navigate(R.id.navigation_server);
        }

        // Disconnects from, and removes all traces of, all connected and/or discovered endpoints
        Nearby.getConnectionsClient(context).stopAllEndpoints();
        // Clear connected endpoints
        connectedEndpoints.clear();
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
                            // We're advertising!
                            Toast.makeText(activity_context, getString(R.string.advertising), Toast.LENGTH_SHORT).show();
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            // We were unable to start advertising.
                        });
    }

    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                ConnectionInfo temp_connectionInfo;

                @Override
                public void onConnectionInitiated(@NotNull String endpointId, ConnectionInfo connectionInfo) {
                    temp_connectionInfo = connectionInfo;
                    new AlertDialog.Builder(activity_context, R.style.Theme_ConnectionDialog)
                            .setTitle(getString(R.string.accept_connection_to) + " " + connectionInfo.getEndpointName() + "?")
                            .setMessage(getString(R.string.confirm_device_code) + " " + connectionInfo.getAuthenticationToken())
                            .setPositiveButton(
                                    getString(R.string.ok),
                                    (DialogInterface dialog, int which) ->
                                            // The user confirmed, so we can accept the connection.
                                            Nearby.getConnectionsClient(context)
                                                    .acceptConnection(endpointId, payloadCallback))
                            .setNegativeButton(
                                    getString(R.string.cancel),
                                    (DialogInterface dialog, int which) ->
                                            // The user canceled, so we should reject the connection.
                                            Nearby.getConnectionsClient(context).rejectConnection(endpointId))
                            .setIcon(R.drawable.ic_baseline_warning)
                            .show().setCanceledOnTouchOutside(false);

                }

                @Override
                public void onConnectionResult(@NotNull String endpointId, ConnectionResolution result) {

                    switch (result.getStatus().getStatusCode()) {

                        case ConnectionsStatusCodes.STATUS_OK:
                            // We're connected! Can now start sending and receiving data.
                            // Save new connected endpoint
                            connectedEndpoints.put(endpointId, temp_connectionInfo);
                            // Refresh fragment
                            if (deviceRole.equals("Client"))
                                navController.navigate(R.id.navigation_client);
                            else
                                navController.navigate(R.id.navigation_server);

                            Toast.makeText(activity_context, getString(R.string.connected_to) +
                                    " " + temp_connectionInfo.getEndpointName(), Toast.LENGTH_LONG).show();
                            break;

                        case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                            // The connection was rejected by one or both sides.

                            Toast.makeText(activity_context, getString(R.string.connection_rejected), Toast.LENGTH_SHORT).show();
                            break;

                        case ConnectionsStatusCodes.STATUS_ERROR:
                            // The connection broke before it was able to be accepted.

                            Toast.makeText(activity_context, getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
                            break;

                        default:
                            // Unknown status code
                    }
                }

                @Override
                public void onDisconnected(@NotNull String endpointId) {
                    /* We've been disconnected from this endpoint. No more data can be
                        sent or received. */
                    // Retrieve old endpoint info
                    ConnectionInfo endpointInfo = connectedEndpoints.get(endpointId);

                    if (endpointInfo != null) {
                        Toast.makeText(activity_context, getString(R.string.device_disconnected)
                                + " " + endpointInfo.getEndpointName(), Toast.LENGTH_LONG).show();
                        // Remove old endpoint
                        connectedEndpoints.remove(endpointId);
                        // Refresh fragments
                        if (deviceRole.equals("Client"))
                            navController.navigate(R.id.navigation_client);
                        else
                            navController.navigate(R.id.navigation_server);
                    }
                }

            };

    public void sendMessage(String endpointId, String msg) {

        if (endpointId != null) {
            // Convert the message to Bytes
            byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
            // Generate payload and send it to destination
            Payload bytesPayload = Payload.fromBytes(bytes);
            Nearby.getConnectionsClient(context).sendPayload(endpointId, bytesPayload);
        }
    }

    private PayloadCallback payloadCallback = new PayloadCallback() {

        @Override
        public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {

            if (payload.getType() == Payload.Type.BYTES) {
                // Convert the payload from Bytes to a String
                String msg = new String(payload.asBytes(), StandardCharsets.UTF_8);
                switch (msg) {

                    case "Change":
                        // Request to change Server
                        /*
                         * TODO:Only working with two devices, need to update for multi device.
                         * Idea:Send to other devices a request to switch off-on discovery without
                         * disconnecting. Maybe send new Server endpointId and connect with requestConnection
                         * Unknown:Connection needs to authenticate again?
                         */
                        startDiscovery();
                        Nearby.getConnectionsClient(context).stopAdvertising();
                        savedUIData.setClient_status_switch(true);
                        savedUIData.setServer_status_switch(false);
                        navController.navigate(R.id.navigation_client);
                        break;

                    default:
                        Toast.makeText(activity_context, msg, Toast.LENGTH_LONG).show();
                        break;
                }
            }
        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate payloadTransferUpdate) {
            // Used with files payload to keep tracking of the transfer
        }
    };

    private void startDiscovery() {

        DiscoveryOptions discoveryOptions =
                new DiscoveryOptions.Builder().setStrategy(STRATEGY).build();

        Nearby.getConnectionsClient(context)
                .startDiscovery(SERVICE_ID, endpointDiscoveryCallback, discoveryOptions)
                .addOnSuccessListener(
                        (Void unused) -> {
                            // We're discovering!
                            Toast.makeText(activity_context, getString(R.string.discovering), Toast.LENGTH_SHORT).show();
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            // We're unable to start discovering.
                        });
    }

    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {

                @Override
                public void onEndpointFound(@NotNull String endpointId, @NotNull DiscoveredEndpointInfo info) {

                    // An endpoint was found. We request a connection to it.
                    Nearby.getConnectionsClient(context)
                            .requestConnection(getUserNickname(), endpointId, connectionLifecycleCallback)
                            .addOnSuccessListener(
                                    (Void unused) -> {
                                        /* We successfully requested a connection. Now both sides
                                            must accept before the connection is established. */
                                    })
                            .addOnFailureListener(
                                    (Exception e) -> {
                                        // Nearby Connections failed to request the connection.
                                    });
                }

                @Override
                public void onEndpointLost(@NotNull String endpointId) {
                    // A previously discovered endpoint has gone away.
                }
            };



    private void setSelectedVideoNames(){


    }
}