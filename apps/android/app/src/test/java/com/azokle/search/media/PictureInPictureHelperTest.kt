package com.azokle.search.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PictureInPictureHelperTest {

    @Test
    fun testStandard16By9AspectRatioPreserved() {
        val (w, h) = PictureInPictureHelper.calculateClampedDimensions(16, 9)
        assertEquals(16, w)
        assertEquals(9, h)
    }

    @Test
    fun testStandard4By3AspectRatioPreserved() {
        val (w, h) = PictureInPictureHelper.calculateClampedDimensions(4, 3)
        assertEquals(4, w)
        assertEquals(3, h)
    }

    @Test
    fun testUltraWideVideoAspectRatioClampedToMax() {
        // 32:9 is ~3.55, which exceeds Android max 2.39
        val (w, h) = PictureInPictureHelper.calculateClampedDimensions(32, 9)
        assertEquals(239, w)
        assertEquals(100, h)
        assertTrue(w.toFloat() / h.toFloat() <= 2.39f)
    }

    @Test
    fun testUltraTallVerticalVideoAspectRatioClampedToMin() {
        // 9:32 is ~0.28, which is narrower than Android min 1/2.39 (~0.4184)
        val (w, h) = PictureInPictureHelper.calculateClampedDimensions(9, 32)
        assertEquals(100, w)
        assertEquals(239, h)
        assertTrue(w.toFloat() / h.toFloat() >= 1f / 2.39f)
    }

    @Test
    fun testNullOrZeroFallbackTo16By9() {
        val (zeroW, zeroH) = PictureInPictureHelper.calculateClampedDimensions(0, 0)
        assertEquals(16, zeroW)
        assertEquals(9, zeroH)

        val (negW, negH) = PictureInPictureHelper.calculateClampedDimensions(-10, 20)
        assertEquals(16, negW)
        assertEquals(9, negH)
    }
}
