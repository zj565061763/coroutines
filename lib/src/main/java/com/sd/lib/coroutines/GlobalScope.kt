package com.sd.lib.coroutines

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private val FGlobalScope = CoroutineScope(
  SupervisorJob() + Dispatchers.Main + CoroutineName("FGlobalScope")
)

/**
 * 全局协程，默认调度器[Dispatchers.Main]
 */
fun fGlobalLaunch(
  context: CoroutineContext = EmptyCoroutineContext,
  start: CoroutineStart = CoroutineStart.DEFAULT,
  block: suspend CoroutineScope.() -> Unit,
): Job {
  return FGlobalScope.launch(
    context = context,
    start = start,
    block = block,
  )
}