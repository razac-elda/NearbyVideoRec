package com.example.nearbyvideorec.ui.video;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;



import com.example.nearbyvideorec.R;
import com.example.nearbyvideorec.SavedUIData;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

public class VideoFragment extends Fragment {


    private VideoViewModel videoViewModel;
    private SavedUIData savedUIData;


    private Context myc;


    private String command;
    private String space = " ";

    private String nameOutputFilemp4 = "prova2.mp4";
    private File f;
    private FileOutputStream fos;
    private String fileNameTxt = "myListpatth.txt";

    private Button btn_merge;


    private String getNameOutputFilemp4() {

        return nameOutputFilemp4;

    }

    private String getDirectoryNameMoviesPathString() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator +
                Environment.DIRECTORY_MOVIES + File.separator;
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        videoViewModel = new ViewModelProvider(this).get(VideoViewModel.class);
        View root = inflater.inflate(R.layout.fragment_video, container, false);


        savedUIData = SavedUIData.INSTANCE;
        btn_merge = (Button) root.findViewById(R.id.btn_merge);

        // aggiunta bottone più listener
        btn_merge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //COMMAND FOR ANDROID < 10(Q)
                runCommand("-f concat -safe 0 -i",
                        getDirectoryNameMoviesPathString() + fileNameTxt,
                        "-c copy",
                        getDirectoryNameMoviesPathString() + nameOutputFilemp4
                );

            }
        });

        return root;
    }

    // METODO WRAPPER DEL COMANDO
    public void runCommand(String prefix, String filepathInput, String middleOption, String filepathOutput) {
        //ESEMPIO DI COMANDO

        //generate string command
        String cmd = prefix + space + filepathInput + space + middleOption + space + filepathOutput;
        //execute command
        int rc = FFmpeg.execute(cmd);

        switch (rc) {
            case RETURN_CODE_SUCCESS:
                if (myc == null) {
                    myc = requireContext();
                }
                Toast.makeText(myc, "RESULTCODE" + rc + "DONE", Toast.LENGTH_SHORT).show();
                Log.i(Config.TAG, "Command execution completed successfully.");
                break;

            case RETURN_CODE_CANCEL:
                if (myc == null) {
                    myc = requireContext();
                }
                Toast.makeText(myc, "RESULTCODE" + rc + "RESULT CODE CANCEL", Toast.LENGTH_SHORT).show();
                Log.i(Config.TAG, "Command execution cancelled by user.");
                break;
            default:
                if (myc == null) {
                    myc = requireContext();
                }
                Toast.makeText(myc, "RESULTCODE" + rc + "RESULT CODE FAILED", Toast.LENGTH_SHORT).show();
                Log.i(Config.TAG, String.format("Command execution failed with rc=%d and the output below.", rc));
                Config.printLastCommandOutput(Log.INFO);
                break;
        }

    }

    //myListpatth.txt
    public void generateFileTxT(String filename) {
        f = new File(getDirectoryNameMoviesPathString(), filename);

        try {
            fos = new FileOutputStream(f);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            //TEST SU CELL ANDROID 7   FUNZIONANTE
            //todo da sostuire con i path 
            String s = "file" + space +
                    "\'/storage/emulated/0/Movies/video_20201205_1252.mp4\'" + "\n" +
                    "file" + space +
                    "\'/storage/emulated/0/Movies/video_20201205_1253.mp4\'" + "\n" +
                    "file" + space + "\'/storage/emulated/0/Movies/video_20201205_1253.mp4\'";

            fos.write(s.getBytes());


        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
