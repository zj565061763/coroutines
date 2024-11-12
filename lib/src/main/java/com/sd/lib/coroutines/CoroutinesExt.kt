package com.sd.lib.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

val Dispatchers.fMain: MainCoroutineDispatcher
   get() = runCatching { Main.immediate }.getOrDefault(Main)

/**
 * 启动全局协程，[block]总是在主线程运行
 */
@OptIn(DelicateCoroutinesApi::class)
fun fGlobalLaunch(
   context: CoroutineContext = EmptyCoroutineContext,
   start: CoroutineStart = CoroutineStart.DEFAULT,
   block: suspend CoroutineScope.() -> Unit,
) {
   GlobalScope.launch(
      context = context,
      start = start,
      block = {
         withContext(Dispatchers.fMain) {
            block()
         }
      },
   )
}