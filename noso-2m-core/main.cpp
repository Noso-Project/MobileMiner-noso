#include <set>
#include <mutex>
#include <tuple>
#include <array>
#include <vector>
#include <string>
#include <thread>
#include <random>
#include <chrono>
#include <numeric>
#include <iomanip>
#include <cassert>
#include <iostream>

#include <signal.h>
#include <unistd.h>

#include "md5-c.hpp"
#include "cxxopts.hpp"

#define DEFAULT_MINER_ADDRESS "N3tXc1QjGcCJ3asHV2UvUSZwqU1t9Dp"
#define DEFAULT_MINER_ID 0
#define DEFAULT_THREADS_COUNT 1

#define NOSOHASH_COUNTER_MIN 100'000'000
#define NOSOHASH_TEST_MAX 200'000
#define NOSO_MAX_DIFF "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"

const std::string PROCESS_START = "MINERBINSTART";
const std::string PROCESS_END = "MINERBINSTOP";

const std::string HASH_TEST_START = "HASHTEST_START";
const std::string HASH_TEST_STOP = "HASHTEST_STOP";

const std::string MINER_TEST_START = "MINERTEST_START";
const std::string MINER_TEST_STOP = "MINERTEST_STOP";

const std::string MINER_START = "MINER_START";
const std::string MINER_STOP = "MINER_STOP";

const std::string EXIT_COMMAND = "EXIT";
const std::string MINE_COMMAND = "MINE";
const std::string UPDATE_BLOCK = "CHANGEBLOCK";
const std::string UPDATE_TARGET = "CHANGETARGET";
const std::string UPDATE_ADDRESS = "CHANGEADDRESS";
const std::string SPEED_REPORT = "SPEEDREPORT";
const std::string TEST_NOSOHASH_COMMAND = "TESTHASH";
const std::string TEST_HASH_COMMAND = "TESTHASH";
const std::string TEST_MINER_COMMAND = "TESTMINER";

// Variables for speedReport
auto          lastSpeedStamp = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
std::uint32_t lastSpeedCount { 0 };

const char NOSOHASH_HASHEABLE_CHARS[] {
    "!\"#$%&')*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~" };
const std::size_t NOSOHASH_HASHEABLE_COUNT =  93 - 1; // strlen( NOSOHASH_HASHEABLE_COUNT );
inline std::string nosohash_prefix( int num ) {
    return std::string {
        NOSOHASH_HASHEABLE_CHARS[ num / NOSOHASH_HASHEABLE_COUNT ],
        NOSOHASH_HASHEABLE_CHARS[ num % NOSOHASH_HASHEABLE_COUNT ], };
}

inline int nosohash_char( int num ) {
    assert( 32 <= num && num <= 504 );
    while ( num > 126 ) num -= 95;
    assert( 32 <= num && num <= 126 );
    return num;
};

#ifndef NDEBUG
const std::array<char, 16> HEXCHAR_DOMAIN { '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F' };
#endif
inline int hex_char2dec( char hexchar ) {
    assert( std::find( HEXCHAR_DOMAIN.begin(), HEXCHAR_DOMAIN.end(), hexchar ) != HEXCHAR_DOMAIN.end() );
    return  ( '0' <= hexchar && hexchar <= '9' ) ? hexchar - '0' :
            ( 'A' <= hexchar && hexchar <= 'F' ) ? hexchar - 'A' + 10 : 0;
}

