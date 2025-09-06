package com.holo.oshi.common.model

/**
 * 공통 Value Classes - 모든 MSA 서비스에서 사용
 * 타입 안전성과 도메인 모델링을 위한 인라인 클래스들
 */

// ========== ID Value Classes ==========

@JvmInline
value class MemberId(val value: Long) {
    init {
        require(value > 0) { "MemberId must be positive: $value" }
    }
    
    companion object {
        val NONE = MemberId(-1L)
        fun of(value: Long) = MemberId(value)
    }
}

@JvmInline
value class UserId(val value: String) {
    init {
        require(value.isNotBlank()) { "UserId cannot be blank" }
        require(value.length <= 50) { "UserId too long: ${value.length}" }
    }
}

// ========== Branch & Generation ==========

@JvmInline
value class BranchCode(val value: String) {
    companion object {
        val VALID_BRANCHES = setOf("jp", "en", "id")
        
        val JP = BranchCode("jp")
        val EN = BranchCode("en")
        val ID = BranchCode("id")
        
        fun parse(value: String): Result<BranchCode> = runCatching {
            BranchCode(value.lowercase())
        }
    }
    
    init {
        require(value in VALID_BRANCHES) { 
            "Invalid branch: $value. Valid: ${VALID_BRANCHES.joinToString()}" 
        }
    }
}

@JvmInline
value class GenerationCode(val value: String) {
    companion object {
        private val VALID_PATTERN = Regex("gen\\d+|gamers|hope|id\\d+|regloss|advent|justice|myth|council|promise")
        
        fun parse(value: String): Result<GenerationCode> = runCatching {
            GenerationCode(value.lowercase())
        }
    }
    
    init {
        require(value.isNotBlank()) { "Generation cannot be blank" }
        require(value.matches(VALID_PATTERN)) {
            "Invalid generation format: $value"
        }
    }
}

// ========== Score & Metrics ==========

@JvmInline
value class TraitScore(val value: Int) {
    companion object {
        val MIN = TraitScore(0)
        val MAX = TraitScore(100)
        val HIGH_THRESHOLD = TraitScore(80)
        val MEDIUM_THRESHOLD = TraitScore(50)
    }
    
    init {
        require(value in 0..100) { "Trait score must be between 0 and 100: $value" }
    }
    
    fun isHigh() = value >= HIGH_THRESHOLD.value
    fun isMedium() = value in MEDIUM_THRESHOLD.value until HIGH_THRESHOLD.value
    fun isLow() = value < MEDIUM_THRESHOLD.value
    
    operator fun compareTo(other: TraitScore) = value.compareTo(other.value)
    operator fun plus(other: TraitScore) = TraitScore((value + other.value).coerceIn(0, 100))
}

@JvmInline
value class SimilarityScore(val value: Double) {
    init {
        require(value in 0.0..1.0) { "Similarity must be between 0 and 1: $value" }
    }
    
    fun toPercentage() = (value * 100).toInt()
    fun isHighConfidence() = value >= 0.8
    fun isMediumConfidence() = value in 0.5..0.79
    fun isLowConfidence() = value < 0.5
}

// ========== Text & Names ==========

@JvmInline
value class MemberName(val value: String) {
    init {
        require(value.isNotBlank()) { "Member name cannot be blank" }
        require(value.length <= 100) { "Member name too long: ${value.length}" }
    }
    
    fun toSearchableText() = value.lowercase().trim()
}

@JvmInline
value class SearchQuery(val value: String) {
    companion object {
        const val MIN_LENGTH = 1
        const val MAX_LENGTH = 500
    }
    
    init {
        require(value.length in MIN_LENGTH..MAX_LENGTH) {
            "Search query length must be between $MIN_LENGTH and $MAX_LENGTH"
        }
    }
    
    fun normalize() = value.trim().lowercase()
    fun tokens() = normalize().split(Regex("\\s+"))
}

// ========== Embedding & Vector ==========

@JvmInline
value class EmbeddingDimension(val value: Int) {
    companion object {
        val GEMINI = EmbeddingDimension(1536)
        val OPENAI_SMALL = EmbeddingDimension(1536)
        val OPENAI_LARGE = EmbeddingDimension(3072)
    }
    
    init {
        require(value > 0 && value % 8 == 0) {
            "Embedding dimension must be positive and divisible by 8: $value"
        }
    }
}

@JvmInline
value class EmbeddingVector(val values: DoubleArray) {
    init {
        require(values.isNotEmpty()) { "Embedding vector cannot be empty" }
        require(values.size == EmbeddingDimension.GEMINI.value) {
            "Invalid embedding dimension: ${values.size}"
        }
    }
    
    fun dimension() = EmbeddingDimension(values.size)
    
    fun cosineSimilarity(other: EmbeddingVector): SimilarityScore {
        require(values.size == other.values.size) {
            "Vectors must have same dimension"
        }
        
        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0
        
        for (i in values.indices) {
            dotProduct += values[i] * other.values[i]
            normA += values[i] * values[i]
            normB += other.values[i] * other.values[i]
        }
        
        val similarity = dotProduct / (kotlin.math.sqrt(normA) * kotlin.math.sqrt(normB))
        return SimilarityScore(similarity.coerceIn(0.0, 1.0))
    }
}

// ========== Cache Keys ==========

@JvmInline
value class CacheKey(val value: String) {
    companion object {
        fun memberKey(id: MemberId) = CacheKey("member:${id.value}")
        fun searchKey(query: SearchQuery, limit: Int = 10) = 
            CacheKey("search:${query.normalize()}:$limit")
        fun vectorKey(memberId: MemberId) = CacheKey("vector:${memberId.value}")
    }
    
    init {
        require(value.isNotBlank()) { "Cache key cannot be blank" }
        require(!value.contains(" ")) { "Cache key cannot contain spaces" }
    }
}

// ========== Time & Duration ==========

@JvmInline
value class Milliseconds(val value: Long) {
    init {
        require(value >= 0) { "Milliseconds cannot be negative: $value" }
    }
    
    fun toSeconds() = value / 1000.0
    fun toMinutes() = value / 60_000.0
}

// ========== Extension Functions ==========

/**
 * Result 타입과 함께 사용하기 위한 확장 함수들
 */
inline fun <T> resultOf(block: () -> T): Result<T> = runCatching(block)

inline fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> =
    fold(
        onSuccess = { transform(it) },
        onFailure = { Result.failure(it) }
    )

/**
 * Result 성공시 일반 함수 실행 (suspend 아님)
 */
inline fun <T> Result<T>.onSuccessAction(
    crossinline action: (T) -> Unit
): Result<T> {
    if (isSuccess) {
        action(getOrThrow())
    }
    return this
}