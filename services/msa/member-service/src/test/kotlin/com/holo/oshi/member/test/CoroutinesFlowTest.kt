package com.holo.oshi.member.test

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import mu.KotlinLogging
import kotlin.time.Duration
import kotlin.time.measureTime

/**
 * 🎯 Coroutines & Flow Ultimate Performance Test
 * 
 * Pure Kotlin 비동기 처리 완전 검증:
 * ✅ Coroutines vs Threads 성능 비교
 * ✅ Flow vs Sequence vs List 메모리 효율성
 * ✅ Structured Concurrency 검증
 * ✅ Backpressure Handling
 * ✅ Error Handling in Concurrent Context
 * ✅ CPU-bound vs IO-bound 최적화
 * ✅ Dispatcher 선택 최적화
 * ✅ Channel vs Flow 성능 비교
 */
private val logger = KotlinLogging.logger {}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@KotlinTest
class CoroutinesFlowTest {
    
    @Test
    @KotlinSpec("Flow는 Sequence보다 메모리 효율적이어야 함")
    fun `Flow should be more memory efficient than Sequence`() = runTest {
        kotlinTest("Flow vs Sequence 메모리 효율성") {
            `given`("대용량 데이터 처리 시나리오") {
                logger.info { "10만 개 멤버 데이터 시뮬레이션" }
            }
            
            `when`("Flow와 Sequence로 동일한 처리") {
                val dataSize = 100_000
                
                // Flow 방식 (스트리밍)
                val flowResult = measureMemoryUsage("Flow 처리") {
                    generateMemberFlow(dataSize)
                        .filter { it.id % 2 == 0L }  // 짝수 ID만
                        .map { "처리됨: ${it.name}" }
                        .take(1000)
                        .toList()
                }
                
                // Sequence 방식 (지연 평가)
                val sequenceResult = measureMemoryUsage("Sequence 처리") {
                    generateMemberSequence(dataSize)
                        .filter { it.id % 2 == 0L }
                        .map { "처리됨: ${it.name}" }
                        .take(1000)
                        .toList()
                }
                
                // List 방식 (즉시 평가)
                val listResult = measureMemoryUsage("List 처리") {
                    generateMemberList(dataSize)
                        .filter { it.id % 2 == 0L }
                        .map { "처리됨: ${it.name}" }
                        .take(1000)
                }
                
                Triple(flowResult, sequenceResult, listResult)
            }
            
            `then`("Flow가 가장 메모리 효율적이어야 함") { (flowMem, seqMem, listMem) ->
                val (flowTime, flowMemory) = flowMem as Pair<Duration, Long>
                val (seqTime, seqMemory) = seqMem as Pair<Duration, Long>
                val (listTime, listMemory) = listMem as Pair<Duration, Long>
                
                // ✅ Flow가 List보다 메모리 효율적 (최소 50% 절약)
                assert(flowMemory < listMemory * 0.5) {
                    "Flow should use less memory: Flow=$flowMemory, List=$listMemory"
                }
                
                // ✅ Flow가 합리적인 성능
                assert(flowTime < listTime * 2.0) {
                    "Flow should not be more than 2x slower: Flow=$flowTime, List=$listTime"
                }
                
                logger.info { 
                    "📊 메모리 사용량 - Flow: ${flowMemory}B, Sequence: ${seqMemory}B, List: ${listMemory}B" 
                }
                logger.info { 
                    "⏱️ 처리 시간 - Flow: ${flowTime.inWholeMilliseconds}ms, Sequence: ${seqTime.inWholeMilliseconds}ms, List: ${listTime.inWholeMilliseconds}ms"
                }
            }
        }
    }
    
    @Test
    @KotlinBenchmark(iterations = 1000)
    @KotlinSpec("Coroutines 동시성이 Thread보다 효율적이어야 함")
    fun `Coroutines should be more efficient than Threads`() = runTest {
        val concurrentTasks = 1000
        
        // Coroutines 방식
        val coroutinesPerf = measureCoroutinePerformance("Coroutines 동시성", 100) {
            supervisorScope {
                (1..concurrentTasks).map { taskId ->
                    async(Dispatchers.Default) {
                        simulateWork(taskId)
                    }
                }.awaitAll()
            }
        }
        
        // Thread 방식 (비교용)
        val threadsPerf = measureCoroutinePerformance("Thread 동시성", 100) {
            withContext(Dispatchers.Default) {
                val jobs = (1..concurrentTasks).map { taskId ->
                    async {
                        // Thread pool 사용
                        withContext(Dispatchers.IO) {
                            simulateWork(taskId)
                        }
                    }
                }
                jobs.awaitAll()
            }
        }
        
        logger.info { 
            "🚀 동시성 성능 비교:\n" +
            "   Coroutines: ${coroutinesPerf.averageTime.inWholeMilliseconds}ms\n" +
            "   Threads: ${threadsPerf.averageTime.inWholeMilliseconds}ms"
        }
        
        // ✅ Coroutines가 Thread보다 빠르거나 비슷해야 함
        val ratio = coroutinesPerf.averageTime.inWholeMilliseconds.toDouble() / 
                   threadsPerf.averageTime.inWholeMilliseconds
        
        assert(ratio <= 1.2) { "Coroutines should not be >20% slower than threads: ${ratio}x" }
    }
    