#ifndef NDEBUG
const std::array<int, 16> HEXDEC_DOMAIN{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
#endif
inline char hex_dec2char( int hexdec ) {
    assert( std::find( HEXDEC_DOMAIN.begin(), HEXDEC_DOMAIN.end(), hexdec ) != HEXDEC_DOMAIN.end() );
    return  (  0 <= hexdec && hexdec <=  9 ) ? hexdec + '0' :
            ( 10 <= hexdec && hexdec <= 15 ) ? hexdec + 'A' - 10 : '\0';
}

class CNosoHasher {
private:
    char m_base[19]; // 18 = 9-chars-prefix + 9-chars-counter
    char m_hash[33];
    char m_diff[33];
    char m_stat[129][128]; // 1+128 rows x 128 columns
    // the 1st row is the input of hash function with len = 128 = 9-chars-prefix + 9 chars-counter + 30/31-chars-address + N-fill-chars rest
    MD5Context m_md5_ctx;
    constexpr static const char hexchars_table[] = "0123456789ABCDEF";
    constexpr static std::uint16_t nosohash_chars_table[505] {
    // for ( int i = 0; i < 505; ++i ) {    // as 4 * 126 = 504 maximum value
    //     int n = i >= 32 ? i : 0;
    //     while ( n > 126 ) n -= 95;       // as 127 - 95 =  32 minimum value
    //     // std::cout << std::setw( 3 ) << i << ", ";
    //     std::cout << std::setw( 3 ) << n << ", ";
    //     if ( i % 24 == 0 ) std::cout << std::endl;
    // }
  0,
  0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,
  0,   0,   0,   0,   0,   0,   0,  32,  33,  34,  35,  36,  37,  38,  39,  40,  41,  42,  43,  44,  45,  46,  47,  48,
 49,  50,  51,  52,  53,  54,  55,  56,  57,  58,  59,  60,  61,  62,  63,  64,  65,  66,  67,  68,  69,  70,  71,  72,
 73,  74,  75,  76,  77,  78,  79,  80,  81,  82,  83,  84,  85,  86,  87,  88,  89,  90,  91,  92,  93,  94,  95,  96,
 97,  98,  99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120,
121, 122, 123, 124, 125, 126,  32,  33,  34,  35,  36,  37,  38,  39,  40,  41,  42,  43,  44,  45,  46,  47,  48,  49,
 50,  51,  52,  53,  54,  55,  56,  57,  58,  59,  60,  61,  62,  63,  64,  65,  66,  67,  68,  69,  70,  71,  72,  73,
 74,  75,  76,  77,  78,  79,  80,  81,  82,  83,  84,  85,  86,  87,  88,  89,  90,  91,  92,  93,  94,  95,  96,  97,
 98,  99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121,
122, 123, 124, 125, 126,  32,  33,  34,  35,  36,  37,  38,  39,  40,  41,  42,  43,  44,  45,  46,  47,  48,  49,  50,
 51,  52,  53,  54,  55,  56,  57,  58,  59,  60,  61,  62,  63,  64,  65,  66,  67,  68,  69,  70,  71,  72,  73,  74,
 75,  76,  77,  78,  79,  80,  81,  82,  83,  84,  85,  86,  87,  88,  89,  90,  91,  92,  93,  94,  95,  96,  97,  98,
 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122,
123, 124, 125, 126,  32,  33,  34,  35,  36,  37,  38,  39,  40,  41,  42,  43,  44,  45,  46,  47,  48,  49,  50,  51,
 52,  53,  54,  55,  56,  57,  58,  59,  60,  61,  62,  63,  64,  65,  66,  67,  68,  69,  70,  71,  72,  73,  74,  75,
 76,  77,  78,  79,  80,  81,  82,  83,  84,  85,  86,  87,  88,  89,  90,  91,  92,  93,  94,  95,  96,  97,  98,  99,
100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123,
124, 125, 126,  32,  33,  34,  35,  36,  37,  38,  39,  40,  41,  42,  43,  44,  45,  46,  47,  48,  49,  50,  51,  52,
 53,  54,  55,  56,  57,  58,  59,  60,  61,  62,  63,  64,  65,  66,  67,  68,  69,  70,  71,  72,  73,  74,  75,  76,
 77,  78,  79,  80,  81,  82,  83,  84,  85,  86,  87,  88,  89,  90,  91,  92,  93,  94,  95,  96,  97,  98,  99, 100,
101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124,
    };
    inline void _hash() {
        int row, col;
        for( row = 1; row < 129; row++ ) {
            for( col = 0; col < 127; col++ ) {
                // m_stat[row][col] = nosohash_char(        m_stat[row-1][col] + m_stat[row-1][col+1] );
                m_stat[row][col] = nosohash_chars_table[ m_stat[row-1][col] + m_stat[row-1][col+1] ];
            }
            // m_stat[row][127] = nosohash_char(        m_stat[row-1][127] + m_stat[row-1][0] );
            m_stat[row][127] = nosohash_chars_table[ m_stat[row-1][127] + m_stat[row-1][0] ];
        }
        int i;
        for( i = 0; i < 32; i++ )
            // m_hash[i] = hex_dec2char( nosohash_char(
            //                             m_stat[128][ ( i * 4 ) + 0 ] +
            //                             m_stat[128][ ( i * 4 ) + 1 ] +
            //                             m_stat[128][ ( i * 4 ) + 2 ] +
            //                             m_stat[128][ ( i * 4 ) + 3 ] ) % 16 );
            m_hash[i] = hex_dec2char( nosohash_chars_table[
                                        m_stat[128][ ( i * 4 ) + 0 ] +
                                        m_stat[128][ ( i * 4 ) + 1 ] +
                                        m_stat[128][ ( i * 4 ) + 2 ] +
                                        m_stat[128][ ( i * 4 ) + 3 ] ] % 16 );
        m_hash[32] = '\0';
        assert( strlen( m_hash ) == 32 );
    }
    inline void _md5() {
        assert( strlen( m_hash ) == 32 );
        md5Init( &m_md5_ctx );
        md5Update( &m_md5_ctx, (uint8_t *)m_hash, 32 );
        md5Finalize( &m_md5_ctx );
        // sprintf( m_hash,
        //         "%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X",
        //         m_md5_ctx.digest[ 0], m_md5_ctx.digest[ 1],
        //         m_md5_ctx.digest[ 2], m_md5_ctx.digest[ 3],
        //         m_md5_ctx.digest[ 4], m_md5_ctx.digest[ 5],
        //         m_md5_ctx.digest[ 6], m_md5_ctx.digest[ 7],
        //         m_md5_ctx.digest[ 8], m_md5_ctx.digest[ 9],
        //         m_md5_ctx.digest[10], m_md5_ctx.digest[11],
        //         m_md5_ctx.digest[12], m_md5_ctx.digest[13],
        //         m_md5_ctx.digest[14], m_md5_ctx.digest[15] );
        m_hash[ 0] = hexchars_table[m_md5_ctx.digest[ 0] >>  4];
        m_hash[ 1] = hexchars_table[m_md5_ctx.digest[ 0] & 0xF];
        m_hash[ 2] = hexchars_table[m_md5_ctx.digest[ 1] >>  4];
        m_hash[ 3] = hexchars_table[m_md5_ctx.digest[ 1] & 0xF];
        m_hash[ 4] = hexchars_table[m_md5_ctx.digest[ 2] >>  4];
        m_hash[ 5] = hexchars_table[m_md5_ctx.digest[ 2] & 0xF];
        m_hash[ 6] = hexchars_table[m_md5_ctx.digest[ 3] >>  4];
        m_hash[ 7] = hexchars_table[m_md5_ctx.digest[ 3] & 0xF];
        m_hash[ 8] = hexchars_table[m_md5_ctx.digest[ 4] >>  4];
        m_hash[ 9] = hexchars_table[m_md5_ctx.digest[ 4] & 0xF];
        m_hash[10] = hexchars_table[m_md5_ctx.digest[ 5] >>  4];
        m_hash[11] = hexchars_table[m_md5_ctx.digest[ 5] & 0xF];
        m_hash[12] = hexchars_table[m_md5_ctx.digest[ 6] >>  4];
        m_hash[13] = hexchars_table[m_md5_ctx.digest[ 6] & 0xF];
        m_hash[14] = hexchars_table[m_md5_ctx.digest[ 7] >>  4];
        m_hash[15] = hexchars_table[m_md5_ctx.digest[ 7] & 0xF];
        m_hash[16] = hexchars_table[m_md5_ctx.digest[ 8] >>  4];
        m_hash[17] = hexchars_table[m_md5_ctx.digest[ 8] & 0xF];
        m_hash[18] = hexchars_table[m_md5_ctx.digest[ 9] >>  4];
        m_hash[19] = hexchars_table[m_md5_ctx.digest[ 9] & 0xF];
        m_hash[20] = hexchars_table[m_md5_ctx.digest[10] >>  4];
        m_hash[21] = hexchars_table[m_md5_ctx.digest[10] & 0xF];
        m_hash[22] = hexchars_table[m_md5_ctx.digest[11] >>  4];
        m_hash[23] = hexchars_table[m_md5_ctx.digest[11] & 0xF];
        m_hash[24] = hexchars_table[m_md5_ctx.digest[12] >>  4];
        m_hash[25] = hexchars_table[m_md5_ctx.digest[12] & 0xF];
        m_hash[26] = hexchars_table[m_md5_ctx.digest[13] >>  4];
        m_hash[27] = hexchars_table[m_md5_ctx.digest[13] & 0xF];
        m_hash[28] = hexchars_table[m_md5_ctx.digest[14] >>  4];
        m_hash[29] = hexchars_table[m_md5_ctx.digest[14] & 0xF];
        m_hash[30] = hexchars_table[m_md5_ctx.digest[15] >>  4];
        m_hash[31] = hexchars_table[m_md5_ctx.digest[15] & 0xF];
        m_hash[32] = '\0';
        assert( strlen( m_hash ) == 32 );
    }
public:
    CNosoHasher( const char prefix[10], const char address[32] ) {
        constexpr static const char NOSOHASH_FILLER_CHARS[] = "%)+/5;=CGIOSYaegk";
        constexpr static const int NOSOHASH_FILLER_COUNT = 17; // strlen( NOSOHASH_FILLER_CHARS );
        assert( strlen( prefix ) == 9 && ( strlen( address ) == 30 || strlen( address ) == 31 ) );
        memcpy( m_base, prefix, 9 );
        sprintf( m_base + 9, "%09d", 0 ); // placehold for 9-digits-counter
        assert( strlen( m_base ) == 18 ); // 18 = 9 + 9 = 9-chars-prefix + 9-digits-counter
        int addr_len = strlen( address );
        memcpy( m_stat[0], m_base, 9 );
        memcpy( m_stat[0] + 9, m_base + 9, 9 );  // update the 9-digits-counter part as the same as it is updated in base
        memcpy( m_stat[0] + 9 + 9, address, addr_len );
        int len = 18 + addr_len; // 48/49 = 9 + 9 + 30/31 = 9-chars-prefix + 9-digits-counter + 30/31-chars-address
        int div = ( 128 - len ) / NOSOHASH_FILLER_COUNT;
        int mod = ( 128 - len ) % NOSOHASH_FILLER_COUNT;
        for ( int i = 0; i < div; i++ ) {
            memcpy( m_stat[0] + len, NOSOHASH_FILLER_CHARS, NOSOHASH_FILLER_COUNT );
            len += NOSOHASH_FILLER_COUNT;
        }
        memcpy( m_stat[0] + len, NOSOHASH_FILLER_CHARS, mod );
        assert( std::none_of( m_stat[0], m_stat[0] + 128, []( int c ){ return 33 > c || c > 126; } ) );
    }
    const char* GetBase( std::uint32_t counter ) {
        // TODO consider case counter > 999'999'999 => base len > 18. Currently
        // it does not happen, each single thread can hash/search under
        // 700'000'000 hashes each block
        sprintf( m_base + 9, "%09d", NOSOHASH_COUNTER_MIN + counter ); // update 9-digits-counter part
        assert( strlen( m_base ) == 18 ); // 18 = 9 + 9 = 9-chars-prefix + 9-digits-counter
        memcpy( m_stat[0] + 9, m_base + 9, 9 );  // update the 9-digits-counter part as it was updated in base
        assert( std::none_of( m_stat[0], m_stat[0] + 128, []( int c ){ return 33 > c || c > 126; } ) );
        return m_base;
    }
    const char* GetHash() {
        _hash();
        _md5();
        return m_hash;
    }
    const char* GetDiff( const char target[33] ) {
        assert( strlen( m_hash ) == 32 && strlen( target ) == 32 );
        for ( std::size_t i = 0; i < 32; i ++ )
            m_diff[i] = toupper( hex_dec2char( abs( hex_char2dec( m_hash[ i ] ) - hex_char2dec( target[ i ] ) ) ) );
        m_diff[32] = '\0';
        assert( strlen( m_diff ) == 32 );
        return m_diff;
    }
};
constexpr const char CNosoHasher::hexchars_table[];
constexpr std::uint16_t CNosoHasher::nosohash_chars_table[];

class CMineThread {
protected:
    char m_address[32];
    char m_prefix[10];
    //std::uint32_t m_blck_no { 0 };
    char best_diff[33] { NOSO_MAX_DIFF };
    char m_lb_hash[33];
    //std::uint32_t m_computed_hashes_count { 0 };
    std::uint32_t noso_hash_counter { 0 };
public:
    CMineThread( const char prefix[10], const char address[32] ) {
        assert( strlen( prefix ) == 9 && ( strlen( address ) == 30 || strlen( address ) == 31 ) );
        strcpy( m_prefix, prefix );
        strcpy( m_address, address );
    }
    void UpdateTargetHash(const char lb_hash[33]) {
        assert( strlen( lb_hash ) == 32 );
        strcpy( m_lb_hash, lb_hash );
    }
    void UpdateTargetDiff(const char target_diff[33]){
        assert( strlen( target_diff ) == 32 );
        strcpy( best_diff, target_diff);
    }
    void UpdateComputedHashesCount( std::uint32_t more ) {
        noso_hash_counter += more;
    }
    void ResetComputedHashesCount() {
        noso_hash_counter = 0;
    }
    
    std::uint32_t GetComputedHashesCount() {
        return noso_hash_counter;
    }
    
    void Mine();
};

char g_miner_address[32] { DEFAULT_MINER_ADDRESS };
std::uint32_t g_miner_id { DEFAULT_MINER_ID };
std::uint32_t g_threads_count { DEFAULT_THREADS_COUNT };
bool mining { false };
bool hashtest { false };

void tokenize(std::string const &str, const char delim, 
            std::vector<std::string> &out) 
{ 
    // construct a stream from the string 
    std::stringstream ss(str); 
 
    std::string s; 
    while (std::getline(ss, s, delim)) { 
        out.push_back(s); 
    } 
}

void reportSpeed(std::vector<std::shared_ptr<CMineThread>> mine_objects){
    std::uint32_t newSpeedCounter = std::accumulate(
                    mine_objects.begin(), mine_objects.end(), 0,
                    []( int a, const std::shared_ptr<CMineThread> &o ) { return a + o->GetComputedHashesCount(); } );
    auto lastEndStamp = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
    std::cout << "SPEEDREPORT " << (newSpeedCounter*1000)/(lastEndStamp-lastSpeedStamp) << std::endl;
    lastSpeedCount = newSpeedCounter;
    lastSpeedStamp = lastEndStamp;
}


int main() {
    bool closeMiner = false;
    std::string line;

    std::vector<std::thread> mine_threads;
    std::vector<std::shared_ptr<CMineThread>> mine_objects;

    while(!closeMiner){
        // Read user input
        std::getline(std::cin, line);
        // Extract params from input       
        std::vector<std::string> lineParams;
        tokenize(line, ' ', lineParams);

        // Evaluate and run command:
        if(lineParams[0] == SPEED_REPORT){
            reportSpeed(mine_objects);
        }else if(lineParams[0] == UPDATE_ADDRESS){
            std::cout << "Not yet implemented..." << std::endl;
        }else if(lineParams[0] == UPDATE_TARGET){
            if(mining){
                std::string newTargetDiff = lineParams[1];

                char target_diff[33];
                strcpy( target_diff, newTargetDiff.c_str());
                
                for ( auto mo : mine_objects ){
                    mo->UpdateTargetDiff(target_diff);
                }
            }
        }else if(lineParams[0] == UPDATE_BLOCK){
            if(mining){
                std::string newTargetHash = lineParams[1];
                std::string newTargetDiff = lineParams[2];

                char lb_hash[33];
                strcpy( lb_hash, newTargetHash.c_str());

                char target_diff[33];
                strcpy( target_diff, newTargetDiff.c_str());
                
                for ( auto mo : mine_objects ){
                    mo->UpdateTargetHash(lb_hash);
                    mo->UpdateTargetDiff(target_diff);
                    mo->ResetComputedHashesCount();
                }
            }
        }else if(lineParams[0] == MINE_COMMAND){
            mining = true;
            hashtest = false;
            int maxCPU = stoi(lineParams[1]); // CPU cores to Use
            int minerID = stoi(lineParams[2]); // MinerID
            std::string miningAddress = lineParams[3]; // MinerAddress
            std::string targetHash = lineParams[4]; // MinerAddress
            std::string targetDiff = lineParams[5]; // MinerAddress
            
            std::cout << "Address: " << miningAddress << std::endl;
            std::cout << "MinerID: " << minerID << std::endl;
            std::cout << "TargetHash: " << targetHash << std::endl;
            std::cout << "TargetDiff: " << targetDiff << std::endl;
            
            const std::string miner_prefix { nosohash_prefix( minerID ) };
            auto miner_thread_prefix = []( int num, const std::string &prefix ) {
                std::string result = std::string { prefix + nosohash_prefix( num ) };
                result.append( 9 - result.size(), '!' );
                return result; 
            };
            std::vector<std::string> miner_thread_prefixes;

            for ( std::uint32_t i = 0; i < maxCPU; i++ ) {
                miner_thread_prefixes.push_back( miner_thread_prefix( i, miner_prefix ) );
            }

            for ( std::uint32_t i = 0; i < maxCPU; i++ ){
                mine_objects.push_back( std::make_shared<CMineThread>( miner_thread_prefixes[i].c_str(), miningAddress.c_str() ) );
            }

            // Set Target and Diff values
            char lb_hash[33];
            strcpy( lb_hash, targetHash.c_str());
            char target_diff[33];
            strcpy( target_diff, targetDiff.c_str());
            for ( auto mo : mine_objects ){ mo->UpdateTargetHash(lb_hash); mo->UpdateTargetDiff(target_diff);}
            
            lastSpeedStamp = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
            for ( std::uint32_t i = 0; i < g_threads_count; i++ ){
                mine_threads.emplace_back( &CMineThread::Mine, mine_objects[i] );
            }
        }else if(lineParams[0] == TEST_HASH_COMMAND){
            // Parse the CPU's number
            g_threads_count = stoi(lineParams[1]);

            // Set Mining and Test values to true
            mining = true;
            hashtest = true;

            std::cout << HASH_TEST_START << std::endl;

            const std::string miner_prefix { nosohash_prefix( g_miner_id ) };
            auto miner_thread_prefix = []( int num, const std::string &prefix ) {
                std::string result = std::string { prefix + nosohash_prefix( num ) };
                result.append( 9 - result.size(), '!' );
                return result; 
            };
            std::vector<std::string> miner_thread_prefixes;

            for ( std::uint32_t i = 0; i < g_threads_count; i++ ) {
                miner_thread_prefixes.push_back( miner_thread_prefix( i, miner_prefix ) );
            }

            std::vector<std::thread> g_mine_threads;
            std::vector<std::shared_ptr<CMineThread>> g_mine_objects;

            for ( std::uint32_t i = 0; i < g_threads_count; i++ ){
                g_mine_objects.push_back( std::make_shared<CMineThread>( miner_thread_prefixes[i].c_str(), g_miner_address ) );
            }

            // Set Dummy Block Values
            std::uint32_t blck_no { 5183 };
            char lb_hash[33];
            std::string target = "00000351DAA852B30A3FA945F1406144";
            strcpy( lb_hash, target.c_str());
            for ( auto mo : g_mine_objects ) mo->UpdateTargetHash( lb_hash );
            
            auto start = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
            for ( std::uint32_t i = 0; i < g_threads_count; i++ ){
                g_mine_threads.emplace_back( &CMineThread::Mine, g_mine_objects[i] );
            }

            for ( std::uint32_t i = 0; i < g_threads_count; i++ ){
                g_mine_threads.at(i).join();
            }          
            
            auto end = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
            
            hashtest = false;
            mining = false;
            
            // Compute Speed
            std::uint32_t computed_hashes_count = std::accumulate(
                    g_mine_objects.begin(), g_mine_objects.end(), 0,
                    []( int a, const std::shared_ptr<CMineThread> &o ) { return a + o->GetComputedHashesCount(); } );

            std::cout << "CPUS " << g_threads_count << " SPEED " << (computed_hashes_count*1000)/(end-start) << std::endl;
            std::cout << HASH_TEST_STOP << std::endl;
        }else if(lineParams[0] == TEST_MINER_COMMAND){
            // Parse the CPU's number
            g_threads_count = stoi(lineParams[1]);
            std::cout << MINER_TEST_START << std::endl;

            const std::string miner_prefix { nosohash_prefix( g_miner_id ) };
            auto miner_thread_prefix = []( int num, const std::string &prefix ) {
                std::string result = std::string { prefix + nosohash_prefix( num ) };
                result.append( 9 - result.size(), '!' );
                return result; 
            };
            std::vector<std::string> miner_thread_prefixes;

            for ( std::uint32_t i = 0; i < g_threads_count; i++ ) {
                miner_thread_prefixes.push_back( miner_thread_prefix( i, miner_prefix ) );
            }

            for ( std::uint32_t cpus = 1; cpus <= g_threads_count; cpus++ ) {
                mining = true;
                hashtest = true;

                std::vector<std::thread> g_mine_threads;
                std::vector<std::shared_ptr<CMineThread>> g_mine_objects;

                for ( std::uint32_t i = 0; i < cpus; i++ ){
                    g_mine_objects.push_back( std::make_shared<CMineThread>( miner_thread_prefixes[i].c_str(), g_miner_address ) );
                }

                // Set Dummy Block Values
                char lb_hash[33];
                std::string targetHash = "00000351DAA852B30A3FA945F1406144";
                strcpy( lb_hash, targetHash.c_str());
                for ( auto mo : g_mine_objects ) mo->UpdateTargetHash( lb_hash );
                
                auto start = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
                for ( std::uint32_t i = 0; i < cpus; i++ ){
                    g_mine_threads.emplace_back( &CMineThread::Mine, g_mine_objects[i] );
                }

                for ( std::uint32_t i = 0; i < cpus; i++ ){
                    g_mine_threads.at(i).join();
                }          
                
                auto end = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
                
                hashtest = false;
                mining = false;
                
                // Compute Speed
                std::uint32_t computed_hashes_count = std::accumulate(
                        g_mine_objects.begin(), g_mine_objects.end(), 0,
                        []( int a, const std::shared_ptr<CMineThread> &o ) { return a + o->GetComputedHashesCount(); } );

                std::cout << "CPUS " << cpus << " SPEED " << (computed_hashes_count*1000)/(end-start) << std::endl;
            }
            std::cout << MINER_TEST_STOP << std::endl;
        }else if(lineParams[0] == EXIT_COMMAND){
            closeMiner = true;
            std::cout << "Killed command triggered, exiting... " << std::endl;            
        }else{
            std::cout << "Unknown Command: " << lineParams[0] << std::endl;
        }
    }            
    return 0;
}

#define COUT_NOSO_TIME std::cout << NOSO_TIMESTAMP << "(" << std::setfill('0') << std::setw(3) << NOSO_BLOCK_AGE << "))"

std::mutex mtx_print;

void CMineThread::Mine() {
    CNosoHasher noso_hasher( m_prefix, m_address );
    this->ResetComputedHashesCount();
    char best_base[19];
    char best_hash[33];
    char sent_diff[33] { NOSO_MAX_DIFF };
        
    while (mining) {
        const char *base { noso_hasher.GetBase( noso_hash_counter++ ) };
        const char *hash { noso_hasher.GetHash() };
        const char *diff { noso_hasher.GetDiff( m_lb_hash ) };

        if ( strcmp( diff, best_diff ) < 0 ) {
            strcpy( best_diff, diff );
            strcpy( best_hash, hash );
            strcpy( best_base, base );
        }
        
        if ( strcmp( best_diff, sent_diff ) < 0 ) {
            //std::cout << "SOLUTION " << m_blck_no << " " << best_base << " " << best_hash << " " << best_diff  << std::endl;
            strcpy( sent_diff, best_diff );
        }

        if(hashtest && noso_hash_counter >= NOSOHASH_TEST_MAX){
            break;
        }
    }

    //this->UpdateComputedHashesCount( noso_hash_counter );
    //m_blck_no = 0;
}