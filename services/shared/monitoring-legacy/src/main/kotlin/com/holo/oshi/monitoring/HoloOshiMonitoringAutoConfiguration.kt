package com.holo.oshi.monitoring

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 🎯 Kotlin-style 모니터링 자동 설정
 * 모든 MSA 서비스에서 자동으로 로드됨
 */
@Configuration
@ConditionalOnProperty(
    name = ["holo.monitoring.enabled"], 
    havingValue = "true", 
    matchIfMissing = true
)
@Import(KotlinCoroutinesCollector::class)
class HoloOshiMonitoringAutoConfiguration {

    /**
     * 코틀린 스타일 메트릭 Bean (완전히 단순한 방식)
     */
    @Bean
    fun simpleKotlinMetrics(): SimpleKotlinMetrics {
        logger.info { "🔧 SimpleKotlinMetrics 초기화" }
        return SimpleKotlinMetrics()
    }

    /**
     * 공통 태그 설정 (홀로라이브 도메인 특화)
     */
    @Bean
    fun holoOshiMeterRegistryCustomizer(
        @Value("\${spring.application.name:unknown-service}") serviceName: String,
        @Value("\${spring.profiles.active:default}") profile: String,
        @Value("\${holo.environment:local}") environment: String
    ): MeterRegistryCustomizer<MeterRegistry> {
        return MeterRegistryCustomizer { registry ->
            registry.config().commonTags(
                "service", serviceName,
                "environment", environment,
                "profile", profile,
                "domain", "holo-oshi-finder",
                "kotlin_version", "2.2.10",
                "architecture", "msa"
            )
            logger.info { "📊 공통 메트릭 태그 설정 완료: $serviceName [$environment]" }
        }
    }

    /**
     * 코틀린 친화적 히스토그램 설정
     */
    @Bean
    @ConditionalOnProperty("holo.monitoring.histogram.enabled", matchIfMissing = true)
    fun kotlinHistogramCustomizer(): MeterRegistryCustomizer<MeterRegistry> {
        return MeterRegistryCustomizer { registry ->
            registry.config()
                // 불필요한 메트릭 필터링
                .meterFilter(
                    io.micrometer.core.instrument.config.MeterFilter.denyNameStartsWith("holo.temp")
                )
                .meterFilter(
                    io.micrometer.core.instrument.config.MeterFilter.denyNameStartsWith("test")
                )
            
            logger.info { "📈 코틀린 친화적 히스토그램 설정 완료" }
        }
    }
}

/**
 * 메트릭 관련 설정 프로퍼티
 */
@org.springframework.boot.context.properties.ConfigurationProperties(prefix = "holo.monitoring")
data class HoloMonitoringProperties(
    var enabled: Boolean = true,
    var histogram: HistogramProperties = HistogramProperties(),
    var coroutines: CoroutinesProperties = CoroutinesProperties()
)

data class HistogramProperties(
    var enabled: Boolean = true,
    var percentiles: List<Double> = listOf(0.5, 0.9, 0.95, 0.99)
)

data class CoroutinesProperties(
    var enabled: Boolean = true,
    var collectionIntervalSeconds: Long = 10,
    var debugProbes: Boolean = true
)