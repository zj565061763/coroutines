package com.sd.demo.coroutines

import app.cash.turbine.test
import com.sd.lib.coroutines.FKeyedState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KeyedStateTest {
   @get:Rule
   val mainDispatcherRule = MainDispatcherRule()

   @Test
   fun `test flow before update`() = runTest {
      val state = FKeyedState { 100 }
      assertEquals(0, state.size())
      state.flowOf("").test {
         assertEquals(100, awaitItem())
         assertEquals(1, state.size())
      }
      assertEquals(0, state.size())
   }

   @Test
   fun `test flow after update`() = runTest {
      val state = FKeyedState { 100 }

      state.update("", 101)
      runCurrent()
      assertEquals(1, state.size())

      state.flowOf("").test {
         assertEquals(101, awaitItem())
         assertEquals(1, state.size())
      }

      assertEquals(1, state.size())
   }

   @Test
   fun `test update multi times`() = runTest {
      val state = FKeyedState { 100 }
      state.flowOf("").test {
         assertEquals(100, awaitItem())
         repeat(10) { state.update("", 101) }
         assertEquals(101, awaitItem())
         state.update("", 102)
         assertEquals(102, awaitItem())
      }
   }

   @Test
   fun `test updateAndRelease`() = runTest {
      val state = FKeyedState { 100 }
      val flow = state.flowOf("")

      flow.test {
         assertEquals(100, awaitItem())
         state.updateAndRelease("", 101)
         assertEquals(101, awaitItem())
         assertEquals(1, state.size())
      }

      assertEquals(0, state.size())
      flow.test {
         assertEquals(100, awaitItem())
         assertEquals(1, state.size())
      }
      assertEquals(0, state.size())
   }
}