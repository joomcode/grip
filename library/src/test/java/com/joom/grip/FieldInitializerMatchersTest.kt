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

import com.joom.grip.mirrors.FieldMirror
import com.joom.mockito.given
import com.joom.mockito.mock
import org.junit.Test

class FieldInitializerMatchersTest {
  private val stringValueField = mock<FieldMirror>().apply {
    given(value).thenReturn("String")
  }
  private val intValueField = mock<FieldMirror>().apply {
    given(value).thenReturn(42)
  }
  private val nullValueField = mock<FieldMirror>()

  @Test
  fun testWithFieldInitializerTrue() = stringValueField.testFieldInitializer(true) {
    withFieldInitializer { _, _ -> true }
  }

  @Test
  fun testWithFieldInitializerFalse() = stringValueField.testFieldInitializer(false) {
    withFieldInitializer { _, _ -> false }
  }

  @Test
  fun testWithFieldInitializerStringTrue() = stringValueField.testFieldInitializer(true) {
    withFieldInitializer<String>()
  }

  @Test
  fun testWithFieldInitializerStringFalse() = nullValueField.testFieldInitializer(false) {
    withFieldInitializer<String>()
  }

  @Test
  fun testWithFieldInitializerIntTrue() = intValueField.testFieldInitializer(true) {
    withFieldInitializer<Int>()
  }

  @Test
  fun testWithFieldInitializerIntFalse() = stringValueField.testFieldInitializer(false) {
    withFieldInitializer<Int>()
  }

  @Test
  fun testHasFieldInitializerTrue() = stringValueField.testFieldInitializer(true) { hasFieldInitializer() }

  @Test
  fun testHasFieldInitializerFalse() = nullValueField.testFieldInitializer(false) { hasFieldInitializer() }

  private inline fun FieldMirror.testFieldInitializer(condition: Boolean, body: () -> ((Grip, FieldMirror) -> Boolean)) =
    assertAndVerify(condition, body) { value }
}
