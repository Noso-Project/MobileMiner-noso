package com.s7evensoftware.mobileminer.noso

import android.util.Log
import androidx.core.util.Pools
import kotlinx.coroutines.delay
import java.io.*
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.collections.ArrayList

class Network {
    companion object {

        fun SyncNodes():ArrayList<NodeInfo> {
            val NODEarray = ArrayList<NodeInfo>()

            DBManager.getServers().forEach { node ->
                getNodeStatus(node.Address, node.Port).let { nodestat ->
                    if(nodestat.Address != ""){
                        NODEarray.add(nodestat)
                    }
                }
            }
            return NODEarray
        }

        fun Concensus(NODEarray:ArrayList<NodeInfo>, viewModel:MainViewModel):ConcensusResult? {
            var result = ConcensusResult()
            var selectedNode: NodeInfo?
            var ArrT:ArrayList<ConcensusData>

            // Get the consensus block number
            ArrT = ArrayList()
            for(node in NODEarray){
                addValue(node.Lastblock.toString(), ArrT)
                result.LastBlock = getHighest(ArrT).toLong()
            }

            // Get the consensus summary
            ArrT = ArrayList()
            for(node in NODEarray){
                addValue(node.Branch, ArrT)
                result.LastBranch = getHighest(ArrT)
            }

            // Get the consensus pendings
            ArrT = ArrayList()
            for(node in NODEarray){
                addValue(node.Pendings.toString(), ArrT)
                result.Pendings = getHighest(ArrT).toLong()
            }

            // Get the consensus LBHash
            ArrT = ArrayList()
            for(node in NODEarray){
                addValue(node.LBHash, ArrT)
                result.LBHash = getHighest(ArrT)
            }

            // Get the consensus NMSDiff
            ArrT = ArrayList()
            for(node in NODEarray){
                addValue(node.NMSDiff, ArrT)
                result.NMSDiff = getHighest(ArrT)
            }

            // Get the consensus Last block time end
            ArrT = ArrayList()
            for(node in NODEarray){
                addValue(node.LBTimedEnd.toString(), ArrT)
                result.LBTimeEnd = getHighest(ArrT).toLong()
            }

            // Get the consensus Last block time end
            ArrT = ArrayList()
            for(node in NODEarray){
                addValue(node.LBMiner, ArrT)
                result.LBMiner = getHighest(ArrT)
            }

            //Select a random server for the upcoming requests
            selectedNode = getRandomServer(NODEarray, result.LastBlock, result.LastBranch, result.Pendings)
            result.Address = selectedNode.Address
            result.Port = selectedNode.Port
            viewModel.LastNodeSelected = selectedNode

            return result
        }

        fun getNodeStatus(address:String, port:Int):NodeInfo{
            val serverAddress = InetSocketAddress(address, port)
            Log.e("mpNetwork","Requesting Node Status to $address:$port")
            try{
                val clientSocket = Socket()
                clientSocket.connect(serverAddress, NODE_TIMEOUT)
                val clientChannel = PrintWriter(BufferedWriter(OutputStreamWriter(clientSocket.getOutputStream())), true)
                val inputStreamReader = InputStreamReader(clientSocket.getInputStream())
                val bufferReader = BufferedReader(inputStreamReader)

                clientChannel.println("NODESTATUS")
                val response = bufferReader.readLine()
                clientSocket.close()

                return stringToNodeInfo(response, address, port)
            }catch (t: SocketTimeoutException){
                Log.e("mpNetwork","Request to $address -> Timed Out")
            }catch (c: ConnectException){ // No internet ?
                Log.e("mpNetwork","Connection error, check the internet")
            }catch (r: java.io.IOException){ // No internet ?
                Log.e("mpNetwork","Reading error, malformed input? : ${r.message}")
            }catch (e:java.lang.Exception){ // Something else....
                Log.e("mpNetwork","Unhandled Exception: "+e.message)
            }
            return NodeInfo()
        }

        fun getPoolData(address: String, port: Int, viewModel: MainViewModel):PoolData {
            val serverAddress = InetSocketAddress(address, port)
            Log.e("mpNetwork","Connecting to Pool:  $address:$port")
            try{
                val clientSocket = Socket()
                clientSocket.connect(serverAddress, NODE_TIMEOUT)
                val clientChannel = PrintWriter(BufferedWriter(OutputStreamWriter(clientSocket.getOutputStream())), true)
                val inputStreamReader = InputStreamReader(clientSocket.getInputStream())
                val bufferReader = BufferedReader(inputStreamReader)

                clientChannel.println(
                    "SOURCE ${viewModel.MinerAddress}")
                val response = bufferReader.readLine()
                clientSocket.close()

                Log.e("Network", "Pool: $response")
                val poolInfo = response.split(" ")

                if(poolInfo[0] == "OK"){
                    val poolData = PoolData()
                    poolData.Address = address
                    poolData.Port = port
                    poolData.MinerID = poolInfo[1]
                    poolData.NosoAddress = poolInfo[2]
                    poolData.TargetDiff = poolInfo[3]
                    poolData.TargetHash = poolInfo[4]
                    poolData.CurrentBlock = poolInfo[5].toLong()
                    poolData.PoolBalance = poolInfo[6].toLong()
                    poolData.PoolTilPayment = poolInfo[7].toInt()
                    poolData.PoolPayStr = poolInfo[8].replace(":"," ",true)

                    val poolPayStr = poolData.PoolPayStr.split(" ")

                    val tempData = PoolPayData()
                    if(poolPayStr.size > 1){
                            tempData.Block = poolPayStr[0].toLong()
                            tempData.Amount = poolPayStr[1].toLong()
                            tempData.OrderID = poolPayStr[2]
                    }

                    if(poolData.CurrentBlock > viewModel.LastBlock.value!!){
                        Log.e("Network", "Poll Connection Success")
                        poolData.Connected = true
                    }

                    if(viewModel.lastPoolPayment.value!!.OrderID != tempData.OrderID){
                        viewModel.OutPutInfo += "\n*** New Pool Payment ***"
                        viewModel.lastPoolPayment.value = tempData
                    }

                    viewModel.currentPoolStatic = poolData
                    viewModel.currentPool.postValue(poolData)
                    return poolData
                }

                return PoolData()
            }catch (t: SocketTimeoutException){
                Log.e("mpNetwork","Connection to $address:$port TimedOut, retrying...")
                viewModel.OutPutInfo += "\nConnection to $address:$port TimedOut, retrying..."
                return PoolData()
            }catch (c: ConnectException){ // No internet ?
                Log.e("mpNetwork","Connection error, check the internet, retrying...")
                viewModel.OutPutInfo += "\nConnection error, check the internet, retrying..."
                return PoolData()
            }catch (r: java.io.IOException){ // No internet ?
                Log.e("mpNetwork","Reading error, malformed input? : ${r.message}")
                return PoolData()
            }catch (e:java.lang.Exception){ // Something else....
                Log.e("mpNetwork","Unhandled Exception: $e")
                return PoolData()
            }
        }

        suspend fun sendPoolShare(
            solution: Solution,
            viewModel: MainViewModel,
            retries: Int
        ):Boolean {
            val serverAddress = InetSocketAddress(viewModel.currentPoolStatic.Address, viewModel.currentPoolStatic.Port)
            Log.e("mpNetwork","Sending Share to ${viewModel.currentPoolStatic.Address}:${viewModel.currentPoolStatic.Port}")
            try{
                val clientSocket = Socket()
                clientSocket.connect(serverAddress, NODE_TIMEOUT)
                val clientChannel = PrintWriter(BufferedWriter(OutputStreamWriter(clientSocket.getOutputStream())), true)
                val inputStreamReader = InputStreamReader(clientSocket.getInputStream())
                val bufferReader = BufferedReader(inputStreamReader)

                clientChannel.println(
                    "SHARE ${viewModel.MinerAddress} ${solution.Hash}")
                val response = bufferReader.readLine()
                clientSocket.close()

                Log.e("Network", "Solution Response(Share): $response")
                val Result = response.split(" ")
                val accepted = Result[0].toBoolean()

                if(accepted){
                    viewModel.acceptedShares.postValue(viewModel.acceptedShares.value?.inc())
                    Log.e("Network","Solution Sent was accepted")
                    viewModel.OutPutInfo += "\n\n################################"
                    viewModel.OutPutInfo += "\nSending Solution"
                    viewModel.OutPutInfo += "\nTarget: ${solution.Target}"
                    viewModel.OutPutInfo += "\nHash: ${solution.Hash}"
                    viewModel.OutPutInfo += "\nDiff: ${solution.Diff}"
                    viewModel.OutPutInfo += "\n!!Solution Sent Was Accepted!!"
                    viewModel.OutPutInfo += "\n################################\n"
                }else{
                    // Update Target Diff
                    if(Result[1] < viewModel.TargetDiffStatic){
                        viewModel.TargetDiffStatic = Result[1]
                        viewModel.TargetDiff.postValue(Result[1])
                    }

                    Log.e("Network","Solution Sent Rejected")
                    viewModel.OutPutInfo += "\n\nSending Solution"
                    viewModel.OutPutInfo += "\nTarget: ${solution.Target}"
                    viewModel.OutPutInfo += "\nHash: ${solution.Hash}"
                    viewModel.OutPutInfo += "\nDiff: ${solution.Diff}"
                    viewModel.OutPutInfo += "\nSolution Sent Was Rejected\n"
                }
                viewModel.TriggerOutputUpdate.postValue(viewModel.OutPutInfo.length)
                return accepted
            }catch (t: SocketTimeoutException){
                Log.e("mpNetwork","Request to ${viewModel.MinerAddress} -> Timed Out, retrying...")
                if(retries > 0){
                    delay((5-retries)*1000L)
                    return sendPoolShare(solution,viewModel,retries-1)
                }
                return false
            }catch (c: ConnectException){ // No internet ?
                Log.e("mpNetwork","Connection error, check the internet, retrying...")
                if(retries > 0){
                    delay((5-retries)*1000L)
                    return sendPoolShare(solution,viewModel,retries-1)
                }
                return false
            }catch (r: java.io.IOException){ // No internet ?
                Log.e("mpNetwork","Reading error, malformed input? : ${r.message}")
                return false
            }catch (e:java.lang.Exception){ // Something else....
                Log.e("mpNetwork","Unhandled Exception: $e")
                return false
            }
        }

        suspend fun sendSolution(
            solution: Solution,
            viewModel: MainViewModel,
            retries: Int
        ):Boolean {
            val serverAddress = InetSocketAddress(viewModel.concensusResult!!.Address, viewModel.concensusResult!!.Port)
            Log.e("mpNetwork","Sending Solution to ${viewModel.concensusResult?.Address}:${viewModel.concensusResult?.Port}")
            try{
                val clientSocket = Socket()
                clientSocket.connect(serverAddress, NODE_TIMEOUT)
                val clientChannel = PrintWriter(BufferedWriter(OutputStreamWriter(clientSocket.getOutputStream())), true)
                val inputStreamReader = InputStreamReader(clientSocket.getInputStream())
                val bufferReader = BufferedReader(inputStreamReader)

                clientChannel.println(
                    "BESTHASH 1 2 3 4 ${viewModel.MinerAddress} ${solution.Hash} ${viewModel.LastBlock.value?.plus(1)} ${System.currentTimeMillis()/1000}")
                val response = bufferReader.readLine()
                clientSocket.close()

                Log.e("Network", "Solution Response: $response")
                val Result = response.split(" ")
                val accepted = Result[0].toBoolean()

                if(Result[1] < viewModel.TargetDiffStatic){
                    viewModel.TargetDiffStatic = Result[1]
                    viewModel.TargetDiff.postValue(Result[1])
                }

                if(accepted){
                    Log.e("Network","Solution Sent was accepted")
                    viewModel.OutPutInfo += "\n\n################################"
                    viewModel.OutPutInfo += "\nSending Solution"
                    viewModel.OutPutInfo += "\nTarget: ${solution.Target}"
                    viewModel.OutPutInfo += "\nHash: ${solution.Hash}"
                    viewModel.OutPutInfo += "\nDiff: ${solution.Diff}"
                    viewModel.OutPutInfo += "\n!!Solution Sent Was Accepted!!"
                    viewModel.OutPutInfo += "\n################################\n"
                }else{
                    Log.e("Network","Solution Sent Rejected")
                    viewModel.OutPutInfo += "\n\nSending Solution"
                    viewModel.OutPutInfo += "\nTarget: ${solution.Target}"
                    viewModel.OutPutInfo += "\nHash: ${solution.Hash}"
                    viewModel.OutPutInfo += "\nDiff: ${solution.Diff}"
                    viewModel.OutPutInfo += "\nSolution Sent Was Rejected\n"
                }
                viewModel.TriggerOutputUpdate.postValue(viewModel.OutPutInfo.length)
                return accepted
            }catch (t: SocketTimeoutException){
                Log.e("mpNetwork","Request to ${viewModel.MinerAddress} -> Timed Out, retrying...")
                if(retries > 0){
                    delay((5-retries)*1000L)
                    return sendSolution(solution,viewModel,retries-1)
                }
                return false
            }catch (c: ConnectException){ // No internet ?
                Log.e("mpNetwork","Connection error, check the internet, retrying...")
                if(retries > 0){
                    delay((5-retries)*1000L)
                    return sendSolution(solution,viewModel,retries-1)
                }
                return false
            }catch (r: java.io.IOException){ // No internet ?
                Log.e("mpNetwork","Reading error, malformed input? : ${r.message}")
                return false
            }catch (e:java.lang.Exception){ // Something else....
                Log.e("mpNetwork","Unhandled Exception: $e")
                return false
            }
        }

        private fun getRandomServer(NODEarray: ArrayList<NodeInfo>, block:Long, brach:String, pendings:Long): NodeInfo {
            val candidateServer = ArrayList<NodeInfo>()
            for(server in NODEarray){
                if(
                    server.Branch == brach &&
                    server.Lastblock == block &&
                    server.Pendings == pendings //&&
                    //(DELTA_TRIGGER || server.Delta == 0L)
                ){
                    candidateServer.add(server)
                }
            }

            if(candidateServer.size > 0){
                ThreadLocalRandom.current().nextInt(candidateServer.size).let {
                    return candidateServer[it]
                }
            }
            return NodeInfo()
        }

        fun stringToNodeInfo(input: String, address: String, port: Int):NodeInfo{
            val tokens = StringTokenizer(input)
            val values = ArrayList<String>()
            while(tokens.hasMoreTokens()){
                values.add(tokens.nextToken())
            }

            val nodeInfo = NodeInfo()
            nodeInfo.Address = address
            nodeInfo.Port = port
            nodeInfo.Lastblock = values[2].toLong()
            nodeInfo.LBHash = values[10]
            nodeInfo.NMSDiff = values[11]
            nodeInfo.LBTimedEnd = values[12].toLong()
            nodeInfo.LBMiner = values[13]
            return nodeInfo
        }

        fun getHighest(ArrT:ArrayList<ConcensusData>):String {
            var Maximum = 0
            var MaxIndex = 0

            for(cd in ArrT){
                if(cd.Count > Maximum){
                    Maximum = cd.Count
                    MaxIndex = ArrT.indexOf(cd)
                }
            }
            return ArrT[MaxIndex].Value
        }

        fun addValue(Tvalue:String, ArrT: ArrayList<ConcensusData>){
            var Added = false
            var ThisItem = ConcensusData()

            for(cd in ArrT){
                if(Tvalue.equals(cd.Value)){
                    cd.Count++
                    Added = true
                }
            }

            if(!Added){
                ThisItem.Value = Tvalue
                ThisItem.Count = 1
                ArrT.add(ThisItem)
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
    }
}