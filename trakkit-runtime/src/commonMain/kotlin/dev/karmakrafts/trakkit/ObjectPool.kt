/*
 * Copyright 2025 Karma Krafts & associates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.karmakrafts.trakkit

import kotlin.math.ceil

internal class ObjectPool<T : Any>(
    initialSize: Int = 10,
    private val maxSize: Int = 10000,
    private val overAllocationFactor: Float = 1.2F,
    private val storageFactory: (Int) -> MutableList<T> = ::ArrayList,
    private val acquireCallback: T.() -> Unit = {},
    private val elementFactory: () -> T
) : Iterable<T> {
    companion object {
        private const val INVALID_INDEX: Int = -1
    }

    init {
        require(overAllocationFactor >= 1F) { "Overallocation factor must be >= 1.0" }
        require(maxSize > 0) { "Max size must be > 0" }
        require(initialSize in 1..maxSize) { "Initial size must be between 1 and $maxSize" }
    }

    private var storage: MutableList<T> = storageFactory(initialSize)
    private var used: ArrayList<Int> = ArrayList()

    override fun iterator(): Iterator<T> = storage.iterator()

    private fun findNextFreeIndex(): Int {
        if (used.isEmpty()) return 0
        val sorted = used.sorted()
        for (index in sorted.first()..<storage.size) {
            if (index in used) continue
            return index
        }
        return INVALID_INDEX
    }

    fun getOrCreate(): T {
        val storageSize = storage.size
        if (used.size == storageSize) {
            if (storageSize == maxSize) return elementFactory()
            val newStorageSize = ceil((storageSize + 1).toFloat() * overAllocationFactor).toInt().coerceIn(1..maxSize)
            val oldStorage = storage
            storage = storageFactory(newStorageSize)
            storage.addAll(oldStorage)
            for (index in storageSize..<newStorageSize) {
                storage[index] = elementFactory()
            }
        }
        val index = findNextFreeIndex()
        if (index == INVALID_INDEX) return elementFactory()
        val value = storage[index]
        value.acquireCallback()
        used += index
        return value
    }

    fun release(value: T) {
        used -= storage.indexOf(value)
    }

    inline fun <reified R> borrow(closure: (T) -> R): R {
        val value = getOrCreate()
        return try {
            closure(value)
        }
        finally {
            release(value)
        }
    }
}