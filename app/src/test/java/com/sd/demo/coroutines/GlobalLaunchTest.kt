package com.sd.demo.coroutines

import app.cash.turbine.test
import com.sd.lib.coroutines.fGlobalLaunch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableSharedFlow
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
      val flow = MutableSharedFlow<Any>()
      flow.test {
         fGlobalLaunch {
            flow.emit(1)
         }.also {
            assertEquals(1, awaitItem())
         }

         fGlobalLaunch {
            throw CancellationException()
         }

         fGlobalLaunch {
            flow.emit(2)
         }.also {
            assertEquals(2, awaitItem())
         }

         fGlobalLaunch {
            currentCoroutineContext().cancel()
         }

         fGlobalLaunch {
            flow.emit(3)
         }.also {
            assertEquals(3, awaitItem())
         }
      }
   }
}