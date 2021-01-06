package it.snasaunive.nearbyvideorec;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.collection.SimpleArrayMap;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

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

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

import it.snasaunive.nearbyvideorec.ui.CameraPreview;

public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private BottomNavigationView navView;
    /*
     * Singleton implemented with enum, used to save UI elements status when switching fragments.
     * Each fragment uses this singleton to obtain own UI element status, only if needed.
     * Any element status that needs to be monitored during execution must be managed.
     */
    private SavedUIData savedUIData;
    // Select strategy for nearby connection.
    private static final Strategy STRATEGY = Strategy.P2P_STAR;
    private String deviceRole;

    private Context context;
    private Context activity_context;
    private String SERVICE_ID;
    private HashMap<String, ConnectionInfo> connectedEndpoints;
    private ArrayList<String> fileNames;
    private ArrayList<String> inputFiles;

    private CameraPreview cameraPreview;

    private final Integer REQUEST_CODE_BY_INTENT_FILE_CHOOSER = 2048;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getApplicationContext();
        activity_context = MainActivity.this;
        SERVICE_ID = getPackageName();
        connectedEndpoints = new HashMap<>();
        fileNames = new ArrayList<>();
        cameraPreview = null;
        inputFiles = new ArrayList<>();

        setContentView(R.layout.activity_main);

        // Singleton created for the first and only time.
        savedUIData = SavedUIData.INSTANCE;

        navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_client, R.id.navigation_server, R.id.navigation_video, R.id.navigation_preview)
                .build();
        // Later we use the navController to refresh the fragment.
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);


    }

    // When client/server switch is turned on.
    public void requestConnect(String caller) {

        if (caller.equals("CLIENT")) {
            // Caller is CLIENT, check if it's also active as server.
            if (!savedUIData.getServer_status_switch()) {
                deviceRole = "Client";
                startDiscovery();
                savedUIData.setClient_status_switch(true);
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
            if (savedUIData.getClient_status_switch()) {
                // Caller is client and want to become a Server.
                String endpointId = null;

                // Get Server ID, only one entry on connectedEndpoints when acting as Client.
                for (String endpoint : connectedEndpoints.keySet())
                    endpointId = endpoint;
                sendMessage(endpointId, "swap_client_server");

            } else {
                startAdvertising();
                savedUIData.setServer_status_switch(true);
                deviceRole = "Server";
                navController.navigate(R.id.navigation_server);
            }
        }
    }

    // When client/server switch is turned off.
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

        // Disconnects from, and removes all traces of, all connected and/or discovered endpoints.
        Nearby.getConnectionsClient(context).stopAllEndpoints();
        // Clear connected endpoints
        connectedEndpoints.clear();
    }

    // Called to clear selected files related data for merging.
    public void clearFilesPath() {
        inputFiles.clear();
        fileNames.clear();
        navController.navigate(R.id.navigation_video);
    }

    public void sendMessage(String endpointId, String msg) {

        if (endpointId != null) {
            // Convert the message to Bytes
            byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
            // Generate payload and send it to destination.
            Payload bytesPayload = Payload.fromBytes(bytes);
            Nearby.getConnectionsClient(context).sendPayload(endpointId, bytesPayload);
        }
    }

    // Smartphone manufacturer and model
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
                String temp_endpointId;

                @Override
                public void onConnectionInitiated(@NotNull String endpointId, ConnectionInfo connectionInfo) {
                    temp_connectionInfo = connectionInfo;
                    temp_endpointId = endpointId;

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
                            // Save new connected endpoint.
                            connectedEndpoints.put(temp_endpointId, temp_connectionInfo);

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

                            Toast.makeText(activity_context, getString(R.string.connection_rejected),
                                    Toast.LENGTH_SHORT).show();
                            break;

                        case ConnectionsStatusCodes.STATUS_ERROR:
                            // The connection broke before it was able to be accepted.

                            Toast.makeText(activity_context, getString(R.string.connection_error),
                                    Toast.LENGTH_SHORT).show();
                            break;

                        default:
                            // Unknown status code.
                    }

                    temp_connectionInfo = null;
                    temp_endpointId = null;
                }

                @Override
                public void onDisconnected(@NotNull String endpointId) {
                    /* We've been disconnected from this endpoint. No more data can be
                        sent or received. */
                    // Retrieve old endpoint info.
                    ConnectionInfo endpointInfo = connectedEndpoints.get(endpointId);

                    if (endpointInfo != null) {
                        Toast.makeText(activity_context, getString(R.string.device_disconnected)
                                + " " + endpointInfo.getEndpointName(), Toast.LENGTH_LONG).show();
                        // Remove old endpoint.
                        connectedEndpoints.remove(endpointId);
                        if (endpointInfo.getEndpointName().equals(savedUIData.getRecording_device()))
                            savedUIData.setRecording(false);
                        // Refresh fragments.
                        if (deviceRole.equals("Client"))
                            navController.navigate(R.id.navigation_client);
                        else
                            navController.navigate(R.id.navigation_server);
                    }
                }

            };

    private PayloadCallback payloadCallback = new PayloadCallback() {

        private SimpleArrayMap<Long, Payload> incomingFilePayloads = new SimpleArrayMap<>();
        private SimpleArrayMap<Long, Payload> completedFilePayloads = new SimpleArrayMap<>();
        private SimpleArrayMap<Long, String> filePayloadFilenames = new SimpleArrayMap<>();

        @Override
        public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {

            if (payload.getType() == Payload.Type.BYTES) {
                // Convert the payload from Bytes to a String.
                String msg = new String(payload.asBytes(), StandardCharsets.UTF_8);
                String[] parts = msg.split(":");
                if (parts[0].equals("filename")) {
                    msg = parts[0];
                }
                switch (msg) {

                    case "swap_client_server":
                        // Request to change Server.
                        if (!savedUIData.getRecording()) {
                            sendMessage(endpointId, "allow_swap");
                            for (String endpoint : connectedEndpoints.keySet()) {
                                if (!endpoint.equals(endpointId)) {
                                    sendMessage(endpoint, "change_server");
                                }
                            }

                            savedUIData.setClient_status_switch(true);
                            savedUIData.setServer_status_switch(false);

                            requestDisconnect("SERVER");
                            requestConnect("CLIENT");
                        } else {
                            sendMessage(endpointId, "deny_swap");
                        }
                        break;

                    case "allow_swap":
                        requestDisconnect("CLIENT");
                        savedUIData.setClient_status_switch(false);
                        requestConnect("SERVER");
                        break;

                    case "deny_swap":
                        savedUIData.setServer_status_switch(false);
                        Toast.makeText(activity_context, getString(R.string.recording_ongoing), Toast.LENGTH_LONG).show();
                        break;

                    case "change_server":
                        requestDisconnect("CLIENT");
                        requestConnect("CLIENT");
                        break;

                    case "start_rec":
                        navController.navigate(R.id.navigation_preview);
                        navView.setVisibility(View.INVISIBLE);

                        // Delay to allow device to show the new loaded fragment and then take the loaded fragment
                        // reference.
                        final Handler starterHandler = new Handler();
                        starterHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                NavHostFragment navHostFragment =
                                        (NavHostFragment) getSupportFragmentManager().
                                                findFragmentById(R.id.nav_host_fragment);
                                cameraPreview = (CameraPreview) navHostFragment.
                                        getChildFragmentManager().getFragments().get(0);
                                if (cameraPreview != null)
                                    cameraPreview.startRec();
                            }
                        }, 1000);
                        break;

                    case "stop_rec":
                        if (cameraPreview != null)
                            cameraPreview.stopRec();
                        navController.navigate(R.id.navigation_client);
                        navView.setVisibility(View.VISIBLE);
                        sendRecording(endpointId);
                        break;

                    case "filename":

                        long payloadId = Long.parseLong(parts[1]);
                        String filename = "video_" + parts[2] + ".mp4";
                        filePayloadFilenames.put(payloadId, filename);
                        processFilePayload(payload.getId());
                        break;

                    default:
                        Toast.makeText(activity_context, msg, Toast.LENGTH_LONG).show();
                        break;
                }
            } else {
                if (payload.getType() == Payload.Type.FILE)
                    incomingFilePayloads.put(payload.getId(), payload);
            }
        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String endpointId,
                                            @NonNull PayloadTransferUpdate payloadTransferUpdate) {
            // Used with files payload to keep tracking of the transfer
            if (payloadTransferUpdate.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                long payloadId = payloadTransferUpdate.getPayloadId();
                Payload payload = incomingFilePayloads.remove(payloadId);
                if (payload != null) {
                    completedFilePayloads.put(payloadId, payload);
                    if (payload.getType() == Payload.Type.FILE) {
                        processFilePayload(payloadId);
                    }
                }
            }
        }

        private void processFilePayload(long payloadId) {
            Payload filePayload = completedFilePayloads.get(payloadId);
            String filename = filePayloadFilenames.get(payloadId);
            if (filePayload != null && filename != null) {
                completedFilePayloads.remove(payloadId);
                filePayloadFilenames.remove(payloadId);

                File payloadFile = filePayload.asFile().asJavaFile();
                payloadFile.renameTo(new File(payloadFile.getParentFile(), filename));
            }
        }
    };

    private void sendRecording(String endpointId){
        Uri uri = Utils.getUriSavedVideo();
        Payload videoPayload = null;

        try {
            ParcelFileDescriptor video = getContentResolver().openFileDescriptor(uri, "r");
            videoPayload = Payload.fromFile(video);
            String filenameMsg = "filename" + ":" + videoPayload.getId() + ":" + Utils.getTimeStampString();
            sendMessage(endpointId, filenameMsg);
            Nearby.getConnectionsClient(context).sendPayload(endpointId, videoPayload);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

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

    public void openMyFolder() {

        Intent fileChooser = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        //fileChooser.setType("video/mp4"); se voglio vedere solo file.mp4, devo levare mimetypes
        fileChooser.setType("video/*");
        //you can choose video :  .mp4,.mkv
        String[] mimetypes = {"video/mp4", "video/x-matroska"};
        fileChooser.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);

        fileChooser.addCategory(Intent.CATEGORY_OPENABLE);
        fileChooser = Intent.createChooser(fileChooser, "Open folder");

        if (fileChooser.resolveActivity(context.getPackageManager()) != null)
            startActivityForResult(fileChooser, REQUEST_CODE_BY_INTENT_FILE_CHOOSER);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_BY_INTENT_FILE_CHOOSER && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            String path = Utils.getPathFromURI(context, uri);
            if (path != null) {
                int lastIndex = path.lastIndexOf(File.separator);
                if (lastIndex != -1) {
                    fileNames.add(path.substring(lastIndex + 1));
                    inputFiles.add(path);
                }
                navController.navigate(R.id.navigation_video);
            } else {
                new AlertDialog.Builder(activity_context, R.style.Theme_ConnectionDialog)
                        .setTitle(getString(R.string.error))
                        .setMessage(getString(R.string.unsupported_storage))
                        .setPositiveButton(getString(R.string.ok), null)
                        .setIcon(R.drawable.ic_baseline_warning)
                        .show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(activity_context, R.style.Theme_ConnectionDialog)
                .setTitle(getString(R.string.exit_app))
                .setMessage(getString(R.string.exit_app_text))
                .setPositiveButton(getString(R.string.ok), (DialogInterface dialog, int which) -> finish())
                .setNegativeButton(getString(R.string.cancel), null)
                .setIcon(R.drawable.ic_baseline_warning)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Nearby.getConnectionsClient(context).stopAllEndpoints();
    }

    public HashMap<String, ConnectionInfo> getConnectedEndpoints() {
        return connectedEndpoints;
    }

    public ArrayList<String> getInputFiles() {
        return inputFiles;
    }

    public ArrayList<String> getFileNames() {
        return fileNames;
    }

}