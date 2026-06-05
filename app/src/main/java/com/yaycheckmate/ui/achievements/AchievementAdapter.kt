package com.yaycheckmate.ui.achievements

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yaycheckmate.data.entity.ObjectItem
import com.yaycheckmate.databinding.ItemAchievementBinding
import com.yaycheckmate.utils.GameConstants

data class AchievementItem(
    val achievement: GameConstants.Achievement,
    val isUnlocked: Boolean
)

class AchievementAdapter : ListAdapter<AchievementItem, AchievementAdapter.ViewHolder>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<AchievementItem>() {
            override fun areItemsTheSame(o: AchievementItem, n: AchievementItem) = o.achievement.id == n.achievement.id
            override fun areContentsTheSame(o: AchievementItem, n: AchievementItem) = o == n
        }
    }

    inner class ViewHolder(private val binding: ItemAchievementBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AchievementItem) {
            binding.tvIcon.text = item.achievement.icon
            binding.tvTitle.text = item.achievement.title
            binding.tvDesc.text = item.achievement.description
            binding.tvXpReward.text = "+${item.achievement.xpReward} XP"
            if (item.isUnlocked) {
                binding.root.alpha = 1f
                binding.tvLocked.visibility = android.view.View.GONE
                binding.tvUnlocked.visibility = android.view.View.VISIBLE
            } else {
                binding.root.alpha = 0.45f
                binding.tvLocked.visibility = android.view.View.VISIBLE
                binding.tvUnlocked.visibility = android.view.View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAchievementBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))
}
