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

package com.joom.grip.mirrors.annotation

import com.joom.grip.ClassRegistry
import com.joom.grip.mirrors.AnnotationMirror
import com.joom.grip.mirrors.Type

internal class AnnotationInstanceReader(
  asmApi: Int,
  annotationType: Type.Object,
  visible: Boolean,
  classRegistry: ClassRegistry,
  callback: (AnnotationMirror) -> Unit
) : AbstractAnnotationReader<AnnotationMirror>(asmApi, classRegistry, callback) {

  private val builder =
    AnnotationMirror.Builder()
      .type(annotationType)
      .visible(visible)
      .addValues(classRegistry.getAnnotationMirror(annotationType))

  override fun addValue(name: String?, value: Any) {
    builder.addValue(name!!, value)
  }

  override fun buildResult(): AnnotationMirror = builder.build()
}
