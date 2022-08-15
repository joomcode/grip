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
import org.junit.Test
import kotlin.io.path.Path

class CombinedGripTest {

  private val grip = createCombinedGrip()

  @Test
  fun testClasses() {
    val paths = listOf(FIRST_PATH, SECOND_PATH, THIRD_PATH)

    assertClassesResultContains<Class1>(
      grip select classes from paths where (name(contains("Class1")) and isPublic())
    )

    assertClassesResultContains<Class2>(
      grip select classes from paths where (name(contains("Class2")) and isPublic())
    )

    assertClassesResultContains<Class1>(
      grip select classes from paths where (annotatedWith(getObjectType<Annotation1>()))
    )
  }

  @Test
  fun testMethods() {
    val paths = listOf(FIRST_PATH, SECOND_PATH, THIRD_PATH)

    val classes = grip select classes from paths where name(contains("Class"))
    val methods = grip select methods from classes where (not(isStatic()) and not(isConstructor()))
    assertEquals(1, methods.execute()[getObjectType<Class1>()]!!.size)
    assertEquals(1, methods.execute()[getObjectType<Class2>()]!!.size)
  }

  @Test(expected = IllegalArgumentException::class)
  fun throwsForNotAddedPath() {
    (grip select classes from Path("/some-path") where isPublic()).execute()
  }

  @Test(expected = IllegalArgumentException::class)
  fun throwsMultipleGripInstancesContainSamePath() {
    val grip = CombinedGripFactory.INSTANCE.create(
      TestGripFactory.create(FIRST_PATH, Class1::class),
      TestGripFactory.create(FIRST_PATH, Class1::class),
    )

    grip.fileRegistry.contains(FIRST_PATH)
  }

  @Test(expected = IllegalArgumentException::class)
  fun throwsMultipleGripInstancesContainSameType() {
    val grip = CombinedGripFactory.INSTANCE.create(
      TestGripFactory.create(FIRST_PATH, Class1::class),
      TestGripFactory.create(SECOND_PATH, Class1::class),
    )

    grip.fileRegistry.contains(getObjectType<Class1>())
  }

  private fun createCombinedGrip(): Grip {
    return CombinedGripFactory.INSTANCE.create(
      TestGripFactory.create(FIRST_PATH, Class1::class, Annotation1::class),
      TestGripFactory.create(SECOND_PATH, Class2::class, Annotation2::class),
      TestGripFactory.create(THIRD_PATH, Enum1::class),
    )
  }

  private inline fun <reified T : Any> assertClassesResultContains(query: Query<ClassesResult>) {
    val result = query.execute()
    val type = getObjectType<T>()
    assert(result.classes.any { it.type == type })
  }

  private companion object {
    private val FIRST_PATH = Path("/first")
    private val SECOND_PATH = Path("/second")
    private val THIRD_PATH = Path("/third")
  }
}
