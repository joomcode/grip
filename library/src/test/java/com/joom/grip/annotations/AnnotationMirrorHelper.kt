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

package com.joom.grip.annotations

import com.joom.grip.mirrors.AbstractAnnotationMirror
import com.joom.grip.mirrors.AnnotationMirror
import com.joom.grip.mirrors.Type
import com.joom.grip.mirrors.buildAnnotation
import com.joom.grip.mirrors.getObjectTypeByInternalName
import java.util.Collections

fun createAnnotationMirror(
  annotationName: String,
  visible: Boolean = false
): AnnotationMirror {
  return createAnnotationMirror(annotationName, emptyMap(), visible)
}

fun createAnnotationMirror(
  annotationName: String,
  defaultValue: Any,
  visible: Boolean = false
): AnnotationMirror {
  return createAnnotationMirror(annotationName, "value", defaultValue, visible)
}

fun createAnnotationMirror(
  annotationName: String,
  methodName: String,
  defaultValue: Any,
  visible: Boolean = false
): AnnotationMirror {
  return createAnnotationMirror(annotationName, Collections.singletonMap(methodName, defaultValue), visible)
}

fun createAnnotationMirror(
  annotationName: String,
  vararg values: Pair<String, Any>,
  visible: Boolean = false
): AnnotationMirror {
  return createAnnotationMirror(annotationName, hashMapOf(*values), visible)
}

fun createAnnotationMirror(
  annotationName: String,
  values: Map<String, Any>,
  visible: Boolean = false
): AnnotationMirror {
  return buildAnnotation(getAnnotationType(annotationName), visible) {
    addValues(SimpleAnnotationMirror(getAnnotationType(annotationName), values, visible))
  }
}

fun getAnnotationType(annotationName: String): Type.Object {
  return getObjectTypeByInternalName(annotationName)
}

private class SimpleAnnotationMirror(
  override val type: Type.Object,
  override val values: Map<String, Any>,
  override val visible: Boolean = false
) : AbstractAnnotationMirror() {
  override val resolved: Boolean
    get() = false
}
