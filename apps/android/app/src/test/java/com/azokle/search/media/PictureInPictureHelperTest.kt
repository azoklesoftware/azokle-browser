/*
 * Copyright (c) 2026 The Azokle Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.azokle.search.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PictureInPictureHelperTest {

    @Test
    fun testClampedDimensions_standard16by9() {
        val (w, h) = PictureInPictureHelper.calculateClampedDimensions(1920, 1080)
        assertEquals(1920, w)
        assertEquals(1080, h)
    }

    @Test
    fun testClampedDimensions_invalidInputs() {
        val (w, h) = PictureInPictureHelper.calculateClampedDimensions(0, 0)
        assertEquals(16, w)
        assertEquals(9, h)
    }

    @Test
    fun testClampedDimensions_extremeUltraWide() {
        // Ultra-wide beyond 2.39:1 (e.g. 32:9 ~ 3.55:1) must clamp to max 2.39:1
        val (w, h) = PictureInPictureHelper.calculateClampedDimensions(32, 9)
        assertEquals(239, w)
        assertEquals(100, h)
    }

    @Test
    fun testClampedDimensions_extremeTallVertical() {
        // Very tall aspect ratio beyond 1:2.39 (e.g. 9:32 ~ 0.28:1) must clamp to min 1:2.39
        val (w, h) = PictureInPictureHelper.calculateClampedDimensions(9, 32)
        assertEquals(100, w)
        assertEquals(239, h)
    }

    @Test
    fun testPipActionConstants() {
        assertEquals("com.azokle.search.PIP_PLAY", PictureInPictureHelper.ACTION_PIP_PLAY)
        assertEquals("com.azokle.search.PIP_PAUSE", PictureInPictureHelper.ACTION_PIP_PAUSE)
        assertEquals("com.azokle.search.PIP_REWIND", PictureInPictureHelper.ACTION_PIP_REWIND)
        assertEquals("com.azokle.search.PIP_FORWARD", PictureInPictureHelper.ACTION_PIP_FORWARD)
    }
}

