package com.yaycheckmate.app.ui.library

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.yaycheckmate.app.R
import com.yaycheckmate.app.data.LostObjectEntity
import com.yaycheckmate.app.databinding.ActivityLibraryBinding
import com.yaycheckmate.app.ui.add.AddObjectActivity
import com.yaycheckmate.app.ui.detail.DetailActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * RecyclerView library with grid/list toggle backed by Room Flow + MVVM.
 */
class LibraryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLibraryBinding
    private val viewModel: LibraryViewModel by viewModels()
    private var adapter: ObjectListAdapter? = null
    private var latestItems: List<LostObjectEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLibraryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_toggle_view) {
                viewModel.toggleGrid()
                item.title = getString(
                    if (viewModel.isGrid.value) R.string.toggle_list else R.string.toggle_grid,
                )
                true
            } else false
        }

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, AddObjectActivity::class.java))
        }
        binding.toolbar.menu.findItem(R.id.action_toggle_view)?.title = getString(R.string.toggle_list)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isGrid.collectLatest { grid ->
                        binding.recycler.layoutManager = if (grid) {
                            GridLayoutManager(this@LibraryActivity, 2)
                        } else {
                            LinearLayoutManager(this@LibraryActivity)
                        }
                        rebuildAdapter(grid)
                        adapter?.submitList(latestItems)
                    }
                }
                launch {
                    viewModel.objectsFlow.collectLatest { list ->
                        latestItems = list
                        adapter?.submitList(list)
                    }
                }
            }
        }
    }

    private fun rebuildAdapter(grid: Boolean) {
        adapter = ObjectListAdapter(
            isGrid = grid,
            filesDir = filesDir,
        ) { entity ->
            startActivity(DetailActivity.createIntent(this, entity.id))
        }
        binding.recycler.adapter = adapter
    }
}
