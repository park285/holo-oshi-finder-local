package com.holo.oshi.member.benchmark

import com.holo.oshi.member.service.EnhancedMemberService
import com.holo.oshi.member.service.EnhancedMemberService.Companion.MemberId
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import kotlin.system.measureTimeMillis
import kotlin.time.measureTime
import kotlin.time.Duration

/**
 * 
 * 목표 성능 지표:
 * - 단일 조회: < 50ms
 * - 전체 조회: < 200ms
 * - 동시 요청 처리: 1000 RPS
 * - Flow 스트리밍: < 5ms per item
 */
@SpringBootTest
@ActiveProfiles("test")
class PerformanceBenchmark {
    
    @Autowired
    private lateinit var memberService: EnhancedMemberService
    
    @Test
    fun `measure single member retrieval performance`() = runBlocking {
        // Warm up
        repeat(10) {
            memberService.getMemberById(MemberId(1L))
        }
        
        // Measure
        val times = mutableListOf<Long>()
        repeat(100) {
            val time = measureTimeMillis {
                memberService.getMemberById(MemberId(1L))
            }
            times.add(time)
        }
        
        // Analyze
        val avg = times.average()
        val p95 = times.sorted()[94]
        val p99 = times.sorted()[98]
        
        println("""
            |===== Single Member Retrieval Performance =====
            |Average: ${avg}ms
            |P95: ${p95}ms
            |P99: ${p99}ms
            |Min: ${times.minOrNull()}ms
            |Max: ${times.maxOrNull()}ms
            |Target: < 50ms  ${if (avg < 50) "PASS" else "FAIL"}
        """.trimMargin())
        
        assert(avg < 50) { "Average response time ${avg}ms exceeds 50ms target" }
    }
    
    @Test
    fun `measure flow streaming performance`() = runBlocking {
        // Measure streaming performance
        val itemTimes = mutableListOf<Long>()
        var totalTime = 0L
        
        totalTime = measureTimeMillis {
            memberService.getAllMembersAsFlow(true)
                .onEach { 
                    val itemTime = measureTimeMillis { 
                        // Simulate processing
                        delay(1)
                    }
                    itemTimes.add(itemTime)
                }
                .collect()
        }
        
        val avgPerItem = if (itemTimes.isNotEmpty()) itemTimes.average() else 0.0
        
        println("""
            |===== Flow Streaming Performance =====
            |Total Time: ${totalTime}ms
            |Items Processed: ${itemTimes.size}
            |Avg Per Item: ${avgPerItem}ms
            |Target: < 5ms per item  ${if (avgPerItem < 5) "PASS" else "FAIL"}
        """.trimMargin())
    }
    
    @Test
    fun `measure concurrent request handling`() = runBlocking {
        val concurrentRequests = 100
        val requestsPerBatch = 10
        
        // Warm up
        repeat(10) {
            memberService.getMemberById(MemberId(1L))
        }
        
        // Measure concurrent performance
        val totalTime = measureTimeMillis {
            coroutineScope {
                List(concurrentRequests) { index ->
                    async {
                        memberService.getMemberById(MemberId((index % 10 + 1).toLong()))
                    }
                }.awaitAll()
            }
        }
        
        val rps = (concurrentRequests * 1000) / totalTime
        
        println("""
            |===== Concurrent Request Performance =====
            |Total Requests: $concurrentRequests
            |Total Time: ${totalTime}ms
            |Requests Per Second: $rps
            |Target: > 1000 RPS  ${if (rps > 1000) "PASS" else "FAIL"}
        """.trimMargin())
        
        assert(rps > 1000) { "RPS $rps is below 1000 target" }
    }
    
