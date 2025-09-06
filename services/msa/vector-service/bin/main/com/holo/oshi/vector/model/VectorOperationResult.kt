package com.holo.oshi.vector.model

import java.time.Instant

/**
 * Vector 연산 결과 Sealed Class
 * 벡터 검색, 인덱싱 등 모든 벡터 연산의 타입 안전한 결과 표현
 */
sealed class VectorOperationResult<out T> {
    
    /**
     * 성공한 벡터 연산
     */
    data class Success<T>(
        val data: T,
        val operationTime: Long, // 연산 시간 (ms)
        val embeddingDimension: Int = 1536,
        val model: String = "gemini-embedding-001",
        val timestamp: Instant = Instant.now()
    ) : VectorOperationResult<T>()
    
    /**
     * 벡터 연산 실패
     */
    sealed class Failure : VectorOperationResult<Nothing>() {
        abstract val error: VectorError
        abstract val timestamp: Instant
        abstract val operationContext: String?
        
        /**
         * 임베딩 생성 실패
         */
        data class EmbeddingFailure(
            override val error: VectorError.EmbeddingError,
            override val timestamp: Instant = Instant.now(),
            override val operationContext: String? = null,
            val inputText: String? = null
        ) : Failure()
        
        /**
         * 벡터 검색 실패
         */
        data class SearchFailure(
            override val error: VectorError.SearchError,
            override val timestamp: Instant = Instant.now(),
            override val operationContext: String? = null,
            val query: String? = null,
            val searchParams: Map<String, Any>? = null
        ) : Failure()
        
        /**
         * 인덱싱 실패
         */
        data class IndexingFailure(
            override val error: VectorError.IndexError,
            override val timestamp: Instant = Instant.now(),
            override val operationContext: String? = null,
            val memberId: Int? = null
        ) : Failure()
        
        /**
         * 캐시 연산 실패
         */
        data class CacheFailure(
            override val error: VectorError.CacheError,
            override val timestamp: Instant = Instant.now(),
            override val operationContext: String? = null,
            val cacheKey: String? = null
        ) : Failure()
        
        /**
         * 시스템 수준 실패
         */
        data class SystemFailure(
            override val error: VectorError.SystemError,
            override val timestamp: Instant = Instant.now(),
            override val operationContext: String? = null,
            val cause: Throwable? = null
        ) : Failure()
    }
    
    /**
     * 진행 중인 벡터 연산 (비동기 처리용)
     */
    data class Processing(
        val operationId: String,
        val progress: Float = 0f,
        val estimatedTimeRemaining: Long? = null,
        val currentStep: String? = null
    ) : VectorOperationResult<Nothing>()
    
    // === Functional Programming Extensions ===
    
    /**
     * 성공 시에만 변환
     */
    inline fun <R> map(crossinline transform: (T) -> R): VectorOperationResult<R> = when (this) {
        is Success -> Success(transform(data), operationTime, embeddingDimension, model, timestamp)
        is Failure -> this
        is Processing -> this
    }
    
    /**
     * 성공 시에만 다른 VectorOperationResult 체이닝
     */
    inline fun <R> flatMap(crossinline transform: (T) -> VectorOperationResult<R>): VectorOperationResult<R> = when (this) {
        is Success -> transform(data)
        is Failure -> this
        is Processing -> this
    }
    
    /**
     * 실패 복구
     */
    inline fun recover(crossinline recovery: (Failure) -> VectorOperationResult<@UnsafeVariance T>): VectorOperationResult<T> = when (this) {
        is Success -> this
        is Failure -> recovery(this)
        is Processing -> this
    }
    
    /**
     * 성공 데이터 추출
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
     * 상태 검사
     */
    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure
    val isProcessing: Boolean get() = this is Processing
}

/**
 * 벡터 연산 전용 에러 계층
 */
