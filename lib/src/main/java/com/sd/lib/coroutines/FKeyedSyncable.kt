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
    return withContext(Dispatchers.Main) {
      _holder[key]?.sync() ?: newSyncable(key, block).let { syncable ->
        _holder[key] = syncable
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
      _loadingState.update(key, state = true)
      try {
        block()
      } finally {
        _loadingState.updateAndRelease(key, state = false)
      }
    }
  }
}

suspend fun <T> FKeyedSyncable<T>.syncOrThrow(
  key: String,
  block: suspend () -> T,
): T = sync(key, block).getOrThrow()