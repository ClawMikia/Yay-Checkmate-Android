package com.yaycheckmate.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.yaycheckmate.YayCheckmateApp
import com.yaycheckmate.databinding.FragmentDashboardBinding
import com.yaycheckmate.ui.register.RegisterObjectActivity
import com.yaycheckmate.ui.search.SearchActivity
import com.yaycheckmate.utils.GameConstants
import com.yaycheckmate.utils.toFormattedDuration
import com.yaycheckmate.viewmodel.DashboardViewModel
import com.yaycheckmate.viewmodel.DashboardViewModelFactory

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels {
        DashboardViewModelFactory(requireActivity().application as YayCheckmateApp)
    }

    private lateinit var objectAdapter: DashboardObjectAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeData()

        binding.fabRegister.setOnClickListener {
            startActivity(Intent(requireContext(), RegisterObjectActivity::class.java))
        }

        binding.btnRegisterFirst.setOnClickListener {
            startActivity(Intent(requireContext(), RegisterObjectActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        objectAdapter = DashboardObjectAdapter { objectItem ->
            val intent = Intent(requireContext(), SearchActivity::class.java)
            intent.putExtra(SearchActivity.EXTRA_OBJECT_ID, objectItem.id)
            startActivity(intent)
        }
        binding.rvObjects.apply {
            adapter = objectAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeData() {
        viewModel.allActiveObjects.observe(viewLifecycleOwner) { objects ->
            objectAdapter.submitList(objects)
            binding.tvObjectCount.text = objects.size.toString()
            if (objects.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.rvObjects.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.rvObjects.visibility = View.VISIBLE
            }
        }

        viewModel.userStats.observe(viewLifecycleOwner) { stats ->
            stats ?: return@observe
            binding.tvRank.text = stats.rank
            binding.tvLevel.text = "Lv. ${stats.level}"
            binding.tvXp.text = "${stats.totalXp} XP"
            binding.tvFound.text = stats.foundObjects.toString()
            binding.tvSuccessRate.text = "${stats.successRate.toInt()}%"
            binding.tvAvgTime.text = stats.averageSearchSeconds.toFormattedDuration()
            binding.tvMostLost.text = if (stats.mostLostItem.isEmpty()) "—" else stats.mostLostItem
            binding.tvMostLocation.text = if (stats.mostCommonLocation.isEmpty()) "—" else stats.mostCommonLocation
            binding.tvCoins.text = "${stats.coinsEarned} 💎"

            val currentLevelXp = GameConstants.getXpForLevel(stats.level)
            val nextLevelXp = GameConstants.getXpForNextLevel(stats.level)
            val progress = if (nextLevelXp > currentLevelXp) {
                ((stats.totalXp - currentLevelXp).toFloat() / (nextLevelXp - currentLevelXp) * 100).toInt()
            } else 100
            binding.progressXp.progress = progress.coerceIn(0, 100)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshStats()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
