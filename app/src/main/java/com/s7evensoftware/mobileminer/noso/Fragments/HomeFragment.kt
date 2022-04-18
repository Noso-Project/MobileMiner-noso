package com.s7evensoftware.mobileminer.noso.Fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.lifecycle.ViewModelProvider
import com.s7evensoftware.mobileminer.noso.*
import com.s7evensoftware.mobileminer.noso.databinding.AutotestRowSpeedBinding
import com.s7evensoftware.mobileminer.noso.databinding.FragmentHomeBinding

class HomeFragment : Fragment(), View.OnClickListener {

    companion object {
        fun newInstance() = HomeFragment()
    }

    private lateinit var binding:FragmentHomeBinding
    private lateinit var callback:HomeFragmentListener
    private lateinit var viewModel:MainViewModel

    override fun onAttach(context: Context) {
        callback = context as HomeFragmentListener
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        binding = FragmentHomeBinding.inflate(layoutInflater)
        PrepareView()
        RestoreMiner()
        return binding.root
    }

    fun hideAutoTestResult(){
        binding.homeFragmentAutotestResults.removeAllViews()
        binding.homeFragmentAutotestResults.visibility = View.GONE
    }

    private fun RestoreMiner() {
        if(viewModel.isMinerStarted){
            binding.homeFragmentTester.visibility = View.GONE
            binding.homeFragmentLauncher.visibility = View.GONE

            binding.minerContainer1.visibility = View.VISIBLE
            binding.minerContainer2.visibility = View.VISIBLE
            binding.minerContainer3.visibility = View.VISIBLE

            binding.minerFocusTrick.requestFocus()
        }
    }

    private fun PrepareView() {

        // Bind Buttons
        binding.homeFragmentAutotest.setOnClickListener(this)
        binding.homeFragmentTest.setOnClickListener(this)
        binding.homeFragmentStartMiner.setOnClickListener(this)
        binding.minerStop.setOnClickListener(this)

        // Observers
        viewModel.MinerSynced.observe(viewLifecycleOwner) { status ->
            when (status) {
                MINER_SYNC_PENDING -> {
                    binding.minerStatusDot.drawable.setTint(requireContext().getColor(R.color.colorRedUnsync))
                    binding.minerStatus.setText(R.string.miner_status_syncing)
                }
                MINER_SYNC_DONE -> {
                    binding.minerStatusDot.drawable.setTint(requireContext().getColor(R.color.colorYellow))
                    binding.minerStatus.setText(R.string.miner_status_sync_done)
                }
                MINER_BLOCK_CHANGE -> {
                    binding.minerStatusDot.drawable.setTint(requireContext().getColor(R.color.colorYellow))
                    binding.minerStatus.setText(R.string.miner_status_waiting_new)
                }
                MINER_MINNING -> {
                    binding.minerStatusDot.drawable.setTint(requireContext().getColor(R.color.colorGreenSync))
                    binding.minerStatus.setText(R.string.miner_status_mining)
                }
            }
        }

        viewModel.BlockAge.observe(viewLifecycleOwner) { age ->
            if (age <= 600) {
                binding.minerBlockAge.text = "$age seg"
            }
        }

        viewModel.MinerAddress.observe(viewLifecycleOwner) { address ->
            binding.minerAddress.text = address
        }

        viewModel.CPUtoUse.observe(viewLifecycleOwner) { cpus ->
            binding.minerCpuNumber.text = cpus.toString()
        }

        viewModel.LastBlock.observe(viewLifecycleOwner) { block ->
            binding.minerBlockNumber.text = block.toString()
        }

        viewModel.TargetHash.observe(viewLifecycleOwner) { target ->
            binding.minerTarget.text = target.substring(0, 10)
        }

        viewModel.TargetDiff.observe(viewLifecycleOwner) { target ->
            binding.minerBest.text = target.substring(0, 15)
        }

        viewModel.MinerRealTimeSpeed.observe(viewLifecycleOwner) { speed ->
            if (speed != 0 && viewModel.isMining) {
                binding.homeFragmentSpeedMeter.trembleDegree = 10F
                binding.homeFragmentSpeedColor.trembleDegree = 10F
                binding.homeFragmentSpeedMeter.speedTo(speed.toFloat(), 500)
                binding.homeFragmentSpeedColor.speedTo(speed.toFloat(), 500)
            } else {
                binding.homeFragmentSpeedMeter.trembleDegree = 0F
                binding.homeFragmentSpeedColor.trembleDegree = 0F
                binding.homeFragmentSpeedMeter.speedTo(0F, 100)
                binding.homeFragmentSpeedColor.speedTo(0F, 100)
            }
        }

        viewModel.SingleTestResult.observe(viewLifecycleOwner) { cpus ->
            if (cpus != -1 && cpus < viewModel.CPUcores) {
                binding.homeFragmentSpeedMeter.trembleDegree = 10F
                binding.homeFragmentSpeedColor.trembleDegree = 10F
                binding.homeFragmentSpeedMeter.speedTo(
                    viewModel.MinerTestResults!![cpus].toFloat() + 3000,
                    3000
                )
                binding.homeFragmentSpeedColor.speedTo(
                    viewModel.MinerTestResults!![cpus].toFloat() + 3000,
                    3000
                )

                if (viewModel.isAutoTest) {
                    val coreTest = layoutInflater.inflate(R.layout.autotest_row_speed, null, false)
                    val coreTestBinding = AutotestRowSpeedBinding.bind(coreTest)
                    coreTestBinding.autotestRowCpus.text = (cpus + 1).toString()
                    coreTestBinding.autotestRowSpeed.text =
                        viewModel.MinerTestResults!![cpus].toString() + " H/s"
                    binding.homeFragmentAutotestResults.visibility = View.VISIBLE
                    binding.homeFragmentAutotestResults.addView(coreTest)
                }
            }
        }

        viewModel.TestingStatus.observe(viewLifecycleOwner) { status ->
            if (status) {
                binding.homeFragmentCpuNumber.setOnClickListener(null)
                binding.homeFragmentAutotest.setOnClickListener(null)
                binding.homeFragmentTest.setOnClickListener(null)

                binding.homeFragmentSpeedMeter.trembleDegree = 10F
                binding.homeFragmentSpeedColor.trembleDegree = 10F
                binding.homeFragmentSpeedMeter.speedTo(
                    binding.homeFragmentCpuNumber.value * 3000,
                    3000
                )
                binding.homeFragmentSpeedColor.speedTo(
                    binding.homeFragmentCpuNumber.value * 3000,
                    3000
                )
            } else {
                viewModel.MinerTestResults?.let { results ->
                    binding.homeFragmentSpeedMeter.trembleDegree = 0F
                    binding.homeFragmentSpeedColor.trembleDegree = 0F
                    if (viewModel.SingleTestResult.value!! != -1) {
                        binding.homeFragmentSpeedMeter.speedTo(
                            results[viewModel.SingleTestResult.value!!].toFloat(),
                            500
                        )
                        binding.homeFragmentSpeedColor.speedTo(
                            results[viewModel.SingleTestResult.value!!].toFloat(),
                            500
                        )
                    }
                }

                binding.homeFragmentAutotest.setOnClickListener(this)
                binding.homeFragmentAutotest.visibility = View.VISIBLE

                binding.homeFragmentTest.setOnClickListener(this)
                binding.homeFragmentTest.visibility = View.VISIBLE

                binding.homeFragmentTestProgress.visibility = View.GONE
                binding.homeFragmentAutotestProgress.visibility = View.GONE
            }
        }
    }

