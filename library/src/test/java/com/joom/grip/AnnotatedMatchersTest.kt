/*
 * Copyright 2021 SIA Joom
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

import com.joom.grip.mirrors.Annotated
import com.joom.grip.mirrors.ImmutableAnnotationCollection
import com.joom.grip.mirrors.buildAnnotation
import com.joom.grip.mirrors.getObjectTypeByInternalName
import com.joom.mockito.given
import com.joom.mockito.mock
import org.junit.Test

class AnnotatedMatchersTest {
  val annotated = mock<Annotated>().apply {
    given(annotations).thenReturn(
      ImmutableAnnotationCollection(
        buildAnnotation(getObjectTypeByInternalName("com/joom/mocks/Annotation"), visible = true)
      )
    )
  }

  @Test
  fun testAnnotatedWithByTypeTrue() = annotated.testAnnotations(true) {
    annotatedWith(getObjectTypeByInternalName("com/joom/mocks/Annotation"))
  }

  @Test
  fun testAnnotatedWithByTypeFalse() = annotated.testAnnotations(false) {
    annotatedWith(getObjectTypeByInternalName("com/joom/mocks/AnotherAnnotation"))
  }

  @Test
  fun testAnnotatedWithByPredicateTrue() = annotated.testAnnotations(true) {
    annotatedWith { _, annotation -> annotation.values.isEmpty() }
  }

  @Test
  fun testAnnotatedWithByPredicateFalse() = annotated.testAnnotations(false) {
    annotatedWith { _, annotation -> annotation.values.isNotEmpty() }
  }

  @Test
  fun testAnnotatedWithByTypeAndPredicateTrue() = annotated.testAnnotations(true) {
    annotatedWith(getObjectTypeByInternalName("com/joom/mocks/Annotation")) { _, annotation ->
      annotation.values.isEmpty()
    }
  }

  @Test
  fun testAnnotatedWithByTypeAndPredicateFalse() = annotated.testAnnotations(false) {
    annotatedWith(getObjectTypeByInternalName("com/joom/mocks/AnotherAnnotation")) { _, annotation ->
      annotation.values.isNotEmpty()
    }
  }

  private inline fun Annotated.testAnnotations(condition: Boolean, body: () -> ((Grip, Annotated) -> Boolean)) =
    assertAndVerify(condition, body) { annotations }
}
