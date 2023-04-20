package org.y20k.pdmb

import android.app.Application
import com.google.android.material.color.DynamicColors
import org.y20k.pdmb.helpers.AppThemeHelper
import org.y20k.pdmb.helpers.LogHelper
import org.y20k.pdmb.helpers.PreferencesHelper
import org.y20k.pdmb.helpers.PreferencesHelper.initPreferences

class pdmb: Application() {


    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(pdmb::class.java)


    /* Implements onCreate */
    override fun onCreate() {
        super.onCreate()
        LogHelper.v(TAG, "Trackbook application started.")
        DynamicColors.applyToActivitiesIfAvailable(this);
        // initialize single sharedPreferences object when app is launched
        initPreferences()
        // set Dark / Light theme state
        AppThemeHelper.setTheme(PreferencesHelper.loadThemeSelection())
    }


    /* Implements onTerminate */
    override fun onTerminate() {
        super.onTerminate()
        LogHelper.v(TAG, "Trackbook application terminated.")
    }

}