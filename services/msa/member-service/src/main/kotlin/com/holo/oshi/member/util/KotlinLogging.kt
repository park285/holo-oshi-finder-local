package com.holo.oshi.member.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

/**
 * 코틀린다운 로깅 유틸리티
 * - Lazy 로거 생성
 * - String Template 활용
 * - 구조화된 로깅
 * - 성능 최적화
 */

@Suppress("NOTHING_TO_INLINE")
object KotlinLogging {
    
    inline fun <reified T> logger(): KLogger {
        return KLogger(LoggerFactory.getLogger(T::class.java))
    }
    
    fun logger(clazz: KClass<*>): KLogger {
        return KLogger(LoggerFactory.getLogger(clazz.java))
    }
    
    fun logger(name: String): KLogger {
        return KLogger(LoggerFactory.getLogger(name))
    }
    
    @PublishedApi
    internal fun <T : Any> getClassForLogging(javaClass: Class<T>): Class<*> {
        return javaClass.enclosingClass?.takeIf {
            it.kotlin.objectInstance?.javaClass == javaClass
        } ?: javaClass
    }
}

/**
 * 코틀린 로거 래퍼
 * - 지연 평가 메시지
 * - 구조화된 컨텍스트 로깅
 * - MDC 자동 관리
 */
class KLogger(@PublishedApi internal val logger: Logger) {
    
    inline fun trace(message: () -> String) {
        if (logger.isTraceEnabled) {
            logger.trace(message())
        }
    }
    
    inline fun trace(throwable: Throwable, message: () -> String) {
        if (logger.isTraceEnabled) {
            logger.trace(message(), throwable)
        }
    }
    
    inline fun debug(message: () -> String) {
        if (logger.isDebugEnabled) {
            logger.debug(message())
        }
    }
    
    inline fun debug(throwable: Throwable, message: () -> String) {
        if (logger.isDebugEnabled) {
            logger.debug(message(), throwable)
        }
    }
    
    inline fun info(message: () -> String) {
        if (logger.isInfoEnabled) {
            logger.info(message())
        }
    }
    
    inline fun info(throwable: Throwable, message: () -> String) {
        if (logger.isInfoEnabled) {
            logger.info(message(), throwable)
        }
    }
    
    inline fun warn(message: () -> String) {
        if (logger.isWarnEnabled) {
            logger.warn(message())
        }
    }
    
    inline fun warn(throwable: Throwable, message: () -> String) {
        if (logger.isWarnEnabled) {
            logger.warn(message(), throwable)
        }
    }
    
    inline fun error(message: () -> String) {
        if (logger.isErrorEnabled) {
            logger.error(message())
        }
    }
    
    inline fun error(throwable: Throwable, message: () -> String) {
        if (logger.isErrorEnabled) {
            logger.error(message(), throwable)
        }
    }
    
    // 구조화된 로깅을 위한 컨텍스트 로깅
    inline fun info(context: Map<String, Any?>, message: () -> String) {
        if (logger.isInfoEnabled) {
            val contextStr = context.entries.joinToString(", ") { "${it.key}=${it.value}" }
            logger.info("${message()} [$contextStr]")
        }
    }
    
    inline fun warn(context: Map<String, Any?>, message: () -> String) {
        if (logger.isWarnEnabled) {
            val contextStr = context.entries.joinToString(", ") { "${it.key}=${it.value}" }
            logger.warn("${message()} [$contextStr]")
        }
    }
    
    inline fun error(context: Map<String, Any?>, throwable: Throwable, message: () -> String) {
        if (logger.isErrorEnabled) {
            val contextStr = context.entries.joinToString(", ") { "${it.key}=${it.value}" }
            logger.error("${message()} [$contextStr]", throwable)
        }
    }
}

/**
 * 성능 측정을 위한 로깅 유틸리티
 */
inline fun <T> KLogger.withTiming(
    operation: String,
    block: () -> T
): T {
    val start = System.currentTimeMillis()
    return try {
        val result = block()
        val duration = System.currentTimeMillis() - start
        this.info { "$operation 완료: ${duration}ms" }
        result
    } catch (e: Exception) {
        val duration = System.currentTimeMillis() - start
        this.error(e) { "$operation 실패: ${duration}ms" }
        throw e
    }
}

// suspend 버전
suspend inline fun <T> KLogger.withTimingSuspend(
    operation: String,
    crossinline block: suspend () -> T
): T {
    val start = System.currentTimeMillis()
    return try {
        val result = block()
        val duration = System.currentTimeMillis() - start
        this.info { "$operation 완료: ${duration}ms" }
        result
    } catch (e: Exception) {
        val duration = System.currentTimeMillis() - start
        this.error(e) { "$operation 실패: ${duration}ms" }
        throw e
    }
}

/**
 * 비즈니스 메트릭 로깅
 */
fun KLogger.metric(
    metricName: String,
    value: Number,
    unit: String = "",
    context: Map<String, Any?> = emptyMap()
) {
    val contextStr = if (context.isNotEmpty()) {
        context.entries.joinToString(", ", prefix = " [", postfix = "]") { "${it.key}=${it.value}" }
    } else ""
    
    info { "METRIC: $metricName=$value$unit$contextStr" }
}

/**
 * 감사 로깅 (비즈니스 중요 이벤트)
 */
fun KLogger.audit(
    action: String,
    userId: String? = null,
    resourceId: String? = null,
    success: Boolean = true,
    details: Map<String, Any?> = emptyMap()
) {
    val status = if (success) "SUCCESS" else "FAILED"
    val context = buildMap {
        userId?.let { put("userId", it) }
        resourceId?.let { put("resourceId", it) }
        putAll(details)
    }
    
    val contextStr = context.entries.joinToString(", ", prefix = " [", postfix = "]") { "${it.key}=${it.value}" }
    info { "AUDIT: $action $status$contextStr" }
}