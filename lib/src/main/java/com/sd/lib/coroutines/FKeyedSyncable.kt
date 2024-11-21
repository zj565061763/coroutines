package com.sd.lib.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class FKeyedSyncable<T> {
   private val _holder = mutableMapOf<String, FSyncable<T>>()
   private val _loadingState = FKeyedState { false }

   suspend fun sync(
      key: String,
      block: suspend () -> T,
   ): Result<T> {
      return withContext(Dispatchers.fPreferMainImmediate) {
         val syncable = _holder.getOrPut(key) { newSyncable(key, block) }
         if (syncable.isSyncing) {
            syncable.sync()
         } else {
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