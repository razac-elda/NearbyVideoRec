package it.snasaunive.nearbyvideorec;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.icu.text.SimpleDateFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.widget.Toast;

import com.arthenica.mobileffmpeg.ExecuteCallback;
import com.arthenica.mobileffmpeg.FFmpeg;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

public final class Utils {

    private static Uri uriSavedVideo;
    private static ContentResolver resolver;


    public static Uri getUriSavedVideo() {
        return uriSavedVideo;
    }

    public static ContentResolver getResolver() {
        return resolver;
    }

    public static String createNameVideoFile() {
        return "merged_" + getTimeStampString() + ".mp4";
    }

    public static String getTimeStampString() {
        return new SimpleDateFormat("dd-MM-yy_HH-mm-ss", Locale.getDefault()).format(new Date());
    }

    @SuppressWarnings("deprecation")
    public static FileDescriptor createVideoFile(Context context) throws FileNotFoundException {

        resolver = context.getContentResolver();
        String videoFileName = "rec_" + getTimeStampString() + ".mp4";
        ContentValues valuesVideos = new ContentValues();

        if (Build.VERSION.SDK_INT >= 29) {
            // Compatible with Android Scoped Storage
            Uri collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
            valuesVideos.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/NearbyVideoRec");
            valuesVideos.put(MediaStore.Video.Media.TITLE, videoFileName);
            valuesVideos.put(MediaStore.Video.Media.DISPLAY_NAME, videoFileName);
            valuesVideos.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            valuesVideos.put(MediaStore.Video.Media.IS_PENDING, 1);
            uriSavedVideo = resolver.insert(collection, valuesVideos);

        } else {
            // For legacy storage device
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

    public static boolean checkCameraAPI(Context context) {
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


    // parameter :res. the input for resolution in this format  heightxwidht or widthxheight example: "1920x1080"
    public static void mergeVideo(Context context, ArrayList<String> inputFiles, String res, String fps) {
        // File name and path where it will be created.
        String fileOutputName = createNameVideoFile();
        String directory = Environment.getExternalStorageDirectory().getAbsolutePath() +
                File.separator + Environment.DIRECTORY_MOVIES + File.separator + "NearbyVideoRec" + File.separator;

        // Create directory if it does not exist.
        File newDirectory = new File(directory);
        if (!newDirectory.exists())
            newDirectory.mkdirs();

        /*  We use StringBuilder to create the FFmpeg command with this format:
         * ffmpeg -i input1.mp4 -i input2.webm -i input3.mov \
         * -filter_complex "[0:v:0][0:a:0][1:v:0][1:a:0][2:v:0][2:a:0]concat=n=3:v=1:a=1[outv][outa]" \
         * -s 1920x1080 -r 30 -codec:v libx264 -crf 24 -preset veryfast \
         * -map "[outv]" -map "[outa]" output.mp4
         */

        String dim[] = res.split("x");
        String scale = "scale=" + dim[0] + ":" + dim[1];

        StringBuilder files = new StringBuilder();
        StringBuilder inputStream = new StringBuilder();
        inputStream.append("-filter_complex \"");

        for (String file : inputFiles)
            files.append("-i ").append("'").append(file).append("'").append(" ");

        for (int n_file = 0; n_file < inputFiles.size(); n_file++) {
            inputStream.append("[").append(n_file).append(":v]")
                    .append(scale).append(",").append("setsar=1").append("[fv").append(n_file).append("];");
        }

        for (int n_file = 0; n_file < inputFiles.size(); n_file++) {
            inputStream.append(" ").append("[fv").append(n_file).append("]").append(" ").append("[").append(n_file).append(":a]").append(" ");
        }

        inputStream.append("concat=n=").append(inputFiles.size()).append(":v=1:a=1[v][a]\" ");
        String codec = "-codec:v libx264 -crf 24";
        String preset = "-preset ultrafast";

        String cmd =
                files.toString() +
                        inputStream.toString() +
                        " -r " + fps + " " + codec + " " + preset + " " +
                        "-map \"[v]\" " + "-map \"[a]\" " + directory + fileOutputName;

        FFmpeg.executeAsync(cmd, new ExecuteCallback() {
            @Override
            public void apply(long executionId, int returnCode) {
                switch (returnCode) {
                    case RETURN_CODE_SUCCESS:

                        String newName = Utils.createNameVideoFile();

                        //process for rename video
                        File old = new File(directory, fileOutputName);
                        File n = new File(directory, newName);
                        if (old.renameTo(n)) {
                            System.out.println("RINOMINAZIONE RIUSCITA");
                        }
                        //put video in mediastore
                        //create file used for mediastore insert
                        File generatedVideo = new File(directory, newName);
                        //prende la durata del video usando mediaplayer
                        MediaPlayer mp = MediaPlayer.create(context, Uri.fromFile(generatedVideo));
                        int duration = mp.getDuration();
                        mp.release();

                        ContentResolver resolver = context.getContentResolver();
                        ContentValues values = new ContentValues();
                        //aggiunta sul mediastore
                        values.put(MediaStore.Video.Media.TITLE, newName);
                        values.put(MediaStore.Video.Media.DISPLAY_NAME, newName);
                        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
                        values.put(MediaStore.Video.Media.DATA, generatedVideo.getAbsolutePath());
                        //inserisce la durata del video da mostrare
                        values.put(MediaStore.Video.VideoColumns.DURATION, duration);

                        resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);

                        Toast.makeText(context, R.string.merge_success, Toast.LENGTH_SHORT).show();
                        break;
                    case RETURN_CODE_CANCEL:
                        Toast.makeText(context, R.string.merge_cancel, Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        Toast.makeText(context, R.string.merge_not_found, Toast.LENGTH_LONG).show();
                        break;
                }


            }
        });
    }

    // Convert an Uri to an absolute path
    public static String getPathFromURI(Context context, Uri uri) {
        String selection = null;
        String[] selectionArgs = null;
        if (DocumentsContract.isDocumentUri(context.getApplicationContext(), uri)) {
            if (isExternalStorageDocument(uri)) {
                String docId = DocumentsContract.getDocumentId(uri);
                String[] split = docId.split(":");
                String type = split[0];
                System.out.println("-------------------------------------------------------------------------");
                System.out.println(docId);
                System.out.println(type);
                System.out.println("---------------------------------------------------------------------------------");

                if (type.contains("primary")) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                } else {
                    //crea il path se il video è su scheda sd
                    return "storage" + "/" + type + "/" + split[1];

                }
            } else if (isDownloadsDocument(uri)) {
                String id = DocumentsContract.getDocumentId(uri);
                uri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));
            } else if (isMediaDocument(uri)) {
                String docId = DocumentsContract.getDocumentId(uri);
                String[] split = docId.split(":");
                String type = split[0];
                if ("image".equals(type)) {
                    uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                selection = "_id=?";
                selectionArgs = new String[]{split[1]};
            }
        }
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {MediaStore.Video.Media.DATA};
            Cursor cursor;
            try {
                cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                if (cursor.moveToFirst()) {
                    String result = cursor.getString(column_index);
                    cursor.close();
                    return result;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }


}
