package com.holo.oshi.llmanalyzer.controller

import mu.KotlinLogging
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.time.measureTime

@RestController
class HealthController {
    
    companion object {
        private val logger = KotlinLogging.logger {}
    }
    
    @GetMapping("/health")
    fun health(): Map<String, Any> {
        val responseTime = measureTime {
            logger.info { "LLM Analyzer Service 헬스체크 요청 처리 중" }
        }
        
        return mapOf(
            "status" to "UP",
            "service" to "llm-analyzer-service",
            "port" to 50004,
            "version" to "1.0.0",
            "ai_models_available" to mapOf(
                "gemini-2.5-pro" to true,
                "gemini-2.5-flash" to true,
                "provider" to true
            ),
            "cache_status" to "CONNECTED",
            "responseTime" to "${responseTime.inWholeMilliseconds}ms",
            "timestamp" to java.time.Instant.now()
        )
    }
}