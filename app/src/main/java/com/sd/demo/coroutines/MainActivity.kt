package com.sd.demo.coroutines

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.sd.demo.coroutines.databinding.ActivityMainBinding
import com.sd.lib.coroutines.fGlobalLaunch

class MainActivity : AppCompatActivity() {
   private val _binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContentView(_binding.root)
      _binding.btn.setOnClickListener {
         launchGlobal()
      }
   }
}

private fun launchGlobal() {
   logMsg { "1" }
   fGlobalLaunch {
      logMsg { "2" }
   }
   logMsg { "3" }
}

inline fun logMsg(block: () -> String) {
   Log.i("coroutines-demo", block())
}