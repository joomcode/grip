/*
 * Copyright 2019 Michael Rozumyanskiy
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

package io.michaelrocks.grip

import io.michaelrocks.grip.io.DefaultFileFormatDetector
import io.michaelrocks.grip.io.DefaultFileSinkFactory
import io.michaelrocks.grip.io.DefaultFileSourceFactory
import io.michaelrocks.grip.io.FileFormatDetector
import io.michaelrocks.grip.io.FileSink
import io.michaelrocks.grip.io.FileSource
import io.michaelrocks.grip.mirrors.DefaultReflector
import java.io.File
import java.util.ArrayList

object GripFactory {
  @JvmOverloads
  fun create(file: File, vararg files: File, outputDirectory: File? = null): Grip {
    val allFiles = ArrayList<File>(files.size + 1)
    allFiles.add(file)
    allFiles.addAll(files)
    return create(allFiles, outputDirectory = outputDirectory)
  }

  @JvmOverloads
  fun create(
    classpath: Iterable<File>,
    outputDirectory: File? = null,
    fileFormatDetector: FileFormatDetector = DefaultFileFormatDetector(),
    fileSourceFactory: FileSource.Factory = DefaultFileSourceFactory(fileFormatDetector),
    fileSinkFactory: FileSink.Factory = DefaultFileSinkFactory()
  ): Grip {
    val fileRegistry = DefaultFileRegistry(classpath, fileSourceFactory)
    val reflector = DefaultReflector()
    val classRegistry = DefaultClassRegistry(fileRegistry, reflector)
    val classProducer = if (outputDirectory != null) {
      DefaultClassProducer(fileRegistry, fileSinkFactory, fileFormatDetector, outputDirectory)
    } else {
      UnsupportedClassProducer("Cannot produce a class because output directory isn't set")
    }
    return DefaultGrip(fileRegistry, classRegistry, classProducer)
  }
}
