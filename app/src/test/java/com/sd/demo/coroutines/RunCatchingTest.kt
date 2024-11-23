package com.sd.demo.coroutines

import com.sd.lib.coroutines.fRunCatchingEscape
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RunCatchingTest {
   @Test
   fun `test runCatchingEscape`() = runTest {
      runCatching {
         fRunCatchingEscape {
            throw CancellationException("error")
         }
      }.also { result ->
         assertEquals("error", (result.exceptionOrNull() as CancellationException).message)
      }
   }

   @Test
   fun `test runCatchingEscape escape none default`() = runTest {
      runCatching {
         fRunCatchingEscape(
            escape = { it is IllegalStateException },
         ) {
            error("error")
         }
      }.also { result ->
         assertEquals("error", (result.exceptionOrNull() as IllegalStateException).message)
      }
   }

   @Test
   fun `test runCatchingEscape escape all`() = runTest {
      runCatching {
         fRunCatchingEscape(
            escape = { true },
         ) {
            error("escape all")
         }
      }.also { result ->
         assertEquals("escape all", (result.exceptionOrNull() as IllegalStateException).message)
      }
   }

   @Test
   fun `test runCatchingEscape catch all`() = runTest {
      runCatching {
         fRunCatchingEscape(
            escape = { false },
         ) {
            error("error")
         }
      }.also { result ->
         assertEquals(true, result.isSuccess)
      }
   }
}