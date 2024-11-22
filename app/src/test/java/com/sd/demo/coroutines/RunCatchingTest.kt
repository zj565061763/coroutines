package com.sd.demo.coroutines

import com.sd.lib.coroutines.fRunCatchingIgnore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RunCatchingTest {
   @Test
   fun `test runCatchingIgnore`() = runTest {
      runCatching {
         fRunCatchingIgnore {
            throw CancellationException("error")
         }
      }.also { result ->
         assertEquals("error", (result.exceptionOrNull() as CancellationException).message)
      }
   }

   @Test
   fun `test runCatchingIgnore ignore none default`() = runTest {
      runCatching {
         fRunCatchingIgnore(
            ignore = { it is IllegalStateException },
         ) {
            error("error")
         }
      }.also { result ->
         assertEquals("error", (result.exceptionOrNull() as IllegalStateException).message)
      }
   }

   @Test
   fun `test runCatchingIgnore ignore All`() = runTest {
      runCatching {
         fRunCatchingIgnore(
            ignore = { true },
         ) {
            error("ignore All")
         }
      }.also { result ->
         assertEquals("ignore All", (result.exceptionOrNull() as IllegalStateException).message)
      }
   }

   @Test
   fun `test runCatchingIgnore ignore None`() = runTest {
      runCatching {
         fRunCatchingIgnore(
            ignore = { false },
         ) {
            error("error")
         }
      }.also { result ->
         assertEquals(true, result.isSuccess)
      }
   }
}