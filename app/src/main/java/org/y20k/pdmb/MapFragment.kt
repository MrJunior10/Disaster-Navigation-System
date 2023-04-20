

package org.y20k.pdmb

import YesNoDialog
import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.os.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.y20k.pdmb.core.Track
import org.y20k.pdmb.core.TracklistElement
import org.y20k.pdmb.helpers.*
import org.y20k.pdmb.ui.MapFragmentLayoutHolder


class MapFragment : Fragment(), YesNoDialog.YesNoDialogListener, MapOverlayHelper.MarkerListener {

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(MapFragment::class.java)


    /* Main class variables */
    private var bound: Boolean = false
    private val handler: Handler = Handler(Looper.getMainLooper())
    private var trackingState: Int = Keys.STATE_TRACKING_NOT
    private var gpsProviderActive: Boolean = false
    private var networkProviderActive: Boolean = false
    private var track: Track = Track()
    private lateinit var currentBestLocation: Location
    private lateinit var layout: MapFragmentLayoutHolder
    private lateinit var trackerService: TrackerService


    /* Overrides onCreate from Fragment */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO make only MapFragment's status bar transparent - see: https://gist.github.com/Dvik/a3de88d39da9d1d6d175025a56c5e797#file-viewextension-kt and https://proandroiddev.com/android-full-screen-ui-with-transparent-status-bar-ef52f3adde63
        // get current best location
        currentBestLocation = LocationHelper.getLastKnownLocation(activity as Context)
        // get saved tracking state
        trackingState = PreferencesHelper.loadTrackingState()
    }


    /* Overrides onStop from Fragment */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // initialize layout
        val statusBarHeight: Int = UiHelper.getStatusBarHeight(activity as Context)
        layout = MapFragmentLayoutHolder(activity as Context, this as MapOverlayHelper.MarkerListener, inflater, container, statusBarHeight, currentBestLocation, trackingState)

        // set up buttons
        layout.currentLocationButton.setOnClickListener {
            layout.centerMap(currentBestLocation, animated = true)
        }
        layout.mainButton.setOnClickListener {
            handleTrackingManagementMenu()
        }
        layout.saveButton.setOnClickListener {
            saveTrack()
        }
        layout.clearButton.setOnClickListener {
            if (track.wayPoints.isNotEmpty()) {
                YesNoDialog(this as YesNoDialog.YesNoDialogListener).show(context = activity as Context, type = Keys.DIALOG_DELETE_CURRENT_RECORDING, message = R.string.dialog_delete_current_recording_message, yesButton = R.string.dialog_delete_current_recording_button_discard)
            } else {
                trackerService.clearTrack()
            }
        }

        return layout.rootView
    }


    /* Overrides onStart from Fragment */
    override fun onStart() {
        super.onStart()
        // request location permission if denied
        if (ContextCompat.checkSelfPermission(activity as Context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        // bind to TrackerService
        activity?.bindService(Intent(activity, TrackerService::class.java), connection, Context.BIND_AUTO_CREATE)
    }


    /* Overrides onResume from Fragment */
    override fun onResume() {
        super.onResume()
//        if (bound) {
//            trackerService.addGpsLocationListener()
//            trackerService.addNetworkLocationListener()
//        }
    }


    /* Overrides onPause from Fragment */
    override fun onPause() {
        super.onPause()
        layout.saveState(currentBestLocation)
        if (bound && trackingState != Keys.STATE_TRACKING_ACTIVE) {
            trackerService.removeGpsLocationListener()
            trackerService.removeNetworkLocationListener()
        }
    }


    /* Overrides onStop from Fragment */
    override fun onStop() {
        super.onStop()
        // unbind from TrackerService
        activity?.unbindService(connection)
        handleServiceUnbind()
    }

    /* Register the permission launcher for requesting location */
    private val requestLocationPermissionLauncher = registerForActivityResult(RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            // permission was granted - re-bind service
            activity?.unbindService(connection)
            activity?.bindService(Intent(activity, TrackerService::class.java),  connection,  Context.BIND_AUTO_CREATE)
            LogHelper.i(TAG, "Request result: Location permission has been granted.")
        } else {
            // permission denied - unbind service
            activity?.unbindService(connection)
        }
        layout.toggleLocationErrorBar(gpsProviderActive, networkProviderActive)
    }

    /* Register the permission launcher for starting the tracking service */
    private val startTrackingPermissionLauncher = registerForActivityResult(RequestPermission()) { isGranted: Boolean ->
        logPermissionRequestResult(isGranted)
        // start service via intent so that it keeps running after unbind
        startTrackerService()
        trackerService.startTracking()
    }

    /* Register the permission launcher for resuming the tracking service */
    private val resumeTrackingPermissionLauncher = registerForActivityResult(RequestPermission()) { isGranted: Boolean ->
        logPermissionRequestResult(isGranted)
        // start service via intent so that it keeps running after unbind
        startTrackerService()
        trackerService.resumeTracking()
    }

    /* Logs the request result of the Activity Recognition permission launcher */
    private fun logPermissionRequestResult(isGranted: Boolean) {
        if (isGranted) {
            LogHelper.i(TAG, "Request result: Activity Recognition permission has been granted.")
        } else {
            LogHelper.i(TAG, "Request result: Activity Recognition permission has NOT been granted.")
        }
    }

    /* Overrides onYesNoDialog from YesNoDialogListener */
    override fun onYesNoDialog(type: Int, dialogResult: Boolean, payload: Int, payloadString: String) {
        super.onYesNoDialog(type, dialogResult, payload, payloadString)
        when (type) {
            Keys.DIALOG_EMPTY_RECORDING -> {
                if (dialogResult) {
                    // user tapped resume
                    trackerService.resumeTracking()
                }
            }
            Keys.DIALOG_DELETE_CURRENT_RECORDING -> {
                if (dialogResult) {
                    trackerService.clearTrack()
                }
            }
        }
    }


    /* Overrides onMarkerTapped from MarkerListener */
    override fun onMarkerTapped(latitude: Double, longitude: Double) {
        super.onMarkerTapped(latitude, longitude)
        if (bound) {
            track = TrackHelper.toggleStarred(activity as Context, track, latitude, longitude)
            layout.overlayCurrentTrack(track, trackingState)
            trackerService.track = track
        }
    }


    /* Start recording waypoints */
    private fun startTracking() {
        // request activity recognition permission on Android Q+ if denied
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(activity as Context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_DENIED) {
            startTrackingPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        } else {
            // start service via intent so that it keeps running after unbind
            startTrackerService()
            trackerService.startTracking()
        }
    }


    /* Resume recording waypoints */
    private fun resumeTracking() {
        // request activity recognition permission on Android Q+ if denied
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(activity as Context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_DENIED) {
            resumeTrackingPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        } else {
            // start service via intent so that it keeps running after unbind
            startTrackerService()
            trackerService.resumeTracking()
        }
    }


    /* Start tracker service */
    private fun startTrackerService() {
        val intent = Intent(activity, TrackerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // ... start service in foreground to prevent it being killed on Oreo
            activity?.startForegroundService(intent)
        } else {
            activity?.startService(intent)
        }
    }


    /* Handles state when service is being unbound */
    private fun handleServiceUnbind() {
        bound = false
        // unregister listener for changes in shared preferences
        PreferencesHelper.unregisterPreferenceChangeListener(sharedPreferenceChangeListener)
        // stop receiving location updates
        handler.removeCallbacks(periodicLocationRequestRunnable)
    }



    /* Starts / pauses tracking and toggles the recording sub menu_bottom_navigation */
    private fun handleTrackingManagementMenu() {
        when (trackingState) {
            Keys.STATE_TRACKING_PAUSED -> resumeTracking()
            Keys.STATE_TRACKING_ACTIVE -> trackerService.stopTracking()
            Keys.STATE_TRACKING_NOT -> startTracking()
        }
    }


    /* Saves track - shows dialog, if recording is still empty */
    private fun saveTrack() {
        if (track.wayPoints.isEmpty()) {
            YesNoDialog(this as YesNoDialog.YesNoDialogListener).show(context = activity as Context, type = Keys.DIALOG_EMPTY_RECORDING, message = R.string.dialog_error_empty_recording_message, yesButton = R.string.dialog_error_empty_recording_button_resume)
        } else {
            CoroutineScope(IO).launch {
                // step 1: create and store filenames for json and gpx files
                track.trackUriString = FileHelper.getTrackFileUri(activity as Context, track).toString()
                track.gpxUriString = FileHelper.getGpxFileUri(activity as Context, track).toString()
                // step 2: save track
                FileHelper.saveTrackSuspended(track, saveGpxToo = true)
                // step 3: save tracklist - suspended
                FileHelper.addTrackAndSaveTracklistSuspended(activity as Context, track)
                // step 3: clear track
                trackerService.clearTrack()
                // step 4: open track in TrackFragement
                withContext(Main) {
                    openTrack(track.toTracklistElement(activity as Context))
                }
            }
        }
    }


    /* Opens a track in TrackFragment */
    private fun openTrack(tracklistElement: TracklistElement) {
        val bundle: Bundle = Bundle()
        bundle.putString(Keys.ARG_TRACK_TITLE, tracklistElement.name)
        bundle.putString(Keys.ARG_TRACK_FILE_URI, tracklistElement.trackUriString)
        bundle.putString(Keys.ARG_GPX_FILE_URI, tracklistElement.gpxUriString)
        bundle.putLong(Keys.ARG_TRACK_ID, TrackHelper.getTrackId(tracklistElement))
        findNavController().navigate(R.id.action_map_fragment_to_track_fragment, bundle)
    }


    /*
     * Defines the listener for changes in shared preferences
     */
    private val sharedPreferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        when (key) {
            Keys.PREF_TRACKING_STATE -> {
                if (activity != null) {
                    trackingState = PreferencesHelper.loadTrackingState()
                    layout.updateMainButton(trackingState)
                }
            }
        }
    }
    /*
     * End of declaration
     */


    /*
     * Defines callbacks for service binding, passed to bindService()
     */
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            bound = true
            // get reference to tracker service
            val binder = service as TrackerService.LocalBinder
            trackerService = binder.service
            // get state of tracking and update button if necessary
            trackingState = trackerService.trackingState
            layout.updateMainButton(trackingState)
            // register listener for changes in shared preferences
            PreferencesHelper.registerPreferenceChangeListener(sharedPreferenceChangeListener)
            // start listening for location updates
            handler.removeCallbacks(periodicLocationRequestRunnable)
            handler.postDelayed(periodicLocationRequestRunnable, 0)
        }
        override fun onServiceDisconnected(arg0: ComponentName) {
            // service has crashed, or was killed by the system
            handleServiceUnbind()
        }
    }
    /*
     * End of declaration
     */


    /*
     * Runnable: Periodically requests location
     */
    private val periodicLocationRequestRunnable: Runnable = object : Runnable {
        override fun run() {
            // pull current state from service
            currentBestLocation = trackerService.currentBestLocation
            track = trackerService.track
            gpsProviderActive = trackerService.gpsProviderActive
            networkProviderActive = trackerService.networkProviderActive
            trackingState = trackerService.trackingState
            // update location and track
            layout.markCurrentPosition(currentBestLocation, trackingState)
            layout.overlayCurrentTrack(track, trackingState)
            layout.updateLiveStatics(length = track.length, duration = track.duration, trackingState = trackingState)
            // center map, if it had not been dragged/zoomed before
            if (!layout.userInteraction) { layout.centerMap(currentBestLocation, true)}
            // show error snackbar if necessary
            layout.toggleLocationErrorBar(gpsProviderActive, networkProviderActive)
            // use the handler to start runnable again after specified delay
            handler.postDelayed(this, Keys.REQUEST_CURRENT_LOCATION_INTERVAL)
        }
    }
    /*
     * End of declaration
     */

}
