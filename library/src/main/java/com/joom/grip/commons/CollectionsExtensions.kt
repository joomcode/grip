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

package com.joom.grip.commons

import java.util.Collections
import java.util.SortedMap
import java.util.SortedSet

internal fun <T> Collection<T>.immutable(): Collection<T> = Collections.unmodifiableCollection(this)
internal fun <T> List<T>.immutable(): List<T> = Collections.unmodifiableList(this)
internal fun <K, V> Map<K, V>.immutable(): Map<K, V> = Collections.unmodifiableMap(this)
internal fun <T> Set<T>.immutable(): Set<T> = Collections.unmodifiableSet(this)
internal fun <K, V> SortedMap<K, V>.immutable(): SortedMap<K, V> = Collections.unmodifiableSortedMap(this)
internal fun <T> SortedSet<T>.immutable(): SortedSet<T> = Collections.unmodifiableSortedSet(this)
internal inline fun <T : Any> Iterable<T>.singleOrNullIfNotFound(message: () -> String, predicate: (T) -> Boolean): T? {
  var found: T? = null
  forEach {
    if (predicate(it)) {
      if (found != null) {
        throw IllegalArgumentException(message())
      } else {
        found = it
      }
    }
  }

  return found
}
