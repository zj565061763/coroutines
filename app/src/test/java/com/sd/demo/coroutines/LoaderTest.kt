package com.sd.demo.coroutines

import app.cash.turbine.test
import com.sd.lib.coroutines.FLoader
import com.sd.lib.coroutines.awaitIdle
import com.sd.lib.coroutines.tryLoad
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

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
  fun `test load when cancel()`() = runTest {
    val loader = FLoader()
    launch {
      loader.load { delay(Long.MAX_VALUE) }
    }.also { job ->
      runCurrent()
      loader.cancel()
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
  fun `test loadingFlow when cancel()`() = runTest {
    val loader = FLoader()
    loader.loadingFlow.test {
      launch {
        loader.load { delay(Long.MAX_VALUE) }
      }.also {
        runCurrent()
        loader.cancel()
      }
      assertEquals(false, awaitItem())
      assertEquals(true, awaitItem())
      assertEquals(false, awaitItem())
    }
  }

  @Test
  fun `test tryLoad`() = runTest {
    val loader = FLoader()

    val job = launch {
      loader.load { delay(Long.MAX_VALUE) }
    }.also {
      runCurrent()
    }

    runCatching {
      loader.tryLoad { 1 }
    }.also { result ->
      assertEquals(true, result.exceptionOrNull() is CancellationException)
    }

    job.cancelAndJoin()
  }

  @Test
  fun `test awaitIdle`() = runTest {
    val loader = FLoader()

    launch {
      loader.load { delay(5_000) }
    }.also {
      runCurrent()
    }

    val count = AtomicInteger(0)
    launch {
      count.incrementAndGet()
      loader.awaitIdle()
      count.incrementAndGet()
    }.also {
      runCurrent()
      assertEquals(1, count.get())
      advanceUntilIdle()
      assertEquals(2, count.get())
    }
  }
}