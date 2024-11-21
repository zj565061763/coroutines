package com.sd.lib.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class FKeyedSyncable<T> {
   private val _holder = mutableMapOf<String, FSyncable<T>>()
   private val _loadingState = FKeyedState { false }

   suspend fun syncOrThrow(
      key: String,
      block: suspend () -> T,
   ): T {
      return sync(key, block).getOrThrow()
   }

   suspend fun sync(
      key: String,
      block: suspend () -> T,
   ): Result<T> {
      return withContext(Dispatchers.preferMainImmediate) {
         _holder[key]?.sync() ?: newSyncable(key, block).let { syncable ->
            _holder[key] = syncable.also { check(!it.isSyncing) }
            try {
               syncable.sync()
            } finally {
               _holder.remove(key)
            }
         }
      }
   }

   fun syncingFlow(key: String): Flow<Boolean> {
      return _loadingState.flowOf(key)
   }

   private fun newSyncable(
      key: String,
      block: suspend () -> T,
   ): FSyncable<T> {
      return FSyncable {
         try {
            _loadingState.update(key, true)
            block()
         } finally {
            _loadingState.updateAndRelease(key, false)
         }
      }
   }
}