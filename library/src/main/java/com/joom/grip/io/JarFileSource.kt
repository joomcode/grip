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

import java.io.IOException
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarFile

class JarFileSource(private val jarPath: Path) : FileSource {

  private val jar = JarFile(jarPath.toFile(), true)

  override fun listFiles(callback: (String, FileSource.EntryType) -> Unit) {
    fun JarEntry.toEntryType() = when {
      isDirectory -> FileSource.EntryType.DIRECTORY
      name.endsWith(".class", ignoreCase = true) -> FileSource.EntryType.CLASS
      else -> FileSource.EntryType.FILE
    }

    for (entry in jar.entries()) {
      callback(entry.name, entry.toEntryType())
    }
  }

  override fun readFile(path: String): ByteArray {
    return jar.getJarEntry(path).let { entry ->
      jar.getInputStream(entry).use { stream -> stream.readBytes() }
    }
  }

  override fun close() {
    try {
      jar.close()
    } catch (exception: IOException) {
      // Ignore the exception.
    }
  }

  override fun toString(): String {
    return "JarFileSource($jarPath)"
  }
}
