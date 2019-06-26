package de.schooltec.datapass

/**
 * @author Martin Hellwig
 * @since  2019-06-26
 */
interface UpdateWidgetAnimationDrawer {
    /**
     * Updates the widget with the globally set data.
     *
     * @param progress         Current progress to set for the circular progress bar
     * @param trafficUnit      Unit for the wasted / available traffic ('MB' or 'GB')
     * @param traffic          The actual proportion of wasted / available traffic (e.g. 2.1 / 5.5)
     * @param lastUpdate       Timestamp of the last update (according to the datapass homepage)
     * @param hint             An optional hint (e.g. if WiFi is still on)
     * @param setClickListener Whether to set a click listener for another manual update. Only set if animation is finished to avoid
     * unnecessary animation tasks to start.
     */
    fun updateWidget(
        progress: Int,
        trafficUnit: String?,
        traffic: String?,
        lastUpdate: String?,
        hint: String?,
        setClickListener: Boolean
    )

    /**
     * Updates the widget with the globally set data.
     */
    fun updateWidgetWithFinalValues()
}
