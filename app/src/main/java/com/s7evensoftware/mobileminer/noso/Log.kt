package com.s7evensoftware.mobileminer.noso

import android.util.Log

class Log {
    companion object {
        fun e(origin:String, content:String){
            Log.e(origin, content)
            Nosocoreunit.appendLog(origin, content)
        }
    }
}