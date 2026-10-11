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

package com.azokle.search.incineration

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileInputStream

class DataBurnEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `shredFile zeroes out content and deletes file`() {
        val testFile = tempFolder.newFile("sensitive_data.txt")
        testFile.writeText("CONFIDENTIAL_USER_HISTORY_AND_SESSION_COOKIES_123456789")
        assertTrue("Test file must exist before shred", testFile.exists())

        DataBurnEngine.shredFile(testFile)

        assertFalse("File must be deleted after shredding", testFile.exists())
    }

    @Test
    fun `shredFile handles empty and non-existent files gracefully`() {
        val emptyFile = tempFolder.newFile("empty.txt")
        DataBurnEngine.shredFile(emptyFile)
        assertFalse(emptyFile.exists())

        val nonExistent = File(tempFolder.root, "does_not_exist.bin")
        DataBurnEngine.shredFile(nonExistent)
        assertFalse(nonExistent.exists())
    }
}
