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

import com.joom.grip.commons.toAbsoluteNormalized
import com.joom.grip.io.FileSource
import com.joom.grip.mirrors.Type
import com.joom.grip.mirrors.getObjectType
import com.joom.grip.mirrors.getObjectTypeByInternalName
import com.joom.mockito.RETURNS_DEEP_STUBS
import com.joom.mockito.any
import com.joom.mockito.given
import com.joom.mockito.mock
import java.nio.file.Paths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class FileRegistryImplTest {
  @Test
  fun containsFile() {
    val source = mock<FileSource>()
    val path = Paths.get("source1")
    val factory = mock<FileSource.Factory>()
    given(factory.createFileSource(path.toAbsoluteNormalized())).thenReturn(source)
    val registry = FileRegistryImpl(listOf(path), factory)
    assertTrue(registry.contains(path))
    assertFalse(registry.contains(Paths.get("source2")))
  }

  @Test
  fun containsType() {
    val source = mock<FileSource>()
    given(source.listFiles(any())).thenAnswer {
      @Suppress("UNCHECKED_CAST")
      val callback = it.arguments[0] as (name: String, type: FileSource.EntryType) -> Unit
      callback("Type1.class", FileSource.EntryType.CLASS)
    }
    val path = Paths.get("source")
    val factory = mock<FileSource.Factory>()
    given(factory.createFileSource(path.toAbsoluteNormalized())).thenReturn(source)
    val registry = FileRegistryImpl(listOf(path), factory)
    assertTrue(registry.contains(getObjectTypeByInternalName("Type1")))
    assertFalse(registry.contains(getObjectTypeByInternalName("Type2")))
  }

  @Test
  fun classpath() {
    val classpath = (1..1000).map { Paths.get("source$it") }
    val factory = mock<FileSource.Factory>(RETURNS_DEEP_STUBS)
    val registry = FileRegistryImpl(classpath, factory)
    assertEquals(classpath.map { it.toAbsoluteNormalized() }, registry.classpath().toList())
  }

  @Test
  fun readClass() {
    val data = ByteArray(0)
    val source = mock<FileSource>()
    given(source.listFiles(any())).thenAnswer {
      @Suppress("UNCHECKED_CAST")
      val callback = it.arguments[0] as (name: String, type: FileSource.EntryType) -> Unit
      callback("Type1.class", FileSource.EntryType.CLASS)
    }
    given(source.readFile("Type1.class")).thenReturn(data)
    val path = Paths.get("source")
    val factory = mock<FileSource.Factory>()
    given(factory.createFileSource(path.toAbsoluteNormalized())).thenReturn(source)
    val registry = FileRegistryImpl(listOf(path), factory)
    assertSame(data, registry.readClass(getObjectTypeByInternalName("Type1")))
    assertThrows<IllegalArgumentException> { registry.readClass(getObjectTypeByInternalName("Type2")) }
  }

  @Test
  fun findTypesForFile() {
    val source1 = mock<FileSource>()
    given(source1.listFiles(any())).thenAnswer {
      @Suppress("UNCHECKED_CAST")
      val callback = it.arguments[0] as (name: String, type: FileSource.EntryType) -> Unit
      callback("Type1.class", FileSource.EntryType.CLASS)
    }
    val source2 = mock<FileSource>()
    val path1 = Paths.get("file1")
    val path2 = Paths.get("file2")
    val factory = mock<FileSource.Factory>()
    given(factory.createFileSource(path1.toAbsoluteNormalized())).thenReturn(source1)
    given(factory.createFileSource(path2.toAbsoluteNormalized())).thenReturn(source2)
    val registry = FileRegistryImpl(listOf(path1, path2), factory)
    assertEquals(listOf(getObjectTypeByInternalName("Type1")), registry.findTypesForPath(path1).toList())
    assertEquals(listOf<Type.Object>(), registry.findTypesForPath(path2).toList())
    assertThrows<IllegalArgumentException> { registry.findTypesForPath(Paths.get("file3")) }
  }

  @Test
  fun close() {
    val factory = mock<FileSource.Factory>(RETURNS_DEEP_STUBS)
    val registry = FileRegistryImpl(listOf(Paths.get("source")), factory)
    registry.close()

    assertThrows<IllegalStateException> { registry.contains(Paths.get("source")) }
    assertThrows<IllegalStateException> { registry.contains(getObjectType<Any>()) }
    assertThrows<IllegalStateException> { registry.classpath() }
    assertThrows<IllegalStateException> { registry.readClass(getObjectType<Any>()) }
    assertThrows<IllegalStateException> { registry.findTypesForPath(Paths.get("source")) }
    registry.close()
  }

  private inline fun <reified T : Throwable> assertThrows(noinline body: () -> Any) {
    assertThrows(T::class.java, body)
  }

  private fun <T : Throwable> assertThrows(exceptionClass: Class<T>, body: () -> Any) {
    try {
      body()
      throw AssertionError("$exceptionClass expected but no exception was thrown")
    } catch (exception: Throwable) {
      if (exception.javaClass != exceptionClass) {
        throw AssertionError("$exceptionClass expected but ${exception.javaClass} was thrown")
      }
    }
  }
}
