package com.yaycheckmate.ui.achievements

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.yaycheckmate.YayCheckmateApp
import com.yaycheckmate.databinding.FragmentAchievementsBinding
import com.yaycheckmate.utils.GameConstants
import com.yaycheckmate.viewmodel.DashboardViewModel
import com.yaycheckmate.viewmodel.DashboardViewModelFactory
import kotlinx.coroutines.launch

class AchievementsFragment : Fragment() {

    private var _binding: FragmentAchievementsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels {
        DashboardViewModelFactory(requireActivity().application as YayCheckmateApp)
    }

    private lateinit var achievementAdapter: AchievementAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAchievementsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as YayCheckmateApp
        achievementAdapter = AchievementAdapter()
        binding.rvAchievements.apply {
            adapter = achievementAdapter
            layoutManager = GridLayoutManager(requireContext(), 2)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val unlocked = app.userStatsRepository.getUnlockedAchievements()
            val items = GameConstants.ACHIEVEMENTS.map { achievement ->
                AchievementItem(achievement, unlocked.contains(achievement.id))
            }
            achievementAdapter.submitList(items)
            val count = unlocked.size
            val total = GameConstants.ACHIEVEMENTS.size
            binding.tvProgress.text = "$count / $total Achievements Unlocked"
            binding.progressAchievements.progress = if (total > 0) (count * 100 / total) else 0
        }

        viewModel.userStats.observe(viewLifecycleOwner) { stats ->
            stats ?: return@observe
            binding.tvRankName.text = stats.rank
            binding.tvLevel.text = "Level ${stats.level}"
            binding.tvTotalXp.text = "${stats.totalXp} XP Total"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
