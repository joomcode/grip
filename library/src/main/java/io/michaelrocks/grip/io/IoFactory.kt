/*
 * Copyright 2021 Michael Rozumyanskiy
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

package io.michaelrocks.grip.io

import java.io.File

object IoFactory : FileSource.Factory, FileSink.Factory {
  override fun createFileSource(inputFile: File): FileSource {
    return when (inputFile.fileType) {
      FileType.EMPTY -> EmptyFileSource
      FileType.DIRECTORY -> DirectoryFileSource(inputFile)
      FileType.JAR -> JarFileSource(inputFile)
    }
  }

  override fun createFileSink(inputFile: File, outputFile: File): FileSink {
    return when (inputFile.fileType) {
      FileType.EMPTY -> EmptyFileSink
      FileType.DIRECTORY -> DirectoryFileSink(outputFile)
      FileType.JAR -> JarFileSink(outputFile)
    }
  }

  private val File.fileType: FileType
    get() = when {
      !exists() || isDirectory -> FileType.DIRECTORY
      extension.endsWith("jar", ignoreCase = true) -> FileType.JAR
      else -> error("Unknown file type for file $this")
    }

  private enum class FileType {
    EMPTY,
    DIRECTORY,
    JAR
  }
}
