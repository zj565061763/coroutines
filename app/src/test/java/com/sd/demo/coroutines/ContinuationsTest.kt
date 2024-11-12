package com.sd.demo.coroutines

import com.sd.lib.coroutines.FContinuations
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class ContinuationsTest {
   @Test
   fun `test resumeAll`() = runTest {
      val continuations = FContinuations<Int>()
      val count = AtomicInteger(0)

      repeat(3) {
         launch {
            val result = continuations.await()
            count.updateAndGet { it + result }
         }
      }

      runCurrent()

      // resumeAll
      continuations.resumeAll(1)
      continuations.resumeAll(2)

      advanceUntilIdle()
      assertEquals(3, count.get())
   }

   @Test
   fun `test resumeAllWithException`() = runTest {
      val continuations = FContinuations<Int>()
      val count = AtomicInteger(0)

      repeat(3) {
         launch {
            try {
               continuations.await()
            } catch (e: Throwable) {
               assertEquals("resumeAllWithException 1", e.message)
               count.updateAndGet { it + 1 }
            }
         }
      }

      runCurrent()

      // resumeAllWithException
      continuations.resumeAllWithException(Exception("resumeAllWithException 1"))
      continuations.resumeAllWithException(Exception("resumeAllWithException 2"))

      advanceUntilIdle()
      assertEquals(3, count.get())
   }

   @Test
   fun `test cancelAll`() = runTest {
      val continuations = FContinuations<Int>()
      val count = AtomicInteger(0)

      repeat(3) {
         launch {
            try {
               continuations.await()
            } catch (e: Throwable) {
               assertEquals(true, e is CancellationException)
               count.updateAndGet { it + 1 }
            }
         }
      }

      runCurrent()

      // cancelAll
      continuations.cancelAll()
      continuations.cancelAll()

      advanceUntilIdle()
      assertEquals(3, count.get())
   }

   @Test
   fun `test cancelAll with cause`() = runTest {
      val continuations = FContinuations<Int>()
      val count = AtomicInteger(0)

      repeat(3) {
         launch {
            try {
               continuations.await()
            } catch (e: Throwable) {
               assertEquals("cancelAll with cause 1", e.message)
               count.updateAndGet { it + 1 }
            }
         }
      }

      runCurrent()

      // cancelAll with cause
      continuations.cancelAll(Exception("cancelAll with cause 1"))
      continuations.cancelAll(Exception("cancelAll with cause 2"))

      advanceUntilIdle()
      assertEquals(3, count.get())
   }

   @Test
   fun `test cancel outside`() = runTest {
      val continuations = FContinuations<Int>()
      val count = AtomicInteger(0)

      val scope = TestScope(testScheduler)
      repeat(3) {
         scope.launch {
            try {
               continuations.await()
            } catch (e: Throwable) {
               assertEquals(true, e is CancellationException)
               count.updateAndGet { it + 1 }
            }
         }
      }

      runCurrent()

      // cancel outside
      scope.cancel()

      advanceUntilIdle()
      assertEquals(3, count.get())
   }
}