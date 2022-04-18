package com.s7evensoftware.mobileminer.noso.Fragments

import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.s7evensoftware.mobileminer.noso.MainViewModel
import com.s7evensoftware.mobileminer.noso.Nosocoreunit
import com.s7evensoftware.mobileminer.noso.R
import com.s7evensoftware.mobileminer.noso.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment(), View.OnClickListener {

    private lateinit var binding:FragmentSettingsBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var callback:SettingsFragmentListener
    private var localMode = false

    companion object {
        fun newInstance() = SettingsFragment()
    }

    override fun onAttach(context: Context) {
        callback = context as SettingsFragmentListener
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        viewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        binding = FragmentSettingsBinding.inflate(layoutInflater)
        PrepareView()

        return binding.root
    }

    private fun PrepareView() {
        binding.settingsFragmentSave.setOnClickListener(this)
        binding.settingsFragmentAddressQr.setOnClickListener(this)
        binding.settingsFragmentAddressPaste.setOnClickListener(this)
        binding.settingsFragmentCpuSlider.valueTo = viewModel.CPUcores.toFloat()
        binding.settingsFragmentMinerID.value = viewModel.MinerID
        binding.settingsFragmentMode.setOnClickListener(this)

        binding.settingsFragmentCpuSlider.addOnChangeListener { _, value, _ ->
            binding.settingsFragmentCpuNumber.text = value.toInt().toString()
        }

        viewModel.CPUtoUse.observe(viewLifecycleOwner) { number ->
            binding.settingsFragmentCpuSlider.value = number.toFloat()
        }

        viewModel.MinerAddress.observe(viewLifecycleOwner) { address ->
            binding.settingsFragmentAddress.setText(address)
        }

        viewModel.poolString.observe(viewLifecycleOwner) { poolString ->
            binding.settingsFragmentPoolSource.setText(poolString)
        }

        viewModel.isSoloMining.observe(viewLifecycleOwner) { solo ->
            if(solo){
                localMode = solo
                binding.settingsFragmentMode.isChecked = !solo
                binding.settingsFragmentSourceContainer.visibility = View.GONE
                binding.settingsFragmentMineridContainer.visibility = View.VISIBLE
            }else{
                localMode = solo
                binding.settingsFragmentMode.isChecked = !solo
                binding.settingsFragmentSourceContainer.visibility = View.VISIBLE
                binding.settingsFragmentMineridContainer.visibility = View.GONE
            }
        }




    }

    interface SettingsFragmentListener {
        fun onQRScann()
        fun onSaveSettings()
    }

    override fun onClick(v: View) {
        when(v.id){
            R.id.settings_fragment_address_qr -> {
                callback.onQRScann()
            }
            R.id.settings_fragment_mode -> {
                localMode = !binding.settingsFragmentMode.isChecked
                if(localMode){
                    binding.settingsFragmentSourceContainer.visibility = View.GONE
                    binding.settingsFragmentMineridContainer.visibility = View.VISIBLE
                }else{
                    binding.settingsFragmentMode.isChecked = !localMode
                    binding.settingsFragmentSourceContainer.visibility = View.VISIBLE
                    binding.settingsFragmentMineridContainer.visibility = View.GONE
                }
                //viewModel.isSoloMining.value = !binding.settingsFragmentMode.isChecked
            }
            R.id.settings_fragment_address_paste -> {
                val clipboard = requireActivity().getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                if(clipboard.hasPrimaryClip()){
                    if(clipboard.primaryClipDescription?.getMimeType(0) == "text/plain"){
                        val clip = clipboard.primaryClip?.getItemAt(0)
                        binding.settingsFragmentAddress.setText(clip?.text.toString())
                        binding.settingsFragmentAddress.setSelection(binding.settingsFragmentAddress.text.length)
                    }
                }
            }
            R.id.settings_fragment_save -> {
                viewModel.MinerAddress.value = binding.settingsFragmentAddress.text.toString()
                viewModel.CPUtoUse.value = binding.settingsFragmentCpuSlider.value.toInt()
                viewModel.MinerID = binding.settingsFragmentMinerID.value
                viewModel.isSoloMining.value = !binding.settingsFragmentMode.isChecked

                if(viewModel.getMinerAddres().isNotEmpty() && viewModel.getMinerAddres().isNotBlank() && Nosocoreunit.IsValidAddress(viewModel.getMinerAddres())){
                    callback.onSaveSettings()
                }else{
                    Snackbar.make(v, R.string.invalid_address, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }


}