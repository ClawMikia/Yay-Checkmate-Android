package com.yaycheckmate.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.yaycheckmate.R
import com.yaycheckmate.databinding.ActivityOnboardingBinding
import com.yaycheckmate.ui.MainActivity
import com.yaycheckmate.utils.PreferencesManager
import kotlinx.coroutines.launch

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var prefsManager: PreferencesManager

    data class OnboardingPage(val emoji: String, val title: String, val desc: String)

    private val pages = listOf(
        OnboardingPage("🔍", "Welcome to Yay Checkmate", "Find What Matters.\nYour AI-powered object finder with offline vision technology."),
        OnboardingPage("📸", "Register Your Objects", "Take photos from multiple angles. The more photos, the better the detection accuracy."),
        OnboardingPage("🗺️", "Smart Heatmap", "The app learns where you usually find your objects and prioritizes those locations."),
        OnboardingPage("🎮", "Earn XP & Rank Up", "Every find earns you XP and coins. Rise through the ranks from Rookie Detective to Legendary Finder!"),
        OnboardingPage("🤖", "Meet Checky", "Your AI Search Companion will guide you through every search session. Let's get started!")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = PreferencesManager(this)

        val adapter = OnboardingAdapter(pages)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { _, _ -> }.attach()

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position == pages.lastIndex) {
                    binding.btnNext.text = "Let's Go!"
                } else {
                    binding.btnNext.text = "Next"
                }
            }
        })

        binding.btnNext.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current < pages.lastIndex) {
                binding.viewPager.currentItem = current + 1
            } else {
                finishOnboarding()
            }
        }

        binding.btnSkip.setOnClickListener { finishOnboarding() }
    }

    private fun finishOnboarding() {
        lifecycleScope.launch {
            prefsManager.setOnboardingDone(true)
            startActivity(Intent(this@OnboardingActivity, MainActivity::class.java))
            finish()
        }
    }

    inner class OnboardingAdapter(private val items: List<OnboardingPage>) :
        RecyclerView.Adapter<OnboardingAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tvEmoji: TextView = view.findViewById(R.id.tvOnboardEmoji)
            val tvTitle: TextView = view.findViewById(R.id.tvOnboardTitle)
            val tvDesc: TextView = view.findViewById(R.id.tvOnboardDesc)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_onboarding_page, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val page = items[position]
            holder.tvEmoji.text = page.emoji
            holder.tvTitle.text = page.title
            holder.tvDesc.text = page.desc
        }

        override fun getItemCount() = items.size
    }
}
