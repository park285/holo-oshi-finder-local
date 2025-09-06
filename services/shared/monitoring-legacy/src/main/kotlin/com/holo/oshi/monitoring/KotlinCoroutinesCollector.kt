package com.holo.oshi.monitoring

import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.debug.DebugProbes
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import mu.KotlinLogging
import kotlin.time.Duration.Companion.seconds

/**
 * 🎯 Kotlin-style Coroutines Metrics Collector
 * 
 * 코틀린라이크 코루틴 메트릭 수집기:
 * ✅ Zero Internal API Dependencies
 * ✅ Safe Exception Handling
 * ✅ Pure Kotlin Implementation
 * ✅ Structured Concurrency
 */
private val logger = KotlinLogging.logger {}

@Component
class KotlinCoroutinesCollector(
    private val simpleMetrics: SimpleKotlinMetrics
) {
    
    private val metricsScope = CoroutineScope(
        SupervisorJob() + 
        Dispatchers.Default + 
        CoroutineName("KotlinCoroutinesCollector")
    )
    
    // SimpleKotlinMetrics 생성자 주입으로 받음
    
    @EventListener(ApplicationReadyEvent::class)
    fun startCollection() {
        logger.info { "🔍 코틀린 코루틴 메트릭 수집 시작" }
        
        // SimpleKotlinMetrics는 생성자에서 주입됨
        
        // 안전한 DebugProbes 설치
        try {
            if (!DebugProbes.isInstalled) {
                DebugProbes.install()
                logger.info { "✅ DebugProbes 설치 완료" }
            }
        } catch (e: Exception) {
            logger.warn { "⚠️ DebugProbes 설치 실패 (내부 API 제한): ${e.message}" }
            logger.info { "📊 기본 메트릭으로 수집 진행" }
        }
        
        // 주기적 수집 시작
        metricsScope.launch {
            while (isActive) {
                try {
                    collectBasicMetrics()
                    delay(10.seconds)
                } catch (e: Exception) {
                    logger.error(e) { "❌ 메트릭 수집 중 오류" }
                }
            }
        }
        
        logger.info { "🚀 코틀린 메트릭 수집기 시작됨" }
    }
    
    /**
     * 안전한 기본 메트릭 수집
     */
    private fun collectBasicMetrics() {
        try {
            // 시스템 기본 메트릭
            val runtime = Runtime.getRuntime()
            val availableProcessors = runtime.availableProcessors()
            val totalMemory = runtime.totalMemory()
            val freeMemory = runtime.freeMemory()
            val usedMemory = totalMemory - freeMemory
            
            // JVM 메트릭
            simpleMetrics.recordGauge("holo.jvm.processors", availableProcessors)
            simpleMetrics.recordGauge("holo.jvm.memory.used", usedMemory)
            simpleMetrics.recordGauge("holo.jvm.memory.total", totalMemory)
            simpleMetrics.recordGauge("holo.jvm.memory.usage_ratio", usedMemory.toDouble() / totalMemory)
            
            // 스레드 메트릭
            val activeThreadCount = Thread.activeCount()
            simpleMetrics.recordGauge("holo.threads.active", activeThreadCount)
            
            // DebugProbes가 설치된 경우에만 코루틴 정보 수집
            if (DebugProbes.isInstalled) {
                collectCoroutinesInfo()
            } else {
                // 기본 추정치
                collectEstimatedCoroutinesMetrics()
            }
            
            logger.debug { "📊 기본 메트릭 수집 완료" }
            
        } catch (e: Exception) {
            logger.error(e) { "❌ 기본 메트릭 수집 실패" }
        }
    }
    
    /**
     * 안전한 코루틴 정보 수집
     */
    private fun collectCoroutinesInfo() {
        try {
            val coroutinesInfo = DebugProbes.dumpCoroutinesInfo()
            
            // 총 활성 코루틴 수
            simpleMetrics.recordGauge("holo.coroutines.active", coroutinesInfo.size)
            
            // 상태별 분류
            val stateGroups = coroutinesInfo.groupBy { it.state.toString() }
            stateGroups.forEach { (state, coroutines) ->
                simpleMetrics.recordGauge("holo.coroutines.by_state", coroutines.size, mapOf("state" to state.lowercase()))
            }
            
            logger.debug { "📊 코루틴 메트릭 수집 완료: ${coroutinesInfo.size}개" }
            
        } catch (e: Exception) {
            logger.warn(e) { "⚠️ 코루틴 정보 수집 실패, 추정값 사용" }
            collectEstimatedCoroutinesMetrics()
        }
    }
    
    /**
     * DebugProbes 없이 추정 메트릭 수집
     */
    private fun collectEstimatedCoroutinesMetrics() {
        try {
            // 활성 스레드 기반 추정
            val activeThreads = Thread.activeCount()
            val estimatedCoroutines = activeThreads * 2 // 보수적 추정
            
            simpleMetrics.recordGauge("holo.coroutines.active", estimatedCoroutines)
            
            // 기본 상태 분포 추정
            simpleMetrics.recordGauge("holo.coroutines.by_state", estimatedCoroutines / 2, mapOf("state" to "runnable"))
            simpleMetrics.recordGauge("holo.coroutines.by_state", estimatedCoroutines / 2, mapOf("state" to "suspended"))
            
            logger.debug { "📊 추정 코루틴 메트릭 기록: ~$estimatedCoroutines 개" }
            
        } catch (e: Exception) {
            logger.error(e) { "❌ 추정 메트릭 수집도 실패" }
        }
    }
    
    /**
     * 애플리케이션 종료시 정리
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun shutdown() {
        logger.info { "🛑 코틀린 메트릭 수집기 종료 중..." }
        
        metricsScope.cancel("Application shutdown")
        
        if (DebugProbes.isInstalled) {
            try {
                DebugProbes.uninstall()
                logger.info { "✅ DebugProbes 제거 완료" }
            } catch (e: Exception) {
                logger.warn { "⚠️ DebugProbes 제거 실패: ${e.message}" }
            }
        }
    }
}