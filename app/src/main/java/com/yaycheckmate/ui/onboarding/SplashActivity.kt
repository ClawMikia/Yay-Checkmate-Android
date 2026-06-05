package com.yaycheckmate.ui.onboarding

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.yaycheckmate.databinding.ActivitySplashBinding
import com.yaycheckmate.ui.MainActivity
import com.yaycheckmate.utils.PreferencesManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var prefsManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = PreferencesManager(this)

        binding.tvTagline.alpha = 0f
        binding.tvTagline.animate().alpha(1f).setDuration(1000).setStartDelay(500).start()
        binding.ivLogo.scaleX = 0.5f
        binding.ivLogo.scaleY = 0.5f
        binding.ivLogo.animate().scaleX(1f).scaleY(1f).setDuration(800).start()

        lifecycleScope.launch {
            delay(2500)
            val onboardingDone = prefsManager.isOnboardingDone.first()
            if (onboardingDone) {
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            } else {
                startActivity(Intent(this@SplashActivity, OnboardingActivity::class.java))
            }
            finish()
        }
    }
}
