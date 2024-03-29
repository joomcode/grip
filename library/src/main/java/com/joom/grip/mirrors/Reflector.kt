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

package com.joom.grip.mirrors

import com.joom.grip.ClassRegistry
import com.joom.grip.commons.given
import com.joom.grip.mirrors.annotation.AnnotationInstanceReader
import com.joom.grip.mirrors.annotation.AnnotationValueReader
import com.joom.grip.mirrors.signature.GenericDeclaration
import com.joom.grip.mirrors.signature.LazyClassSignatureMirror
import com.joom.grip.mirrors.signature.LazyMethodSignatureMirror
import com.joom.grip.mirrors.signature.asGenericDeclaration
import com.joom.grip.mirrors.signature.inheritLazily
import com.joom.grip.mirrors.signature.resolveGenericDeclarationLazily
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor

internal interface Reflector {
  fun reflect(data: ByteArray, classRegistry: ClassRegistry, forAnnotation: Boolean): ClassMirror
}

internal class ReflectorImpl(
  private val asmApi: Int,
) : Reflector {
  override fun reflect(data: ByteArray, classRegistry: ClassRegistry, forAnnotation: Boolean): ClassMirror {
    val reader = ClassReader(data)
    return if (forAnnotation) {
      readClassMirror(reader, classRegistry, true)
    } else {
      LazyClassMirror(reader) { readClassMirror(reader, classRegistry, false) }
    }
  }

  private fun readClassMirror(reader: ClassReader, classRegistry: ClassRegistry, forAnnotation: Boolean): ClassMirror {
    val visitor = ReflectorClassVisitor(asmApi, classRegistry, forAnnotation)
    reader.accept(visitor, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
    return visitor.toClassMirror()
  }

  private class ReflectorClassVisitor(
    asmApi: Int,
    private val classRegistry: ClassRegistry,
    private val forAnnotation: Boolean
  ) : ClassVisitor(asmApi) {

    private val builder = ClassMirror.Builder()
    private lateinit var classGenericDeclaration: GenericDeclaration

    fun toClassMirror(): ClassMirror = builder.build()

    override fun visit(
      version: Int,
      access: Int,
      name: String,
      signature: String?,
      superName: String?,
      interfaces: Array<out String>?,
    ) {
      val enclosingGenericDeclaration =
        classRegistry.resolveGenericDeclarationLazily(getObjectTypeByInternalName(name))
      val signatureMirror = signature?.let { LazyClassSignatureMirror(api, enclosingGenericDeclaration, it) }
      classGenericDeclaration =
        signatureMirror?.let {
          enclosingGenericDeclaration.inheritLazily { it.asGenericDeclaration() }
        } ?: enclosingGenericDeclaration
      builder.apply {
        version(version)
        access(access)
        name(name)
        signature(signatureMirror)
        superName(superName)
        interfaces(interfaces)
        genericDeclaration(classGenericDeclaration)
      }
    }

    override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
      return given(!forAnnotation) {
        AnnotationInstanceReader(api, getObjectType(desc), visible, classRegistry) {
          builder.addAnnotation(it)
        }
      }
    }

    override fun visitField(access: Int, name: String, desc: String, signature: String?, value: Any?): FieldVisitor? {
      return given(!forAnnotation) {
        val fieldBuilder = FieldMirror.Builder(api, classGenericDeclaration).apply {
          access(access)
          name(name)
          type(getType(desc))
          signature(signature)
          value(value)
        }
        ReflectorFieldVisitor(api, classRegistry, fieldBuilder) {
          builder.addField(it)
        }
      }
    }

    override fun visitMethod(
      access: Int,
      name: String,
      desc: String,
      signature: String?,
      exceptions: Array<out String>?,
    ): MethodVisitor {
      val signatureMirror = signature?.let { LazyMethodSignatureMirror(api, classGenericDeclaration, it) }
      val methodGenericDeclaration =
        signatureMirror?.let {
          classGenericDeclaration.inheritLazily { it.asGenericDeclaration() }
        } ?: classGenericDeclaration
      val methodBuilder = MethodMirror.Builder().apply {
        val type = getMethodType(desc)
        access(access)
        name(name)
        type(type)
        signature(signatureMirror)
        exceptions(exceptions)
        genericDeclaration(methodGenericDeclaration)
      }
      return ReflectorMethodVisitor(api, classRegistry, forAnnotation, methodBuilder) {
        if (it.isConstructor) {
          builder.addConstructor(it)
        } else {
          builder.addMethod(it)
        }
      }
    }

    override fun visitInnerClass(name: String, outerName: String?, innerName: String?, access: Int) {
      val innerType = getObjectTypeByInternalName(name)
      val outerType = outerName?.let { getObjectTypeByInternalName(it) }
      builder.addInnerClass(InnerClass(innerType, outerType, innerName, access))
    }

    override fun visitOuterClass(owner: String, name: String?, desc: String?) {
      val enclosingType = getObjectTypeByInternalName(owner)
      if (name != null && desc != null) {
        builder.enclosure(Enclosure.Method.Named(enclosingType, name, getMethodType(desc)))
      } else {
        builder.enclosure(Enclosure.Method.Anonymous(enclosingType))
      }
    }

    override fun visitSource(source: String?, debug: String?) {
      builder.source(source)
      builder.debug(debug)
    }
  }

  private class ReflectorFieldVisitor(
    asmApi: Int,
    private val classRegistry: ClassRegistry,
    private val builder: FieldMirror.Builder,
    private val callback: (FieldMirror) -> Unit
  ) : FieldVisitor(asmApi) {

    override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor =
      AnnotationInstanceReader(api, getObjectType(desc), visible, classRegistry) {
        builder.addAnnotation(it)
      }

    override fun visitEnd() = callback(builder.build())
  }

  private class ReflectorMethodVisitor(
    asmApi: Int,
    private val classRegistry: ClassRegistry,
    private val forAnnotation: Boolean,
    private val builder: MethodMirror.Builder,
    private val callback: (MethodMirror) -> Unit
  ) : MethodVisitor(asmApi) {

    override fun visitParameterAnnotation(parameter: Int, desc: String, visible: Boolean): AnnotationVisitor? =
      given(!forAnnotation) {
        AnnotationInstanceReader(api, getObjectType(desc), visible, classRegistry) {
          builder.addParameterAnnotation(parameter, it)
        }
      }

    override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? =
      given(!forAnnotation) {
        AnnotationInstanceReader(api, getObjectType(desc), visible, classRegistry) {
          builder.addAnnotation(it)
        }
      }

    override fun visitAnnotationDefault(): AnnotationVisitor =
      AnnotationValueReader(api, classRegistry) {
        builder.defaultValue(it)
      }

    override fun visitEnd() = callback(builder.build())
  }
}
