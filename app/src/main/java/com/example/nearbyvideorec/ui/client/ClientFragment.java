package com.example.nearbyvideorec.ui.client;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.nearbyvideorec.MainActivity;
import com.example.nearbyvideorec.R;
import com.example.nearbyvideorec.SavedUIData;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class ClientFragment extends Fragment {

    private ClientViewModel clientViewModel;
    private SavedUIData savedUIData;

    private SwitchMaterial status_switch;
    private Button send_button;
    private TextView connection_status;

    // Switch listener to activate as a client.
    private final View.OnClickListener switch_onClickListener = new View.OnClickListener() {
        @SuppressLint("ShowToast")
        @Override
        public void onClick(View v) {
            boolean status = ((SwitchMaterial) v).isChecked();
            // Store status in SavedUIData.
            savedUIData.setClient_status_switch(status);
            if (status) {
                ((MainActivity)requireActivity()).requestConnect("CLIENT");
            } else {
                ((MainActivity)requireActivity()).requestDisconnect("CLIENT");
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
        clientViewModel =
                new ViewModelProvider(this).get(ClientViewModel.class);
        View root = inflater.inflate(R.layout.fragment_client, container, false);
        savedUIData = SavedUIData.INSTANCE;

        // Switch click listener.
        status_switch = (SwitchMaterial) root.findViewById(R.id.client_status_switch);
        status_switch.setOnClickListener(switch_onClickListener);

        // Button click listener.
        send_button = (Button) root.findViewById(R.id.send_button_test);
        send_button.setOnClickListener(button_onClickListener);

        connection_status = (TextView) root.findViewById(R.id.client_status);

        // Restore status from SavedUIData.
        status_switch.setChecked(savedUIData.getClient_status_switch());
        connection_status.setText(savedUIData.getClient_connection_status());

        return root;
    }

}