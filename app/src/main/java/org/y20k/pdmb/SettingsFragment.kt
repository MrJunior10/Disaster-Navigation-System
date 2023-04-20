

package org.y20k.pdmb


import YesNoDialog
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.preference.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.y20k.pdmb.core.Tracklist
import org.y20k.pdmb.helpers.AppThemeHelper
import org.y20k.pdmb.helpers.FileHelper
import org.y20k.pdmb.helpers.LengthUnitHelper
import org.y20k.pdmb.helpers.LogHelper

class SettingsFragment : PreferenceFragmentCompat(), YesNoDialog.YesNoDialogListener {

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(SettingsFragment::class.java)


    /* Overrides onViewCreated from PreferenceFragmentCompat */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // set the background color
        view.setBackgroundColor(resources.getColor(R.color.app_window_background, null))
    }


    /* Overrides onCreatePreferences from PreferenceFragmentCompat */
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        val context = preferenceManager.context
        val screen = preferenceManager.createPreferenceScreen(context)

        // set up "Restrict to GPS" preference
        val preferenceGpsOnly: SwitchPreferenceCompat = SwitchPreferenceCompat(activity as Context)
        preferenceGpsOnly.title = getString(R.string.pref_gps_only_title)
        preferenceGpsOnly.setIcon(R.drawable.ic_gps_24dp)
        preferenceGpsOnly.key = Keys.PREF_GPS_ONLY
        preferenceGpsOnly.summaryOn = getString(R.string.pref_gps_only_summary_gps_only)
        preferenceGpsOnly.summaryOff = getString(R.string.pref_gps_only_summary_gps_and_network)
        preferenceGpsOnly.setDefaultValue(false)

        // set up "Use Imperial Measurements" preference
        val preferenceImperialMeasurementUnits: SwitchPreferenceCompat = SwitchPreferenceCompat(activity as Context)
        preferenceImperialMeasurementUnits.title = getString(R.string.pref_imperial_measurement_units_title)
        preferenceImperialMeasurementUnits.setIcon(R.drawable.ic_square_foot_24px)
        preferenceImperialMeasurementUnits.key = Keys.PREF_USE_IMPERIAL_UNITS
        preferenceImperialMeasurementUnits.summaryOn = getString(R.string.pref_imperial_measurement_units_summary_imperial)
        preferenceImperialMeasurementUnits.summaryOff = getString(R.string.pref_imperial_measurement_units_summary_metric)
        preferenceImperialMeasurementUnits.setDefaultValue(LengthUnitHelper.useImperialUnits())

        // set up "App Theme" preference
        val preferenceThemeSelection: ListPreference = ListPreference(activity as Context)
        preferenceThemeSelection.title = getString(R.string.pref_theme_selection_title)
        preferenceThemeSelection.setIcon(R.drawable.ic_smartphone_24dp)
        preferenceThemeSelection.key = Keys.PREF_THEME_SELECTION
        preferenceThemeSelection.summary = "${getString(R.string.pref_theme_selection_summary)} ${AppThemeHelper.getCurrentTheme(activity as Context)}"
        preferenceThemeSelection.entries = arrayOf(getString(R.string.pref_theme_selection_mode_device_default), getString(R.string.pref_theme_selection_mode_light), getString(R.string.pref_theme_selection_mode_dark))
        preferenceThemeSelection.entryValues = arrayOf(Keys.STATE_THEME_FOLLOW_SYSTEM, Keys.STATE_THEME_LIGHT_MODE, Keys.STATE_THEME_DARK_MODE)
        preferenceThemeSelection.setOnPreferenceChangeListener { preference, newValue ->
            if (preference is ListPreference) {
                val index: Int = preference.entryValues.indexOf(newValue)
                preferenceThemeSelection.summary = "${getString(R.string.pref_theme_selection_summary)} ${preference.entries[index]}"
                return@setOnPreferenceChangeListener true
            } else {
                return@setOnPreferenceChangeListener false
            }
        }

        // set up "Delete Non-Starred" preference
        val preferenceDeleteNonStarred: Preference = Preference(activity as Context)
        preferenceDeleteNonStarred.title = getString(R.string.pref_delete_non_starred_title)
        preferenceDeleteNonStarred.setIcon(R.drawable.ic_delete_24dp)
        preferenceDeleteNonStarred.summary = getString(R.string.pref_delete_non_starred_summary)
        preferenceDeleteNonStarred.setOnPreferenceClickListener{
            YesNoDialog(this as YesNoDialog.YesNoDialogListener).show(context = activity as Context, type = Keys.DIALOG_DELETE_NON_STARRED, message = R.string.dialog_yes_no_message_delete_non_starred, yesButton = R.string.dialog_yes_no_positive_button_delete_non_starred)
            return@setOnPreferenceClickListener true
        }

        // set up "Recording Accuracy" preference
        val preferenceRecordingAccuracy: SwitchPreferenceCompat = SwitchPreferenceCompat(activity as Context)
        preferenceRecordingAccuracy.title = getString(R.string.pref_recording_accuracy_title)
        preferenceRecordingAccuracy.setIcon(R.drawable.ic_timeline_24dp)
        preferenceRecordingAccuracy.key = Keys.PREF_RECORDING_ACCURACY_HIGH
        preferenceRecordingAccuracy.summaryOn = getString(R.string.pref_recording_accuracy_summary_high)
        preferenceRecordingAccuracy.summaryOff = getString(R.string.pref_recording_accuracy_summary_default)
        preferenceRecordingAccuracy.setDefaultValue(false)

        // set up "Altitude Smoothing" preference
