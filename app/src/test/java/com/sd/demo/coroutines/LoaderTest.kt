package com.sd.demo.coroutines

import app.cash.turbine.test
import com.sd.lib.coroutines.FLoader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoaderTest {

   @Test
   fun `test load when success`() = runTest {
      val loader = FLoader()
      assertEquals(null, loader.state.result)
      loader.load {
         1
      }.also { result ->
         assertEquals(true, loader.state.result!!.isSuccess)
         assertEquals(1, result.getOrThrow())
      }
   }

   @Test
   fun `test load when error in block`() = runTest {
      val loader = FLoader()
      loader.load {
         error("error in block")
      }.also { result ->
         assertEquals("error in block", result.exceptionOrNull()!!.message)
         assertEquals("error in block", loader.state.result!!.exceptionOrNull()!!.message)
      }
   }

   @Test
   fun `test load when loading`() = runTest {
      val loader = FLoader()

      val job = launch {
         loader.load { delay(Long.MAX_VALUE) }
      }.also {
         runCurrent()
      }

      loader.load {
         assertEquals(true, job.isCancelled)
         assertEquals(true, job.isCompleted)
         2
      }.also { result ->
         assertEquals(2, result.getOrThrow())
      }
   }

   @Test
   fun `test load when cancelLoad`() = runTest {
      val loader = FLoader()
      launch {
         loader.load { delay(Long.MAX_VALUE) }
      }.also { job ->
         runCurrent()
         loader.cancelLoad()
         assertEquals(true, job.isCancelled)
         assertEquals(true, job.isCompleted)
      }
   }

   @Test
   fun `test load when throw CancellationException in block`() = runTest {
      val loader = FLoader()
      launch {
         loader.load { throw CancellationException() }
      }.also { job ->
         runCurrent()
         assertEquals(true, job.isCancelled)
         assertEquals(true, job.isCompleted)
      }
   }

   @Test
   fun `test load when cancel in block`() = runTest {
      val loader = FLoader()
      launch {
         loader.load { currentCoroutineContext().cancel() }
      }.also { job ->
         runCurrent()
         assertEquals(true, job.isCancelled)
         assertEquals(true, job.isCompleted)
      }
   }

   @Test
   fun `test load when error in onFinish block`() = runTest {
      val loader = FLoader()
      runCatching {
         loader.load(
            onFinish = { error("onFinish error") },
         ) {}
      }.also { result ->
         assertEquals("onFinish error", result.exceptionOrNull()!!.message)
      }
   }

   @Test
   fun `test loadingFlow when params true`() = runTest {
      val loader = FLoader()
      loader.loadingFlow.test {
         loader.load(notifyLoading = true) {}
         assertEquals(false, awaitItem())
         assertEquals(true, awaitItem())
         assertEquals(false, awaitItem())
      }
   }

   @Test
   fun `test loadingFlow when params false`() = runTest {
      val loader = FLoader()
      loader.loadingFlow.test {
         loader.load(notifyLoading = false) {}
         assertEquals(false, awaitItem())
      }
   }

   @Test
   fun `test loadingFlow when Reload`() = runTest {
      val loader = FLoader()
      loader.loadingFlow.test {
         launch {
            loader.load { delay(Long.MAX_VALUE) }
         }.also {
            runCurrent()
            loader.load { }
         }
         assertEquals(false, awaitItem())
         assertEquals(true, awaitItem())
         assertEquals(false, awaitItem())
         assertEquals(true, awaitItem())
         assertEquals(false, awaitItem())
      }
   }

   @Test
   fun `test loadingFlow when cancelLoad`() = runTest {
      val loader = FLoader()
      loader.loadingFlow.test {
         launch {
            loader.load { delay(Long.MAX_VALUE) }
         }.also {
            runCurrent()
            loader.cancelLoad()
         }
         assertEquals(false, awaitItem())
         assertEquals(true, awaitItem())
         assertEquals(false, awaitItem())
      }
   }

   @Test
   fun `test callback when load success`() = runTest {
      val loader = FLoader()
      mutableListOf<String>().also { list ->
         loader.load(
            onFinish = { list.add("onFinish") },
            onLoad = { list.add("onLoad") },
         ).also {
            assertEquals("onLoad|onFinish", list.joinToString("|"))
         }
      }
   }

   @Test
   fun `test callback when load error`() = runTest {
      val loader = FLoader()
      mutableListOf<String>().also { list ->
         loader.load(
            onFinish = { list.add("onFinish") },
            onLoad = {
               list.add("onLoad")
               error("error")
            },
         ).also {
            assertEquals("onLoad|onFinish", list.joinToString("|"))
         }
      }
   }

   @Test
   fun `test callback when cancelLoad`() = runTest {
      val loader = FLoader()
      val listCallback = mutableListOf<String>()

      launch {
         loader.load(
            onFinish = { listCallback.add("onFinish") },
            onLoad = {
               listCallback.add("onLoad")
               delay(Long.MAX_VALUE)
            },
         )
      }

      runCurrent()
      loader.cancelLoad()

      assertEquals("onLoad|onFinish", listCallback.joinToString("|"))
   }
}