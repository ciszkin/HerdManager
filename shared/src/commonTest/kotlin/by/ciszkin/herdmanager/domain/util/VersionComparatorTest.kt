package by.ciszkin.herdmanager.domain.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VersionComparatorTest {

    @Test
    fun `isNewerAvailable returns false when currentVersion is null`() {
        assertFalse(VersionComparator.isNewerAvailable(null, "1.0.0"))
    }

    @Test
    fun `isNewerAvailable returns false when latestVersion is null`() {
        assertFalse(VersionComparator.isNewerAvailable("1.0.0", null))
    }

    @Test
    fun `isNewerAvailable returns false when both versions are null`() {
        assertFalse(VersionComparator.isNewerAvailable(null, null))
    }

    @Test
    fun `isNewerAvailable correctly identifies newer version`() {
        assertTrue(VersionComparator.isNewerAvailable("1.0.0", "1.0.1"))
        assertTrue(VersionComparator.isNewerAvailable("1.0.0", "1.1.0"))
        assertTrue(VersionComparator.isNewerAvailable("1.0.0", "2.0.0"))
        assertTrue(VersionComparator.isNewerAvailable("1.9.9", "2.0.0"))
    }

    @Test
    fun `isNewerAvailable correctly identifies same version`() {
        assertFalse(VersionComparator.isNewerAvailable("1.0.0", "1.0.0"))
        assertFalse(VersionComparator.isNewerAvailable("1.2.3", "1.2.3"))
    }

    @Test
    fun `isNewerAvailable correctly identifies older version`() {
        assertFalse(VersionComparator.isNewerAvailable("1.0.1", "1.0.0"))
        assertFalse(VersionComparator.isNewerAvailable("1.1.0", "1.0.0"))
        assertFalse(VersionComparator.isNewerAvailable("2.0.0", "1.0.0"))
    }

    @Test
    fun `isNewerAvailable handles version with 'v' prefix`() {
        assertTrue(VersionComparator.isNewerAvailable("v1.0.0", "1.0.1"))
        assertTrue(VersionComparator.isNewerAvailable("1.0.0", "v1.0.1"))
        assertTrue(VersionComparator.isNewerAvailable("v1.0.0", "v1.0.1"))
        assertFalse(VersionComparator.isNewerAvailable("v1.0.1", "v1.0.0"))
    }

    @Test
    fun `isNewerAvailable handles versions with different lengths`() {
        assertTrue(VersionComparator.isNewerAvailable("1.0", "1.0.1"))
        assertFalse(VersionComparator.isNewerAvailable("1", "1.0.0")) // "1" becomes [1,0,0], same as "1.0.0"
        assertTrue(VersionComparator.isNewerAvailable("1", "2.0.0"))
        assertFalse(VersionComparator.isNewerAvailable("1.0.1", "1.0"))
    }

    @Test
    fun `isNewerAvailable handles versions with missing patch version`() {
        assertTrue(VersionComparator.isNewerAvailable("1.0.0", "1.1"))
        assertTrue(VersionComparator.isNewerAvailable("1.0.0", "2"))
        assertFalse(VersionComparator.isNewerAvailable("1.1.0", "1.1"))
    }

    @Test
    fun `isNewerAvailable handles versions with more than 3 parts`() {
        // Extra numeric parts are ignored (only major.minor.patch matter), not
        // treated as separate version components.
        assertFalse(VersionComparator.isNewerAvailable("1.0.0", "1.0.1.0"))
        assertFalse(VersionComparator.isNewerAvailable("1.0.1.0", "1.0.1"))
        assertFalse(VersionComparator.isNewerAvailable("1.0.1", "1.0.1.0"))
    }

    @Test
    fun `isNewerAvailable never claims an update for unparsable versions`() {
        // Non-numeric segments make the whole version unparsable: no update claim.
        assertFalse(VersionComparator.isNewerAvailable("1.0.0", "1.0.a"))
        assertFalse(VersionComparator.isNewerAvailable("1.a.0", "1.0.1"))
        assertFalse(VersionComparator.isNewerAvailable("1.0.a", "1.0.0"))
        assertFalse(VersionComparator.isNewerAvailable("1.0.0", "banana"))
    }

    @Test
    fun `isNewerAvailable handles empty string versions`() {
        assertFalse(VersionComparator.isNewerAvailable("", ""))
        assertFalse(VersionComparator.isNewerAvailable("1.0.0", ""))
        assertFalse(VersionComparator.isNewerAvailable("", "1.0.0"))
    }

    @Test
    fun `isNewerAvailable treats pre-releases as older than release`() {
        assertTrue(VersionComparator.isNewerAvailable("1.0.0-alpha", "1.0.0"))
        assertFalse(VersionComparator.isNewerAvailable("1.0.0", "1.0.0-alpha"))
        assertFalse(VersionComparator.isNewerAvailable("1.0.0", "1.0.0-rc.1"))
    }

    @Test
    fun `isNewerAvailable orders pre-release identifiers semantically`() {
        assertTrue(VersionComparator.isNewerAvailable("1.0.0-alpha", "1.0.0-beta"))
        assertTrue(VersionComparator.isNewerAvailable("1.0.0-alpha.1", "1.0.0-alpha.2"))
        assertTrue(VersionComparator.isNewerAvailable("1.0.0-rc.1", "1.0.0"))
        assertFalse(VersionComparator.isNewerAvailable("1.0.0-beta", "1.0.0-alpha"))
        assertFalse(VersionComparator.isNewerAvailable("1.0.0-alpha.2", "1.0.0-alpha.1"))
    }

    @Test
    fun `isNewerAvailable ignores build metadata`() {
        assertFalse(VersionComparator.isNewerAvailable("1.0.0+build.5", "1.0.0+build.9"))
        assertFalse(VersionComparator.isNewerAvailable("1.0.0", "1.0.0+build.1"))
        assertTrue(VersionComparator.isNewerAvailable("1.0.0+old", "1.0.1+new"))
    }

    @Test
    fun `isNewerAvailable handles complex version scenarios`() {
        // Testing major version updates
        assertTrue(VersionComparator.isNewerAvailable("0.5.7", "0.5.8"))
        assertTrue(VersionComparator.isNewerAvailable("0.5.7", "0.6.0"))
        assertTrue(VersionComparator.isNewerAvailable("0.5.7", "1.0.0"))

        // Testing edge cases around major version boundaries
        assertTrue(VersionComparator.isNewerAvailable("0.9.9", "1.0.0"))
        assertTrue(VersionComparator.isNewerAvailable("0.9.9", "1.0.1"))

        // Testing same major version with different minor
        assertTrue(VersionComparator.isNewerAvailable("1.0.0", "1.1.0"))
        assertFalse(VersionComparator.isNewerAvailable("1.1.0", "1.0.0"))
    }
}