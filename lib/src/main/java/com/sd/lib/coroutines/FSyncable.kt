package com.sd.lib.coroutines

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

interface FSyncable<T> {
   /** 是否正在同步中 */
   val isSyncing: Boolean

   /** 是否正在同步中状态流 */
   val syncingFlow: Flow<Boolean>

   /** 同步并等待结果 */
   suspend fun sync(): Result<T>
}

suspend fun <T> FSyncable<T>.syncOrThrow(): T {
   return sync().getOrThrow()
}

/**
 * 如果[FSyncable]正在同步中，则会挂起直到同步结束
 */
suspend fun FSyncable<*>.awaitIdle() {
   syncingFlow.first { !it }
}

/**
 * 调用[FSyncable.sync]时，如果[FSyncable]处于空闲状态，则当前协程会切换到主线程执行[onSync]，
 * 如果执行未完成时又有新协程调用[FSyncable.sync]方法，则新协程会挂起等待结果，
 * 注意：[onSync]中的所有异常都会被捕获，包括[CancellationException]
 */
fun <T> FSyncable(
   onSync: suspend () -> T,
): FSyncable<T> = SyncableImpl(onSync)

private class SyncableImpl<T>(
   private val onSync: suspend () -> T,
) : FSyncable<T> {
   private val _continuations = FContinuations<Result<T>>()
   private val _syncingFlow = MutableStateFlow(false)

   private var _syncing: Boolean
      get() = _syncingFlow.value
      set(value) {
         _syncingFlow.value = value
      }

   override val isSyncing: Boolean
      get() = _syncing

   override val syncingFlow: Flow<Boolean>
      get() = _syncingFlow.asStateFlow()

   override suspend fun sync(): Result<T> {
      if (currentCoroutineContext()[SyncElement]?.syncable === this@SyncableImpl) {
         throw ReSyncException("Can not call sync in the onSync block.")
      }
      return withContext(Dispatchers.preferMainImmediate) {
         if (_syncing) {
            _continuations.await()
         } else {
            try {
               _syncing = true
               withContext(SyncElement(this@SyncableImpl)) {
                  runCatching {
                     coroutineScope { onSync() }
                  }
               }.onFailure { error ->
                  /** 只检查[ReSyncException]，把其他异常当作普通异常，包括[CancellationException] */
                  if (error is ReSyncException) throw error
               }.also { result ->
                  _continuations.resumeAll(result)
               }
            } catch (e: Throwable) {
               _continuations.cancelAll()
               throw e
            } finally {
               _syncing = false
            }
         }
      }
   }
}

private class SyncElement(
   val syncable: FSyncable<*>,
) : AbstractCoroutineContextElement(SyncElement) {
   companion object Key : CoroutineContext.Key<SyncElement>
}

private class ReSyncException(message: String) : IllegalStateException(message)