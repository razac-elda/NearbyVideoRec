package it.snasaunive.nearbyvideorec;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.controls.Engine;
import com.otaliastudios.cameraview.controls.Mode;

public class CameraPreview extends Fragment {

    private CameraView camera;

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

        camera.addCameraListener(((MainActivity) requireActivity()).getCameraListener());

        camera.setMode(Mode.VIDEO);

        ((MainActivity) requireActivity()).setCamera(camera);

        return root;
    }
}