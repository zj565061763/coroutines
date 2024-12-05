package com.sd.demo.coroutines

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sd.demo.coroutines.databinding.SampleGlobalLaunchBinding
import com.sd.lib.coroutines.fGlobalLaunch
import kotlinx.coroutines.Dispatchers

class SampleGlobalLaunch : AppCompatActivity() {
  private val _binding by lazy { SampleGlobalLaunchBinding.inflate(layoutInflater) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(_binding.root)
    _binding.btnLaunchGlobalImmediate.setOnClickListener {
      launchGlobalImmediate()
    }
    _binding.btnLaunchGlobalDispatched.setOnClickListener {
      launchGlobalDispatched()
    }
  }
}

private fun launchGlobalImmediate() {
  logMsg { "1" }
  fGlobalLaunch {
    logMsg { "2" }
  }
  logMsg { "3" }
}

private fun launchGlobalDispatched() {
  logMsg { "1" }
  fGlobalLaunch(Dispatchers.Main) {
    logMsg { "2" }
  }
  logMsg { "3" }
}