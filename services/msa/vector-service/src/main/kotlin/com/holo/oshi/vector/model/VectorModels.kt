package com.holo.oshi.vector.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

// Request/Response DTOs
data class VectorSearchRequest(
    val query: String,
    val limit: Int = 10,
    val threshold: Double = 0.7,
    val activeOnly: Boolean = true
)

data class VectorSearchResponse(
    val results: List<VectorSearchResult> = emptyList(),
    val queryEmbedding: List<Double>? = null,
    val searchTime: Long = 0L,
    val totalResults: Int = 0
)

data class VectorSearchResult(
    val memberId: Int,  // String -> Int 수정
    val memberName: String,
    val score: Double,
    val branch: String,
    val generation: String,  // Int -> String 수정 (실제 DB는 "gen1", "gen3" 등)
    val unit: String?,
    val isActive: Boolean,
    val traits: Map<String, Any>? = null
)

data class VectorIndexRequest(
    val memberId: Int,  // String -> Int 수정
    val forceReindex: Boolean = false
)

data class VectorIndexResponse(
    val memberId: Int,  // String -> Int 수정
    val status: String,
    val embeddingSize: Int,
    val model: String,
    val timestamp: Instant
)

// Entity for member_embeddings table
@Table("member_embeddings")
data class MemberEmbedding(
    @Id
    val id: Long? = null,
    
    @Column("member_id")
    val memberId: Int,  // String -> Int 수정
    
    @Column("embedding")
    val embedding: String, // pgvector stores as string, convert to/from float array
    
    @Column("model_version")
    val modelVersion: String = "gemini-1.5-pro",
    
    @Column("created_at")
    val createdAt: Instant = Instant.now(),
    
    @Column("updated_at")
    val updatedAt: Instant = Instant.now()
)

// Events for async processing
data class EmbeddingUpdateRequested(
    val memberId: Int,  // String -> Int 수정
    val requestId: String,
    val timestamp: Instant = Instant.now()
)

data class EmbeddingUpdated(
    val memberId: Int,  // String -> Int 수정
    val embeddingSize: Int,
    val model: String,
    val timestamp: Instant = Instant.now()
)

// Cache keys
object VectorCacheKeys {
    fun searchKey(query: String, limit: Int, activeOnly: Boolean) = 
        "vector:search:${query.hashCode()}:$limit:$activeOnly"
    
    fun embeddingKey(memberId: Int) = "vector:embedding:$memberId"  // String -> Int 수정
    
    const val TTL_SECONDS = 300L // 5 minutes
}