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

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isDirectory

object IoFactory : FileSource.Factory, FileSink.Factory {

  override fun createFileSource(inputPath: Path): FileSource {
    return when (inputPath.sourceType) {
      SourceType.EMPTY -> EmptyFileSource
      SourceType.DIRECTORY -> DirectoryFileSource(inputPath)
      SourceType.JAR -> JarFileSource(inputPath)
      SourceType.JRT -> JrtFileSource(inputPath)
    }
  }

  override fun createFileSink(inputPath: Path, outputPath: Path): FileSink {
    return when (inputPath.sourceType) {
      SourceType.EMPTY -> EmptyFileSink
      SourceType.DIRECTORY -> DirectoryFileSink(outputPath)
      SourceType.JAR -> JarFileSink(outputPath)
      SourceType.JRT -> EmptyFileSink
    }
  }

  private val Path.sourceType: SourceType
    get() = when {
      fileSystem.toString().startsWith("jrt") -> SourceType.JRT
      !exists() || isDirectory() -> SourceType.DIRECTORY
      extension.endsWith("jar", ignoreCase = true) -> SourceType.JAR
      else -> error("Unknown file type for file $this")
    }

  private enum class SourceType {
    EMPTY,
    DIRECTORY,
    JAR,
    JRT
  }
}
