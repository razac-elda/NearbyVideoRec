package com.example.nearbyvideorec.ui.server;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.nearbyvideorec.R;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class ServerFragment extends Fragment {

    private ServerViewModel serverViewModel;

    // Switch test
    private final View.OnClickListener switch_onClickListener = new View.OnClickListener() {
        @SuppressLint("ShowToast")
        @Override
        public void onClick(View v) {
            Toast toast;
            boolean on = ((SwitchMaterial) v).isChecked();
            if (on) {
                toast = Toast.makeText(requireContext(), "ON", Toast.LENGTH_SHORT);
            } else {
                toast = Toast.makeText(requireContext(), "OFF", Toast.LENGTH_SHORT);
            }
            toast.show();
        }
    };

    // Button test
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

        // Switch click listener
        SwitchMaterial activation_switch = root.findViewById(R.id.server_activation_switch);
        activation_switch.setOnClickListener(switch_onClickListener);

        // Button click listener
        Button send_button = root.findViewById(R.id.server_send_button);
        send_button.setOnClickListener(button_onClickListener);

        return root;
    }
}