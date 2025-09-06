package com.holo.oshi.common.extensions

import arrow.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import mu.KotlinLogging
import kotlin.time.Duration
import kotlin.time.measureTimedValue

/**
 * 🎯 Safe Kotlin Extensions (문제 완전 해결 버전)
 * 
 * 세계최고 수준의 코틀린라이크 확장함수들:
 * ✅ Zero Inline Issues (모든 inline은 top-level)
 * ✅ Proper Visibility (public/private 명확히 구분)
 * ✅ Type-Safe Operations
 * ✅ Error-Safe Implementation
 * ✅ Netflix/Google 스타일 API 디자인
 * ✅ Pure Kotlin Idioms
 */

internal val logger = KotlinLogging.logger {} // internal로 변경

// ============ Either Extensions (Railway-Oriented) ============

/**
 * Either 체인 처리 (Top-level inline - 문제 없음)
 */
inline fun <A, B, C> Either<A, B>.flatMapSafe(
    crossinline f: (B) -> Either<A, C>
): Either<A, C> = when (this) {
    is Either.Left -> this
    is Either.Right -> f(value)
}

/**
 * Either → Result 변환
 */
inline fun <A : Throwable, B> Either<A, B>.toResultSafe(): Result<B> =
    fold(
        ifLeft = { Result.failure(it) },
        ifRight = { Result.success(it) }
    )

/**
 * Result → Either 변환
 */
inline fun <T> Result<T>.toEitherSafe(): Either<Throwable, T> =
    fold(
        onSuccess = { it.right() },
        onFailure = { it.left() }
    )

// ============ Flow Extensions (Safe Implementations) ============

/**
 * Flow 청크 처리 (안전한 구현)
 */
fun <T> Flow<T>.safeChunked(size: Int): Flow<List<T>> = flow {
    val chunk = mutableListOf<T>()
    collect { item ->
        chunk.add(item)
        if (chunk.size >= size) {
            emit(chunk.toList())
            chunk.clear()
        }
    }
    if (chunk.isNotEmpty()) {
        emit(chunk.toList())
    }
}

/**
 * Flow 병렬 처리 (안전한 구현)
 */
fun <T, R> Flow<T>.mapParallelSafe(
    concurrency: Int = 8,
    transform: suspend (T) -> R
): Flow<R> = this
    .map { item ->
        CoroutineScope(Dispatchers.Default).async { transform(item) }
    }
    .buffer(concurrency)
    .map { deferred -> deferred.await() }

/**
 * Flow 에러 복구 (안전한 구현)
 */
fun <T> Flow<T>.recoverSafe(
    recovery: suspend (Throwable) -> Flow<T>
): Flow<T> = catch { exception ->
    logger.warn(exception) { "Flow 에러 발생, 복구 시도" }
    emitAll(recovery(exception))
}

/**
 * Flow 타임아웃 처리
 */
fun <T> Flow<T>.timeoutSafe(
    timeout: Duration,
    defaultValue: suspend () -> T
): Flow<T> = flow {
    try {
        withTimeout(timeout.inWholeMilliseconds) {
            collect { emit(it) }
        }
    } catch (e: TimeoutCancellationException) {
        logger.warn { "Flow 타임아웃, 기본값 사용: $timeout" }
        emit(defaultValue())
    }
}

// ============ Coroutines Extensions (Safe) ============

/**
 * 재시도 로직 (안전한 구현)
 */
suspend fun <T> retrySafe(
    times: Int = 3,
    initialDelay: Duration = Duration.parse("1s"),
    factor: Double = 2.0,
    block: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(times - 1) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            logger.warn { "재시도 ${attempt + 1}/$times 실패: ${e.message}" }
            delay(currentDelay)
            currentDelay = Duration.parse("${(currentDelay.inWholeMilliseconds * factor).toLong()}ms")
        }
    }
    return block() // 마지막 시도
}

/**
 * 타임아웃 처리 (안전한 구현)
 */
