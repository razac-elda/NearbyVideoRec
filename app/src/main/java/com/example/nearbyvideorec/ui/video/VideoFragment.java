package com.example.nearbyvideorec.ui.video;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;

import com.example.nearbyvideorec.R;
import com.example.nearbyvideorec.SavedUIData;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

public class VideoFragment extends Fragment {


    private VideoViewModel videoViewModel;
    private SavedUIData savedUIData;

    private String o = "/storage/emulated/0/Movies/ooo.mp4 ";
    private Context myc;
    private String fullPath;
    private String topath;
    private String command ;
    private String space = " ";
    private String nameOutputFileAvi = "prova.avi";
    private String nameOutputFilemp4 = "prova2.mp4";
    private File f;
    private FileOutputStream fos;
//todo provare a fare il concat usando un file txt

    private String getNameOutputFileAvi(){
        return nameOutputFileAvi;
    }

    private String getDirectoryNameMoviesPathString(){
        return Environment.getExternalStorageDirectory().getAbsolutePath()+ File.separator + Environment.DIRECTORY_MOVIES +File.separator;
    }
 //    /storage/emulated/0/Movies

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        videoViewModel =
                new ViewModelProvider(this).get(VideoViewModel.class);
        View root = inflater.inflate(R.layout.fragment_video, container, false);
        savedUIData = SavedUIData.INSTANCE;

        //run command
        //video_20201204_0655.mp4
        /* runCommand("-i",
                getDirectoryNameMoviesPathString() + "video_20201205_1020.mp4",
                "-vcodec copy -acodec copy",
                getDirectoryNameMoviesPathString()+getNameOutputFile()
                );
       //   s  =      "-i filinput -vcodec copy -acodec copy fileout"
        */
        f = new File(getDirectoryNameMoviesPathString(),"myListpatth.txt");
        try {
            fos = new FileOutputStream(f);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {

                    String s = "file" + space +
                            "\'/storage/emulated/0/Movies/video_20201204_0655.mp4\'" + "\n" +
                            "file" + space +
                            "\'/storage/emulated/0/Movies/video_20201204_0656.mp4\'" + "\n" +
                            "file" + space +"\'/storage/emulated/0/Movies/video_20201205_1019.mp4\'";

            fos.write(s.getBytes());

        }catch (Exception e ){
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        runCommand("-f concat -safe 0 -i",
                getDirectoryNameMoviesPathString() + "myListpatth.txt",
                "-c copy",
                getDirectoryNameMoviesPathString()+ "prova2.mp4"
        );



        return root;
    }

    public void runCommand(String prefix , String filepathInput, String middleOption , String filepathOutput ){

        //ffmpeg -i filename.mp4 -vcodec copy -acodec copy filename.avi
        //generate string command
        String cmd = prefix + space + filepathInput +space+ middleOption +space+ filepathOutput;
        //execute command
        int rc = FFmpeg.execute(cmd);
        if (rc == RETURN_CODE_SUCCESS) {
            if (myc == null){ myc = requireContext();}
            Toast.makeText(myc, "a"+ rc + "FINE" , Toast.LENGTH_SHORT).show();
            Log.i(Config.TAG, "Command execution completed successfully.");
        } else if (rc == RETURN_CODE_CANCEL) {
            Log.i(Config.TAG, "Command execution cancelled by user.");
        } else {
            Log.i(Config.TAG, String.format("Command execution failed with rc=%d and the output below.", rc));
            Config.printLastCommandOutput(Log.INFO);
        }
    }
}