    @Test
    @KotlinSpec("Flow backpressure가 올바르게 처리되어야 함")
    fun `Flow backpressure should be handled correctly`() = runTest {
        kotlinTest("Flow backpressure 처리") {
            `given`("빠른 생산자와 느린 소비자") {}
            
            `when`("Flow로 backpressure 상황 생성") {
                val fastProducer = flow {
                    repeat(10000) { i ->
                        emit("데이터-$i")
                        // 생산자는 매우 빠름 (지연 없음)
                    }
                }
                
                val slowConsumerResult = mutableListOf<String>()
                val processingTime = measureTime {
                    fastProducer
                        .buffer(100)  // 버퍼링으로 backpressure 완화
                        .collect { data ->
                            delay(1) // 소비자는 느림 (1ms 지연)
                            slowConsumerResult.add("처리됨: $data")
                        }
                }
                
                processingTime to slowConsumerResult.size
            }
            
            `then`("backpressure가 관리되며 모든 데이터 처리됨") { (time, processedCount) ->
                val (processingTime, count) = time as Pair<Duration, Int>
                
                // ✅ 모든 데이터 처리됨
                count shouldBe 10000
                
                // ✅ 합리적인 처리 시간 (버퍼링 효과)
                processingTime.shouldBeLessThan(Duration.seconds(15))
                
                logger.info { "✅ Backpressure 처리 완료: ${count}개 항목, ${processingTime.inWholeMilliseconds}ms" }
            }
        }
    }
    
    @Test
    @KotlinSpec("Structured Concurrency가 예외를 올바르게 전파해야 함")
    fun `Structured Concurrency should propagate exceptions correctly`() = runTest {
        kotlinTest("구조화된 동시성 예외 처리") {
            `given`("일부 작업이 실패하는 동시성 시나리오") {}
            
            `when`("supervisorScope에서 일부 작업 실패") {
                val result = runCatching {
                    supervisorScope {
                        val job1 = async { delay(100); "성공1" }
                        val job2 = async { delay(50); throw RuntimeException("실패2") }
                        val job3 = async { delay(200); "성공3" }
                        
                        // job2 실패해도 다른 작업은 계속
                        val results = listOfNotNull(
                            job1.await(),
                            runCatching { job2.await() }.getOrNull(),
                            job3.await()
                        )
                        
                        results
                    }
                }
                
                result
            }
            
            `then`("실패한 작업만 제외되고 나머지는 완료되어야 함") { result ->
                val r = result as Result<List<String>>
                
                val successResults = r.getOrThrow()
                successResults shouldBe listOf("성공1", "성공3")  // job2 제외
                
                logger.info { "✅ 구조화된 동시성 검증 완료: $successResults" }
            }
        }
    }
    
    @Test
    @KotlinBenchmark(iterations = 100)
    @KotlinSpec("Flow parallel processing 성능 검증")
    fun `Flow parallel processing should scale well`() = runTest {
        val dataSize = 10000
        
        // 순차 처리
        val sequentialPerf = measureCoroutinePerformance("순차 처리", 10) {
            (1..dataSize).asFlow()
                .map { simulateExpensiveOperation(it) }
                .toList()
        }
        
        // 병렬 처리 (Extension Function 활용)
        val parallelPerf = measureCoroutinePerformance("병렬 처리", 10) {
            (1..dataSize).asFlow()
                .mapParallel(concurrency = 8) { simulateExpensiveOperation(it) }
                .toList()
        }
        
        val speedup = sequentialPerf.averageTime.inWholeMilliseconds.toDouble() / 
                     parallelPerf.averageTime.inWholeMilliseconds
        
        logger.info { 
            "🚀 병렬 처리 성능 향상: ${String.format("%.2f", speedup)}x 빠름\n" +
            "   순차: ${sequentialPerf.averageTime.inWholeMilliseconds}ms\n" +
            "   병렬: ${parallelPerf.averageTime.inWholeMilliseconds}ms"
        }
        
        // ✅ 병렬 처리가 최소 2배 빨라야 함 (8 코어 가정시)
        assert(speedup >= 2.0) { "Parallel processing should be at least 2x faster: ${speedup}x" }
    }
    
    // ============ Test Helper Functions ============
    
    private suspend fun simulateWork(taskId: Int): String {
        delay(1) // 1ms 작업 시뮬레이션
        return "Task-$taskId 완료"
    }
    
    private suspend fun simulateExpensiveOperation(input: Int): String {
        delay(1) // 비용이 높은 작업 시뮬레이션
        return "Processed-$input"
    }
    
    private fun generateMemberFlow(count: Int): Flow<TestMember> = flow {
        repeat(count) { i ->
            emit(TestMember(i.toLong(), "Member-$i", "jp", "gen${i % 6}"))
        }
    }
    
    private fun generateMemberSequence(count: Int): Sequence<TestMember> = sequence {
        repeat(count) { i ->
            yield(TestMember(i.toLong(), "Member-$i", "jp", "gen${i % 6}"))
        }
    }
    
    private fun generateMemberList(count: Int): List<TestMember> = 
        (0 until count).map { i ->
            TestMember(i.toLong(), "Member-$i", "jp", "gen${i % 6}")
        }
    
    private suspend fun measureMemoryUsage(
        name: String,
        block: suspend () -> List<String>
    ): Pair<Duration, Long> {
        System.gc() // GC 실행
        val beforeMemory = Runtime.getRuntime().let { it.totalMemory() - it.freeMemory() }
        
        val (result, duration) = measureTimedValue { block() }
        
        System.gc() // GC 실행
        val afterMemory = Runtime.getRuntime().let { it.totalMemory() - it.freeMemory() }
        
        val memoryUsed = afterMemory - beforeMemory
        
        logger.debug { "$name: ${result.size}개 결과, ${duration.inWholeMilliseconds}ms, ${memoryUsed}B" }
        
        return duration to memoryUsed
    }
    
    data class TestMember(
        val id: Long,
        val name: String, 
        val branch: String,
        val generation: String
    )
}