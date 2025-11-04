package com.example.rsrpanalyzer.view.signal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.rsrpanalyzer.R
import com.example.rsrpanalyzer.databinding.FragmentTableViewBinding
import com.example.rsrpanalyzer.viewmodel.SignalViewModel

class TableViewFragment : Fragment(R.layout.fragment_table_view) {

    private var _binding: FragmentTableViewBinding? = null
    private val binding get() = _binding!!

    private val signalViewModel: SignalViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTableViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun observeViewModel() {
        signalViewModel.location.observe(viewLifecycleOwner) { location ->
            binding.tvLatitude.text = getString(R.string.latitude_value, location.latitude)
            binding.tvLongitude.text = getString(R.string.longitude_value, location.longitude)
        }

        signalViewModel.rsrp.observe(viewLifecycleOwner) { rsrp ->
            binding.tvRsrpTable.text = getString(R.string.rsrp_value_simple, rsrp)
        }

        signalViewModel.rsrq.observe(viewLifecycleOwner) { rsrq ->
            binding.tvRsrqTable.text = getString(R.string.rsrq_value_simple, rsrq)
        }
    }
}
