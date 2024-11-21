package com.sd.demo.coroutines

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.sd.demo.coroutines.databinding.ActivityMainBinding
import com.sd.lib.coroutines.fGlobalLaunch
import kotlinx.coroutines.Dispatchers

class MainActivity : AppCompatActivity() {
   private val _binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

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

inline fun logMsg(block: () -> String) {
   Log.i("coroutines-demo", block())
}