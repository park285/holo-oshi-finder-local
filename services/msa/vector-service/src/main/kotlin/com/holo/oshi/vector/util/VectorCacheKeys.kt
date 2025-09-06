package com.holo.oshi.vector.util

import java.security.MessageDigest

/**
 * 캐시 키 생성 유틸리티
 * 일관되고 충돌 없는 캐시 키 관리
 */
object VectorCacheKeys {
    
    private const val VECTOR_PREFIX = "vector"
    private const val SEARCH_PREFIX = "search"
    private const val INDEX_PREFIX = "index"
    private const val MEMBER_PREFIX = "member"
    
    /**
     * 벡터 검색 캐시 키 생성
     */
    fun searchKey(query: String, limit: Int, activeOnly: Boolean): String {
        val params = listOf(
            query.lowercase().trim(),
            limit.toString(),
            activeOnly.toString()
        ).joinToString(":")
        
        return "$VECTOR_PREFIX:$SEARCH_PREFIX:${hashKey(params)}"
    }
    
    /**
     * 멤버 인덱스 캐시 키 생성
     */
    fun memberIndexKey(memberId: Int, model: String = "gemini-embedding-001"): String {
        return "$VECTOR_PREFIX:$INDEX_PREFIX:$MEMBER_PREFIX:$memberId:$model"
    }
    
    /**
     * 임베딩 캐시 키 생성
     */
    fun embeddingKey(text: String, model: String = "gemini-embedding-001"): String {
        val textHash = hashKey(text.lowercase().trim())
        return "$VECTOR_PREFIX:embedding:$model:$textHash"
    }
    
    /**
     * 상태 캐시 키 생성
     */
    fun statusKey(): String {
        return "$VECTOR_PREFIX:status"
    }
    
    /**
     * 멤버별 검색 결과 캐시 키
     */
    fun memberSearchKey(memberId: Int, query: String): String {
        val queryHash = hashKey(query.lowercase().trim())
        return "$VECTOR_PREFIX:$MEMBER_PREFIX:$memberId:search:$queryHash"
    }
    
    /**
     * 유사도 검색 캐시 키 (임베딩 벡터 기반)
     */
    fun similarityKey(embeddingHash: String, limit: Int, threshold: Double = 0.0): String {
        return "$VECTOR_PREFIX:similarity:$embeddingHash:$limit:${threshold.toString().replace(".", "_")}"
    }
    
    /**
     * 패턴별 캐시 클리어용 키
     */
    fun clearPattern(memberId: Int? = null): String {
        return if (memberId != null) {
            "$VECTOR_PREFIX:*$MEMBER_PREFIX:$memberId*"
        } else {
            "$VECTOR_PREFIX:*"
        }
    }
    
    /**
     * 검색 패턴별 캐시 클리어
     */
    fun clearSearchPattern(): String {
        return "$VECTOR_PREFIX:$SEARCH_PREFIX:*"
    }
    
    /**
     * 인덱스 패턴별 캐시 클리어
     */
    fun clearIndexPattern(): String {
        return "$VECTOR_PREFIX:$INDEX_PREFIX:*"
    }
    
    /**
     * 문자열을 해시로 변환 (MD5 사용)
     * 캐시 키 길이 제한 및 특수문자 방지
     */
    private fun hashKey(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.fold("") { str, byte -> str + "%02x".format(byte) }
    }
    
    /**
     * TTL 설정용 Duration 상수
     */
    object TTL {
        val SEARCH_RESULT = java.time.Duration.ofMinutes(5)      // 검색 결과: 5분
        val MEMBER_INDEX = java.time.Duration.ofHours(1)         // 멤버 인덱스: 1시간
        val EMBEDDING = java.time.Duration.ofHours(24)           // 임베딩: 24시간
        val STATUS = java.time.Duration.ofMinutes(1)             // 상태 정보: 1분
        val SIMILARITY = java.time.Duration.ofMinutes(10)        // 유사도 검색: 10분
    }
    
    /**
     * 캐시 키 검증
     */
    fun isValidKey(key: String): Boolean {
        return key.startsWith(VECTOR_PREFIX) && 
               key.length <= 250 && // Redis 키 길이 제한
               key.matches(Regex("^[a-zA-Z0-9:._-]+$")) // 안전한 문자만 허용
    }
    
    /**
     * 캐시 키에서 정보 추출
     */
    data class KeyInfo(
        val type: String,
        val subType: String?,
        val identifier: String,
        val parameters: Map<String, String> = emptyMap()
    )
    
    /**
     * 캐시 키 파싱
     */
    fun parseKey(key: String): KeyInfo? {
        if (!isValidKey(key)) return null
        
        val parts = key.split(":")
        if (parts.size < 3 || parts[0] != VECTOR_PREFIX) return null
        
        return KeyInfo(
            type = parts[1],
            subType = parts.getOrNull(2),
            identifier = parts.drop(3).firstOrNull() ?: "",
            parameters = parts.drop(4).chunked(2)
                .mapNotNull { if (it.size == 2) it[0] to it[1] else null }
                .toMap()
        )
    }
}