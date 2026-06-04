package by.ciszkin.herdmanager.domain.util

object VersionComparator {
    /**
     * Compares two semantic version strings.
     * Returns true if currentVersion is less than latestVersion.
     * Handles 'v' prefix (e.g., "v0.5.7" -> "0.5.7").
     */
    fun isNewerAvailable(currentVersion: String?, latestVersion: String?): Boolean {
        if (currentVersion == null || latestVersion == null || currentVersion.isEmpty() || latestVersion.isEmpty()) return false

        val current = parseVersion(currentVersion)
        val latest = parseVersion(latestVersion)

        // Pad versions with zeros if they have fewer than 3 parts
        val paddedCurrent = current + List(3 - current.size) { 0 }
        val paddedLatest = latest + List(3 - latest.size) { 0 }

        // Compare each version part
        for (i in 0 until 3) {
            val currentPart = paddedCurrent[i]
            val latestPart = paddedLatest[i]

            if (currentPart < latestPart) {
                return true
            } else if (currentPart > latestPart) {
                return false
            }
        }

        // All parts are equal
        return false
    }

    private fun parseVersion(version: String): List<Int> {
        // Remove 'v' prefix if present
        val cleanVersion = version.removePrefix("v")

        // Handle empty string case
        if (cleanVersion.isEmpty()) {
            return listOf(0, 0, 0)
        }

        // Split by '.' and convert to integers
        return cleanVersion.split('.')
            .mapNotNull { it.toIntOrNull() }
            .take(3) // Only take major, minor, patch
    }
}
