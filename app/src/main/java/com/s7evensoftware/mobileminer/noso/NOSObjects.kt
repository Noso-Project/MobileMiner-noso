package com.s7evensoftware.mobileminer.noso

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.io.Serializable

class NOSObjects {

}

class PoolData {
    var Address:String = ""
    var Port:Int = 8080
    var MinerID:String = ""
    var NosoAddress:String = ""
    var Connected:Boolean = false
    var CurrentBlock:Long = 0
    var TargetHash:String = ""
    var TargetDiff:String = ""
    var PoolBalance:Long = 0L
    var PoolTilPayment:Int = 30
    var PoolPayStr:String = ""
}

class PoolPayData{
    var Block = 0L
    var Amount = 0L
    var OrderID = ""
}

class ConcensusResult {
    var Address:String = ""
    var Port:Int = 8080
    var LastBlock:Long = 0
    var LastBranch:String = ""
    var Pendings:Long = 0
    var LBHash:String = ""
    var NMSDiff:String = ""
    var LBTimeEnd:Long = 0L
    var LBMiner:String = ""
}


class ConcensusData {
    var Value:String = ""
    var Count:Int = 0
}

class NodeInfo {
    var Address:String = ""
    var Port:Int = 8080
    var Lastblock:Long = 0L
    var Pendings:Long = 0L
    var Branch:String = ""
    var MNsHash:String = ""
    var MNsCount:Int = 0
    var Updated:Int = 0
    var LBHash:String = ""
    var NMSDiff:String = ""
    var LBTimedEnd:Long = 0L
    var LBMiner:String = ""
}

open class SumaryData:RealmObject() {
    @PrimaryKey var Hash = ""
    var Custom = ""
    var Balance:Long = -1
    var Score:Long = -1
    var LastOP:Long = -1
}

class PendingData {
    var Incoming:Long = 0
    var Outgoing:Long = 0
}

open class ServerObject: RealmObject() {
    @PrimaryKey
    var Address:String = "localhost"
    var Port:Int = 8080
    var isDefault:Boolean = false
}

class Solution {
    var Hash:String = ""
    var Target:String = ""
    var Diff:String = ""
}

class PendingInfo {
    var TO_Type:String = ""
    var TO_Sender:String = ""
    var TO_Receiver:String = ""
    var TO_Amount:Long = 0
    var TO_Fee:Long = 0
}

class OrderData : Serializable {
    var Block:Int = -1
    var OrderID:String? = null
    var OrderLines:Int = -1
    var OrderType: String? = ""
    var TimeStamp:Long = -1L
    var Reference:String? = null
    var TrxLine:Int = -1
    var Sender:String? = null // La clave publica de quien envia
    var Address:String? = null
    var Receiver:String? = null
    var AmountFee:Long = -1
    var AmountTrf:Long = -1
    var Signature:String? = null
    var TrfrID:String? = null
}

class WalletObject : Serializable {
    var Hash:String? = null
    var Custom:String? = null
    var PublicKey:String? = null
    var PrivateKey:String? = null
    var Balance:Long = 0
    var Pending:Long = 0
    var Score:Long = 0
    var LastOP:Long = 0
}

class KeyPair : Serializable {
    var PublicKey:String? = null
    var PrivateKey:String? = null
}

enum class KeyType {
    SECP256K1,
    SECP384R1,
    SECP521R1,
    SECT283K1
}

class DivResult {
    var Cociente:String? = null
    var Residuo:String? = null
}