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
import com.joom.grip.classes.Class1
import com.joom.grip.classes.Class2
import com.joom.grip.classes.Enum1
import com.joom.grip.mirrors.getObjectType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.io.path.Path

class GripTest {
  private lateinit var grip: Grip

  @Before
  fun createGrip() {
    grip = TestGripFactory.create(
      Path(""),
      Class1::class,
      Class2::class,
      Annotation1::class,
      Annotation2::class,
      Enum1::class
    )
  }

  @Test
  fun testClasses() {
    val path = Path("")
    assertClassesResultContains<Class1>(
      grip select classes from path where (name(contains("Class1")) and isPublic())
    )
    assertClassesResultContains<Class1>(
      grip select classes from path where (name(endsWith("Class1")) and isPublic())
    )
    assertClassesResultContains<Class2>(
      grip select classes from path where (not(name(endsWith("Class1") or startsWith("Class1"))) and not(isPackagePrivate()))
    )
    assertClassesResultContains<Class1>(
      grip select classes from path where (annotatedWith(getObjectType<Annotation1>()))
    )
    assertClassesResultNotContains<Class1>(
      grip select classes from path where (annotatedWith(getObjectType<Retention>()))
    )
  }

  @Test
  fun testMethods() {
    val path = Path("")
    val classes = grip select classes from path where name(contains("Class"))
    val methods = grip select methods from classes where (not(isStatic()) and not(isConstructor()))
    assertEquals(1, methods.execute()[getObjectType<Class1>()]!!.size)
    assertEquals(1, methods.execute()[getObjectType<Class2>()]!!.size)
  }

  @Test(expected = IllegalStateException::class)
  fun testClose() {
    grip.close()

    val path = Path("")
    (grip select classes from path where name(contains("Class"))).execute()
  }

  private inline fun <reified T : Any> assertClassesResultContains(query: Query<ClassesResult>) {
    val result = query.execute()
    val type = getObjectType<T>()
    assert(result.classes.any { it.type == type })
  }

  private inline fun <reified T : Any> assertClassesResultNotContains(query: Query<ClassesResult>) {
    val result = query.execute()
    val type = getObjectType<T>()
    assert(!result.classes.any { it.type == type })
  }

  private fun assertClassesResultIsEmpty(query: Query<ClassesResult>) {
    val result = query.execute()
    assertTrue(result.classes.isEmpty())
  }

  private fun assertClassesResultIsNotEmpty(query: Query<ClassesResult>) {
    val result = query.execute()
    assertFalse(result.classes.isEmpty())
  }
}
