package com.holo.oshi.vector.controller

import com.holo.oshi.vector.model.*
import com.holo.oshi.vector.service.VectorSearchService
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import kotlin.time.measureTimedValue

@RestController
class VectorController(
    private val vectorSearchService: VectorSearchService
) {
    
    companion object {
        private val logger = KotlinLogging.logger {}
        
        // Value Classes for type safety
        @JvmInline
        value class MemberId(val value: Int) {
            init {
                require(value > 0) { "MemberId must be positive: $value" }
            }
        }
        
        @JvmInline
        value class VectorScore(val value: Double) {
            init {
                require(value in 0.0..1.0) { "VectorScore must be between 0 and 1: $value" }
            }
        }
    }
    
    /**
     * 벡터 유사도 검색 - Pure Kotlin with Result handling
     */
    @PostMapping("/api/vector/search")
    suspend fun search(
        @RequestBody request: VectorSearchRequest
    ): ResponseEntity<VectorSearchResponse> {
        logger.info { "Vector search: query='${request.query}', limit=${request.limit}" }
        
        val (result, duration) = measureTimedValue {
            vectorSearchService.searchSimilar(request)
        }
        
        return result.fold(
            onSuccess = { response ->
                logger.info { 
                    "Search completed: ${response.results.size} results in ${duration.inWholeMilliseconds}ms" 
                }
                ResponseEntity.ok(response)
            },
            onFailure = { error ->
                logger.error(error) { "Search failed in ${duration.inWholeMilliseconds}ms" }
                ResponseEntity.internalServerError().body(
                    VectorSearchResponse(
                        results = emptyList(),
                        searchTime = duration.inWholeMilliseconds,
                        totalResults = 0
                    )
                )
            }
        )
    }
    
    /**
     * 멤버 벡터 인덱싱 - Result handling 적용
     */
    @PostMapping("/api/vector/index")
    suspend fun indexMember(
        @RequestBody request: VectorIndexRequest
    ): ResponseEntity<VectorIndexResponse> {
        logger.info { "Indexing member: ${request.memberId}" }
        
        val (result, duration) = measureTimedValue {
            vectorSearchService.indexMember(request)
        }
        
        return result.fold(
            onSuccess = { response ->
                logger.info { 
                    "Indexing completed: memberId=${response.memberId}, status=${response.status}, duration=${duration.inWholeMilliseconds}ms" 
                }
                ResponseEntity.ok(response)
            },
            onFailure = { error ->
                logger.error(error) { "Indexing failed for memberId=${request.memberId} in ${duration.inWholeMilliseconds}ms" }
                ResponseEntity.internalServerError().body(
                    VectorIndexResponse(
                        memberId = request.memberId,
                        status = "FAILED",
                        embeddingSize = 0,
                        model = "",
                        timestamp = java.time.Instant.now()
                    )
                )
            }
        )
    }
    
    /**
     * 특정 멤버 재인덱싱 - Type Safe with Value Class and Result
     */
    @PutMapping("/api/vector/index/{memberId}")
    suspend fun reindexMember(
        @PathVariable memberIdValue: Int
    ): ResponseEntity<VectorIndexResponse> {
        return runCatching {
            // Value Class로 타입 안전성 확보
            MemberId(memberIdValue)
        }.fold(
            onSuccess = { memberId ->
                logger.info { "Reindexing started: ${memberId.value}" }
                
                val request = VectorIndexRequest(
                    memberId = memberId.value,
                    forceReindex = true
                )
                
                val (result, duration) = measureTimedValue {
                    vectorSearchService.indexMember(request)
                }
                
                result.fold(
                    onSuccess = { response ->
                        logger.info { 
                            "Reindexing completed: memberId=${memberId.value}, status=${response.status}, duration=${duration.inWholeMilliseconds}ms" 
                        }
                        ResponseEntity.ok(response)
                    },
                    onFailure = { error ->
                        logger.error(error) { "Reindexing failed for memberId=${memberId.value}" }
                        ResponseEntity.internalServerError().build()
                    }
                )
            },
            onFailure = { error ->
                logger.error(error) { "Invalid memberId: $memberIdValue" }
                ResponseEntity.badRequest().build()
            }
        )
    }
    
    /**
     * 벡터 DB 상태 조회
     */
    @GetMapping("/api/vector/status")
    suspend fun getStatus(): ResponseEntity<Map<String, Any>> {
        logger.debug { "Vector status requested" }
        
        val status = vectorSearchService.getStatus()
        
        return ResponseEntity.ok(status)
    }
    
    /**
     * 헬스체크
     */
    @GetMapping("/health")
    suspend fun health(): ResponseEntity<Map<String, Any>> {
        logger.debug { "Health check requested" }
        
        return ResponseEntity.ok(mapOf(
            "status" to "UP",
            "service" to "vector-service",
            "port" to 50002,
            "timestamp" to System.currentTimeMillis()
        ))
    }
}