package de.schooltec.datapass

/**
 * Final class just for providing constant preference keys used to store and retrieve values via shared preferences.
 *
 * @author Andreas Hellwig
 */
object PreferenceKeys {
    const val PREFERENCE_FILE_RESULT_DATA = "preference_file_result_data"
    const val PREFERENCE_FILE_MISC = "preference_file_misc"

    const val SAVED_TRAFFIC_PROPORTION = "trafficProportion"
    const val SAVED_TRAFFIC_UNIT = "trafficUnit"
    const val SAVED_TRAFFIC_WASTED_PERCENTAGE = "trafficWastedPercentage"
    const val SAVED_LAST_UPDATE = "lastUpdate"
    const val SAVED_APP_IDS = "app_ids"
    const val SAVED_HINT = "hint"
}
