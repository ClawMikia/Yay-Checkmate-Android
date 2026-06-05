package com.yaycheckmate.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.yaycheckmate.YayCheckmateApp
import com.yaycheckmate.databinding.FragmentHistoryBinding
import com.yaycheckmate.viewmodel.HistoryViewModel
import com.yaycheckmate.viewmodel.HistoryViewModelFactory

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryViewModel by viewModels {
        HistoryViewModelFactory(requireActivity().application as YayCheckmateApp)
    }

    private lateinit var sessionAdapter: SearchSessionAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionAdapter = SearchSessionAdapter()
        binding.rvHistory.apply {
            adapter = sessionAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        viewModel.recentSessions.observe(viewLifecycleOwner) { sessions ->
            sessionAdapter.submitList(sessions)
            if (sessions.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.rvHistory.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.rvHistory.visibility = View.VISIBLE
            }
            val found = sessions.count { it.result == "Found" }
            val total = sessions.size
            binding.tvSummary.text = "$found found out of $total searches"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
