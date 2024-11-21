package com.sd.lib.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext

class FKeyedState<T>(
   /** 获取默认值(主线程) */
   private val getDefault: (key: String) -> T,
) {
   private val _holder: MutableMap<String, KeyedFlow> = mutableMapOf()

   /** 更新[key]对应的状态 */
   fun update(key: String, state: T) {
      updateInternal(
         key = key,
         state = state,
         release = false
      )
   }

   /** 更新[key]对应的状态，并在更新之后尝试释放该状态 */
   fun updateAndRelease(key: String, state: T) {
      updateInternal(
         key = key,
         state = state,
         release = true
      )
   }

   /** 获取[key]对应的状态流 */
   fun flowOf(key: String): Flow<T> {
      return channelFlow {
         collect(key) {
            send(it)
         }
      }
   }

   private fun updateInternal(key: String, state: T, release: Boolean) {
      /** 注意，这里要切换到[Dispatchers.Main]保证按调用顺序更新状态 */
      fGlobalLaunch(Dispatchers.Main) {
         val holder = _holder.getOrPut(key) { KeyedFlow(key, state) }
         holder.update(state)
         if (release) {
            holder.release()
         }
      }
   }

   private suspend fun collect(key: String, block: suspend (T) -> Unit) {
      withContext(Dispatchers.fPreferMainImmediate) {
         val holder = _holder.getOrPut(key) { KeyedFlow(key, getDefault(key)) }
         holder.collect(block)
      }
   }

   private inner class KeyedFlow(
      private val key: String,
      initialState: T,
   ) {
      private var _releaseAble = true
      private val _flow = MutableStateFlow(initialState)

      fun update(value: T) {
         _releaseAble = false
         _flow.value = value
      }

      suspend fun collect(block: suspend (T) -> Unit) {
         try {
            _flow.collect {
               block(it)
            }
         } finally {
            releaseIfIdle()
         }
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