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

import com.joom.grip.mirrors.AnnotationMirror
import com.joom.grip.mirrors.ClassMirror
import com.joom.grip.mirrors.EnumMirror
import com.joom.grip.mirrors.Reflector
import com.joom.grip.mirrors.Type
import com.joom.grip.mirrors.UnresolvedAnnotationMirror
import com.joom.grip.mirrors.buildAnnotation
import com.joom.grip.mirrors.getObjectType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.concurrent.ThreadSafe
import org.objectweb.asm.Opcodes
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

@ThreadSafe
interface ClassRegistry {
  fun getClassMirror(type: Type.Object): ClassMirror
  fun getAnnotationMirror(type: Type.Object): AnnotationMirror
}

internal class ClassRegistryImpl(
  private val fileRegistry: FileRegistry,
  private val reflector: Reflector
) : ClassRegistry, Closeable {
  private val closed = AtomicBoolean(false)
  private val classesByType = ConcurrentHashMap<Type, ClassMirror>()
  private val annotationsByType = ConcurrentHashMap<Type, AnnotationMirror>()

  override fun getClassMirror(type: Type.Object): ClassMirror {
    checkNotClosed()

    return classesByType.getOrPut(type) { readClassMirror(type, false) }
  }

  override fun getAnnotationMirror(type: Type.Object): AnnotationMirror {
    checkNotClosed()

    return annotationsByType.getOrPut(type) {
      if (type !in fileRegistry) {
        UnresolvedAnnotationMirror(type)
      } else {
        val classMirror = readClassMirror(type, true)
        val visible = isAnnotationVisible(classMirror)
        buildAnnotation(type, visible) {
          check(classMirror.access or Opcodes.ACC_ANNOTATION != 0)
          for (method in classMirror.methods) {
            method.defaultValue?.let { addValue(method.name, it) }
          }
        }
      }
    }
  }

  override fun close() {
    classesByType.clear()
    annotationsByType.clear()
    closed.set(true)
  }

  private fun readClassMirror(type: Type.Object, forAnnotation: Boolean): ClassMirror {
    checkNotClosed()

    return try {
      reflector.reflect(fileRegistry.readClass(type), this, forAnnotation)
    } catch (exception: Exception) {
      throw IllegalArgumentException("Unable to read a ClassMirror for ${type.internalName}", exception)
    }
  }

  private fun checkNotClosed() {
    check(!closed.get()) { "ClassRegistry was closed" }
  }

  companion object {
    private val RETENTION_TYPE = getObjectType<Retention>()
    private val RETENTION_POLICY_TYPE = getObjectType<RetentionPolicy>()

    private fun isAnnotationVisible(classMirror: ClassMirror): Boolean {
      val retention = classMirror.annotations[RETENTION_TYPE] ?: return false
      val retentionPolicy = retention.values["value"] as? EnumMirror ?: return false
      check(retentionPolicy.type == RETENTION_POLICY_TYPE) {
        "Class ${classMirror.type} contains @Retention annotation with unexpected value of type ${retentionPolicy.type}"
      }
      return retentionPolicy.value == RetentionPolicy.RUNTIME.name
    }
  }
}
