package main

import (
	"bufio"
	"crypto/md5"
	"fmt"
	"os"
	"strconv"
	"strings"
	"sync"
	"time"
)

const TestHashLimit = 50000
const filler string = "%)+/5;=CGIOSYaegk"
const MaxDiff = "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"
const HasheableChars = "!\"#$%&')*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~"

var isTesting = false
var minerKill = false
var isMining = false
var MinerID = 1
var CurrentMiningCPUs = 1
var MinerCouters [128]int64
var LastTotalHashes int64 = 0
var TotalHashCount int64 = 0
var TargetHash = "00000000000000000000000000000000"
var TargetDiff = MaxDiff
var ThreadPrefix int = 1
var MAINPREFIX string = ""
var MiningAddress string = "N4NsDwKdQJ17RqcAYNpRuSHbkKF28Ct"
var lastSpeedCheck = time.Now().UnixMilli()

func main() {
	resetCounters()
	commandReader := bufio.NewReader(os.Stdin)
	fmt.Println(PROCESS_START)
	for {
		input, _, err := commandReader.ReadLine()
		if err != nil {
			fmt.Println("# Error parsing the command: ", err)
			continue
		} else {
			command := string(input)
			command = strings.ToUpper(command)
			commands := strings.Split(command, " ")

			switch commands[0] {
			case SPEED_REPORT:
				reportSpeed()
			case UPDATE_ADDRESS:
				// CHANGEADDRESS address
				MiningAddress = commands[1]
				fmt.Println("# Setting new address")
				fmt.Println("# Address:", MiningAddress)
			case UPDATE_TARGET:
				// CHANGETARGET tdiff
				TargetDiff = commands[1]
			case UPDATE_BLOCK:
				// CHANGEBLOCK thash tdiff bnumber
				TargetHash = commands[1]
				TargetDiff = commands[2]
				resetCounters()
				fmt.Println("# Setting new block info")
				fmt.Println("# TargetHash:", TargetHash)
				fmt.Println("# TargetDiff:", TargetDiff)
			case MINE_COMMAND:
				// Mine CPU# MID# Address THASH TDIFF
				var maxCPU = str2int(commands[1])
				MinerID = str2int(commands[2])
				MiningAddress = commands[3]
				TargetHash = commands[4]
				TargetDiff = commands[5]
				CurrentMiningCPUs = maxCPU
				var wg = new(sync.WaitGroup)
				mineStart(maxCPU, wg)
				isMining = true
			case EXIT_COMMAND:
				goto exit
			case TEST_HASH_COMMAND:
				isTesting = true
				fmt.Println(HASH_TEST_START)
				var maxCPU int = 1
				maxCPU = str2int(commands[1])

				var wg = new(sync.WaitGroup)
				resetCounters()
				startTime := time.Now().UnixMilli()
				for c := 1; c <= maxCPU; c++ {
					wg.Add(1)
					go mine(c, wg)
				}
				wg.Wait()
				endTime := time.Now().UnixMilli()
				testime := endTime - startTime
				result := TestHashLimit / (testime / 1000)
				fmt.Println("CPUS", maxCPU, "SPEED", result*int64(maxCPU))
				fmt.Println(HASH_TEST_STOP)
				isTesting = false
			case TEST_MINER_COMMAND:
				isTesting = true
				fmt.Println(MINER_TEST_START)
				var maxCPU = str2int(commands[1])
				var wg = new(sync.WaitGroup)
				for m := 1; m <= maxCPU; m++ {
					resetCounters()
					startTime := time.Now().UnixMilli()
					for c := 1; c <= m; c++ {
						wg.Add(1)
						go mine(c, wg)
					}
					wg.Wait()
					endTime := time.Now().UnixMilli()
					testime := endTime - startTime
					result := TestHashLimit / (testime / 1000)
					fmt.Println("CPUS", m, "SPEED", result*int64(m))
				}
				fmt.Println(MINER_TEST_STOP)
				isTesting = false
			default:
				fmt.Printf("Unknown command [%s]\n", command)
			}
		}
	}

exit:
	fmt.Println(PROCESS_END)
}

func reportSpeed() {
	n := time.Now().UnixMilli()
	newTotal := TotalHashCount
	speed := (newTotal - LastTotalHashes) / ((n - lastSpeedCheck) / 1000)
	fmt.Println("SPEEDREPORT", speed)
	LastTotalHashes = newTotal
	lastSpeedCheck = n
}

