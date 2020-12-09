package com.example.nearbyvideorec.ui.server;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.nearbyvideorec.MainActivity;
import com.example.nearbyvideorec.R;
import com.example.nearbyvideorec.SavedUIData;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ServerFragment extends Fragment {

    private final int REQUEST_PERMISSIONS_CODE = 2;
    private final String[] REQUIRED_PERMISSIONS = new String[]{
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.READ_EXTERNAL_STORAGE"
    };
    private View switchView;

    private ServerViewModel serverViewModel;
    private SavedUIData savedUIData;

    private SwitchMaterial status_switch;
    private Button select_button;
    private Button start_button;
    private Button stop_button;
    private TextView selected_device;

    private HashMap<String, ConnectionInfo> connectedDevices;

    // Switch listener, check permissions first and then activate the server.
    private final View.OnClickListener switch_onClickListener = new View.OnClickListener() {
        @SuppressLint("ShowToast")
        @Override
        public void onClick(View v) {
            switchView = v;

            if (allPermissionsGranted()) {
                // All permissions already granted
                manageServer(switchView);
            } else {
                // Missing permissions, ask user to accept
                requestPermissions(REQUIRED_PERMISSIONS, REQUEST_PERMISSIONS_CODE);
            }
        }
    };

    // Select device button listener
    private final View.OnClickListener select_onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            List<String> deviceName = new ArrayList<>();

            /*
            * Create an AlertDialog to choose the target device for start/stop recording.
            * connectedDevices is a HashMap<String, ConnectionInfo> that refers to connectedEndpoints
            * in the MainActivity, where all connected devices are saved.
            * We add the endpoints name to a List<String> deviceName.
            * Since AlertDialog needs a String array we transfer the object from deviceName to
            * deviceNameArray.
            */

            connectedDevices = ((MainActivity) requireActivity()).getConnectedEndpoints();
            for (ConnectionInfo info : connectedDevices.values())
                deviceName.add(info.getEndpointName());

            String[]  deviceNameArray = deviceName.toArray(new String[0]);
            // TODO:Debug muli-device list
            for (String i : deviceNameArray)
                Log.d("A", i);
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            builder.setTitle(R.string.server_select_a_device);
            builder.setItems(deviceNameArray, new DialogInterface.OnClickListener() {

                //Set the TextView with the device chosen
                @Override
                public void onClick(DialogInterface dialog, int pos) {
                    selected_device.setText(deviceNameArray[pos]);
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    };

    // Start recording button listener
    private final View.OnClickListener start_onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String device = selected_device.getText().toString();
            if (!device.equals("No device selected")) {
                for (String key : connectedDevices.keySet()) {
                    if (connectedDevices.get(key).getEndpointName().equals(device))
                        ((MainActivity) requireActivity()).sendMessage(key, "start_rec");
                }
            } else {
                Toast.makeText(requireContext(), "Select a device", Toast.LENGTH_SHORT).show();
            }
        }
    };

    // Stop recording button listener
    private final View.OnClickListener stop_onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String device = selected_device.getText().toString();
            if (!device.equals("No device selected")) {
                for (String key : connectedDevices.keySet()) {
                    if (connectedDevices.get(key).getEndpointName().equals(device))
                        ((MainActivity) requireActivity()).sendMessage(key, "stop_rec");
                }
            } else {
                Toast.makeText(requireContext(), "Select a device", Toast.LENGTH_SHORT).show();
            }
        }
    };

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        serverViewModel =
                new ViewModelProvider(this).get(ServerViewModel.class);
        View root = inflater.inflate(R.layout.fragment_server, container, false);
        savedUIData = SavedUIData.INSTANCE;

        // Switch click listener.
        status_switch = (SwitchMaterial) root.findViewById(R.id.server_activation_switch);
        status_switch.setOnClickListener(switch_onClickListener);

        // Select button click listener.
        select_button = (Button) root.findViewById(R.id.server_select_device_button);
        select_button.setOnClickListener(select_onClickListener);

        // Start button click listener.
        start_button = (Button) root.findViewById(R.id.server_start_rec_button);
        start_button.setOnClickListener(start_onClickListener);

        // Stop button click listener.
        stop_button = (Button) root.findViewById(R.id.server_stop_rec_button);
        stop_button.setOnClickListener(stop_onClickListener);

        selected_device = (TextView) root.findViewById(R.id.selected_device);

        // Restore status from SavedUIData.
        status_switch.setChecked(savedUIData.getServer_status_switch());

        // Restore status from SavedUIData(switch dependant).
        select_button.setEnabled(savedUIData.getServer_status_switch());
        start_button.setEnabled(savedUIData.getServer_status_switch());
        stop_button.setEnabled(savedUIData.getServer_status_switch());

        return root;
    }

    // Invoke when all permissions are accepted
    private void manageServer(View v) {
        boolean status = ((SwitchMaterial) v).isChecked();
        // Store status in SavedUIData.
        savedUIData.setServer_status_switch(status);
        select_button.setEnabled(status);
        start_button.setEnabled(status);
        stop_button.setEnabled(status);

        // Switch on->start server, switch off->disconnect
        if (status) {
            ((MainActivity) requireActivity()).requestConnect("SERVER");
        } else {
            ((MainActivity) requireActivity()).requestDisconnect("SERVER");
        }
    }

    // Permission management

    // Cycle through permissions, reject when at least one is rejected
    private boolean allPermissionsGranted() {
        boolean accept = true;

        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(requireActivity(), permission) != PackageManager.PERMISSION_GRANTED)
                accept = false;
        }
        return accept;
    }

    // Called after "requestPermissions", check if all permissions were accepted
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            if (allPermissionsGranted()) {
                manageServer(switchView);
            } else {
                // User denied some permissions
                Toast.makeText(requireContext(), getString(R.string.permissions_denied), Toast.LENGTH_LONG).show();
                status_switch.setChecked(false);
            }
        }
    }
}