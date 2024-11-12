package com.sd.demo.coroutines

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.sd.demo.coroutines.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
   private val _binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContentView(_binding.root)

      _binding.btn.setOnClickListener {

      }
   }
}

inline fun logMsg(block: () -> String) {
   Log.i("coroutines-demo", block())
}