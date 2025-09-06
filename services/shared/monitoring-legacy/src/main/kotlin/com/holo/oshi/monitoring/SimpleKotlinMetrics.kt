package com.holo.oshi.monitoring

import kotlinx.coroutines.*
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.measureTimedValue

/**
 * Simple Kotlin Metrics
 * 
 * Micrometer 호환성 포기하고 순수 코틀린:
 * - Zero External Dependencies Issues
 * - 100% Kotlin Native
 * - Thread-Safe Operations
 * - Memory Efficient
 * - Export to Prometheus Format
 * - Type-Safe Metrics
 */
private val logger = KotlinLogging.logger {}

@Component
class SimpleKotlinMetrics {
    
    // Internal Storage
    
    private val counters = ConcurrentHashMap<String, AtomicLong>()
    private val gauges = ConcurrentHashMap<String, AtomicReference<Double>>()
    private val timers = ConcurrentHashMap<String, MutableList<Long>>()
    
    // ============ Counter Operations ============
    
    fun incrementCounter(name: String, tags: Map<String, String> = emptyMap()) {
        val metricKey = buildMetricKey(name, tags)
        counters.computeIfAbsent(metricKey) { AtomicLong(0) }.incrementAndGet()
        
        logger.debug { "📊 Counter 증가: $metricKey" }
    }
    
    fun incrementCounterBy(name: String, amount: Double, tags: Map<String, String> = emptyMap()) {
        val metricKey = buildMetricKey(name, tags)
        counters.computeIfAbsent(metricKey) { AtomicLong(0) }.addAndGet(amount.toLong())
    }
    
    // ============ Gauge Operations ============
    
    fun recordGauge(name: String, value: Number, tags: Map<String, String> = emptyMap()) {
        val metricKey = buildMetricKey(name, tags)
        gauges.computeIfAbsent(metricKey) { AtomicReference(0.0) }.set(value.toDouble())
        
        logger.debug { "📊 Gauge 기록: $metricKey = $value" }
    }
    
    // ============ Timer Operations ============
    
    suspend fun <T> timeIt(name: String, tags: Map<String, String> = emptyMap(), block: suspend () -> T): T {
        val (result, duration) = measureTimedValue { block() }
        
        val metricKey = buildMetricKey(name, tags)
        timers.computeIfAbsent(metricKey) { mutableListOf() }
            .add(duration.inWholeNanoseconds)
        
        logger.debug { "⏱️ Timer 기록: $metricKey = ${duration.inWholeMilliseconds}ms" }
        
        return result
    }
    
    // ============ Metrics Export (Prometheus Format) ============
    
    fun exportMetrics(): String = buildString {
        appendLine("# Holo-Oshi Pure Kotlin Metrics")
        appendLine("# HELP Pure Kotlin metrics from holo-oshi-finder")
        appendLine("# TYPE holo_metrics untyped")
        
        // Counters
        counters.forEach { (key, value) ->
            appendLine("$key ${value.get()}")
        }
        
        // Gauges  
        gauges.forEach { (key, value) ->
            appendLine("$key ${value.get()}")
        }
        
        // Timers (평균값)
        timers.forEach { (key, measurements) ->
            if (measurements.isNotEmpty()) {
                val avgMs = measurements.map { it / 1_000_000 }.average()
                appendLine("${key}_avg_ms $avgMs")
                appendLine("${key}_count ${measurements.size}")
            }
        }
    }
    
    // ============ Statistics ============
    
    fun getCounterValue(name: String, tags: Map<String, String> = emptyMap()): Long {
        val metricKey = buildMetricKey(name, tags)
        return counters[metricKey]?.get() ?: 0L
    }
    
    fun getGaugeValue(name: String, tags: Map<String, String> = emptyMap()): Double {
        val metricKey = buildMetricKey(name, tags)
        return gauges[metricKey]?.get() ?: 0.0
    }
    
    fun getTimerStats(name: String, tags: Map<String, String> = emptyMap()): TimerStats? {
        val metricKey = buildMetricKey(name, tags)
        val measurements = timers[metricKey] ?: return null
        
        if (measurements.isEmpty()) return null
        
        val sortedMs = measurements.map { it / 1_000_000 }.sorted()
        return TimerStats(
            count = measurements.size,
            avgMs = sortedMs.average(),
            minMs = sortedMs.first(),
            maxMs = sortedMs.last(),
            p50Ms = percentile(sortedMs, 0.5),
            p95Ms = percentile(sortedMs, 0.95),
            p99Ms = percentile(sortedMs, 0.99)
        )
    }
    
    // ============ Helper Functions ============
    
    private fun buildMetricKey(name: String, tags: Map<String, String>): String {
        if (tags.isEmpty()) return name
        val tagString = tags.entries.joinToString(",") { "${it.key}=${it.value}" }
        return "${name}{$tagString}"
    }
    
    private fun percentile(sorted: List<Long>, percentile: Double): Long {
        if (sorted.isEmpty()) return 0L
        val index = ((sorted.size - 1) * percentile).toInt()
        return sorted[index.coerceIn(0, sorted.size - 1)]
    }
    
    // ============ Health Check ============
    
    fun getMetricsHealth(): MetricsHealth {
        return MetricsHealth(
            totalCounters = counters.size,
            totalGauges = gauges.size,
            totalTimers = timers.size,
            isHealthy = true,
            lastUpdated = kotlinx.datetime.Clock.System.now()
        )
    }
}

// ============ Data Classes ============

data class TimerStats(
    val count: Int,
    val avgMs: Double,
    val minMs: Long,
    val maxMs: Long,
    val p50Ms: Long,
    val p95Ms: Long,  
    val p99Ms: Long
)

data class MetricsHealth(
    val totalCounters: Int,
    val totalGauges: Int,
    val totalTimers: Int,
    val isHealthy: Boolean,
    val lastUpdated: kotlinx.datetime.Instant
)

// ============ Extension Functions (Pure Kotlin Style) ============

/**
 * 홀로라이브 도메인 특화 확장함수
 */
fun SimpleKotlinMetrics.recordMemberSearch(branch: String?, resultCount: Int) {
    val tags = if (branch != null) mapOf("branch" to branch) else emptyMap()
    incrementCounter("holo.member.search.total", tags)
    recordGauge("holo.member.search.results", resultCount, tags)
}

fun SimpleKotlinMetrics.recordVectorSearch(similarity: Double, dimension: Int) {
    recordGauge("holo.vector.similarity.avg", similarity)
    recordGauge("holo.vector.dimension", dimension)
    incrementCounter("holo.vector.search.total")
}

fun SimpleKotlinMetrics.recordLLMAnalysis(model: String, tokens: Int, latencyMs: Long) {
    val tags = mapOf("model" to model)
    incrementCounterBy("holo.llm.tokens.consumed", tokens.toDouble(), tags)
    recordGauge("holo.llm.latency.ms", latencyMs, tags)
    incrementCounter("holo.llm.analysis.total", tags)
}

/**
 * 코루틴 성능 측정 확장함수
 */
suspend fun <T> SimpleKotlinMetrics.measureSuspendFunction(
    name: String,
    tags: Map<String, String> = emptyMap(),
    block: suspend () -> T
): T = timeIt(name, tags, block)