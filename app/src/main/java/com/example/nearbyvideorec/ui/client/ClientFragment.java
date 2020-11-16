package com.example.nearbyvideorec.ui.client;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    // Switch test.
    private final View.OnClickListener switch_onClickListener = new View.OnClickListener() {
        @SuppressLint("ShowToast")
        @Override
        public void onClick(View v) {
            Toast toast = Toast.makeText(requireContext(), "", Toast.LENGTH_SHORT);
            boolean status = ((SwitchMaterial) v).isChecked();
            // Store status in SavedUIData.
            savedUIData.setClient_status_switch(status);
            if (status) {
                ((MainActivity)requireActivity()).requestDiscovery();
                toast.setText("ON");
            } else {
                ((MainActivity)requireActivity()).requestDisconnect();
                toast.setText("OFF");
            }
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
        // Restore status from SavedUIData.
        status_switch.setChecked(savedUIData.getClient_status_switch());

        return root;
    }

}