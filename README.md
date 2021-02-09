# NearbyVideoRec
### Ca'Foscari University - "Software engineering" course project 2020-2021

Android application used to record a video among devices using [Google Nearby Connections API](https://developers.google.com/nearby/connections/overview).

You can connect two or more devices together having one of them acting as a "server", the other devices acting as "clients" wait for a request by the server to start a video recording. The video is sent to the server when a client recieves a request to stop the recording. There is also the possibility to merge all the videos recorded thanks to FFmpeg.

We used the Navigation Component to make a single activity app with each destination as a fragment.

More detailed documentation can be found inside the **docs** folder(Italian :it:).

## Testing

* Clone the repo.
* Using Android Studio run the app on a device Android 7.0+.
* To test all the app functionalities you must have at least two devices.
