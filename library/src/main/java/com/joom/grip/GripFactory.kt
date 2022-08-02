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

import com.joom.grip.io.IoFactory
import com.joom.grip.mirrors.ReflectorImpl
import java.nio.file.Path
import javax.annotation.concurrent.ThreadSafe
import org.objectweb.asm.Opcodes

@ThreadSafe
interface GripFactory {
  fun create(path: Path, vararg paths: Path): Grip
  fun create(paths: Iterable<Path>): Grip

  companion object {
    const val ASM_API_DEFAULT = Opcodes.ASM9

    @JvmStatic
    val INSTANCE = newInstance(ASM_API_DEFAULT)

    @JvmStatic
    fun newInstance(asmApi: Int): GripFactory {
      return GripFactoryImpl(asmApi)
    }
  }
}

internal class GripFactoryImpl(
  private val asmApi: Int,
) : GripFactory {

  override fun create(path: Path, vararg paths: Path): Grip {
    val allPaths = ArrayList<Path>(paths.size + 1)
    allPaths.add(path)
    allPaths.addAll(paths)
    return create(allPaths)
  }

  override fun create(paths: Iterable<Path>): Grip {
    val fileRegistry = FileRegistryImpl(paths, IoFactory)
    val reflector = ReflectorImpl(asmApi)
    val classRegistry = ClassRegistryImpl(fileRegistry, reflector)
    return GripImpl(fileRegistry, classRegistry, fileRegistry, classRegistry)
  }
}
