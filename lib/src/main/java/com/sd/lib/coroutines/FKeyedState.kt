package com.sd.lib.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext

class FKeyedState<T> {
   private val _holder: MutableMap<String, KeyedFlow<T>> = mutableMapOf()

   suspend fun size(): Int {
      return withContext(Dispatchers.fPreferMainImmediate) {
         _holder.size
      }
   }

   suspend fun getOrNull(key: String): T? {
      return withContext(Dispatchers.fPreferMainImmediate) {
         _holder[key]?.getOrNull()
      }
   }

   fun flowOf(key: String): Flow<T> {
      return channelFlow {
         collect(key) {
            send(it)
         }
      }
   }

   fun update(key: String, state: T) {
      updateInternal(
         key = key,
         state = state,
         release = false,
      )
   }

   fun updateAndRelease(key: String, state: T) {
      updateInternal(
         key = key,
         state = state,
         release = true,
      )
   }

   private fun updateInternal(
      key: String,
      state: T,
      release: Boolean,
   ) {
      fGlobalLaunch {
         withContext(Dispatchers.Main) {
            val holder = _holder.getOrPut(key) { KeyedFlow(key, releaseAble = false) }
            holder.emit(state)
            if (release) {
               holder.release()
            }
         }
      }
   }

   private suspend fun collect(
      key: String,
      block: suspend (T) -> Unit,
   ) {
      withContext(Dispatchers.fPreferMainImmediate) {
         val holder = _holder.getOrPut(key) { KeyedFlow(key) }
         holder.collect(block)
      }
   }

   private inner class KeyedFlow<T>(
      private val key: String,
      releaseAble: Boolean = true,
   ) {
      private var _releaseAble = releaseAble
      private val _flow = MutableStateFlow<T?>(null)

      fun emit(value: T) {
         _releaseAble = false
         _flow.value = value
      }

      suspend fun collect(block: suspend (T) -> Unit) {
         try {
            _flow.mapNotNull { it }.collect {
               block(it)
            }
         } finally {
            releaseIfIdle()
         }
      }

      fun getOrNull(): T? {
         return _flow.value
      }

      fun release() {
         _releaseAble = true
         releaseIfIdle()
      }

      private fun releaseIfIdle() {
         if (_releaseAble && _flow.subscriptionCount.value == 0) {
            _holder.remove(key).also {
               check(it === this@KeyedFlow)
            }
         }
      }
   }
}