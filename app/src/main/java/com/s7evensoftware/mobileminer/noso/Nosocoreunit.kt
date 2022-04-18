package com.s7evensoftware.mobileminer.noso

import android.content.Context
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.math.BigInteger
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.absoluteValue

class Nosocoreunit {

    companion object {

        lateinit var context:Context

        @JvmName("setContext1") // fix for declaration name
        fun setContext(context: Context){
            this.context = context
        }

        fun appendLog(origen:String, content:String){
            val fileLog = File(
                context.getExternalFilesDir(null)!!.path
                        +File.separator
                        +NOSPath
                        +File.separator
                        +LogsDirectory
                        +File.separator
                        +LogsFilename)

            try{
                if(!fileLog.exists()){
                    fileLog.parentFile.mkdirs()
                    fileLog.createNewFile()
                }else{
                    if(fileLog.length() >= 51200){
                        if(fileLog.delete()){
                            Log.e("mpDisk","Log file cleared - OK")
                            fileLog.createNewFile()
                        }else{
                            Log.e("mpDisk","Error clearing Log file - ERR")
                        }

                    }
                }

                val buffWrt = BufferedWriter(FileWriter(fileLog, true))
                val currentTime = System.currentTimeMillis()
                val formattedLine = getDateFromUNIX(currentTime)+" "+
                        getTimeFromUNIX(currentTime)+" : "+
                        origen+" -> "+
                        content
                buffWrt.append(formattedLine)
                buffWrt.newLine()
                buffWrt.close()
            }catch (i: IOException){
                Log.e("mpDisk","Error writing log file: "+i.message)
            }
        }

        fun getDateFromUNIX(time:Long):String{
            try {
                val formatter = SimpleDateFormat("dd/MM/yyyy")
                formatter.timeZone = TimeZone.getTimeZone("UTC")
                return formatter.format(time)
            }catch (e:Exception){
                Log.e("mpFunctions","Error parsing date")
            }
            return "00/00/0000"
        }

        fun getTimeFromUNIX(time:Long):String{
            try {
                val formatter = SimpleDateFormat("HH:mm:ss")
                formatter.timeZone = TimeZone.getTimeZone("UTC")
                return formatter.format(time)
            }catch (e:Exception){
                Log.e("mpFunctions","Error parsing date")
            }
            return "00:00:00"
        }

        fun Long2Currency(balance:Long):String{
            var balanceStr = (balance.absoluteValue).toString()
            while(balanceStr.length < 9){
                balanceStr = "0$balanceStr"
            }
            val result = balanceStr.substring(0, balanceStr.length-8) + "." + balanceStr.substring(balanceStr.length-8)
            return if(balance >= 0) result else "-$result"
        }

        fun CheckSource(viewModel: MainViewModel):Boolean {
            var reachedNodes:ArrayList<NodeInfo>
            if(SOURCE.uppercase() == "MAINNET"){
                reachedNodes = Network.SyncNodes()
                if(reachedNodes.size >= (DBManager.getServers().size/2+1)){
                    viewModel.concensusResult = Network.Concensus(reachedNodes, viewModel)
                    return true
                }else{
                    Log.e("CoreUnit","Synced Failed due to number of nodes reached")
                }
            }
            return false
        }

        fun IsValidAddress(address:String):Boolean {
            if(address[0].equals('N') && address.length > 20){
                var OrigHash = address.substring(1, address.length-2)
                if(IsValid58(OrigHash)){
                    var Clave = BMDecto58(BMB58resumen(OrigHash).toString())
                    OrigHash = CoinChar+OrigHash+Clave
                    if(OrigHash == address){
                        return true
                    }
                }
            }
            return false
        }

        fun IsValid58(base58Text:String):Boolean {
            for(c in base58Text){
                if(B58Alphabet.indexOf(c) == -1){
                    return false
                }
            }
            return true
        }

        fun BMB58resumen(numero58:String):Int {
            var total = 0
            for(i in 0..(numero58.length-1)){
                total = total + B58Alphabet.indexOf(numero58[i])
            }
            return total
        }

        fun BMDecto58(numero:String):String {
            var decimalValue:String
            var ResultadoDiv:DivResult
            var restante:String
            var Resultado = ""

            decimalValue = BigInteger(numero).toString()
            while (decimalValue.length >= 2){
                ResultadoDiv = BMDividir(decimalValue, 58)
                decimalValue = ResultadoDiv.Cociente!!
                restante = ResultadoDiv.Residuo!!
                Resultado = B58Alphabet[Integer.parseInt(restante)] + Resultado
            }

            if(Integer.parseInt(decimalValue) >= 58){
                ResultadoDiv = BMDividir(decimalValue, 58)
                decimalValue = ResultadoDiv.Cociente!!
                restante = ResultadoDiv.Residuo!!
                Resultado = B58Alphabet[Integer.parseInt(restante)] + Resultado
            }

            if(Integer.parseInt(decimalValue) > 0){
                Resultado = B58Alphabet[Integer.parseInt(decimalValue)]+Resultado
            }
            return Resultado
        }

        fun BMDividir(numeroA:String, Divisor:Int):DivResult{
            var cociente = ""
            var ThisStep = ""

            for(i in 0..(numeroA.length-1)){
                ThisStep += numeroA[i]
                if(Integer.parseInt(ThisStep) >= Divisor){
                    cociente += (Integer.parseInt(ThisStep) / Divisor).toString()
                    ThisStep = (Integer.parseInt(ThisStep) % Divisor).toString()
                }else{
                    cociente += "0"
                }
            }
            val r = DivResult()
            r.Cociente = ClearLeadingCeros(cociente)
            r.Residuo = ClearLeadingCeros(ThisStep)
            return r
        }

        fun ClearLeadingCeros(numero:String):String{
            // Using BigInteger parser to remove leading zeroes
            return BigInteger(numero).toString()
        }

        fun GetClean(number:Int):Int {
            var result = number
            while(result > 126){
                result -= 95
            }
            return result
        }

        fun RebuildHash(incoming:String):String {
            var result = ""
            var charA:Int;var charB:Int;var charF:Int

            for(i in incoming.indices){
                charA = incoming[i].code
                if(i<incoming.indices.last){
                    charB = incoming[i+1].code
                }else{
                    charB = incoming[0].code
                }
                charF = charA+charB
                charF = GetClean(charF)
                result += charF.toChar().toString()
            }
            return result
        }

        fun NosoHash(inputString:String):String {
            var source = inputString
            var FirstChange = arrayOfNulls<String>(128)
            var ThisSum:Int
            var charA:Int;var charB:Int;var charC:Int;var charD:Int
            val Filler = "%)+/5;=CGIOSYaegk"
            var result = ""

            for(i in source.indices){
                if(source[i].code > 126 || source[i].code < 33){
                    source = ""
                    break
                }
            }

            if(source.length > 63){ source = "" }
            while(source.length <= 128){ source += Filler }
            source = source.substring(0,128)
            FirstChange[0] = RebuildHash(source)
            for(c in 1..127){ FirstChange[c] = RebuildHash(FirstChange[c-1]!!) }

            val FinalHASH = FirstChange[127]!!
            for(c in 0..31){
                charA = FinalHASH[c*4+0].code
                charB = FinalHASH[c*4+1].code
                charC = FinalHASH[c*4+2].code
                charD = FinalHASH[c*4+3].code
                ThisSum = charA+charB+charC+charD
                ThisSum = GetClean(ThisSum)
                ThisSum = ThisSum.mod(16)
                result += Integer.toHexString(ThisSum)
            }
            // Return MD5 in UpperCase
            return getMD5of(result.uppercase()).uppercase()
        }

        fun CheckHashDiff(target:String, thisHash:String):String {
            var valA:Int;var valB:Int;var Difference:Int
            var ResChar:String
            var result = ""

            for(c in 0..31){
                valA = thisHash[c].toString().toInt(16)
                valB = target[c].toString().toInt(16)
                Difference = abs(valA-valB)
                ResChar = Integer.toHexString(Difference)
                result += ResChar
            }
            return result
        }

        fun ResetHashCounter(maxCPU:Int, HashArray: Array<Int?>) {
            for(t in 0 until maxCPU){
                HashArray[t] = 0
            }
        }

        fun getPrefix(minerID:Int):String {
            var firstChar:Int;var secondChar:Int;var HashChars:Int

            HashChars = HasheableChars.length-1
            firstChar = minerID/HashChars
            secondChar = minerID % HashChars
            return HasheableChars[firstChar].toString()+ HasheableChars[secondChar]+"!!!!!!!"
        }

        /** Crypto Functions **/
        fun getMD5of(cadena:String):String {
            val encoded = encodeMD5String(cadena)
            return toHex(encoded)
        }

        fun encodeMD5String(source: String):ByteArray {
            val diggest = MessageDigest.getInstance("MD5")
            return diggest.digest(source.toByteArray())
        }

        fun toHex(encoded:ByteArray):String {
            val hexcoded = StringBuilder()
            for (i in 0 until encoded.size) {
                val hex = Integer.toHexString(0xff and encoded[i].toInt())
                if (hex.length == 1) {
                    hexcoded.append('0')
                }
                hexcoded.append(hex)
            }
            return hexcoded.toString()
        }

    }


}