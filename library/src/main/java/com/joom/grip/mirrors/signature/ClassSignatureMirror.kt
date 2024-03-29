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
import com.joom.grip.mirrors.GenericTypeListWrapper
import com.joom.grip.mirrors.Type
import java.util.Collections
import org.objectweb.asm.signature.SignatureReader

interface ClassSignatureMirror {
  val typeVariables: List<GenericType.TypeVariable>
  val superType: GenericType
  val interfaces: List<GenericType>

  fun toJvmSignature(): String

  class Builder {
    private val typeVariables = LazyList<GenericType.TypeVariable>()
    private var superType: GenericType = OBJECT_RAW_TYPE
    private val interfaces = LazyList<GenericType>()

    fun addTypeVariable(builder: GenericType.TypeVariable) = apply {
      typeVariables += builder
    }

    fun superType(superType: GenericType) = apply {
      this.superType = superType
    }

    fun addInterface(interfaceType: GenericType) = apply {
      interfaces += interfaceType
    }

    fun build(): ClassSignatureMirror = ClassSignatureMirrorImpl(this)

    private class ClassSignatureMirrorImpl(builder: Builder) : ClassSignatureMirror {
      override val typeVariables: List<GenericType.TypeVariable> = builder.typeVariables.detachImmutableCopy()
      override val superType: GenericType = builder.superType
      override val interfaces: List<GenericType> = builder.interfaces.detachImmutableCopy()

      override fun toJvmSignature() = throw UnsupportedOperationException()
    }
  }
}

internal class LazyClassSignatureMirror(
  asmApi: Int,
  enclosingGenericDeclaration: GenericDeclaration,
  private val signature: String,
) : ClassSignatureMirror {

  private val delegate by lazy(LazyThreadSafetyMode.PUBLICATION) {
    ClassSignatureReader(asmApi, enclosingGenericDeclaration).run {
      SignatureReader(signature).accept(this)
      toClassSignature()
    }
  }

  override val typeVariables: List<GenericType.TypeVariable>
    get() = delegate.typeVariables
  override val superType: GenericType
    get() = delegate.superType
  override val interfaces: List<GenericType>
    get() = delegate.interfaces

  override fun toJvmSignature() = signature
}

internal class EmptyClassSignatureMirror(superType: Type?, interfaces: List<Type>) : ClassSignatureMirror {
  override val typeVariables: List<GenericType.TypeVariable>
    get() = Collections.emptyList()
  override val superType =
    superType?.let { GenericType.Raw(it) } ?: OBJECT_RAW_TYPE
  override val interfaces: List<GenericType> =
    if (interfaces.isEmpty()) emptyList() else GenericTypeListWrapper(interfaces)

  override fun toJvmSignature() = ""
}

internal fun ClassSignatureMirror.asGenericDeclaration(): GenericDeclaration {
  return GenericDeclaration(typeVariables)
}

internal fun ClassSignatureMirror.asLazyGenericDeclaration(): GenericDeclaration {
  return LazyGenericDeclaration { asGenericDeclaration() }
}
