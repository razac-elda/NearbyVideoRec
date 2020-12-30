package com.example.nearbyvideorec.ui.video;


import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;

import android.database.Cursor;
import android.net.Uri;

import android.os.Build;
import android.os.Bundle;
import android.os.Environment;

import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import androidx.lifecycle.ViewModelProvider;


import com.example.nearbyvideorec.R;
import com.example.nearbyvideorec.SavedUIData;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;


import static android.app.Activity.RESULT_OK;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

//TODO IMPORTANTE: I TELEFONI DEVONO AVERE UNA SCHEDA SD PRESENTE NEL DISPOSITIVO
public class VideoFragment extends Fragment {


    private VideoViewModel videoViewModel;
    private SavedUIData savedUIData;

    private Context myc;

    private String space = " ";
    private String apostrofo = "\'";


    private File f;
    private FileOutputStream fos;
    private String fileNameTxt = "myListpaths.txt";

    private Button btn_merge;
    private Button btn_intent_files;
    private TextView videoNamesTextView;

    private Uri folderUri;
    private ArrayList<String> paths_list = new ArrayList<String>();
    private ArrayList<String> videoNames = new ArrayList<>();
    private String textprova;

    public void clearVideoNames(){
        if (videoNames != null)
            videoNames.clear();
    }

    public static String bigTextWith(ArrayList<String> listOfNames){
        String bigText = "";
        String defaultText = "Nessun video selezionato";
        if (listOfNames.isEmpty())
            return defaultText;
        else {
            for (String name : listOfNames) {
                bigText.concat(name).concat("\n");
            }
            return bigText;
        }
    }


    private static final int REQUEST_CODE_BY_INTENT_FILE_CHOOSER = 1234;

