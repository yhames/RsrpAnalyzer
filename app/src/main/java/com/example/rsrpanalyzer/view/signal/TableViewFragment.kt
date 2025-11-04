package com.example.rsrpanalyzer.view.signal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.rsrpanalyzer.R
import com.example.rsrpanalyzer.databinding.FragmentTableViewBinding

class TableViewFragment : Fragment(R.layout.fragment_table_view) {

    private var _binding: FragmentTableViewBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTableViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TODO: 이곳에서 RecyclerView 어댑터 설정 및 ViewModel 옵저빙 등
        //       뷰와 관련된 초기화 로직을 구현합니다.
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
