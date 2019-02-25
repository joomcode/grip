/*
 * Copyright 2019 Michael Rozumyanskiy
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

package io.michaelrocks.grip

import io.michaelrocks.grip.mirrors.Type
import java.io.IOException

class ClassAlreadyExistsException(
  val type: Type.Object,
  val reason: String? = null
) : IOException(composeMessage(type, reason)) {
  companion object {
    fun composeMessage(type: Type.Object, reason: String?): String {
      val typeName = type.internalName
      return if (reason == null) typeName else "$typeName: $reason"
    }
  }
}