suspend fun <T> withTimeoutSafe(
    timeout: Duration,
    defaultValue: T,
    block: suspend () -> T
): T = try {
    withTimeout(timeout.inWholeMilliseconds) { block() }
} catch (e: TimeoutCancellationException) {
    logger.warn { "작업 타임아웃: $timeout, 기본값 사용" }
    defaultValue
}

/**
 * 병렬 처리 (안전한 구현)
 */
suspend fun <T, R> List<T>.mapParallelSafe(
    concurrency: Int = 8,
    transform: suspend (T) -> R
): List<R> = coroutineScope {
    val semaphore = Semaphore(concurrency)
    map { item ->
        async {
            semaphore.withPermit {
                transform(item)
            }
        }
    }.awaitAll()
}

// ============ String Extensions (Safe) ============

/**
 * String 검증 체인
 */
inline fun String?.validateNotBlankSafe(): Either<String, String> =
    if (!this.isNullOrBlank()) this.right()
    else "String is null or blank".left()

inline fun String.validateLengthSafe(min: Int, max: Int): Either<String, String> =
    if (length in min..max) this.right()
    else "String length must be between $min and $max, got $length".left()

inline fun String.validatePatternSafe(pattern: Regex, errorMessage: String): Either<String, String> =
    if (matches(pattern)) this.right()
    else errorMessage.left()

/**
 * String 정규화
 */
fun String.normalizeSafe(): String = this.trim().lowercase().replace("\\s+".toRegex(), " ")

// ============ Collection Extensions (Safe) ============

/**
 * List 안전한 접근
 */
fun <T> List<T>.safeGet(index: Int): T? =
    if (index in indices) this[index] else null

/**
 * Map 안전한 값 추출
 */
inline fun <K, V> Map<K, V>.getOrCompute(key: K, defaultValue: () -> V): V =
    this[key] ?: defaultValue()

/**
 * Collection 안전한 청크 분할
 */
fun <T> List<T>.safeChunkedList(size: Int): List<List<T>> =
    if (size <= 0) listOf(this) else chunked(size.coerceAtLeast(1))

// ============ Null Safety Extensions ============

/**
 * 다중 null 체크
 */
inline fun <T1 : Any, T2 : Any, R> ifBothNotNull(
    value1: T1?,
    value2: T2?,
    block: (T1, T2) -> R
): R? = if (value1 != null && value2 != null) {
    block(value1, value2)
} else null

inline fun <T1 : Any, T2 : Any, T3 : Any, R> ifAllNotNull(
    value1: T1?,
    value2: T2?,
    value3: T3?,
    block: (T1, T2, T3) -> R
): R? = if (value1 != null && value2 != null && value3 != null) {
    block(value1, value2, value3)
} else null

// ============ 홀로라이브 도메인 Extensions ============

/**
 * 홀로라이브 특화 확장함수들
 */
fun String.isValidHoloBranch(): Boolean = 
    this.lowercase() in setOf("jp", "en", "id")

fun String.isValidHoloGeneration(): Boolean =
    this.matches("gen\\d+".toRegex())

fun Long.toValidMemberId(): Either<String, Long> =
    if (this > 0) this.right() else "Invalid member ID: $this".left()

/**
 * 검색 쿼리 정규화
 */
fun String.normalizeHoloQuery(): String =
    this.trim()
        .lowercase()
        .replace("\\s+".toRegex(), " ")
        .take(100) // 최대 100자

// ============ Performance Measurement (Safe) ============

/**
 * 성능 측정 (Top-level inline)
 */
inline fun <T> measurePerformance(
    name: String,
    block: () -> T
): Pair<T, Duration> {
    val (result, time) = measureTimedValue { block() }
    return result to time
}

/**
 * Suspend 성능 측정
 */
suspend inline fun <T> measureSuspendPerformance(
    name: String,
    crossinline block: suspend () -> T
): Pair<T, Duration> {
    val (result, time) = measureTimedValue { block() }
    return result to time
}