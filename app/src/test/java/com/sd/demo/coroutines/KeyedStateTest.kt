package com.sd.demo.coroutines

import app.cash.turbine.test
import com.sd.lib.coroutines.FKeyedState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
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
      val state = FKeyedState<Int>()
      assertEquals(null, state.getOrNull(""))

      state.update("", 111)
      advanceUntilIdle()
      assertEquals(111, state.getOrNull(""))

      state.updateAndRelease("", 222)
      advanceUntilIdle()
      assertEquals(null, state.getOrNull(""))
   }

   @Test
   fun `test emitAndRelease`() = runTest {
      val count = AtomicInteger()
      val state = FKeyedState<Int>()

      val job = launch {
         state.flowOf("").collect { data ->
            count.updateAndGet { it + data }
         }
      }.also {
         runCurrent()
         assertEquals(0, count.get())
      }

      state.updateAndRelease("", 1)
      runCurrent()
      assertEquals(1, count.get())
      assertEquals(1, state.getOrNull(""))

      job.cancelAndJoin()
      assertEquals(null, state.getOrNull(""))
   }

   @Test
   fun `test none emit collect`() = runTest {
      val count = AtomicInteger()
      val state = FKeyedState<Int>()

      val job = launch {
         state.flowOf("").collect { data ->
            count.updateAndGet { it + data }
         }
      }.also {
         runCurrent()
         assertEquals(0, count.get())
         assertEquals(1, state.size())
      }

      job.cancelAndJoin()
      assertEquals(null, state.getOrNull(""))
      assertEquals(0, count.get())
      assertEquals(0, state.size())
   }

   @Test
   fun `test flow`() = runTest {
      val state = FKeyedState<Int>()
      state.flowOf("").test {
         repeat(10) { state.update("", 111) }
         assertEquals(111, awaitItem())

         state.update("", 222)
         assertEquals(222, awaitItem())
      }
   }
}