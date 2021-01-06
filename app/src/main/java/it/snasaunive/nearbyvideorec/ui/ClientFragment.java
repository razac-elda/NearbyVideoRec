package it.snasaunive.nearbyvideorec.ui;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.HashMap;

import it.snasaunive.nearbyvideorec.MainActivity;
import it.snasaunive.nearbyvideorec.R;
import it.snasaunive.nearbyvideorec.SavedUIData;

public class ClientFragment extends Fragment {

    private final int REQUEST_PERMISSIONS_CODE = 1;
    private final String[] REQUIRED_PERMISSIONS = new String[]{
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.READ_EXTERNAL_STORAGE"
    };
    private View switchView;

    private SavedUIData savedUIData;

    private SwitchMaterial swc_status;
    private TextView tv_connection_status;
    private TextView tv_help;

    private HashMap<String, ConnectionInfo> connectedDevices;

    // Switch listener, check permissions first and then activate the client.
    private final View.OnClickListener switch_onClickListener = new View.OnClickListener() {
        @SuppressLint("ShowToast")
        @Override
        public void onClick(View v) {
            switchView = v;

            if (allPermissionsGranted()) {
                // All permissions already granted
                manageClient(switchView);
            } else {
                // Missing permissions, ask user to accept
                requestPermissions(REQUIRED_PERMISSIONS, REQUEST_PERMISSIONS_CODE);
            }
        }
    };

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_client, container, false);

        savedUIData = SavedUIData.INSTANCE;

        // Switch click listener.
        swc_status = (SwitchMaterial) root.findViewById(R.id.client_status_switch);
        swc_status.setOnClickListener(switch_onClickListener);

        // Restore status from SavedUIData.
        swc_status.setChecked(savedUIData.getClient_status_switch());

        tv_connection_status = (TextView) root.findViewById(R.id.client_status);
        tv_connection_status.setTextColor(ContextCompat.getColor(requireContext(), R.color.red));

        tv_help = (TextView) root.findViewById(R.id.help);
        String s = getString(R.string.getting_started);
        tv_help.setText(HtmlCompat.fromHtml(s, HtmlCompat.FROM_HTML_MODE_LEGACY));

        // Update TextView with info about the connection
        if (!savedUIData.getServer_status_switch()) {

            /*
             * Display the server name or the absence of connection.
             * connectedDevices is a HashMap<String, ConnectionInfo> that refers to connectedEndpoints
             * in the MainActivity, where all connected devices are saved.
             * Discoverer has only one device so the for-each iterate only one element of the HashMap.
             */

            String server_name = getString(R.string.client_not_connected);
            connectedDevices = ((MainActivity) requireActivity()).getConnectedEndpoints();

            for (ConnectionInfo info : connectedDevices.values()) {
                server_name = getString(R.string.client_connected_to) + " " + info.getEndpointName();
                tv_connection_status.setTextColor(ContextCompat.getColor(requireContext(), R.color.green));
            }
            tv_connection_status.setText(server_name);
        } else {
            // If connected as a server do not show any device in client tab.
            String connected_as_server = getString(R.string.connected_as_a_server);
            tv_connection_status.setText(connected_as_server);
        }

        return root;
    }

    // Invoke when all permissions are accepted
    private void manageClient(View v) {
        boolean status = ((SwitchMaterial) v).isChecked();

        // Store status in SavedUIData.
        savedUIData.setClient_status_switch(status);

        // Switch on->start client, switch off->disconnect
        if (status) {
            ((MainActivity) requireActivity()).requestConnect("CLIENT");
        } else {
            ((MainActivity) requireActivity()).requestDisconnect("CLIENT");
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            if (allPermissionsGranted()) {
                manageClient(switchView);
            } else {
                // User denied some permissions
                Toast.makeText(requireContext(), getString(R.string.permissions_denied), Toast.LENGTH_LONG).show();
                swc_status.setChecked(false);
            }
        }
    }

}