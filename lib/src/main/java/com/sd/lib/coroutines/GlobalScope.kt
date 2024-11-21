package com.sd.lib.coroutines

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

val Dispatchers.preferMainImmediate: MainCoroutineDispatcher
   get() = runCatching { Main.immediate }.getOrElse { Main }

private val FGlobalScope = CoroutineScope(
   SupervisorJob() + Dispatchers.preferMainImmediate + CoroutineName("FGlobalScope")
)

/**
 * 启动全局协程，[block]默认在主线程执行
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