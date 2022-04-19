package com.s7evensoftware.mobileminer.noso

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Job

class MainViewModel: ViewModel() {
    // App version
    var appVersion = "v1.0.0"

    // Pool Variables
    var isFirstRun = true
    var currentPool = MutableLiveData(PoolData())
    var currentPoolStatic = PoolData()
    var poolString = MutableLiveData(DEFAULT_POOL_STRING)
    var lastPoolPayment = PoolPayData()
    var acceptedShares = MutableLiveData(0L)

    var LastNodeSelected:NodeInfo? = null

    // General and Settings Values
    var isSupported = false
    var MinerAddress = MutableLiveData(DEFAULT_ADDRESS)
    var CPUtoUse = MutableLiveData(1)
    var isSoloMining = MutableLiveData(true)

    var MinerSynced = MutableLiveData(MINER_SYNC_PENDING)
    var concensusResult:ConcensusResult? = null
    var ConnectionError = MutableLiveData(false)

    // Block Age Real Time Counter
    var RealTimeValue = MutableLiveData(System.currentTimeMillis())

    // CPU/Mining Testings Vars
    var CPUcores = 1
    var isAutoTest = false
    var TestingStatus = MutableLiveData(false)
    var MinerRealTimeSpeed = MutableLiveData(0)
    var SingleTestResult = MutableLiveData(-1)
    var MinerTestResults:Array<Int>? = null

    // Miner Variables
    var pendingMinerStart = false
    var isMinerStarted = false
    var isMining = false
    var blockAgeTimerJob: Job? = null
    var speedReportJob: Job? = null
    var LastBlock = MutableLiveData(0L)
    var BlockAge = MutableLiveData(0L)
    var TargetHash = MutableLiveData("00000000000000000000000000000000")
    var TargetDiffStatic = MaxDiff
    var TargetDiff = MutableLiveData(MaxDiff)

    var MinerProcessStatus = -1
    var SolutionsCatcher = MutableLiveData<Solution?>()

    var TriggerOutputUpdate = MutableLiveData(0)
    var OutPutInfo:String = ""

    var Source = SOURCE.uppercase()
    var MinerID = DEFAULT_MINER_ID

    fun getMinerAddres():String {
        MinerAddress.value?.let {
            return it
        }
        return DEFAULT_ADDRESS
    }

    fun getCPUtoUse():Int {
        CPUtoUse.value?.let {
            return it
        }
        return 1
    }

    /**
     * @return returns true if is solo mining and false if is pool mining
     */
    fun getMiningMode():Boolean {
        return isSoloMining.value == true
    }

}