    override fun onClick(v: View) {
        when(v.id){
            R.id.home_fragment_autotest -> {
                viewModel.isAutoTest = true
                binding.homeFragmentAutotestResults.removeAllViews()
                binding.homeFragmentAutotest.visibility = View.GONE
                binding.homeFragmentAutotestProgress.visibility = View.VISIBLE
                viewModel.TestingStatus.value = true
                callback.onAutoTesttrigger()
            }
            R.id.home_fragment_test -> {
                viewModel.isAutoTest = false
                binding.homeFragmentAutotestResults.removeAllViews()
                binding.homeFragmentAutotestResults.visibility = View.GONE

                binding.homeFragmentTest.visibility = View.GONE
                binding.homeFragmentTestProgress.visibility = View.VISIBLE
                viewModel.TestingStatus.value = true
                callback.onTesttrigger(binding.homeFragmentCpuNumber.value.toInt())
            }
            R.id.home_fragment_start_miner -> {
                viewModel.isMinerStarted = true

                binding.homeFragmentTester.visibility = View.GONE
                binding.homeFragmentLauncher.visibility = View.GONE

                binding.minerContainer1.visibility = View.VISIBLE
                binding.minerContainer2.visibility = View.VISIBLE
                binding.minerContainer3.visibility = View.VISIBLE

                binding.minerFocusTrick.requestFocus()

                callback.onMinerStart()
            }
            R.id.miner_stop -> {
                viewModel.isMinerStarted = false

                binding.minerContainer1.visibility = View.GONE
                binding.minerContainer2.visibility = View.GONE
                binding.minerContainer3.visibility = View.GONE

                binding.homeFragmentTester.visibility = View.VISIBLE
                binding.homeFragmentLauncher.visibility = View.VISIBLE

                callback.onMinerStop()
            }
        }
    }

    interface HomeFragmentListener {
        fun onAutoTesttrigger()
        fun onTesttrigger(maxCPU:Int)
        fun onMinerStart()
        fun onMinerStop()
    }


}