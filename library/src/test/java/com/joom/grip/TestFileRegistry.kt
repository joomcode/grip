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

import com.joom.grip.mirrors.Type
import com.joom.grip.mirrors.getObjectType
import java.nio.file.Path
import kotlin.reflect.KClass

class TestFileRegistry(private val path: Path, vararg classes: KClass<*>) : FileRegistry {
  private val classesByType = classes.associateBy { getObjectType(it) }

  override fun contains(path: Path): Boolean = this.path == path
  override fun contains(type: Type.Object): Boolean = type in classesByType

  override fun classpath(): Collection<Path> = listOf(path)

  override fun readClass(type: Type.Object): ByteArray {
    val classLoader = classesByType[type]!!.java.classLoader
    return classLoader.getResourceAsStream(type.internalName + ".class").use {
      checkNotNull(it).readBytes()
    }
  }

  override fun findTypesForPath(path: Path): Collection<Type.Object> {
    require(contains(path)) { "Not a valid path $path" }

    return classesByType.keys
  }

  override fun findPathForType(type: Type.Object): Path? {
    return if (contains(type)) path else null
  }
}
