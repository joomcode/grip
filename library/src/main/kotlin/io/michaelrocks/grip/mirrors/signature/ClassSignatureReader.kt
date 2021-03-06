/*
 * Copyright 2016 Michael Rozumyanskiy
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

package io.michaelrocks.grip.mirrors.signature

import org.objectweb.asm.Opcodes
import org.objectweb.asm.signature.SignatureVisitor

internal class ClassSignatureReader(
    enclosingGenericDeclaration: GenericDeclaration
) : SignatureVisitor(Opcodes.ASM5) {

  private val builder = ClassSignatureMirror.Builder()
  private var typeVariableBuilder: TypeVariableBuilder? = null
  private val genericDeclaration = InheritingGenericDeclaration(enclosingGenericDeclaration)

  fun toClassSignature(): ClassSignatureMirror = builder.build()

  override fun visitFormalTypeParameter(name: String) {
    buildTypeVariable()
    typeVariableBuilder = TypeVariableBuilder(name)
  }

  override fun visitClassBound(): SignatureVisitor {
    return GenericTypeReader(genericDeclaration) { typeVariableBuilder!!.classBound(it) }
  }

  override fun visitInterfaceBound(): SignatureVisitor {
    return GenericTypeReader(genericDeclaration) { typeVariableBuilder!!.addInterfaceBound(it) }
  }

  override fun visitSuperclass(): SignatureVisitor {
    buildTypeVariable()
    return GenericTypeReader(genericDeclaration) { builder.superType(it) }
  }

  override fun visitInterface(): SignatureVisitor {
    return GenericTypeReader(genericDeclaration) { builder.addInterface(it) }
  }

  private fun buildTypeVariable() {
    typeVariableBuilder?.build()?.let {
      builder.addTypeVariable(it)
      genericDeclaration.typeVariables += it
    }
    typeVariableBuilder = null
  }
}