    private String generateNameOutputFile() {
        return "Merged_" + getTimeStampString() + ".mp4";
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
        btn_intent_files = (Button) root.findViewById(R.id.btn_select_files);
        videoNamesTextView = (TextView) root.findViewById(R.id.videoNameList);
        textprova = "ciao"+"\n"+"sono una"+"\n"+"stringa di prova"+"\n";
        videoNamesTextView.setText(textprova);

        // aggiunta bottone merge più listener
        btn_merge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //genera il file txt
                generateFileTxT(fileNameTxt);



                String fileOutputName = generateNameOutputFile();
                runCommand("-f concat -safe 0 -i",
                        getDirectoryNameMoviesPathString() + fileNameTxt,
                        "-c:v copy -c:a aac",
                        getDirectoryNameMoviesPathString() + fileOutputName
                );

            }
        });
        //aggiunta bottone file chooser + listener
        btn_intent_files.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                openMyFolder();
            }
        });

        //videoNamesTextView.setText(bigTextWith(videoNames));


        return root;

    }



    //aprire intent file chooser
    public void openMyFolder() {
        Intent chooserfile = new Intent(Intent.ACTION_GET_CONTENT);

        folderUri = Uri.parse(Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator + Environment.DIRECTORY_MOVIES + File.separator);
        chooserfile.setDataAndType(folderUri, "video/mp4");

        chooserfile.addCategory(Intent.CATEGORY_OPENABLE);
        chooserfile = Intent.createChooser(chooserfile, "Open folder");

        //gestione presenza del file manager  //todo da testare
        if(chooserfile.resolveActivityInfo(requireContext().getPackageManager(),0) == null)
            Toast.makeText(requireContext(),"file manager non presente \n ... installarne uno",Toast.LENGTH_SHORT).show();
        else
            startActivityForResult(chooserfile, REQUEST_CODE_BY_INTENT_FILE_CHOOSER);

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == REQUEST_CODE_BY_INTENT_FILE_CHOOSER && resultCode == RESULT_OK && data != null) {
            Uri u = data.getData();
            System.out.println("URI" + u.toString()); // XIAOMI ANDROID 10 : content://com.mi.android.globalFileexplorer.myprovider/external_files/Movies/NOME_VIDEO_SELEZIONATO.MP4

            //todo  fare test x vedere se eliminare la variabile copia dell'uri
            Uri copieduri = u;


            String p = "pathvuoto";
            try {
                p = getPathFromURI(requireContext(), u);
                System.out.println("getPathFromURI  " + p);
                if (p == null){
                    p = myTakePathFromURI(copieduri);
                    System.out.println("myTakePathFromURI  " + p);
                }
                //aggiunta ad array di nomi
                videoNames.add(u.getLastPathSegment());


                System.out.println(videoNames);
                } catch (Exception e) {
                    e.printStackTrace();
                }



            System.out.println("PATH" + p);  //  XIAOMI ANDROID 10 : /storage/emulated/0/Movies/NOME_VIDEO_SELEZIONATO.mp4
            paths_list.add(p);
        }
    }

    // METODO WRAPPER DEL COMANDO
    public void runCommand(String prefix, String filepathInput, String middleOption, String filepathOutput) {

        //generate string command
        String cmd = prefix + space + filepathInput + space + middleOption + space + filepathOutput;
        //execute command
        int rc = FFmpeg.execute(cmd);

        switch (rc) {
            case RETURN_CODE_SUCCESS:
                if (myc == null) {
                    myc = requireContext();
                }
                //Toast.makeText(myc, "RESULTCODE " + rc + " DONE", Toast.LENGTH_SHORT).show();
                Toast.makeText(myc, "video completo generato", Toast.LENGTH_SHORT).show();
                Log.i(Config.TAG, "Command execution completed successfully.");
                //pulisco arraylist di path
                paths_list.clear();

                //pulisco arraylist di nomi
                clearVideoNames();
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
                //Toast.makeText(myc, "RESULTCODE" + rc + "RESULT CODE FAILED", Toast.LENGTH_SHORT).show();
                Toast.makeText(myc, "Selezionare almeno un video", Toast.LENGTH_SHORT).show();
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
            /*
            example
            String s =  "file" + space + apostrofo + "/storage/emulated/0/Movies/video_20201219_0532.mp4" + apostrofo + "\n";
            */

            //SCRITTURA DEI PATH SU FILE DI TESTO
            StringBuilder s = new StringBuilder();
            for (String path : paths_list) {
                s.append("file").append(space).append(apostrofo).append(path).append(apostrofo).append("\n");
            }
            fos.write(s.toString().getBytes());

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


    public static String getTimeStampString() {
        return new SimpleDateFormat("dd-MM-yy_hh-mm-ss", Locale.getDefault()).format(new Date());
    }


    //metodo brutale per prendere il path
    private static String myTakePathFromURI(Uri u){
        String uriString = u.toString();
        String[] parts = uriString.split("/storage");
        String storage = "/storage";
        String path = storage.concat(parts[1]);
        return path;

    }
/*
    public static String getPathFromURI( Context context,  Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                String docId = DocumentsContract.getDocumentId(uri);
                String[] split = docId.split(":");
                String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                 String selection = "_id=?";
                 String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        System.out.println("IL DOCUMENT PROVIDER RITORNERA NULL ");
        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     *//*

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
         String column = "_data";
         String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        System.out.println("il GETDATACOLUMN RITORNERà NULL");
        return null;
    }


    */
/**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     *//*

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    */
/**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     *//*

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    */
/**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     *//*

    public static boolean isMediaDocument(Uri uri) {
        String normalAutority = "com.android.providers.media.documents";
        String huaweiAutorithy = "com.huawei.hidisk.provider"; //todo mettere la stringa strana

        if (normalAutority.equals(uri.getAuthority()) || huaweiAutorithy.equals(uri.getAuthority()))
            return true;
        else return false;
    }

    */
/**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     *//*

    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

*/




        // todo controllare con https://gist.github.com/webserveis/c6d55da4dbfc2fdd13d91dc5b7f85499
        //puo tornare null in alcuni casi con dispositivi vecchi
        private static String getPathFromURI(Context context, Uri uri) throws URISyntaxException {
            boolean needToCheckUri = Build.VERSION.SDK_INT >= 24;
            String selection = null;
            String[] selectionArgs = null;
            // Uri is different in versions after KITKAT (Android 4.4), we need to deal with different Uris.
            if (needToCheckUri && DocumentsContract.isDocumentUri(context.getApplicationContext(), uri)) {
                if (isExternalStorageDocument(uri)) {
                     String docId = DocumentsContract.getDocumentId(uri);
                     String[] split = docId.split(":");
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                } else if (isDownloadsDocument(uri)) {
                    String id = DocumentsContract.getDocumentId(uri);
                    uri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
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
                    selectionArgs = new String[]{ split[1] };
                }
            }
            if ("content".equalsIgnoreCase(uri.getScheme())) {
                String[] projection = { MediaStore.Video.Media.DATA };
                Cursor cursor = null;
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
            return "com.android.providers.media.documents".equals(uri.getAuthority())
                    ||
                    "com.huawei.disk.fileprovider".equals(uri.getAuthority());
        }





}
