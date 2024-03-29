/*
 * Copyright 2022 SIA Joom
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

package com.joom.grip.io

import java.io.Closeable
import java.nio.file.Path

interface FileSource : Closeable {
  fun listFiles(callback: (name: String, type: EntryType) -> Unit)
  fun readFile(path: String): ByteArray

  enum class EntryType {
    CLASS,
    FILE,
    DIRECTORY
  }

  interface Factory {
    fun createFileSource(inputPath: Path): FileSource
  }
}
