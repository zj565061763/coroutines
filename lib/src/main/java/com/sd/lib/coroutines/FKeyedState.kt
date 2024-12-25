package com.sd.lib.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FKeyedState<T>(
  /** 获取默认值(主线程) */
  private val getDefault: (key: String) -> T,
) {
  private val _holder: MutableMap<String, ReleaseAbleFlow<T>> = mutableMapOf()
  private val _mainScope = MainScope()

  /** 获取[key]对应的状态流 */
  fun flowOf(key: String): Flow<T> {
    return channelFlow {
      collectState(key) {
        send(it)
      }
    }.conflate().distinctUntilChanged()
  }

  /** 更新[key]对应的状态 */
  fun update(key: String, state: T) {
    updateState(key = key, state = state, release = false)
  }

  /** 更新[key]对应的状态，并在更新之后尝试释放该[key] */
  fun updateAndRelease(key: String, state: T) {
    updateState(key = key, state = state, release = true)
  }

  private fun updateState(key: String, state: T, release: Boolean) {
    _mainScope.launch {
      val flow = _holder.getOrPut(key) { newFlow(key, state) }
      flow.update(state)
      if (release) {
        flow.release()
      }
    }
  }

  private suspend fun collectState(key: String, block: suspend (T) -> Unit) {
    withContext(Dispatchers.Main) {
      val flow = _holder.getOrPut(key) { newFlow(key, getDefault(key)) }
      flow.collect(block)
    }
  }

  private fun newFlow(key: String, initialState: T): ReleaseAbleFlow<T> {
    return ReleaseAbleFlow(
      initialState = initialState,
      onIdle = { _holder.remove(key) },
    )
  }

  private class ReleaseAbleFlow<T>(
    initialState: T,
    private val onIdle: () -> Unit,
  ) {
    private val _flow = MutableStateFlow(initialState)
    private var _releaseAble = true

    suspend fun collect(block: suspend (T) -> Unit) {
      try {
        _flow.collect {
          block(it)
        }
      } finally {
        releaseIfIdle()
      }
    }

    fun update(state: T) {
      _releaseAble = false
      _flow.value = state
    }

    fun release() {
      _releaseAble = true
      releaseIfIdle()
    }

    private fun releaseIfIdle() {
      if (_releaseAble && _flow.subscriptionCount.value == 0) {
        onIdle()
      }
    }
  }
}