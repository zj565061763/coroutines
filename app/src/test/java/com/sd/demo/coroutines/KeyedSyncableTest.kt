package com.sd.demo.coroutines

import app.cash.turbine.test
import com.sd.lib.coroutines.FKeyedSyncable
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

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
   fun `test syncingFlow when sync success`() = runTest {
      val syncable = FKeyedSyncable<Int>()
      syncable.syncingFlow("").test {
         assertEquals(false, awaitItem())
         syncable.sync("") { 1 }
         assertEquals(true, awaitItem())
         assertEquals(false, awaitItem())
      }
   }
}