package org.y20k.pdmb

import android.content.Intent
import android.content.SharedPreferences
import android.opengl.Visibility
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

import org.osmdroid.config.Configuration
import org.y20k.pdmb.helpers.AppThemeHelper
import org.y20k.pdmb.helpers.LogHelper
import org.y20k.pdmb.helpers.PreferencesHelper
import kotlin.time.ExperimentalTime

class MainActivity : AppCompatActivity() {

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(MainActivity::class.java)


    /* Main class variables */
////    private lateinit var navHostFragment: NavHostFragment
//    private lateinit var bottomNavigationView: BottomNavigationView
//    private lateinit var navController: NavController

    /* Overrides onCreate from AppCompatActivity */
    @OptIn(ExperimentalTime::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // todo: remove after testing finished
        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            StrictMode.setVmPolicy(
                VmPolicy.Builder()
                    .detectNonSdkApiUsage()
                    .penaltyLog()
                    .build()
            )
        }

        // set user agent to prevent getting banned from the osm servers
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
        // set the path for osmdroid's files (e.g. tile cache)
        Configuration.getInstance().osmdroidBasePath = this.getExternalFilesDir(null)

        // set up views
        setContentView(R.layout.activity_main)
        //navHostFragment  = findViewById<BottomNavigationView>(R.id.main_container)
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
        val navController = findNavController(R.id.main_container)
        bottomNavigationView.setupWithNavController(navController = navController)

        // listen for navigation changes
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.track_fragment -> {
                    runOnUiThread {
                        run {
                            // mark menu item "Tracks" as checked
                            bottomNavigationView.menu.findItem(R.id.tracklist_fragment).isChecked = true
                        }
                    }
                }
                else -> {
                    // do nothing
                }
            }
        }

        // register listener for changes in shared preferences
        PreferencesHelper.registerPreferenceChangeListener(sharedPreferenceChangeListener)
        // Get a reference to the button
        val wifiButton = findViewById<Button>(R.id.wifi)
        wifiButton.setOnClickListener {
            val intent = Intent(this, MainActivity2::class.java)
            startActivity(intent)
        }
    }



    /* Overrides onDestroy from AppCompatActivity */
    override fun onDestroy() {
        super.onDestroy()
        // unregister listener for changes in shared preferences
        PreferencesHelper.unregisterPreferenceChangeListener(sharedPreferenceChangeListener)

    }


    /*
     * Defines the listener for changes in shared preferences
     */
    private val sharedPreferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        when (key) {
            Keys.PREF_THEME_SELECTION -> {
                AppThemeHelper.setTheme(PreferencesHelper.loadThemeSelection())
            }
        }
    }
    /*
     * End of declaration
     */
}

