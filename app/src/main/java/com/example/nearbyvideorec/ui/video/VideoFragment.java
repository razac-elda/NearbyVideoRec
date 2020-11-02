package com.example.nearbyvideorec.ui.video;

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

public class VideoFragment extends Fragment {

    private VideoViewModel videoViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        videoViewModel =
                new ViewModelProvider(this).get(VideoViewModel.class);
        View root = inflater.inflate(R.layout.fragment_video, container, false);
        final TextView textView = root.findViewById(R.id.text_notifications);
        videoViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });
        return root;
    }
}