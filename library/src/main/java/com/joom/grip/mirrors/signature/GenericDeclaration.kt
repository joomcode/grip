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

import com.joom.grip.commons.LazyList

interface GenericDeclaration {
  val typeVariables: List<GenericType.TypeVariable>
}

internal interface MutableGenericDeclaration : GenericDeclaration {
  override val typeVariables: MutableList<GenericType.TypeVariable>
}

internal class InheritingGenericDeclaration(
  parent: GenericDeclaration = EmptyGenericDeclaration
) : MutableGenericDeclaration {
  override val typeVariables: MutableList<GenericType.TypeVariable> =
    if (parent.typeVariables.isEmpty()) LazyList() else parent.typeVariables.toMutableList()
}

internal object EmptyGenericDeclaration : GenericDeclaration {
  override val typeVariables: List<GenericType.TypeVariable>
    get() = emptyList()
}

internal fun GenericDeclaration(typeVariables: List<GenericType.TypeVariable>): GenericDeclaration {
  return object : GenericDeclaration {
    override val typeVariables: List<GenericType.TypeVariable> = typeVariables
  }
}

internal class LazyGenericDeclaration(
  builder: () -> GenericDeclaration
) : GenericDeclaration {

  private val delegate by lazy { builder() }

  override val typeVariables: List<GenericType.TypeVariable>
    get() = delegate.typeVariables
}

internal fun GenericDeclaration.inherit(genericDeclaration: GenericDeclaration): GenericDeclaration {
  return InheritingGenericDeclaration(this).apply {
    typeVariables.addAll(genericDeclaration.typeVariables)
  }
}

internal inline fun GenericDeclaration.inheritLazily(
  crossinline genericDeclaration: () -> GenericDeclaration
): GenericDeclaration {
  return LazyGenericDeclaration { inherit(genericDeclaration()) }
}
