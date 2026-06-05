package com.yaycheckmate.ui.dashboard

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yaycheckmate.data.entity.ObjectItem
import com.yaycheckmate.databinding.ItemObjectCardBinding
import com.yaycheckmate.utils.GameConstants

class DashboardObjectAdapter(
    private val onItemClick: (ObjectItem) -> Unit
) : ListAdapter<ObjectItem, DashboardObjectAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ObjectItem>() {
            override fun areItemsTheSame(oldItem: ObjectItem, newItem: ObjectItem) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: ObjectItem, newItem: ObjectItem) = oldItem == newItem
        }
    }

    inner class ViewHolder(private val binding: ItemObjectCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ObjectItem) {
            binding.tvObjectName.text = item.name
            binding.tvCategory.text = item.category
            binding.tvDifficulty.text = item.difficulty
            binding.tvDifficulty.setTextColor(GameConstants.getDifficultyColor(item.difficulty))
            val successRate = if (item.totalSearches > 0) {
                (item.totalFinds.toFloat() / item.totalSearches * 100).toInt()
            } else 0
            binding.tvStats.text = "Found ${item.totalFinds}x • ${successRate}% success"
            binding.tvLastLocation.text = if (item.lastFoundLocation.isEmpty()) "Not found yet"
                else "Last seen: ${item.lastFoundLocation}"
            binding.root.setOnClickListener { onItemClick(item) }
            // Color by difficulty
            val diffColor = GameConstants.getDifficultyColor(item.difficulty)
            binding.viewDifficultyBar.setBackgroundColor(diffColor)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemObjectCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))
}
