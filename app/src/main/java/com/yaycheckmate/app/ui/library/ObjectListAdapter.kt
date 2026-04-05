package com.yaycheckmate.app.ui.library

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.yaycheckmate.app.data.LostObjectEntity
import com.yaycheckmate.app.databinding.ItemObjectGridBinding
import com.yaycheckmate.app.databinding.ItemObjectListBinding
import java.io.File
import java.text.DateFormat
import java.util.Date

private object ObjectDiff : DiffUtil.ItemCallback<LostObjectEntity>() {
    override fun areItemsTheSame(a: LostObjectEntity, b: LostObjectEntity) = a.id == b.id
    override fun areContentsTheSame(a: LostObjectEntity, b: LostObjectEntity) = a == b
}

class ObjectListAdapter(
    private val isGrid: Boolean,
    private val filesDir: File,
    private val onClick: (LostObjectEntity) -> Unit,
) : ListAdapter<LostObjectEntity, RecyclerView.ViewHolder>(ObjectDiff) {

    override fun getItemViewType(position: Int): Int = if (isGrid) VIEW_GRID else VIEW_LIST

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_GRID) {
            val binding = ItemObjectGridBinding.inflate(inflater, parent, false)
            GridVH(binding)
        } else {
            val binding = ItemObjectListBinding.inflate(inflater, parent, false)
            ListVH(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is GridVH -> holder.bind(item)
            is ListVH -> holder.bind(item)
        }
    }

    private inner class GridVH(private val binding: ItemObjectGridBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: LostObjectEntity) {
            binding.title.text = item.name
            binding.date.text = DateFormat.getDateInstance().format(Date(item.createdAtMillis))
            val file = File(filesDir, item.imageRelativePath)
            Glide.with(binding.thumbnail).load(file).centerCrop().into(binding.thumbnail)
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    private inner class ListVH(private val binding: ItemObjectListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: LostObjectEntity) {
            binding.title.text = item.name
            binding.date.text = DateFormat.getDateInstance().format(Date(item.createdAtMillis))
            val file = File(filesDir, item.imageRelativePath)
            Glide.with(binding.thumbnail).load(file).centerCrop().into(binding.thumbnail)
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    companion object {
        private const val VIEW_GRID = 1
        private const val VIEW_LIST = 2
    }
}
