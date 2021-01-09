package it.snasaunive.nearbyvideorec.ui;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;

import it.snasaunive.nearbyvideorec.MainActivity;
import it.snasaunive.nearbyvideorec.R;
import it.snasaunive.nearbyvideorec.SavedUIData;
import it.snasaunive.nearbyvideorec.Utils;

public class VideoFragment extends Fragment {

    private final int REQUEST_PERMISSIONS_CODE = 3;
    private final String[] REQUIRED_PERMISSIONS = new String[]{
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.READ_EXTERNAL_STORAGE"
    };

    private SavedUIData savedUIData;

    private Button btn_merge;
    private Button btn_choose_files;
    private Button btn_clear_files;
    private TextView tv_names;
    private Button btn_remove_lastVideo;


    private ArrayList<String> paths_list;
    private ArrayList<String> videoNames;
    private static final String[] SCALES = {
            //horizontal
            "1920x1080",
            "1280x720",
            //vertical
            "1080x1920",
            "720x1280"
            //todo add common scales
    };

    private final View.OnClickListener choose_OnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (allPermissionsGranted()) {
                // All permissions already granted
                ((MainActivity) requireActivity()).openMyFolder();
            } else {
                // Missing permissions, ask user to accept
                requestPermissions(REQUIRED_PERMISSIONS, REQUEST_PERMISSIONS_CODE);
            }
        }
    };

    private final View.OnClickListener merge_OnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            /* we use AlertDialog for let user choose scaling before merging */
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Choose a scale : ");
            //implemented with one position array for access in "anonymous class"
            //set the default value if user does not select another
            int defaultPos = 0;
            String[] selectedValue = {SCALES[defaultPos]};

            //when dialog opened need a default position
            builder.setSingleChoiceItems(SCALES, defaultPos, new DialogInterface.OnClickListener() {
                //if user clicks another value
                @Override
                public void onClick(DialogInterface dialog, int pos) {
                    selectedValue[0] = SCALES[pos];
                }
            });

            //when user confirm
            builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int pos) {
                    String inputRes = selectedValue[0];
                    Utils.mergeVideo(requireContext(), ((MainActivity) requireActivity()).getInputFiles(), inputRes, "30");
                    ((MainActivity) requireActivity()).clearFilesPath();
                }
            });

            //when user reject
            builder.setNegativeButton("Back", null);

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    };

    private final View.OnClickListener clear_OnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            ((MainActivity) requireActivity()).clearFilesPath();
        }
    };
    private final View.OnClickListener removeLastVideoSelected_OnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            ((MainActivity) requireActivity()).deleteLastVideoSelected();
        }
    };

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_video, container, false);
        savedUIData = SavedUIData.INSTANCE;

        paths_list = new ArrayList<>();
        videoNames = new ArrayList<>();

        btn_choose_files = (Button) root.findViewById(R.id.btn_select_files);
        btn_merge = (Button) root.findViewById(R.id.btn_merge);
        btn_clear_files = (Button) root.findViewById(R.id.btn_clear_files);
        tv_names = (TextView) root.findViewById(R.id.filename);
        btn_remove_lastVideo = (Button) root.findViewById(R.id.btn_remove_lastVideo);

        ArrayList<String> filenames = ((MainActivity) requireActivity()).getFileNames();
        if (filenames.isEmpty()) {
            tv_names.setText(R.string.no_file_selected);
            btn_clear_files.setEnabled(false);
            btn_merge.setEnabled(false);
            btn_remove_lastVideo.setEnabled(false);
        } else {
            for (String name : filenames)
                tv_names.append(name + System.lineSeparator());
        }

        btn_choose_files.setOnClickListener(choose_OnClickListener);
        btn_merge.setOnClickListener(merge_OnClickListener);
        btn_remove_lastVideo.setOnClickListener(removeLastVideoSelected_OnClickListener);
        btn_clear_files.setOnClickListener(clear_OnClickListener);


        return root;

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
                ((MainActivity) requireActivity()).openMyFolder();
            } else {
                // User denied some permissions
                Toast.makeText(requireContext(), getString(R.string.permissions_denied), Toast.LENGTH_LONG).show();
            }
        }
    }

}
