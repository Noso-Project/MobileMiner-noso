package com.s7evensoftware.mobileminer.noso

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.navigation.NavigationBarView
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.s7evensoftware.mobileminer.noso.Fragments.HomeFragment
import com.s7evensoftware.mobileminer.noso.Fragments.LogsFragment
import com.s7evensoftware.mobileminer.noso.Fragments.SettingsFragment
import com.s7evensoftware.mobileminer.noso.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import java.io.*
import kotlin.coroutines.CoroutineContext


class MainActivity :
    AppCompatActivity(),
    CoroutineScope,
    NavigationBarView.OnItemSelectedListener,
    HomeFragment.HomeFragmentListener,
    SettingsFragment.SettingsFragmentListener,
    View.OnClickListener
{
    lateinit var binding: ActivityMainBinding
    lateinit var viewModel:MainViewModel

    private var minerProcess:Process? = null
    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    //Fragments
    private val homeFragment by lazy {
        if(supportFragmentManager.findFragmentByTag(HOME_FRAGMENT) == null){
            HomeFragment.newInstance()
        }else{
            supportFragmentManager.findFragmentByTag(HOME_FRAGMENT) as HomeFragment
        }
    }
    private val logsFragment by lazy {
        if(supportFragmentManager.findFragmentByTag(LOGS_FRAGMENT) == null){
            LogsFragment.newInstance()
        }else{
            supportFragmentManager.findFragmentByTag(LOGS_FRAGMENT) as LogsFragment
        }
    }
    private val settingsFragment by lazy {
        if(supportFragmentManager.findFragmentByTag(SETTINGS_FRAGMENT) == null){
            SettingsFragment.newInstance()
        }else{
            supportFragmentManager.findFragmentByTag(SETTINGS_FRAGMENT) as SettingsFragment
        }
    }

    var pasteQRToMiningAddress = registerForActivityResult(ScanContract()){ result ->
        result.contents?.let { content ->
            if(Nosocoreunit.IsValidAddress(content)){
                viewModel.MinerAddress.value = content
            }else{
                Toast.makeText(this, "Invalid address",Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun RequestPermissions() {
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_NETWORK_STATE
            )
            != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.INTERNET
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.INTERNET
                ),
                1
            )
        }
    }

    private fun CreateDefaultSeedNodes() {
        DBManager.insertDefaultNodes()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set context for read/write tasks
        RequestPermissions()
        // Insert Seed Nodes if empty
        CreateDefaultSeedNodes()
        //Start view model for whole app
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        binding = ActivityMainBinding.inflate(layoutInflater)

        // Restore Settings
        RestoreSettings()
        // PrepareInterface
        PrepareViews()
        // Prepare LowLevel Process
        PrepareMiner()
        // Set GUI
        setContentView(binding.root)
    }

    private fun RestoreSettings(){
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        viewModel.MinerAddress.value = sharedPref.getString(SHAREDPREF_ADDRESS, DEFAULT_ADDRESS)
        viewModel.CPUtoUse.value = sharedPref.getInt(SHAREDPREF_CPUS, DEFAULT_CPUS)
        viewModel.MinerID = sharedPref.getInt(SHAREDPREF_MINERID, DEFAULT_MINER_ID)
        viewModel.isSoloMining.value = sharedPref.getBoolean(SHAREDPREF_MODE, DEFAULT_MINING_MODE)
        viewModel.poolString.value = sharedPref.getString(SHAREDPREF_POOL, DEFAULT_POOL_STRING)
    }

    private fun PrepareViews() {
        //Set button bindings
        binding.mainBottomMenu.setOnItemSelectedListener(this)

        //Observers
        viewModel.MinerSynced.observe(this) { syncStatus ->
            when (syncStatus) {
                MINER_SYNC_DONE -> {
                    Log.e("Main", "Miner Sync Completed")
                    viewModel.OutPutInfo += "\n#####################################\n"
                    viewModel.OutPutInfo += "# Miner Synchronized \n"
                    viewModel.RealTimeValue.value = System.currentTimeMillis()

                    if(viewModel.isSoloMining.value == true){
                        viewModel.concensusResult?.let { result ->
                            if (result.LastBlock > viewModel.LastBlock.value ?: 0) {
                                viewModel.TargetHash.postValue(result.LBHash)
                                viewModel.TargetDiff.postValue(result.NMSDiff)
                                viewModel.TargetDiffStatic = result.NMSDiff
                                viewModel.LastBlock.postValue(result.LastBlock)
                                Log.e(
                                    "Network",
                                    "New Values -> targetHash:${result.LBHash} targetDiff:${result.NMSDiff} LastBlock: ${result.LastBlock} "
                                )
                                //sendCommand("$UPDATE_BLOCK_COMMAND ${result.LBHash} ${result.NMSDiff}")
                            }

                            if (result.NMSDiff < viewModel.TargetDiff.value!!) {
                                viewModel.TargetDiff.postValue(result.NMSDiff)
                                viewModel.TargetDiffStatic = result.NMSDiff
                            }

                            viewModel.BlockAge.value =
                                System.currentTimeMillis() / 1000 - result.LBTimeEnd
                            startBlockCounter()
                            viewModel.OutPutInfo += "# Block: ${result.LastBlock} \n# Age: ${viewModel.BlockAge.value} secs \n# Hash: ${result.LBHash}\n"
                            viewModel.OutPutInfo += "#####################################\n"

                            if (viewModel.pendingMinerStart) {
                                viewModel.isMining = true
                                viewModel.pendingMinerStart = false
                                sendCommand("$MINE_COMMAND ${viewModel.getCPUtoUse()} ${viewModel.MinerID} ${viewModel.getMinerAddres()} ${result.LBHash} ${result.NMSDiff}")
                                speedReportTask()
                            }

                            if (viewModel.BlockAge.value!! >= BLOCK_DEFAULT_TIME) {
                                SyncMiner()
                            } else {
                                viewModel.MinerSynced.value = MINER_MINNING
                            }
                        }
                    }else{
                        var newBlock = false
                        if (viewModel.currentPool.CurrentBlock > viewModel.LastBlock.value ?: 0) {
                            newBlock = true
                            viewModel.TargetHash.postValue(viewModel.currentPool.TargetHash)
                            viewModel.TargetDiff.postValue(viewModel.currentPool.TargetDiff)
                            viewModel.LastBlock.postValue(viewModel.currentPool.CurrentBlock)

                            Log.e("Main","Sync Completed:")
                            Log.e("Main","TargetHash:${viewModel.currentPool.TargetHash}")
                            Log.e("Main","TargetDiff:${viewModel.currentPool.TargetDiff}")
                            Log.e("Main","LastBlock: ${viewModel.currentPool.CurrentBlock}")

                            //sendCommand("$UPDATE_BLOCK_COMMAND ${viewModel.currentPool.TargetHash} ${viewModel.currentPool.TargetDiff}")
                        }

                        if (viewModel.currentPool.TargetDiff < viewModel.TargetDiff.value!!) {
                            viewModel.TargetDiff.postValue(viewModel.currentPool.TargetDiff)
                            viewModel.TargetDiffStatic = viewModel.currentPool.TargetDiff
                        }

                        if(newBlock){
                            viewModel.BlockAge.value = (System.currentTimeMillis()%600000) / 1000
                            startBlockCounter()
                        }

                        viewModel.OutPutInfo += "# Block: ${viewModel.currentPool.CurrentBlock} \n# Age: ${viewModel.BlockAge.value} secs \n# Hash: ${viewModel.currentPool.TargetHash}\n" +
                                "# TDiff: ${viewModel.currentPool.TargetDiff}\n"
                        viewModel.OutPutInfo += "#####################################\n"

                        if (viewModel.pendingMinerStart) {
                            viewModel.isMining = true
                            viewModel.pendingMinerStart = false
                            sendCommand("$MINE_POOL_COMMAND ${viewModel.getCPUtoUse()} ${viewModel.currentPool.MinerID} ${viewModel.currentPool.NosoAddress} ${viewModel.currentPool.TargetHash} ${viewModel.currentPool.TargetDiff}")
                            speedReportTask()
                        }

                        if (viewModel.BlockAge.value!! >= BLOCK_DEFAULT_TIME) {
                            SyncMiner()
                        } else {
                            viewModel.MinerSynced.value = MINER_MINNING
                        }
                    }
                }
            }
        }

        viewModel.BlockAge.observe(this) { currentAge ->
            if (currentAge == (BLOCK_DEFAULT_TIME - 1).toLong()) {
                viewModel.MinerSynced.value = MINER_BLOCK_CHANGE
                minerSoftStop()
                SyncMiner()
            }
        }

        viewModel.SolutionsCatcher.observe(this) { solution ->
            if (solution != null) {
                PushSolution(solution)
            }
        }

        viewModel.TargetDiff.observe(this) { newTarget ->
            sendCommand("$UPDATE_TARGET_COMMAND $newTarget")
        }

        //Set primary fragment
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.main_fragment_container, homeFragment, HOME_FRAGMENT)
            .commit()

    }

    private fun speedReportTask() {
        if(viewModel.speedReportJob != null){
            viewModel.speedReportJob?.cancel()
        }
        viewModel.speedReportJob = launch {
            var firstRep = true
            while(viewModel.isMining){
                delay(if(firstRep){5000} else {30000})
                sendCommand(SPEEDREPORT_COMMAND)
                firstRep = false
            }
        }
    }

    private fun startBlockCounter() {
        if(viewModel.blockAgeTimerJob != null){
            viewModel.blockAgeTimerJob?.cancel()
        }
        viewModel.blockAgeTimerJob = launch {
            while(viewModel.BlockAge.value!! < BLOCK_DEFAULT_TIME){
                delay(1000)
                viewModel.BlockAge.postValue(viewModel.BlockAge.value!!.inc())
            }
            Log.e("Main","Stopping the Timer ${viewModel.BlockAge.value}")
        }
    }

    private fun PrepareMiner() {
        copyBinaries()
        StartMiner()
    }

    private fun copyBinaries(){
        val privatePath = filesDir.absolutePath
        var assetPath = "miner"
        Tools.copyDirectoryContents(this, assetPath, privatePath)
    }

    private fun StartMiner() {
        val privatePath = filesDir.absolutePath

        // Kill the Process if it's been started
        if(minerProcess != null){
            minerProcess?.destroy()
            minerProcess = null
        }

        // Get CPU Arch
        var abi = Build.SUPPORTED_ABIS[0]
        val abiParsing = abi.substringBefore("-")
        val cpuArch = abiParsing.substringBefore("v")
        viewModel.CPUcores = Runtime.getRuntime().availableProcessors()
        viewModel.MinerTestResults = Array(viewModel.CPUcores) { 0 }

        if(viewModel.isFirstRun){
            viewModel.OutPutInfo += "\nCPU Type: $cpuArch"
            viewModel.OutPutInfo += "\nAvailable Cores: ${viewModel.CPUcores}"

            Log.e("Main","CPU Type: $cpuArch")
            Log.e("Main","Available Cores: ${viewModel.CPUcores}")
            viewModel.isFirstRun = false
        }

        // Create Process Builder for miner depending on arch
        val args = arrayOf("./miner_$cpuArch")
        val minerProcessBuilder = ProcessBuilder(*args)
        minerProcessBuilder.directory(File(privatePath))
        minerProcessBuilder.redirectErrorStream()
        minerProcess = minerProcessBuilder.start() // Start Process
        viewModel.MinerProcessStatus = MINER_PROCESS_ON

        viewModel.OutPutInfo += "\nMiner Process Started - OK"
        Log.e("Main","Miner Process Started - OK")

        // Permanent Task to Read Process Output
        launch {
            try {
                minerProcess?.let { miner ->
                    val reader = BufferedReader(InputStreamReader(miner.inputStream))
                    var line: String
                    while (reader.readLine().also { line = it } != null) {
                        Log.e("Main", "$line")
                        parseLine(line)
                    }
                }
            } catch (i: InterruptedIOException){
                viewModel.OutPutInfo += "\n# Miner process reader got closed"
                Log.w("Main", "Error reading process input: $i")
            } catch (e: IOException) {
                viewModel.OutPutInfo += "\n# Error reading process input: $e"
                Log.w("Main", "Error reading process input: $e")
            }catch (n: NullPointerException){
                viewModel.OutPutInfo += "\nProcess was killed"
                Log.w("Main", "Process was killed")
                viewModel.MinerProcessStatus = MINER_PROCESS_KILLED
            }
        }
    }

    private fun SyncMiner() {
        viewModel.OutPutInfo += "\nSynchronizing Miner...\n"
        launch {
            if(viewModel.isSoloMining.value == true){
                while(!Nosocoreunit.CheckSource(viewModel)){
                    delay(1000)
                }
            }else{
                var poolinfo = viewModel.poolString.value?.split(":")
                var poolData = Network.getPoolData(poolinfo!![0], Integer.parseInt(poolinfo[1]), viewModel)
                while(!poolData.Connected){
                    delay(4000)
                    poolData = Network.getPoolData(poolinfo[0], Integer.parseInt(poolinfo[1]), viewModel)
                }
            }

            viewModel.MinerSynced.postValue(MINER_SYNC_DONE)
        }
    }

    private fun parseLine(line: String) {
        if(viewModel.TestingStatus.value!!){
            // Minning Case
            val params = line.split(" ")
            when(params[0]){
                COMMENT_PARAM -> {
                    viewModel.OutPutInfo += "\n$line"
                }
                CPU_PARAM -> {
                    val index = params[1].toInt()-1
                    val speed = params[3].toInt()
                    viewModel.MinerTestResults!![index] = speed
                    viewModel.SingleTestResult.postValue(index)
                    viewModel.OutPutInfo += "\n$line"
                }
                HASH_TEST_START, MINER_TEST_START -> {
                    viewModel.OutPutInfo += "\n\n$line"
                }
                HASH_TEST_STOP,MINER_TEST_STOP -> {
                    viewModel.TestingStatus.postValue(false)
                    viewModel.OutPutInfo += "\n$line"
                }
            }
            viewModel.TriggerOutputUpdate.postValue(viewModel.OutPutInfo.length)
        }else{
            val params = line.split(" ")
            when(params[0]){
                COMMENT_PARAM -> {
                    viewModel.OutPutInfo += "\n$line"
                    viewModel.TriggerOutputUpdate.postValue(viewModel.OutPutInfo.length)
                }
                HASH_TEST_START, MINER_TEST_START -> {
                    viewModel.TestingStatus.postValue(true)
                    viewModel.OutPutInfo += "\n$line"
                    viewModel.TriggerOutputUpdate.postValue(viewModel.OutPutInfo.length)
                }
                SOLUTION_PARAM -> {
                    val ns = Solution()
                    ns.Target = params[3]
                    ns.Hash = params[5]
                    ns.Diff = params[7]
                    viewModel.SolutionsCatcher.postValue(ns)
                }
                SPEEDREPORT_COMMAND -> {
                    viewModel.MinerRealTimeSpeed.postValue(params[1].toInt())
                }
            }

        }
    }

    fun PushSolution(solution: Solution){
        if(viewModel.isSoloMining.value == true){
            launch {
                Network.sendSolution(solution, viewModel, DEFAULT_RETRY)
            }
        }else{
            launch {
                Network.sendPoolShare(solution, viewModel, DEFAULT_RETRY)
            }

        }
    }

    fun sendCommand(command:String){
        try {
            val writer = BufferedWriter(OutputStreamWriter(minerProcess?.outputStream))
            writer.write("$command\n")
            writer.flush()
        } catch (e: Exception) {
            Log.w("Main", "exception", e)
        }
    }



    override fun onAutoTesttrigger() {
        sendCommand("$TEST_MINER_COMMAND ${viewModel.CPUcores}")
    }

    override fun onTesttrigger(maxCPU:Int) {
        sendCommand("$TEST_HASH_COMMAND $maxCPU")
    }

    override fun onMinerStart() {
        Log.e("Main","Start Miner Called")
        SyncMiner()
        viewModel.pendingMinerStart = true
    }

    override fun onMinerStop() {
        viewModel.isMining = false
        viewModel.LastBlock.value = 0
        viewModel.MinerRealTimeSpeed.value = 0
        viewModel.OutPutInfo += "\nStopping Miner and Miner process"
        coroutineContext.cancelChildren()
        StartMiner()
    }

    fun minerSoftStop() {
        viewModel.isMining = false
        viewModel.OutPutInfo += "\nStopping Miner until next block..."
        coroutineContext.cancelChildren()
        viewModel.pendingMinerStart = true
        StartMiner()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.bottom_menu_home -> {
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.main_fragment_container, homeFragment, HOME_FRAGMENT)
                    .commit()
                return true
            }
            R.id.bottom_menu_logs -> {
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.main_fragment_container, logsFragment, LOGS_FRAGMENT)
                    .commit()
                return true
            }
            R.id.bottom_menu_settings -> {
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.main_fragment_container, settingsFragment, SETTINGS_FRAGMENT)
                    .commit()
                return true
            }
        }
        return false
    }

    override fun onClick(v: View) {
        when(v.id){

        }
    }

    override fun onQRScann() {
        val options = ScanOptions()
        options.setPrompt(getString(R.string.import_wallet_from_qr_prompt))
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setOrientationLocked(true)
        pasteQRToMiningAddress.launch(options)
    }

    override fun onSaveSettings() {
        val sharedPref = getPreferences(Context.MODE_PRIVATE) ?: return
        with (sharedPref.edit()){
            putString(SHAREDPREF_ADDRESS, viewModel.getMinerAddres())
            putInt(SHAREDPREF_MINERID, viewModel.MinerID)
            putInt(SHAREDPREF_CPUS, viewModel.getCPUtoUse())
            putBoolean(SHAREDPREF_MODE, viewModel.isSoloMining.value == true)
            putString(SHAREDPREF_POOL, viewModel.poolString.value)
            apply()
        }
        Toast.makeText(this, R.string.settings_saved_prompt, Toast.LENGTH_SHORT).show()
    }

}