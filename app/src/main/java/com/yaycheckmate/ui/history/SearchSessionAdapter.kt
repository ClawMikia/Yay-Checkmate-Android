package com.yaycheckmate.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yaycheckmate.R
import com.yaycheckmate.data.entity.SearchSession
import com.yaycheckmate.databinding.ItemSessionBinding
import com.yaycheckmate.utils.toFormattedDate
import com.yaycheckmate.utils.toFormattedDuration

class SearchSessionAdapter : ListAdapter<SearchSession, SearchSessionAdapter.ViewHolder>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<SearchSession>() {
            override fun areItemsTheSame(o: SearchSession, n: SearchSession) = o.id == n.id
            override fun areContentsTheSame(o: SearchSession, n: SearchSession) = o == n
        }
    }

    inner class ViewHolder(private val binding: ItemSessionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(session: SearchSession) {
            binding.tvObjectName.text = session.objectName
            binding.tvDate.text = session.startTime.toFormattedDate()
            binding.tvDuration.text = session.durationSeconds.toFormattedDuration()
            binding.tvResult.text = session.result
            binding.tvLocation.text = if (session.foundLocation.isEmpty()) "—" else session.foundLocation
            binding.tvConfidence.text = if (session.confidenceScore > 0) "${session.confidenceScore}%" else "—"

            val resultColor = when (session.result) {
                "Found" -> ContextCompat.getColor(binding.root.context, R.color.colorSuccess)
                "Not Found" -> ContextCompat.getColor(binding.root.context, R.color.colorError)
                else -> ContextCompat.getColor(binding.root.context, R.color.colorTextSecondary)
            }
            binding.tvResult.setTextColor(resultColor)
            val resultEmoji = when (session.result) {
                "Found" -> "✅"
                "Not Found" -> "❌"
                else -> "⏹️"
            }
            binding.tvResultEmoji.text = resultEmoji
            if (session.xpEarned > 0) {
                binding.tvXp.text = "+${session.xpEarned} XP"
            } else {
                binding.tvXp.text = ""
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSessionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))
}
