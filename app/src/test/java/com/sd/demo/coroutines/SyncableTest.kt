package com.sd.demo.coroutines

import app.cash.turbine.test
import com.sd.lib.coroutines.FSyncable
import com.sd.lib.coroutines.awaitIdle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
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
      launch {
         val result = syncable.sync()
         assertEquals("error in block", result.exceptionOrNull()!!.message)
      }.also { job ->
         runCurrent()
         assertEquals(false, job.isCancelled)
      }
   }

   @Test
   fun `test sync when throw CancellationException in block`() = runTest {
      val syncable = FSyncable { throw CancellationException() }
      launch {
         val result = syncable.sync()
         assertEquals(true, result.exceptionOrNull() is CancellationException)
      }.also { job ->
         runCurrent()
         assertEquals(false, job.isCancelled)
      }
   }

   @Test
   fun `test sync when cancel in block`() = runTest {
      val syncable = FSyncable { currentCoroutineContext().cancel() }
      launch {
         val result = syncable.sync()
         assertEquals(true, result.exceptionOrNull() is CancellationException)
      }.also { job ->
         runCurrent()
         assertEquals(false, job.isCancelled)
      }
   }

   @Test
   fun `test syncingFlow when sync success`() = runTest {
      val syncable = FSyncable { 1 }
      syncable.syncingFlow.test {
         syncable.sync()
         assertEquals(false, awaitItem())
         assertEquals(true, awaitItem())
         assertEquals(false, awaitItem())
      }
   }

   @Test
   fun `test syncingFlow when error in block`() = runTest {
      val syncable = FSyncable { error("error") }
      syncable.syncingFlow.test {
         syncable.sync()
         assertEquals(false, awaitItem())
         assertEquals(true, awaitItem())
         assertEquals(false, awaitItem())
      }
   }

   @Test
   fun `test syncingFlow when when throw CancellationException in block`() = runTest {
      val syncable = FSyncable { throw CancellationException() }
      syncable.syncingFlow.test {
         syncable.sync()
         assertEquals(false, awaitItem())
         assertEquals(true, awaitItem())
         assertEquals(false, awaitItem())
      }
   }

   @Test
   fun `test syncingFlow when cancel in block`() = runTest {
      val syncable = FSyncable { currentCoroutineContext().cancel() }
      syncable.syncingFlow.test {
         syncable.sync()
         assertEquals(false, awaitItem())
         assertEquals(true, awaitItem())
         assertEquals(false, awaitItem())
      }
   }

   @Test
   fun `test syncingFlow when cancel sync`() = runTest {
      val syncable = FSyncable { delay(Long.MAX_VALUE) }
      syncable.syncingFlow.test {
         launch {
            syncable.sync()
         }.also { job ->
            runCurrent()
            job.cancelAndJoin()
         }
         assertEquals(false, awaitItem())
         assertEquals(true, awaitItem())
         assertEquals(false, awaitItem())
      }
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
         syncable.awaitIdle()
         count.incrementAndGet()
      }.also {
         runCurrent()
         assertEquals(0, count.get())
         advanceUntilIdle()
         assertEquals(1, count.get())
      }
   }

   @Test
   fun `test sync multi times when success `() = runTest {
      val count = AtomicInteger(0)
      val syncable = FSyncable {
         delay(5_000)
         count.incrementAndGet()
      }

      launch {
         val result = syncable.sync()
         assertEquals(1, result.getOrThrow())
         count.incrementAndGet()
      }.also {
         runCurrent()
         assertEquals(0, count.get())
      }

      repeat(3) {
         launch {
            val result = syncable.sync()
            assertEquals(1, result.getOrThrow())
            count.incrementAndGet()
         }
      }

      runCurrent()
      assertEquals(0, count.get())

      advanceUntilIdle()
      assertEquals(5, count.get())
   }

   @Test
   fun `test sync multi times when error in block`() = runTest {
      val count = AtomicInteger(0)
      val syncable = FSyncable {
         delay(5_000)
         error("error in block")
      }

      launch {
         val result = syncable.sync()
         assertEquals("error in block", result.exceptionOrNull()!!.message)
         count.incrementAndGet()
      }.also {
         runCurrent()
         assertEquals(0, count.get())
      }

      repeat(3) {
         launch {
            val result = syncable.sync()
            assertEquals("error in block", result.exceptionOrNull()!!.message)
            count.incrementAndGet()
         }
      }

      runCurrent()
      assertEquals(0, count.get())

      advanceUntilIdle()
      assertEquals(4, count.get())
   }

   @Test
   fun `test sync multi times when throw CancellationException in block`() = runTest {
      val count = AtomicInteger(0)
      val syncable = FSyncable {
         delay(5_000)
         throw CancellationException()
      }

      val jobs = mutableSetOf<Job>()
      repeat(3) {
         launch {
            val result = syncable.sync()
            assertEquals(true, result.exceptionOrNull() is CancellationException)
            count.incrementAndGet()
         }.also {
            jobs.add(it)
         }
      }

      runCurrent()
      assertEquals(3, jobs.size)
      jobs.forEach {
         assertEquals(true, it.isActive)
      }

      advanceUntilIdle()
      assertEquals(3, jobs.size)
      jobs.forEach {
         assertEquals(false, it.isCancelled)
      }

      assertEquals(3, count.get())
   }

   @Test
   fun `test sync multi times when cancel in block`() = runTest {
      val count = AtomicInteger(0)
      val syncable = FSyncable {
         delay(5_000)
         currentCoroutineContext().cancel()
      }

      val jobs = mutableSetOf<Job>()
      repeat(3) {
         launch {
            val result = syncable.sync()
            assertEquals(true, result.exceptionOrNull() is CancellationException)
            count.incrementAndGet()
         }.also {
            jobs.add(it)
         }
      }

      runCurrent()
      assertEquals(3, jobs.size)
      jobs.forEach {
         assertEquals(true, it.isActive)
      }

      advanceUntilIdle()
      assertEquals(3, jobs.size)
      jobs.forEach {
         assertEquals(false, it.isCancelled)
      }

      assertEquals(3, count.get())
   }

   @Test
   fun `test sync multi times when cancel first sync`() = runTest {
      val syncable = FSyncable {
         delay(Long.MAX_VALUE)
      }

      val job1 = launch {
         syncable.sync()
      }.also {
         runCurrent()
         assertEquals(true, it.isActive)
      }

      val job2 = launch {
         syncable.sync()
      }.also {
         runCurrent()
         assertEquals(true, it.isActive)
      }

      job1.cancel()
      advanceUntilIdle()
      assertEquals(true, job1.isCancelled)
      assertEquals(true, job2.isCancelled)
   }

   @Test
   fun `test sync multi times when cancel other sync`() = runTest {
      val syncable = FSyncable {
         delay(Long.MAX_VALUE)
      }

      val job1 = launch {
         syncable.sync()
      }.also {
         runCurrent()
         assertEquals(true, it.isActive)
      }

      val job2 = launch {
         syncable.sync()
      }.also {
         runCurrent()
         assertEquals(true, it.isActive)
      }

      job2.cancelAndJoin()
      assertEquals(true, job1.isActive)
      assertEquals(true, job2.isCancelled)

      job1.cancelAndJoin()
      assertEquals(true, job1.isCancelled)
      assertEquals(true, job2.isCancelled)
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