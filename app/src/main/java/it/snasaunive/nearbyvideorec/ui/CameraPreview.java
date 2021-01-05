package it.snasaunive.nearbyvideorec.ui;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.VideoResult;
import com.otaliastudios.cameraview.controls.Engine;
import com.otaliastudios.cameraview.controls.Mode;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;

import it.snasaunive.nearbyvideorec.R;
import it.snasaunive.nearbyvideorec.Utils;

public class CameraPreview extends Fragment {

    private CameraView camera;
    private ContentResolver resolver;
    private Uri uriSavedVideo;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_camera_preview, container, false);

        camera = root.findViewById(R.id.camera);

        if (Utils.checkCameraAPI(requireContext())) {
            camera.setExperimental(false);
            camera.setEngine(Engine.CAMERA1);
        }

        camera.setLifecycleOwner(getViewLifecycleOwner());

        camera.addCameraListener(new CameraListener() {
            @Override
            public void onVideoTaken(@NonNull VideoResult result) {
                if (Build.VERSION.SDK_INT >= 29) {
                    uriSavedVideo = Utils.getUriSavedVideo();
                    resolver = Utils.getResolver();
                    ContentValues fileDetails = new ContentValues();
                    fileDetails.put(MediaStore.Video.Media.IS_PENDING, 0);
                    resolver.update(uriSavedVideo, fileDetails, null, null);
                }
            }
        });

        camera.setMode(Mode.VIDEO);

        return root;
    }

    public void startRec() {
        FileDescriptor videoFileDescriptor = null;
        try {
            videoFileDescriptor = Utils.createVideoFile(requireContext());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                camera.open();
            }
        }, 50);
        if (videoFileDescriptor != null)
            camera.takeVideo(videoFileDescriptor);
    }

    public void stopRec() {
        final Handler Handler = new Handler();
        Handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                camera.stopVideo();
            }
        }, 10);
        camera.close();
        camera.destroy();
    }

}