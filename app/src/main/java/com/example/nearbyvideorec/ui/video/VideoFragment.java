package com.example.nearbyvideorec.ui.video;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.nearbyvideorec.R;
import com.example.nearbyvideorec.SavedUIData;

public class VideoFragment extends Fragment {

    private VideoViewModel videoViewModel;
    private SavedUIData savedUIData;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        videoViewModel =
                new ViewModelProvider(this).get(VideoViewModel.class);
        View root = inflater.inflate(R.layout.fragment_video, container, false);
        savedUIData = SavedUIData.INSTANCE;

        return root;
    }
}