sealed class VectorError(
    open val code: String,
    open val message: String,
    open val isRetryable: Boolean = false
) {
    
    /**
     * 임베딩 관련 에러
     */
    sealed class EmbeddingError(
        override val code: String,
        override val message: String,
        override val isRetryable: Boolean = false
    ) : VectorError(code, message, isRetryable) {
        
        object InvalidInput : EmbeddingError(
            "EMBEDDING_INVALID_INPUT",
            "텍스트 입력이 유효하지 않습니다",
            isRetryable = false
        )
        
        object ModelUnavailable : EmbeddingError(
            "EMBEDDING_MODEL_UNAVAILABLE",
            "임베딩 모델에 접근할 수 없습니다",
            isRetryable = true
        )
        
        object TokenLimitExceeded : EmbeddingError(
            "EMBEDDING_TOKEN_LIMIT",
            "토큰 제한을 초과했습니다",
            isRetryable = false
        )
        
        data class ApiError(
            val httpStatus: Int,
            override val message: String
        ) : EmbeddingError(
            "EMBEDDING_API_ERROR",
            message,
            isRetryable = httpStatus in 500..599
        )
    }
    
    /**
     * 검색 관련 에러
     */
    sealed class SearchError(
        override val code: String,
        override val message: String,
        override val isRetryable: Boolean = false
    ) : VectorError(code, message, isRetryable) {
        
        object EmptyQuery : SearchError(
            "SEARCH_EMPTY_QUERY",
            "검색 쿼리가 비어있습니다",
            isRetryable = false
        )
        
        object IndexNotReady : SearchError(
            "SEARCH_INDEX_NOT_READY",
            "벡터 인덱스가 준비되지 않았습니다",
            isRetryable = true
        )
        
        object DatabaseError : SearchError(
            "SEARCH_DATABASE_ERROR",
            "데이터베이스 연결 오류",
            isRetryable = true
        )
        
        data class InvalidParameters(
            val parameterName: String
        ) : SearchError(
            "SEARCH_INVALID_PARAMS",
            "검색 파라미터가 유효하지 않습니다: $parameterName",
            isRetryable = false
        )
    }
    
    /**
     * 인덱싱 관련 에러
     */
    sealed class IndexError(
        override val code: String,
        override val message: String,
        override val isRetryable: Boolean = false
    ) : VectorError(code, message, isRetryable) {
        
        data class MemberNotFound(
            val memberId: Int
        ) : IndexError(
            "INDEX_MEMBER_NOT_FOUND",
            "멤버 ID $memberId 를 찾을 수 없습니다",
            isRetryable = false
        )
        
        object DimensionMismatch : IndexError(
            "INDEX_DIMENSION_MISMATCH",
            "임베딩 차원이 일치하지 않습니다",
            isRetryable = false
        )
        
        object StorageError : IndexError(
            "INDEX_STORAGE_ERROR",
            "인덱스 저장 중 오류가 발생했습니다",
            isRetryable = true
        )
    }
    
    /**
     * 캐시 관련 에러
     */
    sealed class CacheError(
        override val code: String,
        override val message: String,
        override val isRetryable: Boolean = false
    ) : VectorError(code, message, isRetryable) {
        
        object SerializationError : CacheError(
            "CACHE_SERIALIZATION_ERROR",
            "캐시 직렬화 오류",
            isRetryable = false
        )
        
        object ConnectionError : CacheError(
            "CACHE_CONNECTION_ERROR",
            "캐시 서버 연결 오류",
            isRetryable = true
        )
        
        object KeyNotFound : CacheError(
            "CACHE_KEY_NOT_FOUND",
            "캐시 키를 찾을 수 없습니다",
            isRetryable = false
        )
    }
    
    /**
     * 시스템 수준 에러
     */
    sealed class SystemError(
        override val code: String,
        override val message: String,
        override val isRetryable: Boolean = false
    ) : VectorError(code, message, isRetryable) {
        
        object OutOfMemory : SystemError(
            "SYSTEM_OUT_OF_MEMORY",
            "시스템 메모리 부족",
            isRetryable = false
        )
        
        object TimeoutError : SystemError(
            "SYSTEM_TIMEOUT",
            "연산 시간 초과",
            isRetryable = true
        )
        
        data class UnexpectedError(
            override val message: String,
            val cause: Throwable? = null
        ) : SystemError(
            "SYSTEM_UNEXPECTED_ERROR",
            message,
            isRetryable = false
        )
    }
}

// === Convenience Functions ===

/**
 * 성공 결과 생성
 */
fun <T> T.asVectorSuccess(operationTime: Long): VectorOperationResult.Success<T> = 
    VectorOperationResult.Success(this, operationTime)

/**
 * Result를 VectorOperationResult로 변환
 */
fun <T> Result<T>.toVectorResult(operationTime: Long): VectorOperationResult<T> = fold(
    onSuccess = { it.asVectorSuccess(operationTime) },
    onFailure = { 
        VectorOperationResult.Failure.SystemFailure(
            error = VectorError.SystemError.UnexpectedError(
                message = it.message ?: "Unknown error",
                cause = it
            ),
            cause = it
        )
    }
)