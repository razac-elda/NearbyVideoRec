package com.example.nearbyvideorec.ui.server;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.nearbyvideorec.MainActivity;
import com.example.nearbyvideorec.R;
import com.example.nearbyvideorec.SavedUIData;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class ServerFragment extends Fragment {

    private ServerViewModel serverViewModel;
    private SavedUIData savedUIData;

    private SwitchMaterial status_switch;
    private Button send_button;

    // Switch test.
    private final View.OnClickListener switch_onClickListener = new View.OnClickListener() {
        @SuppressLint("ShowToast")
        @Override
        public void onClick(View v) {
            Toast toast = Toast.makeText(requireContext(), "", Toast.LENGTH_SHORT);
            boolean status = ((SwitchMaterial) v).isChecked();
            // Store status in SavedUIData.
            savedUIData.setServer_status_switch(status);
            send_button.setEnabled(status);
            if (status) {
                ((MainActivity)requireActivity()).requestAdvertise();
                toast.setText("ON");
            } else {
                ((MainActivity)requireActivity()).requestDisconnect("SERVER");
                toast.setText("OFF");
            }
            toast.show();
        }
    };

    // Button test.
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
        // Restore status from SavedUIData.
        status_switch.setChecked(savedUIData.getServer_status_switch());

        // Button click listener.
        send_button = (Button) root.findViewById(R.id.server_send_button);
        send_button.setOnClickListener(button_onClickListener);
        // Restore status from SavedUIData(switch dependant).
        send_button.setEnabled(savedUIData.getServer_status_switch());

        return root;
    }
}