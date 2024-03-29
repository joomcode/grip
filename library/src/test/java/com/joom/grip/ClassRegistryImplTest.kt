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

import com.joom.grip.classes.Annotation1
import com.joom.grip.classes.Annotation2
import com.joom.grip.mirrors.EnumMirror
import com.joom.grip.mirrors.ReflectorImpl
import com.joom.grip.mirrors.getObjectType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.file.Paths

class ClassRegistryImplTest {
  private lateinit var classRegistry: ClassRegistry

  @Before
  fun createClassRegistry() {
    val fileRegistry = TestFileRegistry(
      Paths.get("/"),
      Annotation1::class,
      Annotation2::class
    )
    val reflector = ReflectorImpl(GripFactory.ASM_API_DEFAULT)
    classRegistry = ClassRegistryImpl(fileRegistry, reflector)
  }

  @Test
  fun testIntAnnotation() {
    val mirror = classRegistry.getClassMirror(getObjectType<Annotation1>())
    assertEquals(1, mirror.annotations.size)
    assertTrue(getObjectType<Annotation1>() in mirror.annotations)
    val annotation = mirror.annotations[getObjectType<Annotation1>()]!!
    assertEquals(1, annotation.values.size)
    assertEquals(42, annotation.values["value"])
  }

  @Test
  fun testEnumAnnotation() {
    val mirror = classRegistry.getClassMirror(getObjectType<Annotation2>())
    assertEquals(1, mirror.annotations.size)
    assertTrue(getObjectType<Annotation2>() in mirror.annotations)
    val annotation = mirror.annotations[getObjectType<Annotation2>()]!!
    assertEquals(1, annotation.values.size)
    assertEquals(
      EnumMirror(getObjectType<AnnotationRetention>(), AnnotationRetention.RUNTIME.toString()),
      annotation.values["value"]
    )
  }
}
