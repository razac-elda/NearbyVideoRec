package com.example.nearbyvideorec.ui.video;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.nearbyvideorec.MainActivity;
import com.example.nearbyvideorec.R;
import com.example.nearbyvideorec.SavedUIData;
import com.example.nearbyvideorec.Utils;

import java.io.IOException;
import java.util.ArrayList;

public class VideoFragment extends Fragment {

    private VideoViewModel videoViewModel;
    private SavedUIData savedUIData;

    private Button btn_merge;
    private Button btn_choose_files;
    private Button btn_clear_files;
    private TextView tv_names;

    private ArrayList<String> paths_list;
    private ArrayList<String> videoNames;

    private final View.OnClickListener choose_OnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ((MainActivity) requireActivity()).openMyFolder();
        }
    };

    private final View.OnClickListener merge_OnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            try {
                Utils.createTextFile(((MainActivity) requireActivity()).getPathList());
            } catch (IOException e) {
                e.printStackTrace();
            }
            Utils.mergeVideo(requireContext());
            ((MainActivity) requireActivity()).clearPaths();
        }
    };

    private final View.OnClickListener clear_OnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ((MainActivity) requireActivity()).clearPaths();
        }
    };

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        videoViewModel = new ViewModelProvider(this).get(VideoViewModel.class);
        View root = inflater.inflate(R.layout.fragment_video, container, false);
        savedUIData = SavedUIData.INSTANCE;

        paths_list = new ArrayList<>();
        videoNames = new ArrayList<>();

        btn_choose_files = (Button) root.findViewById(R.id.btn_select_files);
        btn_merge = (Button) root.findViewById(R.id.btn_merge);
        btn_clear_files = (Button) root.findViewById(R.id.btn_clear_files);
        tv_names = (TextView) root.findViewById(R.id.filename);

        ArrayList<String> filenames = ((MainActivity) requireActivity()).getFileNames();
        if (filenames.isEmpty()) {
            tv_names.setText(R.string.no_file_selected);
            btn_clear_files.setEnabled(false);
            btn_merge.setEnabled(false);
        } else {
            for (String name : filenames)
                tv_names.append(name + "\n");
        }

        btn_choose_files.setOnClickListener(choose_OnClickListener);
        btn_merge.setOnClickListener(merge_OnClickListener);
        btn_clear_files.setOnClickListener(choose_OnClickListener);

        return root;

    }

}
