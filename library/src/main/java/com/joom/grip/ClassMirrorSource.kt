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

import com.joom.grip.mirrors.ClassMirror
import java.nio.file.Path

interface ClassMirrorSource {
  fun getClassMirrors(): Sequence<ClassMirror>
}

class FunctionClassMirrorSource(
  private val classMirrorsProvider: () -> Sequence<ClassMirror>
) : ClassMirrorSource {
  override fun getClassMirrors(): Sequence<ClassMirror> {
    return classMirrorsProvider()
  }
}

internal class FilesClassMirrorSource(
  private val grip: Grip,
  private val paths: Collection<Path>
) : ClassMirrorSource {
  override fun getClassMirrors(): Sequence<ClassMirror> {
    return paths.asSequence().flatMap { path ->
      grip.fileRegistry.findTypesForPath(path).asSequence().map { type ->
        grip.classRegistry.getClassMirror(type)
      }
    }
  }
}
