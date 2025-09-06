package com.holo.oshi.member.controller

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
            logger.info { "Member Service 헬스체크 요청 처리 중" }
        }
        
        return mapOf(
            "status" to "UP",
            "service" to "member-service",
            "port" to 50001,
            "version" to "1.0.0",
            "responseTime" to "${responseTime.inWholeMilliseconds}ms",
            "timestamp" to java.time.Instant.now()
        )
    }
}