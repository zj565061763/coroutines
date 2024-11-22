package com.sd.lib.coroutines

import kotlinx.coroutines.CancellationException

inline fun <R> fRunCatchingIgnore(
   ignore: (Throwable) -> Boolean = { it is CancellationException },
   block: () -> R,
): Result<R> {
   return try {
      Result.success(block())
   } catch (e: Throwable) {
      if (ignore(e)) throw e
      Result.failure(e)
   }
}