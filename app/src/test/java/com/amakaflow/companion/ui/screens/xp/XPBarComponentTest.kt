package com.amakaflow.companion.ui.screens.xp

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for XP level thresholds and progress calculations.
 * AMA-1285
 */
class XPBarComponentTest {

    private val thresholds = listOf(0, 500, 1500, 3500, 7000, 12000, 20000, 35000, 55000, 80000)
    private val levelNames = listOf(
        "Newcomer", "Regular", "Dedicated", "Committed", "Warrior",
        "Veteran", "Elite", "Champion", "Master", "Legend"
    )

    private fun levelForXp(xp: Int): Int {
        for (i in thresholds.indices.reversed()) {
            if (xp >= thresholds[i]) return i + 1
        }
        return 1
    }

    private fun xpToNextLevel(xp: Int): Int {
        val level = levelForXp(xp)
        if (level >= thresholds.size) return 0
        return thresholds[level] - xp
    }

    @Test
    fun `level 1 for zero XP`() {
        assertEquals(1, levelForXp(0))
    }

    @Test
    fun `level boundaries`() {
        assertEquals(1, levelForXp(499))
        assertEquals(2, levelForXp(500))
        assertEquals(3, levelForXp(1500))
        assertEquals(10, levelForXp(80000))
    }

    @Test
    fun `level for XP above max`() {
        assertEquals(10, levelForXp(999999))
    }

    @Test
    fun `xp to next level from zero`() {
        assertEquals(500, xpToNextLevel(0))
    }

    @Test
    fun `xp to next level mid-level`() {
        assertEquals(250, xpToNextLevel(250))
    }

    @Test
    fun `xp to next level at boundary`() {
        assertEquals(1000, xpToNextLevel(500)) // 1500 - 500
    }

    @Test
    fun `xp to next level at max`() {
        assertEquals(0, xpToNextLevel(80000))
        assertEquals(0, xpToNextLevel(99999))
    }

    @Test
    fun `level names match thresholds`() {
        assertEquals("Newcomer", levelNames[levelForXp(0) - 1])
        assertEquals("Regular", levelNames[levelForXp(500) - 1])
        assertEquals("Warrior", levelNames[levelForXp(7000) - 1])
        assertEquals("Legend", levelNames[levelForXp(80000) - 1])
    }

    @Test
    fun `progress fraction at level start is zero`() {
        // At exactly 500 XP (level 2 start), progress within level 2 is 0
        val xp = 500
        val level = levelForXp(xp)
        val currentThreshold = thresholds[level - 1]
        val nextThreshold = thresholds[level]
        val progress = (xp - currentThreshold).toFloat() / (nextThreshold - currentThreshold)
        assertEquals(0f, progress, 0.001f)
    }

    @Test
    fun `progress fraction mid-level`() {
        // At 1000 XP (halfway through level 2: 500-1500)
        val xp = 1000
        val level = levelForXp(xp)
        val currentThreshold = thresholds[level - 1]
        val nextThreshold = thresholds[level]
        val progress = (xp - currentThreshold).toFloat() / (nextThreshold - currentThreshold)
        assertEquals(0.5f, progress, 0.001f)
    }
}
