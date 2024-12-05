package com.sd.lib.coroutines

import kotlinx.coroutines.CancellationException

inline fun <R> fRunCatchingEscape(
  escape: (Throwable) -> Boolean = { it is CancellationException },
  block: () -> R,
): Result<R> {
  return try {
    Result.success(block())
  } catch (e: Throwable) {
    if (escape(e)) throw e
    Result.failure(e)
  }
}