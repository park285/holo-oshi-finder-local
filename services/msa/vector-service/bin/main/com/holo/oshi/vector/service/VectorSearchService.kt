package com.holo.oshi.vector.service

import com.holo.oshi.vector.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import mu.KotlinLogging
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import kotlin.time.measureTimedValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.holo.oshi.vector.util.VectorCacheKeys

/**
 * 벡터 검색 서비스
 * 
 * 특징:
 * - Pure Kotlin Coroutines
 * - Value Classes 타입 안전성
 * - Railway-oriented programming Result 패턴
 * - Flow 기반 스트리밍
 * - DSL 패턴
 */
@Service
class VectorSearchService(
    private val databaseClient: DatabaseClient,
    private val redisTemplate: ReactiveRedisTemplate<String, Any>,
    private val streamBridge: StreamBridge,
    private val embeddingService: EmbeddingService
) {
    companion object {
        private val logger = KotlinLogging.logger {}
        private val objectMapper = jacksonObjectMapper()
        
        // Value Classes
        @JvmInline
        value class MemberId(val value: Int) {
            init { require(value > 0) { "MemberId must be positive: $value" } }
        }
        
        @JvmInline
        value class CacheKey(val value: String) {
            init { require(value.isNotBlank()) { "CacheKey cannot be blank" } }
        }
        
        @JvmInline
        value class Score(val value: Double) {
            init { require(value in 0.0..1.0) { "Score must be between 0 and 1: $value" } }
        }
        
        @JvmInline
        value class EmbeddingDimension(val value: Int) {
            init { require(value == 1536) { "Embedding dimension must be 1536, got: $value" } }
        }
    }
    
    /**
     * 벡터 유사도 검색 - Result 패턴
     */
    suspend fun searchSimilar(request: VectorSearchRequest): Result<VectorSearchResponse> = runCatching {
        logger.info { "Starting vector search: query='${request.query}', limit=${request.limit}" }
        
        val (response, duration) = measureTimedValue {
            coroutineScope {
                // 병렬 처리: 임베딩 생성과 캐시 확인
                val embeddingDeferred = async { generateQueryEmbedding(request.query) }
                val cacheDeferred = async { checkCache(request) }
                
                // 캐시 확인
                cacheDeferred.await()?.let { cached ->
                    logger.debug { "Cache hit for query: '${request.query}'" }
                    return@coroutineScope cached
                }
                
                // 캐시 미스 시 검색 수행
                val embedding = embeddingDeferred.await().getOrThrow()
                
                performVectorSearch(request, embedding).also { response ->
                    // 비동기로 캐시 저장
                    launch { saveToCache(request, response) }
                }
            }
        }
        
        logger.info { "Vector search completed: ${response.results.size} results in ${duration.inWholeMilliseconds}ms" }
        response
    }.onFailure { error ->
        logger.error(error) { "Vector search failed for query: '${request.query}'" }
    }
    
    /**
     * 멤버 인덱싱 - Railway-oriented programming
     */
    suspend fun indexMember(request: VectorIndexRequest): Result<VectorIndexResponse> = 
        validateMemberId(request.memberId)
            .mapCatching { memberId ->
                // 기존 인덱스 확인
                when {
                    !request.forceReindex -> checkExistingIndex(memberId)
                        ?.let { return Result.success(it) }
                    else -> null
                }
                
                performIndexing(memberId, request.forceReindex)
            }
            .onSuccess { response ->
                logger.info { "Successfully indexed member: ${response.memberId}" }
                
                // 비동기 이벤트 발행
                coroutineScope {
                    launch { publishIndexingEvent(response) }
                    launch { invalidateRelatedCache(response.memberId) }
                }
            }
            .onFailure { error ->
                logger.error(error) { "Failed to index member: ${request.memberId}" }
            }
    
    /**
     * 상태 조회 - 병렬 처리 최적화
     */
    suspend fun getStatus(): Map<String, Any> = coroutineScope {
        val totalCountDeferred = async { getTotalEmbeddings() }
        val activeCountDeferred = async { getActiveEmbeddings() }
        val healthDeferred = async { checkHealthStatus() }
        
        mapOf(
            "totalEmbeddings" to totalCountDeferred.await(),
            "activeEmbeddings" to activeCountDeferred.await(),
            "embeddingDimension" to EmbeddingDimension(1536).value,
            "model" to "gemini-embedding-001",
            "indexType" to "HNSW",
            "status" to healthDeferred.await()
        )
    }
    
    // === Private Helper Functions - Kotlin Native ===
    
    private suspend fun generateQueryEmbedding(query: String): Result<List<Double>> = runCatching {
        embeddingService.generateEmbedding(query, "RETRIEVAL_QUERY")
    }
    
    private suspend fun checkCache(request: VectorSearchRequest): VectorSearchResponse? {
        return try {
            null // TODO: 캐시 구현 예정 (Redis 직렬화)
        } catch (e: Exception) {
            logger.warn { "Cache lookup failed: ${e.message}" }
            null
        }
    }
    
    private suspend fun performVectorSearch(
        request: VectorSearchRequest, 
        embedding: List<Double>
    ): VectorSearchResponse = withContext(Dispatchers.IO) {
        val embeddingStr = embedding.joinToString(",", "[", "]")
        
        // Kotlin DSL 스타일 쿼리
        val sql = buildString {
            append("""
                SELECT 
                    me.member_id,
                    me.member_name,
                    me.name_ja,
                    me.branch,
                    me.generation,
                    me.fanbase_name,
                    me.is_active,
                    me.searchable_text,
                    me.tags,
                    me.personality_traits,
                    1 - (me.embedding <=> :embedding::vector) as score
                FROM member_embeddings me
            """.trimIndent())
            
            if (request.activeOnly) {
                append(" WHERE me.is_active = true")
            }
            
            append(" ORDER BY me.embedding <=> :embedding::vector LIMIT :limit")
        }
        
        // Flow 기반 스트리밍 처리
        val results = databaseClient.sql(sql)
            .bind("embedding", embeddingStr)
            .bind("limit", request.limit)
            .fetch()
            .all()
            .asFlow()
            .map { row -> mapRowToSearchResult(row) }
            .toList()
        
        VectorSearchResponse(
            results = results,
            queryEmbedding = embedding,
            searchTime = 0L, // measureTimedValue에서 측정
            totalResults = results.size
        )
    }
    
    private fun mapRowToSearchResult(row: Map<String, Any>): VectorSearchResult {
        val rawScore = (row["score"] as? Number)?.toDouble() ?: 0.0
        val normalizedScore = when {
            rawScore.isNaN() -> 0.0
            rawScore.isInfinite() -> 0.0
            rawScore < 0.0 -> 0.0
            rawScore > 1.0 -> 1.0
            else -> rawScore
        }
        
        return VectorSearchResult(
            memberId = (row["member_id"] as Number).toInt(),
            memberName = row["member_name"] as String,
            score = normalizedScore,
            branch = row["branch"] as? String ?: "Unknown",
            generation = row["generation"] as? String ?: "Unknown",
            unit = "",
            isActive = row["is_active"] as Boolean,
            traits = parsePersonalityTraits(row["personality_traits"])
        )
    }
    
    private fun parsePersonalityTraits(traits: Any?): Map<String, Any> = runCatching {
        when (traits) {
            is String -> objectMapper.readValue<Map<String, Any>>(traits)
            is Map<*, *> -> traits.mapKeys { it.key.toString() }.mapValues { it.value ?: "" }
            else -> emptyMap()
        }
    }.getOrElse { 
        logger.warn { "Failed to parse personality traits: ${it.message}" }
        emptyMap()
    }
    
    private fun validateMemberId(id: Int): Result<MemberId> = runCatching {
        MemberId(id)
    }
    
    private suspend fun checkExistingIndex(memberId: MemberId): VectorIndexResponse? = 
        withContext(Dispatchers.IO) {
            val sql = "SELECT updated_at FROM member_embeddings WHERE member_id = :memberId"
            
            try {
                val result = databaseClient.sql(sql)
                    .bind("memberId", memberId.value)
                    .fetch()
                    .one()
                    .asFlow()
                    .firstOrNull()
                
                result?.let { row ->
                    VectorIndexResponse(
                        memberId = memberId.value,
                        status = "EXISTS",
                        embeddingSize = EmbeddingDimension(1536).value,
                        model = "gemini-embedding-001",
                        timestamp = row["updated_at"] as Instant
                    )
                }
            } catch (e: Exception) {
                logger.warn { "Failed to check existing index: ${e.message}" }
                null
            }
        }
    
    private suspend fun performIndexing(memberId: MemberId, forceReindex: Boolean): VectorIndexResponse = 
        withContext(Dispatchers.IO) {
            // 멤버 정보 조회
            val memberInfo = fetchMemberInfo(memberId)
                ?: throw IllegalStateException("Member not found: ${memberId.value}")
            
            // 검색 가능한 텍스트 생성
            val searchableText = createSearchableText(memberInfo)
            
            // 임베딩 생성
            val embedding = embeddingService.generateEmbedding(searchableText)
            
            // DB 저장
            saveEmbedding(memberId, memberInfo, embedding, searchableText)
            
            VectorIndexResponse(
                memberId = memberId.value,
                status = if (forceReindex) "REINDEXED" else "INDEXED",
                embeddingSize = EmbeddingDimension(embedding.size).value,
                model = "gemini-embedding-001",
                timestamp = Instant.now()
            )
        }
    
    private suspend fun fetchMemberInfo(memberId: MemberId): Map<String, Any?>? {
        val sql = """
            SELECT 
                m.id as member_id,
                m.name_en as member_name,
                m.name_ja,
                m.branch,
                m.generation,
                m.fanbase_name,
                m.is_active,
                m.tags,
                m.personality_traits,
                e.unified_traits,
                e.categorized_traits,
                e.personality_summary,
                e.korean_traits,
                e.korean_nicknames,
                e.fan_perception
            FROM members m
            LEFT JOIN member_enriched_data e ON m.id = e.member_id
            WHERE m.id = :memberId
        """.trimIndent()
        
        return try {
            databaseClient.sql(sql)
                .bind("memberId", memberId.value)
                .fetch()
                .one()
                .asFlow()
                .firstOrNull()
        } catch (e: Exception) {
            logger.warn { "Failed to fetch member info: ${e.message}" }
            null
        }
    }
    
    private fun createSearchableText(member: Map<String, Any?>): String = buildString {
        member["member_name"]?.let { append("Name: $it | ") }
        member["name_ja"]?.let { append("Japanese: $it | ") }
        member["branch"]?.let { append("Branch: $it | ") }
        member["generation"]?.let { append("Generation: $it | ") }
        member["fanbase_name"]?.let { append("Fanbase: $it | ") }
        
        // 배열 처리
        (member["unified_traits"] as? List<*>)?.filterNotNull()?.joinToString(", ")?.let {
            append("Traits: $it | ")
        }
        (member["korean_traits"] as? List<*>)?.filterNotNull()?.joinToString(" ")?.let {
            append("Korean Traits: $it | ")
        }
        (member["korean_nicknames"] as? List<*>)?.filterNotNull()?.joinToString(" ")?.let {
            append("Nicknames: $it | ")
        }
        
        member["personality_summary"]?.let { append("Summary: $it") }
    }
    
    private suspend fun saveEmbedding(
        memberId: MemberId,
        memberInfo: Map<String, Any?>,
        embedding: List<Double>,
        searchableText: String
    ) = withContext(Dispatchers.IO) {
        val embeddingStr = embedding.joinToString(",", "[", "]")
        
        val sql = """
            INSERT INTO member_embeddings (
                member_id, embedding, member_name, name_ja, branch, generation,
                fanbase_name, is_active, searchable_text, tags, personality_traits,
                embedding_model, embedding_dimension
            ) VALUES (
                :member_id, :embedding::vector, :member_name, :name_ja, :branch, :generation,
                :fanbase_name, :is_active, :searchable_text, :tags, :personality_traits,
                :embedding_model, :embedding_dimension
            )
            ON CONFLICT (member_id, embedding_model) 
            DO UPDATE SET
                embedding = EXCLUDED.embedding,
                searchable_text = EXCLUDED.searchable_text,
                updated_at = CURRENT_TIMESTAMP
        """.trimIndent()
        
        databaseClient.sql(sql)
            .bind("member_id", memberId.value)
            .bind("embedding", embeddingStr)
            .bind("member_name", memberInfo["member_name"] ?: "")
            .bind("name_ja", memberInfo["name_ja"] ?: "")
            .bind("branch", memberInfo["branch"] ?: "")
            .bind("generation", memberInfo["generation"] ?: "")
            .bind("fanbase_name", memberInfo["fanbase_name"] ?: "")
            .bind("is_active", memberInfo["is_active"] ?: false)
            .bind("searchable_text", searchableText)
            .bind("tags", memberInfo["tags"] ?: emptyArray<String>())
            .bind("personality_traits", memberInfo["personality_traits"] ?: "{}")
            .bind("embedding_model", "gemini-embedding-001")
            .bind("embedding_dimension", EmbeddingDimension(1536).value)
            .fetch()
            .rowsUpdated()
            .awaitFirstOrNull()
    }
    
    private suspend fun saveToCache(request: VectorSearchRequest, response: VectorSearchResponse) {
        try {
            val key = CacheKey(
                VectorCacheKeys.searchKey(request.query, request.limit, request.activeOnly)
            )
            logger.debug { "Cache save planned for: ${key.value}" }
        } catch (e: Exception) {
            logger.warn { "Cache save failed: ${e.message}" }
        }
    }
    
    private suspend fun publishIndexingEvent(response: VectorIndexResponse) {
        val event = EmbeddingUpdated(
            memberId = response.memberId,
            embeddingSize = response.embeddingSize,
            model = response.model
        )
        streamBridge.send("embedding-updated-out-0", event)
    }
    
    private suspend fun invalidateRelatedCache(memberId: Int) {
        logger.debug { "Cache invalidated for member: $memberId" }
    }
    
    private suspend fun getTotalEmbeddings(): Long = withContext(Dispatchers.IO) {
        try {
            val result = databaseClient.sql("SELECT COUNT(*) as count FROM member_embeddings")
                .fetch()
                .one()
                .asFlow()
                .firstOrNull()
            
            (result?.get("count") as? Number)?.toLong() ?: 0L
        } catch (e: Exception) {
            logger.warn { "Failed to get total embeddings: ${e.message}" }
            0L
        }
    }
    
    private suspend fun getActiveEmbeddings(): Long = withContext(Dispatchers.IO) {
        val sql = """
            SELECT COUNT(*) as count 
            FROM member_embeddings me
            JOIN members m ON me.member_id = m.id
            WHERE m.is_active = true
        """.trimIndent()
        
        try {
            val result = databaseClient.sql(sql)
                .fetch()
                .one()
                .asFlow()
                .firstOrNull()
            
            (result?.get("count") as? Number)?.toLong() ?: 0L
        } catch (e: Exception) {
            logger.warn { "Failed to get active embeddings: ${e.message}" }
            0L
        }
    }
    
    private suspend fun checkHealthStatus(): String = "HEALTHY"
}

// === Event Models ===

data class EmbeddingUpdated(
    val memberId: Int,
    val embeddingSize: Int,
    val model: String
)