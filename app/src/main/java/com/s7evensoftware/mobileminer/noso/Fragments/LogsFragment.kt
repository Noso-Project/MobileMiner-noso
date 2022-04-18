package com.s7evensoftware.mobileminer.noso.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.s7evensoftware.mobileminer.noso.MainViewModel
import com.s7evensoftware.mobileminer.noso.databinding.FragmentLogsBinding

class LogsFragment : Fragment() {

    companion object {
        fun newInstance() = LogsFragment()
    }

    private lateinit var binding: FragmentLogsBinding
    private lateinit var viewModel: MainViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        viewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        binding = FragmentLogsBinding.inflate(layoutInflater)
        PrepareView()
        return binding.root
    }

    private fun PrepareView() {

        binding.logsFragmentContainer.clipToOutline = true

        viewModel.TriggerOutputUpdate.observe(viewLifecycleOwner) {
            binding.logsFragmentOutput.text = viewModel.OutPutInfo
            binding.logsFragmentScrollview.post {
                binding.logsFragmentScrollview.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
    }
}