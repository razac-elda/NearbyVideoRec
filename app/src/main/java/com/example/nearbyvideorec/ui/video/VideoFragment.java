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


import com.example.nearbyvideorec.MainActivity;
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

    //private Context myc;
    private String fileNameTxt = "myListpaths.txt";

    private Button btn_merge;
    private Button btn_intent_files;
    private TextView videoNamesTextView;
    private Button btn_clear_files;
    private ArrayList<String> paths_list = new ArrayList<String>();


    private ArrayList<String> videoNames = new ArrayList<>();


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        videoViewModel = new ViewModelProvider(this).get(VideoViewModel.class);
        View root = inflater.inflate(R.layout.fragment_video, container, false);


        savedUIData = SavedUIData.INSTANCE;


        btn_merge = (Button) root.findViewById(R.id.btn_merge);
        btn_intent_files = (Button) root.findViewById(R.id.btn_select_files);
        videoNamesTextView = (TextView) root.findViewById(R.id.videoNameList);
        btn_clear_files = (Button) root.findViewById(R.id.btn_clear_files);
        //textprova = "ciao"+"\n"+"sono una"+"\n"+"stringa di prova"+"\n";

        videoNamesTextView.setText(savedUIData.getVideoNamesText());

        // aggiunta bottone merge più listener
        btn_merge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //genera il file txt
                ((MainActivity) requireActivity()).generateFileTxT(fileNameTxt);



                String fileOutputName = ((MainActivity) requireActivity()).generateNameOutputFile();
                ((MainActivity) requireActivity()).runCommand("-f concat -safe 0 -i",
                        ((MainActivity) requireActivity()).getDirectoryNameMoviesPathString() + fileNameTxt,
                        "-c:v copy -c:a aac",
                        ((MainActivity) requireActivity()).getDirectoryNameMoviesPathString() + fileOutputName
                );

            }
        });
        //aggiunta bottone file chooser + listener
        btn_intent_files.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ((MainActivity) requireActivity()).openMyFolder();
            }
        });

        btn_clear_files.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ((MainActivity) requireActivity()).pulisciLista();
            }
        });


        return root;

    }





    // METODO WRAPPER DEL COMANDO



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









}
