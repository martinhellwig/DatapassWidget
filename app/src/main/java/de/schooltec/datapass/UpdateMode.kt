package de.schooltec.datapass

/**
 * Enum for the possible update behaviors.
 *
 * @author Martin Hellwig
 * @since  2019-06-26
 */
internal enum class UpdateMode {
    /**
     * Normal update with animation and toasts.
     */
    REGULAR,

    /**
     * Update with animation but without toasts (e.g. for auto update every 6h).
     */
    SILENT,

    /**
     * Update without animation and toasts.
     */
    ULTRA_SILENT
}
