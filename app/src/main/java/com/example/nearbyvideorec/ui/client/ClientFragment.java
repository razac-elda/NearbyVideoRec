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

import com.example.nearbyvideorec.R;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class ClientFragment extends Fragment {

    private ClientViewModel clientViewModel;

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

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        clientViewModel =
                new ViewModelProvider(this).get(ClientViewModel.class);
        View root = inflater.inflate(R.layout.fragment_client, container, false);

        // Switch click listener
        SwitchMaterial activation_switch = root.findViewById(R.id.client_activation_switch);
        activation_switch.setOnClickListener(switch_onClickListener);

        return root;
    }
}