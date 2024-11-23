package com.sd.demo.coroutines

import app.cash.turbine.test
import com.sd.lib.coroutines.FKeyedSyncable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class KeyedSyncableTest {
   @get:Rule
   val mainDispatcherRule = MainDispatcherRule()

   @Test
   fun `test sync success`() = runTest {
      val syncable = FKeyedSyncable<Int>()
      val result = syncable.sync("") { 1 }
      assertEquals(1, result.getOrThrow())
   }

   @Test
   fun `test sync error`() = runTest {
      val syncable = FKeyedSyncable<Int>()
      val result = syncable.sync("") { error("sync error") }
      assertEquals("sync error", (result.exceptionOrNull() as IllegalStateException).message)
   }

   @Test
   fun `test sync multi times`() = runTest {
      val syncable = FKeyedSyncable<Int>()
      val count = AtomicInteger()

      launch {
         val result = syncable.sync("") {
            delay(5_000)
            1
         }
         assertEquals(1, result.getOrThrow())
         count.incrementAndGet()
      }.also {
         runCurrent()
         assertEquals(0, count.get())
      }

      repeat(3) { index ->
         launch {
            val result = syncable.sync("") { index + 100 }
            assertEquals(1, result.getOrThrow())
            count.incrementAndGet()
         }
      }

      runCurrent()
      assertEquals(0, count.get())
      advanceUntilIdle()
      assertEquals(4, count.get())
   }

   @Test
   fun `test syncingFlow when sync success`() = runTest {
      val syncable = FKeyedSyncable<Int>()
      syncable.syncingFlow("").test {
         assertEquals(false, awaitItem())
         syncable.sync("") {
            delay(5_000)
            1
         }

         runCurrent()
         assertEquals(true, awaitItem())

         advanceUntilIdle()
         assertEquals(false, awaitItem())
      }
   }

   @Test
   fun `test syncingFlow when sync error`() = runTest {
      val syncable = FKeyedSyncable<Int>()
      syncable.syncingFlow("").test {
         assertEquals(false, awaitItem())
         syncable.sync("") {
            delay(5_000)
            error("sync error")
         }

         runCurrent()
         assertEquals(true, awaitItem())

         advanceUntilIdle()
         assertEquals(false, awaitItem())
      }
   }

   @Test
   fun `test syncingFlow when sync multi times`() = runTest {
      val syncable = FKeyedSyncable<Int>()
      syncable.syncingFlow("").test {
         assertEquals(false, awaitItem())
         repeat(3) {
            launch {
               syncable.sync("") {
                  delay(5_000)
                  1
               }
            }
         }
         assertEquals(true, awaitItem())
         assertEquals(false, awaitItem())
      }
   }
}