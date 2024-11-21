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
      val state = FKeyedState { 0 }
      state.flowOf("").test {
         assertEquals(0, awaitItem())
      }
   }

   @Test
   fun `test flow after update`() = runTest {
      val state = FKeyedState { 0 }

      state.update("", 1)
      runCurrent()

      state.flowOf("").test {
         assertEquals(1, awaitItem())
         state.update("", 2)
         assertEquals(2, awaitItem())
      }
   }

   @Test
   fun `test updateAndRelease`() = runTest {
      val state = FKeyedState { 0 }
      val flow = state.flowOf("")
      flow.test {
         assertEquals(0, awaitItem())
         state.updateAndRelease("", 1)
         assertEquals(1, awaitItem())
      }
      flow.test {
         assertEquals(0, awaitItem())
      }
   }

   @Test
   fun `test update multi times`() = runTest {
      val state = FKeyedState { 0 }
      state.flowOf("").test {
         assertEquals(0, awaitItem())
         repeat(10) { state.update("", 1) }
         assertEquals(1, awaitItem())
      }
   }
}