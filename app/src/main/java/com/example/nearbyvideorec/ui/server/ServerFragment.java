package com.example.nearbyvideorec.ui.server;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.nearbyvideorec.R;

public class ServerFragment extends Fragment {

    private ServerViewModel serverViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        serverViewModel =
                new ViewModelProvider(this).get(ServerViewModel.class);
        View root = inflater.inflate(R.layout.fragment_server, container, false);

        return root;
    }
}