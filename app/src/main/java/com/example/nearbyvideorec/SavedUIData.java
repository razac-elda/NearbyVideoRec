package com.example.nearbyvideorec;

public class SavedUIData {

    private static SavedUIData instance = null;

    // Declare elements that need to be monitored.
    private Boolean client_status_switch;
    private Boolean server_status_switch;

    // Initial status for each element when fragment is created for the first time.
    public SavedUIData() {
        client_status_switch = false;
        server_status_switch = false;
    }

    public static SavedUIData getInstance() {
        if (instance == null)
            instance = new SavedUIData();
        return instance;
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

}
