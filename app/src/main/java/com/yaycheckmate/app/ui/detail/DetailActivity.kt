package com.yaycheckmate.app.ui.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.yaycheckmate.app.R
import com.yaycheckmate.app.data.LostObjectEntity
import com.yaycheckmate.app.databinding.ActivityDetailBinding
import com.yaycheckmate.app.ui.detection.DetectionActivity
import kotlinx.coroutines.launch
import java.io.File

/**
 * View / edit / delete a stored object and jump into detection focused on this entry.
 */
class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private val viewModel: DetailViewModel by viewModels()
    private var objectId: Long = -1L
    private var current: LostObjectEntity? = null
    private var editing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        objectId = intent.getLongExtra(EXTRA_OBJECT_ID, -1L)
        if (objectId <= 0L) {
            finish()
            return
        }

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnEditToggle.setOnClickListener { toggleEdit() }
        binding.btnDelete.setOnClickListener { confirmDelete() }
        binding.btnDetect.setOnClickListener {
            startActivity(DetectionActivity.createIntent(this, objectId))
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.observe(objectId).collect { entity ->
                    if (entity == null) {
                        finish()
                        return@collect
                    }
                    current = entity
                    bind(entity)
                }
            }
        }
    }

    private fun bind(entity: LostObjectEntity) {
        binding.toolbar.title = entity.name
        val file = File(filesDir, entity.imageRelativePath)
        Glide.with(binding.image).load(file).centerCrop().into(binding.image)

        if (!editing) {
            binding.inputName.setText(entity.name)
            binding.inputDescription.setText(entity.description)
            binding.inputTags.setText(entity.tags)
        }

        binding.inputName.isEnabled = editing
        binding.inputDescription.isEnabled = editing
        binding.inputTags.isEnabled = editing

        binding.metaLocation.text = if (entity.latitude != null && entity.longitude != null) {
            "Last known: ${"%.5f".format(entity.latitude)}, ${"%.5f".format(entity.longitude)}"
        } else {
            "No GPS saved for this object"
        }

        binding.btnEditToggle.text = getString(if (editing) android.R.string.ok else R.string.edit)
    }

    private fun toggleEdit() {
        val entity = current ?: return
        if (!editing) {
            editing = true
            bind(entity)
            return
        }
        val updated = entity.copy(
            name = binding.inputName.text?.toString().orEmpty().trim(),
            description = binding.inputDescription.text?.toString().orEmpty().trim(),
            tags = binding.inputTags.text?.toString().orEmpty().trim(),
        )
        if (updated.name.isBlank()) {
            Toast.makeText(this, "Name required", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.update(updated) {
            editing = false
            Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDelete() {
        val entity = current ?: return
        AlertDialog.Builder(this)
            .setTitle(R.string.delete)
            .setMessage("Remove \"${entity.name}\" from the library?")
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.delete(entity) { finish() }
            }
            .show()
    }

    companion object {
        private const val EXTRA_OBJECT_ID = "detail_object_id"

        fun createIntent(context: Context, id: Long): Intent =
            Intent(context, DetailActivity::class.java).putExtra(EXTRA_OBJECT_ID, id)
    }
}
