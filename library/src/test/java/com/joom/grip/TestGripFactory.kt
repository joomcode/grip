package com.joom.grip

import com.joom.grip.mirrors.ReflectorImpl
import java.nio.file.Path
import kotlin.reflect.KClass

object TestGripFactory {
  fun create(path: Path, vararg classes: KClass<*>): Grip {
    val fileRegistry = TestFileRegistry(path, *classes)
    val reflector = ReflectorImpl(GripFactory.ASM_API_DEFAULT)
    val classRegistry = ClassRegistryImpl(fileRegistry, reflector)

    return GripImpl(fileRegistry, classRegistry)
  }
}