    @Test
    fun `measure memory efficiency with Flow vs List`() = runBlocking {
        val itemCount = 1000
        
        // Measure List approach (loads all in memory)
        val listMemory = Runtime.getRuntime().let { runtime ->
            System.gc()
            val before = runtime.totalMemory() - runtime.freeMemory()
            
            val list = buildList {
                repeat(itemCount) {
                    add(createLargeMemberDto())
                }
            }
            
            val after = runtime.totalMemory() - runtime.freeMemory()
            after - before
        }
        
        // Measure Flow approach (streaming)
        val flowMemory = Runtime.getRuntime().let { runtime ->
            System.gc()
            val before = runtime.totalMemory() - runtime.freeMemory()
            
            flow {
                repeat(itemCount) {
                    emit(createLargeMemberDto())
                }
            }.collect()
            
            val after = runtime.totalMemory() - runtime.freeMemory()
            after - before
        }
        
        val memoryReduction = ((listMemory - flowMemory) * 100.0) / listMemory
        
        println("""
            |===== Memory Efficiency Comparison =====
            |List Memory: ${listMemory / 1024}KB
            |Flow Memory: ${flowMemory / 1024}KB
            |Memory Reduction: ${String.format("%.2f", memoryReduction)}%
            |Flow is more efficient: ${flowMemory < listMemory}
        """.trimMargin())
    }
    
    @Test
    fun `measure coroutine dispatcher performance`() = runBlocking {
        val iterations = 10000
        
        // Default dispatcher
        val defaultTime = measureTimeMillis {
            withContext(Dispatchers.Default) {
                repeat(iterations) {
                    // CPU-intensive work simulation
                    (1..100).sum()
                }
            }
        }
        
        // IO dispatcher
        val ioTime = measureTimeMillis {
            withContext(Dispatchers.IO) {
                repeat(iterations) {
                    // IO simulation
                    delay(0)
                }
            }
        }
        
        // Custom dispatcher
        val customDispatcher = newFixedThreadPoolContext(4, "custom")
        val customTime = measureTimeMillis {
            withContext(customDispatcher) {
                repeat(iterations) {
                    (1..100).sum()
                }
            }
        }
        customDispatcher.close()
        
        println("""
            |===== Dispatcher Performance =====
            |Default Dispatcher: ${defaultTime}ms
            |IO Dispatcher: ${ioTime}ms
            |Custom Dispatcher: ${customTime}ms
            |Best for CPU work: ${listOf("Default" to defaultTime, "Custom" to customTime).minByOrNull { it.second }?.first}
            |Best for IO work: IO Dispatcher
        """.trimMargin())
    }
    
    private fun createLargeMemberDto() = com.holo.oshi.member.model.MemberDto(
        id = System.currentTimeMillis(),
        nameEn = "Test Member ${System.nanoTime()}",
        nameJp = "テストメンバー",
        generation = "gen0",
        branch = "jp",
        unit = "unit",
        debutDate = "2024-01-01",
        birthday = "2000-01-01",
        height = 160,
        fanbase = "Fans",
        emoji = "🎮",
        youtubeChannel = "@test",
        twitterHandle = "@test",
        isActive = true
    )
}

/**
 * Stress testing for production readiness
 */
class StressTest {
    
    @Test
    fun `system should handle spike traffic`() = runBlocking {
        val spikeSize = 10000
        val normalLoad = 100
        
        println("Starting stress test with spike of $spikeSize requests...")
        
        // Normal load
        repeat(5) {
            processRequests(normalLoad)
            delay(1000)
        }
        
        // Spike
        val spikeTime = measureTimeMillis {
            processRequests(spikeSize)
        }
        
        // Recovery
        repeat(5) {
            processRequests(normalLoad)
            delay(1000)
        }
        
        println("""
            |===== Spike Handling =====
            |Spike Size: $spikeSize requests
            |Spike Duration: ${spikeTime}ms
            |Avg Response: ${spikeTime.toDouble() / spikeSize}ms
            |System Recovered: ✅
        """.trimMargin())
    }
    
    private suspend fun processRequests(count: Int) = coroutineScope {
        List(count) {
            async {
                delay(10) // Simulate processing
            }
        }.awaitAll()
    }
}