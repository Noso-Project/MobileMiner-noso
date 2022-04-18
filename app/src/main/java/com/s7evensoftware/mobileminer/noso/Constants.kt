package com.s7evensoftware.mobileminer.noso

const val SOURCE = "mainnet"
const val DEFAULT_ADDRESS = "NHsx5MnV7UrNubBshnhpSnBuwKaqGw"
const val DEFAULT_CPUS = 1
const val DEFAULT_MINING_MODE = true // true = solo mining // false = pool mining
const val NOSPath             = "NOSODATA" // directory
const val LogsDirectory       = "LOGS" // directory
const val LogsFilename        = "error_log.txt" // directory

const val NODE_TIMEOUT        = 1500
const val DELTA_TRIGGER       = true // ff false then nodes with Delta > 0 will be excluded from node selection
const val DEFAULT_SYNC_DELAY  = 10000L
const val DEFAULT_MINER_ID    = 0
const val DEFAULT_POOL_STRING = "45.146.252.103:8082"

const val CoinChar = "N"                   // Char for addresses
const val B58Alphabet = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
const val MaxDiff             = "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF";
const val HasheableChars      = "!\"#\$%&\')*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~ "

// Fragment Constants
const val HOME_FRAGMENT = "homeFragment"
const val LOGS_FRAGMENT = "logsFragment"
const val SETTINGS_FRAGMENT = "settingsFragment"
const val DEFAULT_RETRY = 5
const val BLOCK_DEFAULT_TIME = 600

// Miner Constants
const val MINER_SYNC_PENDING = -1
const val MINER_SYNC_DONE = 1
const val MINER_BLOCK_CHANGE = 2
const val MINER_MINNING = 3

const val MINER_PROCESS_ON = 1
const val MINER_PROCESS_OFF = 2
const val MINER_PROCESS_KILLED = 3

// Miner Commands
const val PROCESS_START = "MINERBINSTART"
const val PROCESS_END = "MINERBINSTOP"

const val HASH_TEST_START = "HASHTEST_START"
const val HASH_TEST_STOP = "HASHTEST_STOP"

const val MINER_TEST_START = "MINERTEST_START"
const val MINER_TEST_STOP = "MINERTEST_STOP"

const val EXIT_COMMAND = "EXIT"
const val MINE_COMMAND = "MINE"
const val MINE_POOL_COMMAND = "MINEPOOL"
const val UPDATE_BLOCK_COMMAND = "CHANGEBLOCK"
const val UPDATE_TARGET_COMMAND = "CHANGETARGET"
const val SPEEDREPORT_COMMAND = "SPEEDREPORT"
const val TEST_NETWORK_COMMAND = "TESTNET"
const val TEST_HASH_COMMAND = "TESTHASH"
const val TEST_MINER_COMMAND = "TESTMINER"

const val CPU_PARAM = "CPUS"
const val COMMENT_PARAM = "#"
const val SOLUTION_PARAM = "SOLUTION"

const val SHAREDPREF_ADDRESS = "MINING_ADDRESS"
const val SHAREDPREF_CPUS = "MINING_CPUS"
const val SHAREDPREF_MINERID = "MINING_ID"
const val SHAREDPREF_MODE = "MINING_MODE"
const val SHAREDPREF_POOL = "POOL_STRING"
