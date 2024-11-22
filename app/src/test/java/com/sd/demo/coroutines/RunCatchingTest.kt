package com.sd.demo.coroutines

import com.sd.lib.coroutines.fRunCatchingIgnore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class RunCatchingTest {
   @Test
   fun `test runCatchingIgnore`() = runTest {
      val count = AtomicInteger()
      launch {
         runCatching {
            fRunCatchingIgnore {
               throw CancellationException("error")
            }
         }.also { result ->
            assertEquals("error", (result.exceptionOrNull() as CancellationException).message)
            count.incrementAndGet()
         }
      }.also { job ->
         job.join()
         assertEquals(1, count.get())
      }
   }

   @Test
   fun `test runCatchingIgnore ignore none default`() = runTest {
      val count = AtomicInteger()
      launch {
         runCatching {
            fRunCatchingIgnore(
               ignore = { it is IllegalStateException },
            ) {
               error("error")
            }
         }.also { result ->
            assertEquals("error", (result.exceptionOrNull() as IllegalStateException).message)
            count.incrementAndGet()
         }
      }.also { job ->
         job.join()
         assertEquals(1, count.get())
      }
   }
}