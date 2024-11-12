package com.sd.lib.coroutines

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FContinuations<T> {
   private val _holder: MutableCollection<CancellableContinuation<T>> = mutableListOf()

   suspend fun await(): T {
      return suspendCancellableCoroutine { cont ->
         synchronized(this@FContinuations) {
            _holder.add(cont)
         }
         cont.invokeOnCancellation {
            synchronized(this@FContinuations) {
               _holder.remove(cont)
            }
         }
      }
   }

   fun resumeAll(value: T) {
      foreach {
         it.resume(value)
      }
   }

   fun resumeAllWithException(exception: Throwable) {
      foreach {
         it.resumeWithException(exception)
      }
   }

   fun cancelAll(cause: Throwable? = null) {
      foreach {
         it.cancel(cause)
      }
   }

   private inline fun foreach(block: (CancellableContinuation<T>) -> Unit) {
      synchronized(this@FContinuations) {
         _holder.toTypedArray().also {
            _holder.clear()
         }
      }.forEach(block)
   }
}