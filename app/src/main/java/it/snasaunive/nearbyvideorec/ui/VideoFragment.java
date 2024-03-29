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

    private ArrayList<String> paths_list;
    private ArrayList<String> videoNames;
    private final String[] RES = {
            // Horizontal
            "1920x1080",
            "1280x720",
            // Vertical
            "1080x1920",
            "720x1280"
    };

    // File chooser listener.
    private final View.OnClickListener choose_OnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (allPermissionsGranted()) {
                // All permissions already granted.
                ((MainActivity) requireActivity()).openMyFolder();
            } else {
                // Missing permissions, ask user to accept.
                requestPermissions(REQUIRED_PERMISSIONS, REQUEST_PERMISSIONS_CODE);
            }
        }
    };

    // Merge listener.
    private final View.OnClickListener merge_OnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            // Create dialog with a set of resolutions to choose from.
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle(R.string.choose_res);
            String[] selectedValue = {RES[0]};

            builder.setSingleChoiceItems(RES, 0, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int pos) {
                    selectedValue[0] = RES[pos];
                }
            });
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int pos) {
                    String inputRes = selectedValue[0];

                    /* After resolution is confirmed we start the merge at 30fps, future implementations could add
                    more dynamic parameters. */
                    Utils.mergeVideo(requireContext(), ((MainActivity) requireActivity()).getInputFiles(), inputRes,
                            "30");
                    // Clear selected files for new merge session.
                    ((MainActivity) requireActivity()).clearFilesPath();
                }
            });
            builder.setNegativeButton(R.string.cancel, null);

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    };

    // Clear path listener
    private final View.OnClickListener clear_OnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            ((MainActivity) requireActivity()).clearFilesPath();
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

        // Check if user selected some files
        ArrayList<String> filenames = ((MainActivity) requireActivity()).getFileNames();
        if (filenames.isEmpty()) {
            tv_names.setText(R.string.no_file_selected);
            btn_clear_files.setEnabled(false);
            btn_merge.setEnabled(false);
        } else {
            // List filenames
            for (String name : filenames)
                tv_names.append(name + System.lineSeparator());
        }

        btn_choose_files.setOnClickListener(choose_OnClickListener);
        btn_merge.setOnClickListener(merge_OnClickListener);
        btn_clear_files.setOnClickListener(clear_OnClickListener);

        return root;
    }

    // Permission management.

    // Cycle through permissions, reject when at least one is rejected.
    private boolean allPermissionsGranted() {
        boolean accept = true;

        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(requireActivity(), permission) != PackageManager.PERMISSION_GRANTED)
                accept = false;
        }
        return accept;
    }

    // Called after "requestPermissions", check if all permissions were accepted.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            if (allPermissionsGranted()) {
                ((MainActivity) requireActivity()).openMyFolder();
            } else {
                // User denied some permissions.
                Toast.makeText(requireContext(), getString(R.string.permissions_denied), Toast.LENGTH_SHORT).show();
            }
        }
    }

}
