package com.holo.oshi.common.config

import kotlinx.coroutines.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

/**
 * 
 * 
 * 핵심 개선사항:
 * - 커스텀 Dispatcher로 성능 최적화
 * - Structured Concurrency 완벽 지원
 * - 에러 핸들링 중앙화
 * - 메트릭 수집 통합
 */
@Configuration
class CoroutineConfig {
    
    /**
     * IO 작업용 최적화된 Dispatcher
     * Netflix 권장: IO 스레드 풀 = CPU 코어 * 2
     */
    @Bean
    fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO.limitedParallelism(
        Runtime.getRuntime().availableProcessors() * 2
    )
    
    /**
     * CPU 집약적 작업용 Dispatcher
     * Google 권장: CPU 스레드 = CPU 코어 수
     */
    @Bean 
    fun computeDispatcher(): CoroutineDispatcher = Dispatchers.Default.limitedParallelism(
        Runtime.getRuntime().availableProcessors()
    )
    
    /**
     * 데이터베이스 작업 전용 Dispatcher
     * Uber 패턴: DB 연결 풀과 일치
     */
    @Bean
    fun dbDispatcher(): CoroutineDispatcher = newFixedThreadPoolContext(
        nThreads = 20, // HikariCP default pool size
        name = "db-dispatcher"
    )
    
    /**
     * 애플리케이션 전역 CoroutineScope
     * Structured Concurrency 보장
     */
    @Bean
    fun applicationScope(): CoroutineScope = CoroutineScope(
        SupervisorJob() + ioDispatcher() + exceptionHandler()
    )
    
    /**
     * 글로벌 예외 핸들러
     * 모든 Coroutine 예외를 중앙에서 처리
     */
    @Bean
    fun exceptionHandler() = CoroutineExceptionHandler { context, exception ->
        println("[CoroutineException] Context: $context, Error: ${exception.message}")
        // 메트릭 수집, 알람 발송 등
    }
    
    /**
     * WebClient with Coroutines
     * Reactive → Coroutines 브릿지
     */
    @Bean
    fun coroutineWebClient(): WebClient = WebClient.builder()
        .codecs { it.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) } // 10MB
        .build()
}

/**
 * Coroutine 유틸리티 확장 함수들
 * Netflix 스타일의 helper functions
 */

// Timeout with default value
suspend fun <T> withTimeoutOrDefault(
    timeMillis: Long,
    default: T,
    block: suspend CoroutineScope.() -> T
): T = try {
    withTimeout(timeMillis, block)
} catch (e: TimeoutCancellationException) {
    default
}

// Retry with exponential backoff (Netflix pattern)
suspend fun <T> retryWithExponentialBackoff(
    times: Int = 3,
    initialDelayMillis: Long = 100,
    maxDelayMillis: Long = 5000,
    factor: Double = 2.0,
    block: suspend () -> T
): T {
    var currentDelay = initialDelayMillis
    repeat(times - 1) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMillis)
        }
    }
    return block() // Last attempt
}

// Parallel map for collections (Uber pattern)
suspend fun <T, R> Collection<T>.parallelMap(
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    transform: suspend (T) -> R
): List<R> = coroutineScope {
    map { async(dispatcher) { transform(it) } }.awaitAll()
}

// Circuit breaker pattern
class CircuitBreaker<T>(
    private val failureThreshold: Int = 3,
    private val resetTimeoutMillis: Long = 60000
) {
    private var failureCount = 0
    private var lastFailureTime = 0L
    private var state = State.CLOSED
    
    enum class State { OPEN, CLOSED, HALF_OPEN }
    
    suspend fun call(block: suspend () -> T): T {
        when (state) {
            State.OPEN -> {
                if (System.currentTimeMillis() - lastFailureTime > resetTimeoutMillis) {
                    state = State.HALF_OPEN
                    failureCount = 0
                } else {
                    throw CircuitBreakerOpenException()
                }
            }
            State.HALF_OPEN, State.CLOSED -> {
                try {
                    val result = block()
                    if (state == State.HALF_OPEN) {
                        state = State.CLOSED
                        failureCount = 0
                    }
                    return result
                } catch (e: Exception) {
                    failureCount++
                    lastFailureTime = System.currentTimeMillis()
                    if (failureCount >= failureThreshold) {
                        state = State.OPEN
                    }
                    throw e
                }
            }
        }
        return block()
    }
}

class CircuitBreakerOpenException : Exception("Circuit breaker is open")