//        val preferenceAltitudeSmoothingValue: SeekBarPreference = SeekBarPreference(activity as Context)
//        preferenceAltitudeSmoothingValue.title = getString(R.string.pref_altitude_smoothing_value_title)
//        preferenceAltitudeSmoothingValue.setIcon(R.drawable.ic_bar_chart_24)
//        preferenceAltitudeSmoothingValue.key = Keys.PREF_ALTITUDE_SMOOTHING_VALUE
//        preferenceAltitudeSmoothingValue.summary = getString(R.string.pref_altitude_smoothing_value_summary)
//        preferenceAltitudeSmoothingValue.showSeekBarValue = true
//        preferenceAltitudeSmoothingValue.min = Keys.MIN_NUMBER_OF_WAYPOINTS_FOR_ELEVATION_CALCULATION
//        preferenceAltitudeSmoothingValue.max = Keys.MAX_NUMBER_OF_WAYPOINTS_FOR_ELEVATION_CALCULATION
//        preferenceAltitudeSmoothingValue.setDefaultValue(Keys.DEFAULT_ALTITUDE_SMOOTHING_VALUE)


        // set up "Reset" preference
        val preferenceResetAdvanced: Preference = Preference(activity as Context)
        preferenceResetAdvanced.title = getString(R.string.pref_reset_advanced_title)
        preferenceResetAdvanced.setIcon(R.drawable.ic_undo_24dp)
        preferenceResetAdvanced.summary = getString(R.string.pref_reset_advanced_summary)
        preferenceResetAdvanced.setOnPreferenceClickListener{
            // reset "Recording Accuracy" preference
            preferenceRecordingAccuracy.isChecked = false
//            preferenceAltitudeSmoothingValue.value = Keys.DEFAULT_ALTITUDE_SMOOTHING_VALUE
            return@setOnPreferenceClickListener true
        }






        // set preference categories
        val preferenceCategoryGeneral: PreferenceCategory = PreferenceCategory(activity as Context)
        preferenceCategoryGeneral.title = getString(R.string.pref_general_title)
        preferenceCategoryGeneral.contains(preferenceImperialMeasurementUnits)
        preferenceCategoryGeneral.contains(preferenceGpsOnly)
        val preferenceCategoryMaintenance: PreferenceCategory = PreferenceCategory(activity as Context)
        preferenceCategoryMaintenance.title = getString(R.string.pref_maintenance_title)
        preferenceCategoryMaintenance.contains(preferenceDeleteNonStarred)

        val preferenceCategoryAdvanced: PreferenceCategory = PreferenceCategory(activity as Context)
        preferenceCategoryAdvanced.title = getString(R.string.pref_advanced_title)
        preferenceCategoryAdvanced.contains(preferenceRecordingAccuracy)
//        preferenceCategoryAdvanced.contains(preferenceAltitudeSmoothingValue)
        preferenceCategoryAdvanced.contains(preferenceResetAdvanced)

        val preferenceCategoryAbout: PreferenceCategory = PreferenceCategory(context)



        // setup preference screen
        screen.addPreference(preferenceCategoryGeneral)
        screen.addPreference(preferenceGpsOnly)
        screen.addPreference(preferenceImperialMeasurementUnits)
        screen.addPreference(preferenceThemeSelection)
        screen.addPreference(preferenceCategoryMaintenance)
        screen.addPreference(preferenceDeleteNonStarred)
        screen.addPreference(preferenceCategoryAdvanced)
        screen.addPreference(preferenceRecordingAccuracy)
//        screen.addPreference(preferenceAltitudeSmoothingValue)
        screen.addPreference(preferenceResetAdvanced)
        screen.addPreference(preferenceCategoryAbout)

        preferenceScreen = screen
    }


    /* Overrides onYesNoDialog from YesNoDialogListener */
    override fun onYesNoDialog(type: Int, dialogResult: Boolean, payload: Int, payloadString: String) {
        when (type) {
            Keys.DIALOG_DELETE_NON_STARRED -> {
                if (dialogResult) {
                    // user tapped delete
                    deleteNonStarred(activity as Context)
                }
            }
            else -> {
                super.onYesNoDialog(type, dialogResult, payload, payloadString)
            }
        }
    }


    /* Removes track and track files for given position - used by TracklistFragment */
    private fun deleteNonStarred(context: Context) {
        CoroutineScope(IO).launch  {
            var tracklist: Tracklist = FileHelper.readTracklist(context)
            val deferred: Deferred<Tracklist> = async { FileHelper.deleteNonStarredSuspended(context, tracklist) }
            // wait for result and store in tracklist
            tracklist = deferred.await()
        }
    }


}