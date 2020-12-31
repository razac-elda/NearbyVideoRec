package com.example.nearbyvideorec;

import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

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
    private String space = " ";
    private String apostrofo = "\'";
    private ArrayList<String> paths_list = new ArrayList<String>();
    private File f;
    private FileOutputStream fos;
    private Uri folderUri;
    private ArrayList<String> nomiLista = new ArrayList<>();
    private static final int REQUEST_CODE_BY_INTENT_FILE_CHOOSER = 1234;


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


// PARTE DEL MERGE ----------------------------------------------------------------------------------


    public String generateNameOutputFile() {
        return "Merged_" + getTimeStampString() + ".mp4";
    }

    public String getDirectoryNameMoviesPathString() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator +
                Environment.DIRECTORY_MOVIES + File.separator;
    }

    public void runCommand(String prefix, String filepathInput, String middleOption, String filepathOutput) {

        //generate string command
        String cmd = prefix + space + filepathInput + space + middleOption + space + filepathOutput;
        //execute command
        int rc = FFmpeg.execute(cmd);

        switch (rc) {
            case RETURN_CODE_SUCCESS:
                if (context == null) {
                    context = getApplicationContext();
                }
                //Toast.makeText(myc, "RESULTCODE " + rc + " DONE", Toast.LENGTH_SHORT).show();
                Toast.makeText(context, "video completo generato", Toast.LENGTH_SHORT).show();
                Log.i(Config.TAG, "Command execution completed successfully.");
                //pulisco arraylist di path
                paths_list.clear();

                //pulisco arraylist di nomi
                nomiLista.clear();
                savedUIData.setVideoNamesText(nomiLista);

                break;

            case RETURN_CODE_CANCEL:
                if (context == null) {
                    context = getApplicationContext();
                }
                Toast.makeText(context, "RESULTCODE" + rc + "RESULT CODE CANCEL", Toast.LENGTH_SHORT).show();
                Log.i(Config.TAG, "Command execution cancelled by user.");
                break;
            default:
                if (context == null) {
                    context = getApplicationContext();
                }
                //Toast.makeText(myc, "RESULTCODE" + rc + "RESULT CODE FAILED", Toast.LENGTH_SHORT).show();
                Toast.makeText(context, "Selezionare almeno un video", Toast.LENGTH_SHORT).show();
                Log.i(Config.TAG, String.format("Command execution failed with rc=%d and the output below.", rc));
                Config.printLastCommandOutput(Log.INFO);
                break;
        }
        navController.navigate(R.id.navigation_video);
    }


    //myListpatth.txt
    public void generateFileTxT(String filename) {
        f = new File(getDirectoryNameMoviesPathString(), filename);

        try {
            fos = new FileOutputStream(f);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            /*
            example
            String s =  "file" + space + apostrofo + "/storage/emulated/0/Movies/video_20201219_0532.mp4" + apostrofo + "\n";
            */

            //SCRITTURA DEI PATH SU FILE DI TESTO
            StringBuilder s = new StringBuilder();
            for (String path : paths_list) {
                s.append("file").append(space).append(apostrofo).append(path).append(apostrofo).append("\n");
            }
            fos.write(s.toString().getBytes());

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    //aprire intent file chooser
    public void openMyFolder() {
        Intent chooserfile = new Intent(Intent.ACTION_GET_CONTENT);

        folderUri = Uri.parse(Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator + Environment.DIRECTORY_MOVIES + File.separator);
        chooserfile.setDataAndType(folderUri, "video/mp4");

        chooserfile.addCategory(Intent.CATEGORY_OPENABLE);
        chooserfile = Intent.createChooser(chooserfile, "Open folder");

        //gestione presenza del file manager  //todo da testare
        if(chooserfile.resolveActivityInfo(context.getPackageManager(),0) == null)
            Toast.makeText(context,"file manager non presente \n ... installarne uno",Toast.LENGTH_SHORT).show();
        else
            startActivityForResult(chooserfile, REQUEST_CODE_BY_INTENT_FILE_CHOOSER);

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == REQUEST_CODE_BY_INTENT_FILE_CHOOSER && resultCode == RESULT_OK && data != null) {
            Uri u = data.getData();
            System.out.println("URI" + u.toString()); // XIAOMI ANDROID 10 : content://com.mi.android.globalFileexplorer.myprovider/external_files/Movies/NOME_VIDEO_SELEZIONATO.MP4
            String uriString = Uri.decode(u.toString());
            System.out.println("URISTRING:   "+uriString);

            String p = "pathvuoto";
            try {
                p = getPathFromURI(context, u);
                System.out.println("getPathFromURI  " + p);
                if (p == null){
                    p = myTakePathFromURI(uriString);
                    System.out.println("myTakePathFromURI  " + p);
                }
            } catch (Exception e) {

                p = myTakePathFromURI(uriString);
                e.printStackTrace();
            }finally {
                //aggiunta ad array di nomi
                String name = takeFileNameFromPath(p);
                nomiLista.add(name);
                savedUIData.setVideoNamesText(nomiLista);
            }
            System.out.println("PATH" + p);  //  XIAOMI ANDROID 10 : /storage/emulated/0/Movies/NOME_VIDEO_SELEZIONATO.mp4
            paths_list.add(p);

            navController.navigate(R.id.navigation_video);
        }
    }


    // todo controllare con https://gist.github.com/webserveis/c6d55da4dbfc2fdd13d91dc5b7f85499
    //puo tornare null in alcuni casi con dispositivi vecchi
    private static String getPathFromURI(Context context, Uri uri) throws URISyntaxException {
        boolean needToCheckUri = Build.VERSION.SDK_INT >= 24;
        String selection = null;
        String[] selectionArgs = null;
        // Uri is different in versions after KITKAT (Android 4.4), we need to deal with different Uris.
        if (needToCheckUri && DocumentsContract.isDocumentUri(context.getApplicationContext(), uri)) {
            if (isExternalStorageDocument(uri)) {
                String docId = DocumentsContract.getDocumentId(uri);
                String[] split = docId.split(":");
                return Environment.getExternalStorageDirectory() + "/" + split[1];
            } else if (isDownloadsDocument(uri)) {
                String id = DocumentsContract.getDocumentId(uri);
                uri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
            } else if (isMediaDocument(uri)) {
                String docId = DocumentsContract.getDocumentId(uri);
                String[] split = docId.split(":");
                String type = split[0];
                if ("image".equals(type)) {
                    uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                selection = "_id=?";
                selectionArgs = new String[]{ split[1] };
            }
        }
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { MediaStore.Video.Media.DATA };
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                if (cursor.moveToFirst()) {
                    String result = cursor.getString(column_index);
                    cursor.close();
                    return result;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }



    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }


    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }


    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority())
                ||
                "com.huawei.disk.fileprovider".equals(uri.getAuthority());
    }

    public void pulisciLista(){
        paths_list.clear();
        nomiLista.clear();
        savedUIData.setVideoNamesText(nomiLista);
        navController.navigate(R.id.navigation_video);
    }

    //Da spostare su utils prende l'ultima occorrenza dello '/'  nel nostro caso serve per prendere il nome del file
    //dato un path
    public String takeFileNameFromPath(String path){

        System.out.println("PATHSTRING " + path);
        String name;
        int lastIndex = path.lastIndexOf("/");
        if (lastIndex != -1){
            name = path.substring(lastIndex+1);
            return name;
        }else return "errore";
    }

    //metodo brutale per prendere il path   (si puo spostare su utils)
    public static String myTakePathFromURI(String uriString){
        //String uriString = u.toString();
        String[] parts = uriString.split("/storage");
        String storage = "/storage";
        String path = storage.concat(parts[1]);
        return path;

    }
    // (si puo spostare su utils)
    public static String getTimeStampString() {
        return new SimpleDateFormat("dd-MM-yy_hh-mm-ss", Locale.getDefault()).format(new Date());
    }



}