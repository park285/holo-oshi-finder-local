package com.holo.oshi.member.model

import java.time.Instant

/**
 * 타입 안전성과 함수형 프로그래밍 패러다임 적용
 */
sealed class ApiResponse<out T> {
    
    /**
     * 성공 응답
     */
    data class Success<T>(
        val data: T,
        val timestamp: Instant = Instant.now(),
        val processingTime: Long? = null
    ) : ApiResponse<T>()
    
    /**
     * 에러 응답 - 계층적 에러 구조
     */
    sealed class Error : ApiResponse<Nothing>() {
        abstract val code: ErrorCode
        abstract val message: String
        abstract val timestamp: Instant
        abstract val cause: Throwable?
        
        data class ValidationError(
            override val code: ErrorCode = ErrorCode.VALIDATION_FAILED,
            override val message: String,
            val field: String? = null,
            override val timestamp: Instant = Instant.now(),
            override val cause: Throwable? = null
        ) : Error()
        
        data class NotFoundError(
            override val code: ErrorCode = ErrorCode.RESOURCE_NOT_FOUND,
            override val message: String,
            val resourceId: String? = null,
            override val timestamp: Instant = Instant.now(),
            override val cause: Throwable? = null
        ) : Error()
        
        data class ServiceError(
            override val code: ErrorCode,
            override val message: String,
            val serviceName: String? = null,
            override val timestamp: Instant = Instant.now(),
            override val cause: Throwable? = null
        ) : Error()
        
        data class InternalError(
            override val code: ErrorCode = ErrorCode.INTERNAL_SERVER_ERROR,
            override val message: String = "Internal server error occurred",
            override val timestamp: Instant = Instant.now(),
            override val cause: Throwable? = null
        ) : Error()
    }
    
    /**
     * 로딩 상태 (리액티브 UI용)
     */
    data class Loading(val progress: Float? = null) : ApiResponse<Nothing>()
    
    // === Functional Programming Extensions ===
    
    /**
     * 성공 시에만 변환 적용
     */
    inline fun <R> map(crossinline transform: (T) -> R): ApiResponse<R> = when (this) {
        is Success -> Success(transform(data), timestamp, processingTime)
        is Error -> this
        is Loading -> this
    }
    
    /**
     * 성공 시에만 변환 적용 (nullable 결과)
     */
    inline fun <R> mapNotNull(crossinline transform: (T) -> R?): ApiResponse<R?> = when (this) {
        is Success -> Success(transform(data), timestamp, processingTime)
        is Error -> this
        is Loading -> this
    }
    
    /**
     * 성공 시에만 다른 ApiResponse 체이닝
     */
    inline fun <R> flatMap(crossinline transform: (T) -> ApiResponse<R>): ApiResponse<R> = when (this) {
        is Success -> transform(data)
        is Error -> this
        is Loading -> this
    }
    
    /**
     * 에러 복구
     */
    inline fun recover(crossinline recovery: (Error) -> ApiResponse<@UnsafeVariance T>): ApiResponse<T> = when (this) {
        is Success -> this
        is Error -> recovery(this)
        is Loading -> this
    }
    
    /**
     * 성공 데이터 추출 (null safe)
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
    
    /**
     * 성공 데이터 추출 또는 기본값
     */
    fun getOrElse(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        else -> default
    }
    
    /**
     * 성공 여부 검사
     */
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading
}

/**
 * 에러 코드 enum
 */
enum class ErrorCode(val httpStatus: Int) {
    // Client Errors (4xx)
    VALIDATION_FAILED(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    RESOURCE_NOT_FOUND(404),
    METHOD_NOT_ALLOWED(405),
    CONFLICT(409),
    RATE_LIMIT_EXCEEDED(429),
    
    // Server Errors (5xx)
    INTERNAL_SERVER_ERROR(500),
    SERVICE_UNAVAILABLE(503),
    GATEWAY_TIMEOUT(504),
    
    // Business Logic Errors
    MEMBER_NOT_FOUND(1001),
    INVALID_MEMBER_DATA(1002),
    DUPLICATE_MEMBER(1003),
    MEMBER_INACTIVE(1004),
    
    // External Service Errors
    AI_SERVICE_ERROR(2001),
    VECTOR_SERVICE_ERROR(2002),
    CACHE_SERVICE_ERROR(2003),
    DATABASE_ERROR(2004)
}

// === Convenience Functions ===

/**
 * 성공 응답 생성
 */
fun <T> T.asApiSuccess(processingTime: Long? = null): ApiResponse.Success<T> = 
    ApiResponse.Success(this, processingTime = processingTime)

/**
 * 에러 응답 생성
 */
fun Throwable.asApiError(code: ErrorCode = ErrorCode.INTERNAL_SERVER_ERROR): ApiResponse.Error.InternalError = 
    ApiResponse.Error.InternalError(code = code, message = message ?: "Unknown error", cause = this)

/**
 * Result를 ApiResponse로 변환
 */
fun <T> Result<T>.toApiResponse(): ApiResponse<T> = fold(
    onSuccess = { it.asApiSuccess() },
    onFailure = { it.asApiError() }
)