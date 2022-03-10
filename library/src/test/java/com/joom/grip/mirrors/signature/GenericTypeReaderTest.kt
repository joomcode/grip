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

package com.joom.grip.mirrors.signature

import com.joom.grip.GripFactory
import com.joom.grip.mirrors.getObjectType
import com.joom.grip.mirrors.signature.GenericType.Array
import com.joom.grip.mirrors.signature.GenericType.Inner
import com.joom.grip.mirrors.signature.GenericType.LowerBounded
import com.joom.grip.mirrors.signature.GenericType.Parameterized
import com.joom.grip.mirrors.signature.GenericType.Raw
import com.joom.grip.mirrors.signature.GenericType.TypeVariable
import com.joom.grip.mirrors.signature.GenericType.UpperBounded
import org.junit.Assert.assertEquals
import org.junit.Test

class GenericTypeReaderTest {
  @Test
  fun testRawType() {
    assertParsedSignatureEquals(
      "Ljava/lang/Boolean;",
      Raw(getObjectType<Boolean>())
    )
  }

  @Test
  fun testTypeVariable() {
    assertParsedSignatureEquals(
      "TT;",
      TypeVariable("T"),
      TypeVariable("T")
    )
  }

  @Test
  fun testGenericArray() {
    assertParsedSignatureEquals(
      "[TT;",
      Array(TypeVariable("T")),
      TypeVariable("T")
    )
  }

  @Test
  fun testParameterizedType() {
    assertParsedSignatureEquals(
      "Ljava/util/Map<TK;TV;>;",
      Parameterized(getObjectType<Map<*, *>>(), TypeVariable("K"), TypeVariable("V")),
      TypeVariable("K"),
      TypeVariable("V")
    )
  }

  @Test
  fun testInnerType() {
    assertParsedSignatureEquals(
      "Ljava/util/Map<TK;TV;>.Entry<TK;TV;>;",
      Inner(
        "Entry",
        Parameterized(getObjectType<Map.Entry<*, *>>(), TypeVariable("K"), TypeVariable("V")),
        Parameterized(getObjectType<Map<*, *>>(), TypeVariable("K"), TypeVariable("V"))
      ),
      TypeVariable("K"),
      TypeVariable("V")
    )
  }

  @Test
  fun testUpperBoundedType() {
    assertParsedSignatureEquals(
      "Ljava/util/List<+TT;>;",
      Parameterized(
        getObjectType<List<*>>(),
        UpperBounded(TypeVariable("T"))
      ),
      TypeVariable("T")
    )
  }

  @Test
  fun testLowerBoundedType() {
    assertParsedSignatureEquals(
      "Ljava/util/List<-TT;>;",
      Parameterized(
        getObjectType<List<*>>(),
        LowerBounded(TypeVariable("T"))
      ),
      TypeVariable("T")
    )
  }

  @Test
  fun testMultiDimensionalArray() {
    assertParsedSignatureEquals(
      "[[[Ljava/util/List<TT;>;",
      Array(
        Array(
          Array(
            Parameterized(getObjectType<List<*>>(), TypeVariable("T"))
          )
        )
      ),
      TypeVariable("T")
    )
  }

  @Test
  fun testParameterizedTypeWithArray() {
    assertParsedSignatureEquals(
      "Ljava/util/List<[TT;>;",
      Parameterized(getObjectType<List<*>>(), Array(TypeVariable("T"))),
      TypeVariable("T")
    )
  }

  @Test
  fun testNestedParameterizedType() {
    assertParsedSignatureEquals(
      "Ljava/util/List<Ljava/util/List<Ljava/lang/Boolean;>;>;",
      Parameterized(
        getObjectType<List<*>>(),
        Parameterized(getObjectType<List<*>>(), Raw(getObjectType<Boolean>()))
      )
    )
  }

  @Test
  fun testShadowedTypeVariable() {
    assertParsedSignatureEquals(
      "TT;",
      TypeVariable("T", Raw(getObjectType<List<*>>())),
      TypeVariable("T"),
      TypeVariable("T", Raw(getObjectType<List<*>>()))
    )
  }

  @Test(expected = IllegalStateException::class)
  fun testUndefinedTypeVariable() {
    readGenericType(GripFactory.ASM_API_DEFAULT, EmptyGenericDeclaration, "TT;")
  }
}

private fun assertParsedSignatureEquals(signature: String, expected: GenericType, vararg typeVariables: TypeVariable) {
  val genericDeclaration = GenericDeclaration(typeVariables.asList())
  val actual = readGenericType(GripFactory.ASM_API_DEFAULT, genericDeclaration, signature)
  assertEquals(expected, actual)
}
