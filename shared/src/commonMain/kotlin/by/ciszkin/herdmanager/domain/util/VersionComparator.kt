package by.ciszkin.herdmanager.domain.util

object VersionComparator {

    private val VERSION_REGEX =
        Regex("""v?(\d+)(?:\.(\d+))?(?:\.(\d+))?(?:-([0-9A-Za-z.-]+))?(?:\+[0-9A-Za-z.-]+)?""")

    /**
     * Returns true when [currentVersion] is an older release than [latestVersion].
     * Handles an optional `v` prefix, missing trailing parts (padded to 0),
     * pre-release identifiers (`-alpha`, `-rc.1` …) and build metadata (`+build`,
     * ignored for precedence). Unparsable versions never claim an update.
     */
    fun isNewerAvailable(currentVersion: String?, latestVersion: String?): Boolean {
        if (currentVersion.isNullOrEmpty() || latestVersion.isNullOrEmpty()) return false
        val current = parseVersion(currentVersion) ?: return false
        val latest = parseVersion(latestVersion) ?: return false
        return compare(current, latest) < 0
    }

    private data class Semver(
        val parts: List<Int>,
        val preRelease: List<String>
    )

    private fun parseVersion(version: String): Semver? {
        val match = VERSION_REGEX.matchEntire(version.trim()) ?: return null
        val parts = (1..3).map { match.groupValues[it].ifEmpty { "0" }.toInt() }
        val preRelease = match.groupValues[4].takeIf { it.isNotEmpty() }?.split(".") ?: emptyList()
        return Semver(parts, preRelease)
    }

    /**
     * Semver precedence:
     * - numeric core parts compare first (larger = newer)
     * - a release beats any pre-release of the same core
     * - pre-release identifiers compare in order: numeric < alphanumeric,
     *   numerics numerically, alphanumerics lexically; shorter list wins when a
     *   prefix matches
     */
    private fun compare(a: Semver, b: Semver): Int {
        for (i in 0..2) {
            val cmp = a.parts[i].compareTo(b.parts[i])
            if (cmp != 0) return cmp
        }
        if (a.preRelease.isEmpty() && b.preRelease.isEmpty()) return 0
        if (a.preRelease.isEmpty()) return 1
        if (b.preRelease.isEmpty()) return -1
        val common = minOf(a.preRelease.size, b.preRelease.size)
        for (i in 0 until common) {
            val cmp = compareIdentifier(a.preRelease[i], b.preRelease[i])
            if (cmp != 0) return cmp
        }
        return a.preRelease.size.compareTo(b.preRelease.size)
    }

    private fun compareIdentifier(x: String, y: String): Int {
        val xNum = x.toIntOrNull()
        val yNum = y.toIntOrNull()
        return when {
            xNum != null && yNum != null -> xNum.compareTo(yNum)
            xNum != null -> -1
            yNum != null -> 1
            else -> x.compareTo(y)
        }
    }
}