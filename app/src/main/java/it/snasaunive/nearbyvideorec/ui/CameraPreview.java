package it.snasaunive.nearbyvideorec.ui;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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

import it.snasaunive.nearbyvideorec.MainActivity;
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

        // Set Camera1 API if device does not support Camera2.
        if (Utils.checkCameraAPI(requireContext())) {
            camera.setExperimental(false);
            camera.setEngine(Engine.CAMERA1);
        }

        camera.setLifecycleOwner(getViewLifecycleOwner());

        // Camera listener, when video is taken on Android 10+ we need to update IS_PENDING to 0.
        camera.addCameraListener(new CameraListener() {
            @Override
            public void onVideoTaken(@NonNull VideoResult result) {
                if (Build.VERSION.SDK_INT >= 29) {
                    // Uri and resolver were saved right after file was created.
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

    // Called from MainActivity.
    public void startRec() {
        // Create FileDescriptor and give it to camera API.
        FileDescriptor videoFileDescriptor = null;
        try {
            videoFileDescriptor = Utils.createVideoFile(requireContext());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        camera.open();
        if (videoFileDescriptor != null)
            camera.takeVideo(videoFileDescriptor);
    }

    // Called from MainActivity
    public void stopRec() {
        camera.stopVideo();
        camera.close();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity) requireActivity()).initializePreviewFragment();
    }
}