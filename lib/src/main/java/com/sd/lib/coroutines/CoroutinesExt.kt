package com.sd.lib.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainCoroutineDispatcher

val Dispatchers.fMain: MainCoroutineDispatcher
   get() = runCatching { Main.immediate }.getOrDefault(Main)