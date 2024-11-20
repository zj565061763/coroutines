package com.sd.lib.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private val FGlobalScope = MainScope()

/**
 * 启动全局协程，[block]总是在主线程按顺序执行
 */
fun fGlobalLaunch(
   context: CoroutineContext = EmptyCoroutineContext,
   start: CoroutineStart = CoroutineStart.DEFAULT,
   block: suspend CoroutineScope.() -> Unit,
) {
   FGlobalScope.launch(
      context = context,
      start = start,
      block = {
         supervisorScope {
            block()
         }
      },
   )
}