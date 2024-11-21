package com.sd.demo.coroutines

import app.cash.turbine.test
import com.sd.lib.coroutines.fGlobalLaunch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GlobalLaunchTest {
   @get:Rule
   val mainDispatcherRule = MainDispatcherRule()

   @Test
   fun test() = runTest {
      val flow = MutableSharedFlow<Int>()
      flow.test {
         fGlobalLaunch {
            flow.emit(1)
         }.also {
            advanceUntilIdle()
            assertEquals(1, awaitItem())
         }

         fGlobalLaunch {
            throw CancellationException()
         }.also {
            advanceUntilIdle()
         }

         fGlobalLaunch {
            flow.emit(2)
         }.also {
            advanceUntilIdle()
            assertEquals(2, awaitItem())
         }

         fGlobalLaunch {
            currentCoroutineContext().cancel()
         }.also {
            advanceUntilIdle()
         }

         fGlobalLaunch {
            flow.emit(3)
         }.also {
            advanceUntilIdle()
            assertEquals(3, awaitItem())
         }
      }
   }
}