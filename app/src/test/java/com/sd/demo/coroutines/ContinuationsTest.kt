package com.sd.demo.coroutines

import app.cash.turbine.test
import com.sd.lib.coroutines.FContinuations
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
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
      val flow = MutableSharedFlow<Any>()
      flow.test {
         repeat(3) {
            launch {
               val result = continuations.await()
               flow.emit(result)
            }
         }

         runCurrent()
         continuations.resumeAll(1)
         continuations.resumeAll(2)

         repeat(3) {
            assertEquals(1, awaitItem())
         }
      }
   }

   @Test
   fun `test resumeAllWithException`() = runTest {
      val continuations = FContinuations<Int>()
      val flow = MutableSharedFlow<Any>()
      flow.test {
         repeat(3) {
            launch {
               try {
                  continuations.await()
                  flow.emit(1)
               } catch (e: Throwable) {
                  flow.emit(e)
               }
            }
         }

         runCurrent()
         continuations.resumeAllWithException(IllegalArgumentException("resumeAllWithException 1"))
         continuations.resumeAllWithException(IllegalStateException("resumeAllWithException 2"))

         repeat(3) {
            assertEquals("resumeAllWithException 1", (awaitItem() as IllegalArgumentException).message)
         }
      }
   }

   @Test
   fun `test cancelAll`() = runTest {
      val continuations = FContinuations<Int>()
      val flow = MutableSharedFlow<Any>()
      flow.test {
         repeat(3) {
            launch {
               try {
                  continuations.await()
                  flow.emit(1)
               } catch (e: Throwable) {
                  flow.emit(e)
               }
            }
         }

         runCurrent()
         continuations.cancelAll()
         continuations.cancelAll()

         repeat(3) {
            assertEquals(true, awaitItem() is CancellationException)
         }
      }
   }

   @Test
   fun `test cancelAll with cause`() = runTest {
      val continuations = FContinuations<Int>()
      val flow = MutableSharedFlow<Any>()
      flow.test {
         repeat(3) {
            launch {
               try {
                  continuations.await()
                  flow.emit(1)
               } catch (e: Throwable) {
                  flow.emit(e)
                  assertEquals("cancelAll with cause 1", e.message)
               }
            }
         }

         runCurrent()
         continuations.cancelAll(IllegalArgumentException("cancelAll with cause 1"))
         continuations.cancelAll(IllegalStateException("cancelAll with cause 2"))

         repeat(3) {
            assertEquals("cancelAll with cause 1", (awaitItem() as IllegalArgumentException).message)
         }
      }
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