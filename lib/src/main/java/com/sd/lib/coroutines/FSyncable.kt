package com.sd.lib.coroutines

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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

suspend fun <T> FSyncable<T>.syncOrThrowCancellation(): Result<T> {
  return sync().onFailure { e ->
    if (e is CancellationException) throw e
  }
}

/** 如果正在同步中，则会挂起直到同步结束 */
suspend fun FSyncable<*>.awaitIdle() {
  if (isSyncing) {
    syncingFlow.first { !it }
  }
}

/**
 * 调用[FSyncable.sync]时，如果[FSyncable]处于空闲状态，则当前协程会切换到主线程执行[onSync]，
 * 如果执行未完成时又有新协程调用[FSyncable.sync]，则新协程会挂起等待结果。
 *
 * 注意：[onSync]引发的所有异常都会被捕获，包括[CancellationException]，即[onSync]不会导致调用[FSyncable.sync]的协程被取消，
 * 如果调用[FSyncable.sync]的协程被取消，那一定是外部导致的。
 *
 * 这样子设计比较灵活，只要[FSyncable.sync]返回了[Result]，调用处就知道[onSync]发生的所有情况，包括取消情况，
 * 可以根据具体情况再做处理，例如知道[onSync]里面被取消了，可以选择继续往上抛出取消异常，或者处理其他逻辑。
 *
 * - 如果希望同步时抛出所有异常，可以使用方法[FSyncable.syncOrThrow]
 * - 如果希望同步时抛出取消异常，可以使用方法[FSyncable.syncOrThrowCancellation]
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
    val dispatcher = with(Dispatchers) { runCatching { Main.immediate }.getOrElse { Main } }
    return withContext(dispatcher) {
      if (_syncing) {
        if (currentCoroutineContext()[SyncElement]?.syncable === this@SyncableImpl) {
          throw ReSyncException("Can not call sync in the onSync block.")
        }
        _continuations.await()
      } else {
        doSync()
      }
    }.also {
      currentCoroutineContext().ensureActive()
    }
  }

  private suspend fun doSync(): Result<T> {
    return try {
      _syncing = true
      withContext(SyncElement(this@SyncableImpl)) {
        onSync()
      }.let { data ->
        Result.success(data).also { _continuations.resumeAll(it) }
      }
    } catch (e: Throwable) {
      Result.failure<T>(e).also { _continuations.resumeAll(it) }
    } finally {
      _syncing = false
    }
  }
}

private class SyncElement(
  val syncable: FSyncable<*>,
) : AbstractCoroutineContextElement(SyncElement) {
  companion object Key : CoroutineContext.Key<SyncElement>
}

/** 嵌套同步异常 */
private class ReSyncException(message: String) : IllegalStateException(message)