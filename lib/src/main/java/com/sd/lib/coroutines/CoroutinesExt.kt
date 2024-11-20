package com.sd.lib.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainCoroutineDispatcher

internal val Dispatchers.preferImmediateMain: MainCoroutineDispatcher
   get() = runCatching { Main.immediate }.getOrDefault(Main)