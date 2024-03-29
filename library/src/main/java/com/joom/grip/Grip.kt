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

import java.io.Closeable
import javax.annotation.concurrent.ThreadSafe

@ThreadSafe
interface Grip : Closeable {
  val fileRegistry: FileRegistry
  val classRegistry: ClassRegistry

  infix fun <M, R> select(projection: Projection<M, R>): FromConfigurator<M, R>
}

internal class GripImpl(
  override val fileRegistry: FileRegistry,
  override val classRegistry: ClassRegistry,
  private val closeables: Iterable<Closeable> = emptyList()
) : Grip {

  private var closed = false

  override fun <M, R> select(projection: Projection<M, R>): FromConfigurator<M, R> {
    check(!closed) { "Grip was closed" }
    return projection.configurator(this)
  }

  override fun close() {
    try {
      closeables.forEach {
        it.close()
      }
    } finally {
      closed = true
    }
  }
}
