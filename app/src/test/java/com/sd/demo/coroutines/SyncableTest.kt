@file:OptIn(ExperimentalCoroutinesApi::class)

package com.sd.demo.coroutines

import com.sd.lib.coroutines.FSyncable
import com.sd.lib.coroutines.awaitIdle
import com.sd.lib.coroutines.syncOrThrow
import com.sd.lib.coroutines.syncOrThrowCancellation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class SyncableTest {
   @get:Rule
   val mainDispatcherRule = MainDispatcherRule()

   @Test
   fun `test sync success`() = runTest {
      val syncable = FSyncable { 1 }
      val result = syncable.sync()
      assertEquals(1, result.getOrThrow())
   }

   @Test
   fun `test sync when error in block`() = runTest {
      val syncable = FSyncable { error("error in block") }
      val result = syncable.sync()
      assertEquals("error in block", result.exceptionOrNull()!!.message)
   }

   @Test
   fun `test sync when throw CancellationException in block`() = runTest {
      val syncable = FSyncable { throw CancellationException() }
      val result = syncable.sync()
      assertEquals(true, result.exceptionOrNull() is CancellationException)
   }

   @Test
   fun `test sync when cancel in block`() = runTest {
      val syncable = FSyncable { currentCoroutineContext().cancel() }
      val result = syncable.sync()
      assertEquals(true, result.exceptionOrNull() is CancellationException)
   }

   @Test
   fun `test syncing`() = runTest {
      testSyncing(
         FSyncable {
            delay(5_000)
         }
      )

      testSyncing(
         FSyncable {
            delay(5_000)
            error("error")
         }
      )

      testSyncing(
         FSyncable {
            delay(5_000)
            throw CancellationException()
         }
      )

      testSyncing(
         FSyncable {
            delay(5_000)
            currentCoroutineContext().cancel()
         }
      )
   }

   @Test
   fun `test syncing when cancel sync`() = runTest {
      val syncable = FSyncable { delay(5_000) }

      assertEquals(false, syncable.isSyncing)
      val job = launch { syncable.sync() }

      runCurrent()
      assertEquals(true, syncable.isSyncing)

      job.cancelAndJoin()
      assertEquals(false, syncable.isSyncing)
   }

   @Test
   fun `test awaitIdle`() = runTest {
      val syncable = FSyncable { delay(5_000) }

      launch {
         syncable.sync()
      }.also {
         runCurrent()
      }

      val count = AtomicInteger(0)
      launch {
         count.incrementAndGet()
         syncable.awaitIdle()
         count.incrementAndGet()
      }

      runCurrent()
      assertEquals(1, count.get())

      advanceUntilIdle()
      assertEquals(2, count.get())
   }

   @Test
   fun `test sync multi times when success `() = runTest {
      testSyncMultiTimes(
         onSync = { 1 },
         onSyncMultiTimes = {
            assertEquals(1, it.getOrThrow())
         },
      )
   }

   @Test
   fun `test sync multi times when error in block`() = runTest {
      testSyncMultiTimes(
         onSync = { error("error in block") },
         onSyncMultiTimes = {
            assertEquals("error in block", it.exceptionOrNull()!!.message)
         },
      )
   }

   @Test
   fun `test sync multi times when throw CancellationException in block`() = runTest {
      testSyncMultiTimes(
         onSync = { throw CancellationException() },
         onSyncMultiTimes = {
            assertEquals(true, it.exceptionOrNull() is CancellationException)
         },
      )
   }

   @Test
   fun `test sync multi times when cancel in block`() = runTest {
      testSyncMultiTimes(
         onSync = { currentCoroutineContext().cancel() },
         onSyncMultiTimes = {
            assertEquals(true, it.exceptionOrNull() is CancellationException)
         },
      )
   }

   @Test
   fun `test sync multi times when cancel first sync`() = runTest {
      val syncable = FSyncable {
         delay(5_000)
      }

      val job1 = launch {
         syncable.sync()
      }.also {
         runCurrent()
      }

      val job2 = launch {
         val result = syncable.sync()
         assertEquals(true, result.exceptionOrNull() is CancellationException)
      }.also {
         runCurrent()
      }

      job1.cancel()
      advanceUntilIdle()
      assertEquals(true, job1.isCompleted)
      assertEquals(true, job2.isCompleted)

      assertEquals(true, job1.isCancelled)
      assertEquals(false, job2.isCancelled)
   }

   @Test
   fun `test sync multi times when cancel other sync`() = runTest {
      val syncable = FSyncable {
         delay(5_000)
      }

      val job1 = launch {
         syncable.sync()
      }.also {
         runCurrent()
      }

      val job2 = launch {
         syncable.sync()
      }.also {
         runCurrent()
      }

      job2.cancelAndJoin()
      assertEquals(true, job1.isActive)
      assertEquals(true, job2.isCancelled)

      job1.cancelAndJoin()
      assertEquals(true, job1.isCancelled)
      assertEquals(true, job2.isCancelled)
   }

   @Test
   fun `test syncOrThrow`() = runTest {
      val syncable = FSyncable<Unit> { error("error in block") }
      runCatching {
         syncable.syncOrThrow()
      }.also { result ->
         assertEquals("error in block", result.exceptionOrNull()!!.message)
      }
   }

   @Test
   fun `test syncOrThrowCancellation CancellationException`() = runTest {
      val syncable = FSyncable<Unit> { throw CancellationException() }
      runCatching {
         syncable.syncOrThrowCancellation()
      }.also { result ->
         assertEquals(true, result.exceptionOrNull() is CancellationException)
      }
   }

   @Test
   fun `test syncOrThrowCancellation other exception`() = runTest {
      val syncable = FSyncable<Unit> { error("error in block") }
      val result = syncable.syncOrThrowCancellation()
      assertEquals("error in block", result.exceptionOrNull()!!.message)
   }

   @Test
   fun `test reSync error`() = runTest {
      val array = arrayOf<FSyncable<*>?>(null)
      FSyncable {
         delay(1_000)
         runCatching {
            array[0]!!.sync()
         }.also { result ->
            assertEquals("Can not call sync in the onSync block.", result.exceptionOrNull()!!.message)
         }
         1
      }.also {
         array[0] = it
         assertEquals(1, it.sync().getOrThrow())
      }
   }
}

private fun TestScope.testSyncing(syncable: FSyncable<*>) {
   assertEquals(false, syncable.isSyncing)
   launch { syncable.sync() }

   runCurrent()
   assertEquals(true, syncable.isSyncing)

   advanceUntilIdle()
   assertEquals(false, syncable.isSyncing)
}

private fun <T> TestScope.testSyncMultiTimes(
   onSync: suspend () -> T,
   onSyncMultiTimes: (Result<T>) -> Unit,
) {
   val count = AtomicInteger(0)
   val syncable = FSyncable {
      delay(5_000)
      count.incrementAndGet()
      onSync()
   }

   repeat(3) {
      launch {
         try {
            val result = syncable.sync()
            onSyncMultiTimes(result)
         } finally {
            assertEquals(true, currentCoroutineContext().isActive)
         }
      }
   }

   runCurrent()
   assertEquals(0, count.get())

   advanceUntilIdle()
   assertEquals(1, count.get())
}