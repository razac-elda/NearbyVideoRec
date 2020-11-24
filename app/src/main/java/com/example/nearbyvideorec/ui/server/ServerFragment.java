package com.example.nearbyvideorec.ui.server;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.nearbyvideorec.MainActivity;
import com.example.nearbyvideorec.R;
import com.example.nearbyvideorec.SavedUIData;
import com.google.android.material.switchmaterial.SwitchMaterial;

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
    private Button send_button;

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

    // Test button to be removed
    private final View.OnClickListener button_onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Toast toast;
            toast = Toast.makeText(requireContext(), "SENT", Toast.LENGTH_SHORT);
            toast.show();
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

        // Button click listener.
        send_button = (Button) root.findViewById(R.id.server_send_button);
        send_button.setOnClickListener(button_onClickListener);

        // Restore status from SavedUIData.
        status_switch.setChecked(savedUIData.getServer_status_switch());

        // Restore status from SavedUIData(switch dependant).
        send_button.setEnabled(savedUIData.getServer_status_switch());

        return root;
    }

    // Invoke when all permissions are accepted
    private void manageServer(View v) {
        boolean status = ((SwitchMaterial) v).isChecked();
        // Store status in SavedUIData.
        savedUIData.setServer_status_switch(status);
        send_button.setEnabled(status);

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