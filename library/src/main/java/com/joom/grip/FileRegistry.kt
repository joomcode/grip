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

package com.joom.grip

import com.joom.grip.commons.closeQuietly
import com.joom.grip.commons.immutable
import com.joom.grip.commons.toAbsoluteNormalized
import com.joom.grip.io.FileSource
import com.joom.grip.mirrors.Type
import com.joom.grip.mirrors.getObjectTypeByInternalName
import java.io.Closeable
import java.nio.file.Path
import javax.annotation.concurrent.ThreadSafe

@ThreadSafe
interface FileRegistry {
  operator fun contains(path: Path): Boolean
  operator fun contains(type: Type.Object): Boolean
  fun readClass(type: Type.Object): ByteArray
  fun findTypesForPath(path: Path): Collection<Type.Object>
  fun findPathForType(type: Type.Object): Path?
  fun classpath(): Collection<Path>
}

internal class FileRegistryImpl(
  classpath: Iterable<Path>,
  private val fileSourceFactory: FileSource.Factory
) : FileRegistry, Closeable {
  private val sources = LinkedHashMap<Path, FileSource>()
  private val pathsByTypes = HashMap<Type.Object, Path>()
  private val typesByPaths = HashMap<Path, MutableCollection<Type.Object>>()

  init {
    classpath.forEach {
      it.toAbsoluteNormalized().let { sourcePath ->
        if (sourcePath !in sources) {
          val fileSource = fileSourceFactory.createFileSource(sourcePath)
          sources[sourcePath] = fileSource
          fileSource.listFiles { path, fileType ->
            if (fileType == FileSource.EntryType.CLASS) {
              val name = path.replace('\\', '/').substringBeforeLast(".class")
              val type = getObjectTypeByInternalName(name)
              pathsByTypes[type] = sourcePath
              typesByPaths.getOrPut(sourcePath) { ArrayList() } += type
            }
          }
        }
      }
    }

    check(sources.isNotEmpty()) { "Classpath is empty" }
  }

  override fun contains(path: Path): Boolean {
    checkNotClosed()
    return path.toAbsoluteNormalized() in sources
  }

  override fun contains(type: Type.Object): Boolean {
    checkNotClosed()
    return type in pathsByTypes
  }

  override fun classpath(): Collection<Path> {
    checkNotClosed()
    return sources.keys.immutable()
  }

  override fun readClass(type: Type.Object): ByteArray {
    checkNotClosed()
    val file = pathsByTypes.getOrElse(type) {
      throw IllegalArgumentException("Unable to find a file for ${type.internalName}")
    }
    val fileSource = sources.getOrElse(file) {
      throw IllegalArgumentException("Unable to find a source for ${type.internalName}")
    }
    return fileSource.readFile("${type.internalName}.class")
  }

  override fun findTypesForPath(path: Path): Collection<Type.Object> {
    require(contains(path)) { "File $path is not added to the registry" }
    return typesByPaths[path.toAbsoluteNormalized()]?.immutable() ?: emptyList()
  }

  override fun findPathForType(type: Type.Object): Path? {
    return pathsByTypes[type]
  }

  override fun close() {
    sources.values.forEach { it.closeQuietly() }
    sources.clear()
    pathsByTypes.clear()
    typesByPaths.clear()
  }

  private fun checkNotClosed() {
    check(sources.isNotEmpty()) { "FileRegistry was closed" }
  }
}