func mineStart(maxCPU int, wg *sync.WaitGroup) {
	fmt.Println(MINER_START)
	fmt.Println("Creating", maxCPU, "miner threads ...")
	TotalHashCount = 0
	lastSpeedCheck = time.Now().UnixMilli()
	for m := 1; m <= maxCPU; m++ {
		wg.Add(1)
		go mine(m, wg)
	}
	fmt.Println("Creation Success")
}

func resetCounters() {
	for c := 0; c < 128; c++ {
		MinerCouters[c] = 100000000
	}
	TotalHashCount = 0
}

func getClean(number int) int {
	result := number
	for result > 126 {
		result = result - 95
	}
	return result
}

func mutateHash(sHash string) string {
	var LHash string = sHash
	var AHash [128]byte
	var charA, charB byte
	var C int
	var HashLen int

	HashLen = len([]rune(LHash))

	for x := 0; x < 128; x++ {
		for i := 0; i < HashLen; i++ {
			charA = LHash[i]
			if i < HashLen-1 {
				charB = LHash[i+1]
			} else {
				charB = LHash[0]
			}
			C = int(charA) + int(charB)
			AHash[i] = byte(getClean(C))
		}
		LHash = string(AHash[:])
	}
	return LHash
}

func NosoHash(source string) string {
	var FinalHASH string
	var ThisSUM int
	var charA, charB, charC, charD int

	result := ""

	for c := 0; c < len([]rune(source)); c++ {
		if source[c] > 126 || source[c] < 33 {
			source = ""
			break
		}
	}

	if len([]rune(source)) > 63 {
		source = ""
	}

	for len([]rune(source)) <= 128 {
		source = source + filler
	}

	source = source[:128]
	FinalHASH = mutateHash(source)
	for c := 0; c <= 31; c++ {
		charA = int(FinalHASH[c*4+0])
		charB = int(FinalHASH[c*4+1])
		charC = int(FinalHASH[c*4+2])
		charD = int(FinalHASH[c*4+3])
		ThisSUM = charA + charB + charC + charD
		ThisSUM = getClean(ThisSUM)
		ThisSUM = ThisSUM % 16
		result = result + fmt.Sprintf("%X", ThisSUM)
	}
	rawInput := []byte(result)
	return fmt.Sprintf("%X", md5.Sum(rawInput))
}

func getPrefix(minerID int) string {
	var firstChar, secondChar rune
	var hashChars int

	hashChars = len([]rune(HasheableChars)) - 1
	firstChar = rune(minerID / hashChars)
	secondChar = rune(minerID % hashChars)
	result := string(HasheableChars[int(firstChar)]) + string(HasheableChars[int(secondChar)])
	return result
}

func hex2Dec(input rune) int {
	HexToDec, err := strconv.ParseInt(string(input), 16, 64)
	if err != nil {
		return 0
	}
	return int(HexToDec)
}

func checkHashDiff(target string, testhash string) string {
	var valA, valB, Difference int
	var resChar string
	var result string = ""

	for c := 0; c < 32; c++ {
		valA = hex2Dec([]rune(testhash)[c])
		valB = hex2Dec([]rune(target)[c])
		Difference = valA - valB
		if Difference < 0 {
			Difference = Difference * -1
		}
		resChar = fmt.Sprintf("%X", Difference)
		result = result + resChar
	}
	return result
}

func mine(myID int, wg *sync.WaitGroup) {
	var BaseHash, ThisHash, ThisDiff string
	var ThisPrefix string = ""

	ThisPrefix = MAINPREFIX + getPrefix(MinerID) + getPrefix(myID)
	for len([]rune(ThisPrefix)) < 18 {
		ThisPrefix = ThisPrefix + "!"
	}

	defer wg.Done()

	EndThread := false

	for EndThread == false {
		BaseHash = ThisPrefix + fmt.Sprint(MinerCouters[myID])
		MinerCouters[myID] = MinerCouters[myID] + 1
		TotalHashCount++
		ThisHash = NosoHash(BaseHash + MiningAddress)
		ThisDiff = checkHashDiff(TargetHash, ThisHash)

		if ThisDiff < TargetDiff {
			if !isTesting {
				fmt.Println("SOLUTION -> TARGET:", TargetHash, "HASH:", BaseHash, "DIFF:", ThisDiff)
				TargetDiff = ThisDiff
			}
		}

		if (isTesting && (MinerCouters[myID] == 100000000+TestHashLimit)) || minerKill {
			EndThread = true
		}
	}
	return
}

func str2int(input string) int {
	value, err := strconv.ParseInt(input, 10, 64)
	if err != nil {
		return 0
	}
	return int(value)
}
