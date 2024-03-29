package it.snasaunive.nearbyvideorec;


// Singleton-enum pattern is the suggested implementation of a Singleton.
public enum SavedUIData {
    INSTANCE;

    // Declare elements that need to be monitored.
    private Boolean client_status_switch;
    private Boolean server_status_switch;
    private Boolean recording;
    private String recording_device;

    // Initial status for each element when fragment is created for the first time.
    SavedUIData() {
        client_status_switch = false;
        server_status_switch = false;
        recording = false;
        recording_device = "None";
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

    public Boolean getRecording() {
        return recording;
    }

    public void setRecording(Boolean recording) {
        this.recording = recording;
    }

    public void setRecording_device(String recording_device){
        this.recording_device = recording_device;
    }

    public String getRecording_device(){
        return recording_device;
    }
}
