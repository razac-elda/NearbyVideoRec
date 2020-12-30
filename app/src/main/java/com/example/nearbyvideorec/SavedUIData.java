package com.example.nearbyvideorec;

import android.widget.TextView;

import java.util.ArrayList;

// Singleton-enum pattern is the suggested implementation of a Singleton nowadays.
public enum SavedUIData {
    INSTANCE;

    // Declare elements that need to be monitored.
    private Boolean client_status_switch;
    private Boolean server_status_switch;
    private String client_connection_status;
    private String videoNamesText;

    // Initial status for each element when fragment is created for the first time.
    SavedUIData() {
        client_status_switch = false;
        server_status_switch = false;
        client_connection_status = "Not connected";
        videoNamesText = "No video selected";

    }

    // Getter and Setters for all the elements.

    public Boolean getClient_status_switch() {
        return client_status_switch;
    }

    public void setClient_status_switch(Boolean client_status_switch) {
        this.client_status_switch = client_status_switch;
    }

    public Boolean getServer_status_switch() {
        return server_status_switch;
    }

    public void setServer_status_switch(Boolean server_status_switch) {
        this.server_status_switch = server_status_switch;
    }

    public String getClient_connection_status() {
        return client_connection_status;
    }

    public void setClient_connection_status(String client_connection_status) {
        this.client_connection_status = client_connection_status;
    }

    public String getVideoNamesText() {
        return videoNamesText;
    }

    public void setVideoNamesText(ArrayList<String> videoNamesListString) {

        if (videoNamesListString.isEmpty())
            this.videoNamesText = "No video selected";
        else{
            String fullString = "";
            for (String name : videoNamesListString) {
                fullString = fullString + name + "\n";
            }
            this.videoNamesText = fullString;
        }
    }

}
