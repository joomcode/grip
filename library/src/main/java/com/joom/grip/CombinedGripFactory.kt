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

import com.joom.grip.commons.immutable
import com.joom.grip.commons.singleOrNullIfNotFound
import com.joom.grip.mirrors.AnnotationMirror
import com.joom.grip.mirrors.ClassMirror
import com.joom.grip.mirrors.Type
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.concurrent.ThreadSafe

@ThreadSafe
interface CombinedGripFactory {
  fun create(grip: Grip, vararg grips: Grip): Grip
  fun create(grips: Iterable<Grip>): Grip

  companion object {
    @JvmStatic
    val INSTANCE: CombinedGripFactory = CombinedGripFactoryImpl()
  }
}

private class CombinedGripFactoryImpl : CombinedGripFactory {
  override fun create(grips: Iterable<Grip>): Grip {
    val classRegistry = CombinedClassRegistryImpl(grips)
    val fileRegistry = CombinedFileRegistryImpl(grips.map { it.fileRegistry })

    return GripImpl(fileRegistry, classRegistry, grips)
  }

  override fun create(grip: Grip, vararg grips: Grip): Grip {
    return create(listOf(grip) + grips.asList())
  }
}

private class CombinedClassRegistryImpl(private val grips: Iterable<Grip>) : ClassRegistry {
  private val annotationTypeToGrip = ConcurrentHashMap<Type.Object, Grip>()
  private val typeToGrip = ConcurrentHashMap<Type.Object, Grip>()
  override fun getAnnotationMirror(type: Type.Object): AnnotationMirror {
    val grip = annotationTypeToGrip.getOrPut(type) {
      grips.first { it.fileRegistry.contains(type) }
    }

    return grip.classRegistry.getAnnotationMirror(type)
  }

  override fun getClassMirror(type: Type.Object): ClassMirror {
    val grip = typeToGrip.getOrPut(type) {
      grips.first { it.fileRegistry.contains(type) }
    }

    return grip.classRegistry.getClassMirror(type)
  }
}

private class CombinedFileRegistryImpl(private val registries: Iterable<FileRegistry>) : FileRegistry {
  private val typeToRegistry = ConcurrentHashMap<Type.Object, FileRegistry>()
  private val pathToRegistry = ConcurrentHashMap<Path, FileRegistry>()

  private val classpath by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    registries.flatMap { it.classpath() }.immutable()
  }

  override fun classpath(): Collection<Path> {
    return classpath
  }

  override fun readClass(type: Type.Object): ByteArray {
    return registryByType(type)?.readClass(type) ?: run {
      throw IllegalArgumentException("Unable to find registry for ${type.internalName}")
    }
  }

  override fun contains(path: Path): Boolean {
    return registryByPath(path) != null
  }

  override fun contains(type: Type.Object): Boolean {
    return registryByType(type) != null
  }

  override fun findPathForType(type: Type.Object): Path? {
    return registryByType(type)?.findPathForType(type)
  }

  override fun findTypesForPath(path: Path): Collection<Type.Object> {
    return registryByPath(path)?.findTypesForPath(path) ?: run {
      throw IllegalArgumentException("File $path is not added to the registry")
    }
  }

  private fun registryByType(type: Type.Object): FileRegistry? {
    typeToRegistry[type]?.let {
      return it
    }

    return registries.singleOrNullIfNotFound({ "Multiple registries contain same type ${type.internalName}" }) {
      it.contains(type)
    }
  }

  private fun registryByPath(path: Path): FileRegistry? {
    pathToRegistry[path]?.let {
      return it
    }

    return registries.singleOrNullIfNotFound({ "Multiple registries contain same path $path" }) {
      it.contains(path)
    }
  }
}
