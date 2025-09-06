package com.holo.oshi.common.response

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.Instant

/**
 * - Sealed Classes로 타입 안전성 보장
 * - Railway-oriented programming 지원
 * - 일관된 에러 처리 
 */

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = ApiResponse.Success::class, name = "success"),
    JsonSubTypes.Type(value = ApiResponse.Error::class, name = "error"),
    JsonSubTypes.Type(value = ApiResponse.Loading::class, name = "loading")
)
sealed class ApiResponse<out T> {
    abstract val timestamp: Instant
    abstract val requestId: String?
    
    /**
     * 성공 응답
     */
    data class Success<T>(
        val data: T,
        val metadata: ResponseMetadata? = null,
        override val timestamp: Instant = Instant.now(),
        override val requestId: String? = null
    ) : ApiResponse<T>()
    
    /**
     * 에러 응답
     */
    data class Error(
        val code: String,
        val message: String,
        val details: Map<String, Any>? = null,
        val retryable: Boolean = false,
        override val timestamp: Instant = Instant.now(),
        override val requestId: String? = null
    ) : ApiResponse<Nothing>()
    
    /**
     * 처리 중 응답 (WebSocket/SSE용)
     */
    data class Loading(
        val message: String = "Processing...",
        val progress: Int? = null,
        override val timestamp: Instant = Instant.now(),
        override val requestId: String? = null
    ) : ApiResponse<Nothing>()
    
    // ==================== Utility Methods ====================
    
    /**
     * 성공 여부 확인
     */
    val isSuccess: Boolean
        get() = this is Success
    
    /**
     * 에러 여부 확인
     */
    val isError: Boolean  
        get() = this is Error
    
    /**
     * 데이터 추출 (null-safe)
     */
    fun getDataOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
    
    /**
     * 에러 정보 추출
     */
    fun getErrorOrNull(): Error? = this as? Error
    
    companion object {
        /**
         * 성공 응답 생성 헬퍼
         */
        fun <T> success(
            data: T, 
            metadata: ResponseMetadata? = null,
            requestId: String? = null
        ): Success<T> = Success(data, metadata, requestId = requestId)
        
        /**
         * 에러 응답 생성 헬퍼
         */
        fun error(
            code: String,
            message: String,
            details: Map<String, Any>? = null,
            retryable: Boolean = false,
            requestId: String? = null
        ): Error = Error(code, message, details, retryable, requestId = requestId)
        
        /**
         * 로딩 응답 생성 헬퍼
         */
        fun loading(
            message: String = "Processing...",
            progress: Int? = null,
            requestId: String? = null
        ): Loading = Loading(message, progress, requestId = requestId)
        
        /**
         * Result를 ApiResponse로 변환
         */
        fun <T> fromResult(
            result: Result<T>,
            requestId: String? = null
        ): ApiResponse<T> = result.fold(
            onSuccess = { data -> success(data, requestId = requestId) },
            onFailure = { exception ->
                error(
                    code = exception::class.simpleName ?: "UNKNOWN_ERROR",
                    message = exception.message ?: "Unknown error occurred",
                    retryable = isRetryableException(exception),
                    requestId = requestId
                )
            }
        )
        
        /**
         * 재시도 가능한 예외 판단
         */
        private fun isRetryableException(exception: Throwable): Boolean = when (exception) {
            is java.net.SocketTimeoutException,
            is java.net.ConnectException,
            is kotlinx.coroutines.TimeoutCancellationException -> true
            else -> false
        }
    }
}

/**
 * 응답 메타데이터
 */
data class ResponseMetadata(
    val processingTimeMs: Long,
    val cacheHit: Boolean = false,
    val version: String = "1.0",
    val deprecation: DeprecationInfo? = null,
    val rateLimit: RateLimitInfo? = null,
    val performance: PerformanceInfo? = null
)

data class DeprecationInfo(
    val deprecated: Boolean,
    val sunset: String?,
    val replacement: String?
)

data class RateLimitInfo(
    val limit: Int,
    val remaining: Int,
    val resetTime: Instant
)

data class PerformanceInfo(
    val dbQueryTime: Long? = null,
    val cacheTime: Long? = null,
    val externalApiTime: Long? = null,
    val totalQueries: Int = 0
)

// ==================== Extension Functions ====================

/**
 * ApiResponse 매핑 확장 함수
 */
inline fun <T, R> ApiResponse<T>.map(crossinline transform: (T) -> R): ApiResponse<R> = when (this) {
    is ApiResponse.Success -> ApiResponse.Success(
        data = transform(data),
        metadata = metadata,
        timestamp = timestamp,
        requestId = requestId
    )
    is ApiResponse.Error -> this
    is ApiResponse.Loading -> this
}

/**
 * ApiResponse 플랫 매핑
 */
inline fun <T, R> ApiResponse<T>.flatMap(crossinline transform: (T) -> ApiResponse<R>): ApiResponse<R> = when (this) {
    is ApiResponse.Success -> transform(data)
    is ApiResponse.Error -> this
    is ApiResponse.Loading -> this
}

/**
 * 에러 처리
 */
inline fun <T> ApiResponse<T>.onError(crossinline action: (ApiResponse.Error) -> Unit): ApiResponse<T> {
    if (this is ApiResponse.Error) action(this)
    return this
}

/**
 * 성공 처리
 */
inline fun <T> ApiResponse<T>.onSuccess(crossinline action: (T) -> Unit): ApiResponse<T> {
    if (this is ApiResponse.Success) action(data)
    return this
}

/**
 * ResponseEntity로 변환
 */
fun <T> ApiResponse<T>.toResponseEntity(): org.springframework.http.ResponseEntity<ApiResponse<T>> = when (this) {
    is ApiResponse.Success -> org.springframework.http.ResponseEntity.ok(this)
    is ApiResponse.Error -> when (code) {
        "NOT_FOUND" -> org.springframework.http.ResponseEntity.notFound().build()
        "VALIDATION_ERROR" -> org.springframework.http.ResponseEntity.badRequest().body(this)
        "UNAUTHORIZED" -> org.springframework.http.ResponseEntity.status(401).body(this)
        "FORBIDDEN" -> org.springframework.http.ResponseEntity.status(403).body(this)
        else -> org.springframework.http.ResponseEntity.status(500).body(this)
    }
    is ApiResponse.Loading -> org.springframework.http.ResponseEntity.status(202).body(this)
}

// ==================== Domain-Specific Errors ====================

object ApiErrors {
    fun memberNotFound(id: Long) = ApiResponse.error(
        code = "MEMBER_NOT_FOUND",
        message = "Member with ID $id not found",
        retryable = false
    )
    
    fun invalidBranch(branch: String) = ApiResponse.error(
        code = "INVALID_BRANCH",
        message = "Invalid branch code: $branch",
        details = mapOf("validBranches" to listOf("jp", "en", "id")),
        retryable = false
    )
    
    fun searchTimeout() = ApiResponse.error(
        code = "SEARCH_TIMEOUT",
        message = "Search operation timed out",
        retryable = true
    )
    
    fun serviceUnavailable(service: String) = ApiResponse.error(
        code = "SERVICE_UNAVAILABLE", 
        message = "$service is currently unavailable",
        retryable = true
    )
    
    fun validationFailed(field: String, reason: String) = ApiResponse.error(
        code = "VALIDATION_FAILED",
        message = "Validation failed for field: $field",
        details = mapOf("field" to field, "reason" to reason),
        retryable = false
    )
}