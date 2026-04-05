package com.yaycheckmate.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.yaycheckmate.app.databinding.ActivityMainBinding
import com.yaycheckmate.app.ui.add.AddObjectActivity
import com.yaycheckmate.app.ui.detection.DetectionActivity
import com.yaycheckmate.app.ui.library.LibraryActivity

/**
 * Brief splash branding, then the main navigation hub for the app.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnAddObject.setOnClickListener {
            startActivity(Intent(this, AddObjectActivity::class.java))
        }
        binding.btnLibrary.setOnClickListener {
            startActivity(Intent(this, LibraryActivity::class.java))
        }
        binding.btnDetection.setOnClickListener {
            startActivity(DetectionActivity.createIntent(this, null))
        }

        if (savedInstanceState == null) {
            binding.splashContainer.visibility = View.VISIBLE
            binding.menuContainer.visibility = View.GONE
            handler.postDelayed({
                binding.splashContainer.visibility = View.GONE
                binding.menuContainer.visibility = View.VISIBLE
            }, 1400L)
        } else {
            binding.splashContainer.visibility = View.GONE
            binding.menuContainer.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
