package com.example.nearbyvideorec;

// Singleton-enum pattern is the suggested implementation of a Singleton nowadays.
public enum SavedUIData {
    INSTANCE;

    // Declare elements that need to be monitored.
    private Boolean client_status_switch;
    private Boolean server_status_switch;

    // Initial status for each element when fragment is created for the first time.
    SavedUIData() {
        client_status_switch = false;
        server_status_switch = false;
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
