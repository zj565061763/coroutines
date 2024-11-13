package com.sd.demo.coroutines

import app.cash.turbine.test
import com.sd.lib.coroutines.FKeyedState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class KeyedStateTest {
   @get:Rule
   val mainDispatcherRule = MainDispatcherRule()

   @Test
   fun `test getOrNull`() = runTest {
      val keyedState = FKeyedState<Int>()
      assertEquals(null, keyedState.getOrNull(""))

      keyedState.emit("", 111)
      assertEquals(111, keyedState.getOrNull(""))

      keyedState.emitAndRelease("", 222)
      assertEquals(null, keyedState.getOrNull(""))
   }

   @Test
   fun `test emitAndRelease`() = runTest {
      val count = AtomicInteger()
      val keyedState = FKeyedState<Int>()

      val job = launch {
         keyedState.flowOf("").collect { state ->
            count.updateAndGet { it + state }
         }
      }.also {
         runCurrent()
         assertEquals(0, count.get())
      }

      keyedState.emitAndRelease("", 1)
      runCurrent()
      assertEquals(1, count.get())
      assertEquals(1, keyedState.getOrNull(""))

      job.cancelAndJoin()
      assertEquals(null, keyedState.getOrNull(""))
   }

   @Test
   fun `test flow`() = runTest {
      val keyedState = FKeyedState<Int>()
      keyedState.flowOf("").test {
         repeat(10) { keyedState.emit("", 111) }
         assertEquals(111, awaitItem())

         keyedState.emit("", 222)
         assertEquals(222, awaitItem())
      }
   }
}