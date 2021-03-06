/*
 * Copyright 2017 Michael Rozumyanskiy
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

import io.michaelrocks.grip.commons.closeQuietly
import io.michaelrocks.grip.commons.immutable
import io.michaelrocks.grip.io.FileSource
import io.michaelrocks.grip.mirrors.Type
import io.michaelrocks.grip.mirrors.getObjectTypeByInternalName
import java.io.Closeable
import java.io.File
import java.util.ArrayList
import java.util.HashMap
import java.util.LinkedHashMap

interface FileRegistry {
  operator fun contains(file: File): Boolean
  operator fun contains(type: Type.Object): Boolean

  fun classpath(): Collection<File>

  fun readClass(type: Type.Object): ByteArray
  fun findTypesForFile(file: File): Collection<Type.Object>
  fun findFileForType(type: Type.Object): File?
}

internal class FileRegistryImpl(
    classpath: Iterable<File>,
    private val fileSourceFactory: FileSource.Factory
) : FileRegistry, Closeable {
  private val sources = LinkedHashMap<File, FileSource>()
  private val filesByTypes = HashMap<Type.Object, File>()
  private val typesByFiles = HashMap<File, MutableCollection<Type.Object>>()

  init {
    classpath.forEach {
      it.canonicalFile.let { file ->
        if (file !in sources) {
          val fileSource = fileSourceFactory.createFileSource(file)
          sources.put(file, fileSource)
          fileSource.listFiles { path, fileType ->
            if (fileType == FileSource.EntryType.CLASS) {
              val name = path.replace('\\', '/').substringBeforeLast(".class")
              val type = getObjectTypeByInternalName(name)
              filesByTypes.put(type, file)
              typesByFiles.getOrPut(file) { ArrayList() } += type
            }
          }
        }
      }
    }

    check(!sources.isEmpty()) { "Classpath is empty" }
  }

  override fun contains(file: File): Boolean {
    checkNotClosed()
    return file.canonicalFile in sources
  }

  override fun contains(type: Type.Object): Boolean {
    checkNotClosed()
    return type in filesByTypes
  }

  override fun classpath(): Collection<File> {
    checkNotClosed()
    return sources.keys.immutable()
  }

  override fun readClass(type: Type.Object): ByteArray {
    checkNotClosed()
    val file = filesByTypes.getOrElse(type) {
      throw IllegalArgumentException("Unable to find a file for ${type.internalName}")
    }
    val fileSource = sources.getOrElse(file) {
      throw IllegalArgumentException("Unable to find a source for ${type.internalName}")
    }
    return fileSource.readFile("${type.internalName}.class")
  }

  override fun findTypesForFile(file: File): Collection<Type.Object> {
    require(contains(file)) { "File $file is not added to the registry" }
    return typesByFiles[file.canonicalFile]?.immutable() ?: emptyList()
  }

  override fun findFileForType(type: Type.Object): File? {
    return filesByTypes[type]
  }

  override fun close() {
    sources.values.forEach { it.closeQuietly() }
    sources.clear()
    filesByTypes.clear()
    typesByFiles.clear()
  }

  private fun checkNotClosed() {
    check(!sources.isEmpty()) { "FileRegistry was closed" }
  }
}
