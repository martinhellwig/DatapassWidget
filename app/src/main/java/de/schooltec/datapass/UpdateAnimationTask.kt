package de.schooltec.datapass

import android.content.Context
import android.os.AsyncTask
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator

/**
 * @author Martin Hellwig
 * @since  2019-06-26
 */
class UpdateAnimationTask
    (
    context: Context,
    private val updateWidgetAnimationDrawer: UpdateWidgetAnimationDrawer
) : AsyncTask<Void, Int, Void>() {

    private val sharedPreferences =
        context.getSharedPreferences(PreferenceKeys.PREFERENCE_FILE_MISC, Context.MODE_PRIVATE)
    private val loadingText = context.getString(R.string.update_loading)

    private var loadingFinished = false
    private var drawingInProgress = false
    private var trafficWastedPercentageFinished = 0

    // Use AccelerateDecelerateInterpolator to ensure a smooth transition between multiple full circle animations
    private val forwardAnimInterpol = AccelerateDecelerateInterpolator()

    // Use OvershootInterpolator to get a nice and slow backwards animation from full circle to target arc,
    // and use a low tension value for better fit of the blue progressbar when being at 360°
    private val backwardAnimInterpol = OvershootInterpolator(0.6f)

    /**
     * Animate circle as long as needed (while animating at least one full circle).
     */
    override fun doInBackground(vararg voids: Void): Void? {
        // Get old percentage value as starting point for the animation
        val oldTrafficWastedPercentage = sharedPreferences.getInt(PreferenceKeys.SAVED_TRAFFIC_WASTED_PERCENTAGE, 0)

        animateCircle(oldTrafficWastedPercentage, 100)

        // Indeterminate animation if fetching data from homepage takes very long
        while (!loadingFinished) {
            animateCircle(100, 0) // Backwards animation
            animateCircle(0, 100) // Forward animation
        }

        animateCircle(100, trafficWastedPercentageFinished) // Animate backwards to desired value

        return null
    }

    /**
     * Animate the circular progress bar from a given start value to a given target value. Also takes the passed
     * time into account! E.g. if the animation should run from 25% to 80% it won't just take 55% * {@value
     * * ANIM_DURATION_HALF}ms but the calculated period according to current selected interpolator.
     *
     * @param startValue  Desired value in percent to start the animation from.
     * @param targetValue Desired value in percent to animate to (e.g. '100' to animate to a full circle)
     */
    private fun animateCircle(startValue: Int, targetValue: Int) {
        if (startValue == targetValue || startValue < 0 || targetValue > 100) return
        val forward = targetValue > startValue

        val interpolator = if (forward) forwardAnimInterpol else backwardAnimInterpol

        // Find the correct interpolated timestamp / offset to the given startValue
        var distance = Integer.MAX_VALUE
        var offset = 0
        for (i in 0..100) {
            val distanceNew = Math.abs(Math.round(interpolator.getInterpolation(i / 100f) * 100f) - startValue)
            if (distanceNew < distance) {
                distance = distanceNew
                offset = i
            }
        }

        val timeStart = System.currentTimeMillis()
        var resultInterPrevious = -1 // Interpolated value of the previous iteration

        while (true) {
            if (drawingInProgress) continue // Avoid unnecessary drawing operations / overload

            var timePassed = (System.currentTimeMillis() - timeStart).toFloat() // Time passed since last drawing

            // Calculate current interpolated value
            if (!forward) timePassed = -timePassed // invert value to assure backwards animation
            timePassed += offset / 100f * ANIM_DURATION_HALF // add interpolated offset
            val resultInter = Math.round(interpolator.getInterpolation(timePassed / ANIM_DURATION_HALF) * 100f)

            if (resultInter == resultInterPrevious) continue // Skip even more unnecessary drawing operations
            resultInterPrevious = resultInter

            // End loop if animation target is reached
            if (forward && resultInter >= targetValue || !forward && resultInter <= targetValue) break

            drawingInProgress = true
            publishProgress(resultInter)
        }
    }

    override fun onProgressUpdate(vararg progress: Int?) {
        val animProgressInterpolated = progress[0] ?: return

        // Update widget with interpolated progress value and set "loading..." text
        updateWidgetAnimationDrawer.updateWidget(animProgressInterpolated, "", loadingText, "", "", false)
    }

    override fun onPostExecute(aVoid: Void?) {
        // Update widget with interpolated progress value and set "loading..." text
        updateWidgetAnimationDrawer.updateWidgetWithFinalValues()
    }

    fun setFinished(trafficWastedPercentage: Int) {
        trafficWastedPercentageFinished = trafficWastedPercentage
        loadingFinished = true
    }

    fun onDrawingFinished() {
        drawingInProgress = false
    }

    companion object {
        // Duration of one-half of the animation in milliseconds
        private const val ANIM_DURATION_HALF = 1000
    }
}
