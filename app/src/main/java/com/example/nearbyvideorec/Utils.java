package com.example.nearbyvideorec;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.util.Date;
import java.util.Locale;

public final class Utils {

    private static Uri uriSavedVideo;
    private static ContentResolver resolver;

    public static Uri getUriSavedVideo() {
        return uriSavedVideo;
    }

    public static ContentResolver getResolver() {
        return resolver;
    }

    private static String getTimeStampString() {
        return new SimpleDateFormat("dd-MM-yy_hh:mm:ss", Locale.getDefault()).format(new Date());
    }

    public static FileDescriptor createFile(Context context) throws FileNotFoundException {

        uriSavedVideo = null;
        resolver = context.getContentResolver();
        String videoFileName = "rec_" + getTimeStampString() + ".mp4";
        ContentValues valuesVideos = new ContentValues();

        if (Build.VERSION.SDK_INT >= 29) {

            Uri collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
            valuesVideos.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/NearbyVideoRec");
            valuesVideos.put(MediaStore.Video.Media.TITLE, videoFileName);
            valuesVideos.put(MediaStore.Video.Media.DISPLAY_NAME, videoFileName);
            valuesVideos.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            valuesVideos.put(MediaStore.Video.Media.IS_PENDING, 1);
            uriSavedVideo = resolver.insert(collection, valuesVideos);

        } else {

            String directory = Environment.getExternalStorageDirectory().getAbsolutePath() +
                    File.separator + Environment.DIRECTORY_MOVIES + File.separator + "NearbyVideoRec";

            File newDirectory = new File(directory);
            if (!newDirectory.exists())
                newDirectory.mkdirs();

            File createdVideo = new File(directory, videoFileName);

            valuesVideos.put(MediaStore.Video.Media.TITLE, videoFileName);
            valuesVideos.put(MediaStore.Video.Media.DISPLAY_NAME, videoFileName);
            valuesVideos.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");

            valuesVideos.put(MediaStore.Video.Media.DATA, createdVideo.getAbsolutePath());
            uriSavedVideo = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, valuesVideos);
        }

        return resolver.openFileDescriptor(uriSavedVideo, "w").getFileDescriptor();
    }

    public static boolean checkCameraAPI(Context context){
        // Check camera API level
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        boolean legacy = false;
        try {
            // Cycle through all cameras
            for (String cameraId : manager.getCameraIdList()) {

                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);

                // If back camera API support is LEGACY we mark it as "legacy" to avoid using Camera2 API
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    if (characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) ==
                            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
                        legacy = true;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return legacy;
    }

}
