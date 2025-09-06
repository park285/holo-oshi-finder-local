package com.holo.oshi.member.service

import arrow.core.*
import com.holo.oshi.common.extensions.*
import com.holo.oshi.member.model.*
import com.holo.oshi.common.model.MemberId
import com.holo.oshi.common.model.BranchCode
import com.holo.oshi.member.repository.CoroutinesMemberRepository
import com.holo.oshi.member.event.MemberEventPublisher
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.springframework.stereotype.Service
import mu.KotlinLogging
import kotlin.time.Duration
import kotlin.time.measureTimedValue

/**
 * Member Service
 * 
 * Coroutines 기반 멤버 관리 서비스
 * 
 * 이 하나의 서비스가 모든 기능을 통합합니다.
 */
@Service
class KotlinMemberService(
    private val repository: CoroutinesMemberRepository,  // 더 코틀린라이크한 Repository 사용
    private val eventPublisher: MemberEventPublisher
) {
    companion object {
        private val logger = KotlinLogging.logger {}
        
        // 홀로라이브 도메인 상수들
        val VALID_BRANCHES = setOf("jp", "en", "id")
        val GENERATION_PATTERN = "gen\\d+".toRegex()
        val MAX_SEARCH_LIMIT = 100
        val DEFAULT_CACHE_TTL = Duration.parse("30m")
    }
    
    // Value Classes는 ValueClasses.kt에서 import됨
    
    // Value Classes는 ValueClasses.kt에서 import됨
    
    // DSL Builder
    
    @DslMarker
    annotation class MemberSearchDsl
    
    @MemberSearchDsl
    class MemberSearchQueryBuilder {
        var branch: BranchCode? = null
        var generation: String? = null
        var activeOnly: Boolean = true
        var tags: MutableSet<String> = mutableSetOf()
        var limit: Int = 20
        var sortBy: String = "name"
        var sortOrder: String = "asc"
        
        /**
         * DSL: branch selection
         */
        fun branch(code: String) = apply {
            this.branch = BranchCode(code.lowercase()) // 기존 ValueClasses.kt 사용
        }
        
        fun jp() = apply { this.branch = BranchCode.JP }
        fun en() = apply { this.branch = BranchCode.EN }
        fun id() = apply { this.branch = BranchCode.ID }
        
        /**
         * DSL: generation selection
         */
        fun generation(gen: String) = apply {
            this.generation = gen
        }
        
        fun gen0() = apply { this.generation = "gen0" }
        fun gen1() = apply { this.generation = "gen1" }
        
        /**
         * DSL: filtering options
         */
        fun includeInactive() = apply { this.activeOnly = false }
        fun activeOnly() = apply { this.activeOnly = true }
        
        /**
         * DSL: tag filtering
         */
        fun withTags(vararg newTags: String) = apply {
            this.tags.addAll(newTags)
        }
        
        fun tagged(tag: String) = apply { this.tags.add(tag) }
        
        /**
         * DSL: result limits
         */
        fun limitTo(count: Int) = apply {
            this.limit = count.coerceIn(1, MAX_SEARCH_LIMIT)
        }
        
        fun top5() = apply { this.limit = 5 }
        fun top20() = apply { this.limit = 20 }
        fun top50() = apply { this.limit = 50 }
        
        /**
         * DSL: sorting
         */
        fun sortByName() = apply { this.sortBy = "name" }
        fun sortByDebut() = apply { this.sortBy = "debut" }
        fun sortByPopularity() = apply { this.sortBy = "popularity" }
        
        fun ascending() = apply { this.sortOrder = "asc" }
        fun descending() = apply { this.sortOrder = "desc" }
        
        internal fun build(): MemberSearchQuery = MemberSearchQuery(
            branch = branch,
            generation = generation,
            activeOnly = activeOnly,
            tags = tags.toSet(),
            limit = limit,
            sortBy = sortBy,
            sortOrder = sortOrder
        )
    }
    
    /**
     * Immutable search query
     */
    data class MemberSearchQuery(
        val branch: BranchCode? = null,
        val generation: String? = null,
        val activeOnly: Boolean = true,
        val tags: Set<String> = emptySet(),
        val limit: Int = 20,
        val sortBy: String = "name",
        val sortOrder: String = "asc"
    ) {
        companion object {
            fun build(block: MemberSearchQueryBuilder.() -> Unit): MemberSearchQuery =
                MemberSearchQueryBuilder().apply(block).build()
        }
        
        // Query description for logging
        val description: String get() = buildString {
            append("검색: ")
            branch?.let { append("${it.value} 지부 ") }
            generation?.let { append("$it ") }
            if (!activeOnly) append("(졸업 포함) ")
            if (tags.isNotEmpty()) append("태그[${tags.joinToString(",")}] ")
            append("최대 $limit 개")
        }
    }
    
    // Service Methods
    
    /**
     * DSL 기반 멤버 검색
     */
    suspend fun searchMembers(
        queryBuilder: MemberSearchQueryBuilder.() -> Unit
    ): Either<DomainError, List<MemberDto>> {
        val query = MemberSearchQuery.build(queryBuilder)
        
        return try {
            logger.info { "검색: ${query.description}" }
            
            // CoroutinesMemberRepository 메서드들을 사용한 검색 로직
            val membersFlow: Flow<Member> = when {
                query.branch != null && query.generation != null -> {
                    repository.findByBranchAndGeneration(query.branch.value, query.generation!!)
                }
                query.branch != null -> {
                    repository.findByBranch(query.branch.value)
                }
                query.generation != null -> {
                    repository.findByGeneration(query.generation!!)
                }
                query.activeOnly -> {
                    repository.findByIsActiveTrue()
                }
                else -> {
                    repository.findByIsActiveTrue() // 기본적으로 활성 멤버만
                }
            }
            
            // Flow → List 변환 및 DTO 변환
            val members = membersFlow.toList()
            val results = members.take(query.limit).map { it.toMemberDto() }
            
            logger.info { "검색 완료: ${results.size}개 결과" }
            
            results.right()
        } catch (e: Exception) {
            logger.error(e) { "멤버 검색 실패" }
            DomainError.RepositoryError("Member search failed: ${e.message}").left()
        }
    }
    
    /**
     * 단일 멤버 조회
     */
    suspend fun getMemberById(id: MemberId): Either<DomainError, MemberDto> =
        try {
            val member = repository.findById(id.value)
                ?: return DomainError.MemberNotFound("Member ${id.value} not found").left()
            
            member.toMemberDto().right()
        } catch (e: Exception) {
            logger.error(e) { "멤버 조회 실패: ${id.value}" }
            DomainError.RepositoryError("Member retrieval failed: ${e.message}").left()
        }
    
    /**
     * 실시간 활성 멤버 스트림
     */
    fun streamActiveMembers(): Flow<MemberDto> = 
        repository.findByIsActiveTrue()  // CoroutinesMemberRepository 메서드 (suspend 아님)
            .map { member -> member.toMemberDto() }
            .catch { exception ->
                logger.error(exception) { "멤버 스트림 오류" }
                throw exception
            }
    
    // 기타 메서드들은 필요시 추가
    
    // Error Handling
    
    sealed interface DomainError {
        val message: String
        val code: String
        
        data class MemberNotFound(override val message: String) : DomainError {
            override val code: String = "MEMBER_NOT_FOUND"
        }
        
        data class InvalidMemberId(override val message: String) : DomainError {
            override val code: String = "INVALID_MEMBER_ID"
        }
        
        data class InvalidBranch(override val message: String) : DomainError {
            override val code: String = "INVALID_BRANCH"
        }
        
        data class InvalidGeneration(override val message: String) : DomainError {
            override val code: String = "INVALID_GENERATION"
        }
        
        data class InvalidMemberName(override val message: String) : DomainError {
            override val code: String = "INVALID_MEMBER_NAME"
        }
        
        data class InvalidSearchLimit(override val message: String) : DomainError {
            override val code: String = "INVALID_SEARCH_LIMIT"
        }
        
        data class RepositoryError(override val message: String) : DomainError {
            override val code: String = "REPOSITORY_ERROR"
        }
        
        data class CacheError(override val message: String) : DomainError {
            override val code: String = "CACHE_ERROR"
        }
        
        data class StreamError(override val message: String) : DomainError {
            override val code: String = "STREAM_ERROR"
        }
        
        data class ValidationError(override val message: String) : DomainError {
            override val code: String = "VALIDATION_ERROR"
        }
        
        data class OperationFailed(override val message: String) : DomainError {
            override val code: String = "OPERATION_FAILED"
        }
    }
    
    // DTOs는 Member.kt에 정의되어 있음
    
    // 비즈니스 DTOs는 필요시 추가
    
    // Helper 메서드들은 필요시 추가
}

// ============ Extension Functions (기존 클래스와 호환) ============

fun Member.toMemberDto(): MemberDto =
    MemberDto(
        id = this.id,
        nameEn = this.nameEn,
        nameJp = this.nameJp,
        generation = this.generation,
        branch = this.branch,
        unit = this.unit,
        debutDate = this.debutDate?.toString(),
        birthday = this.birthday?.toString(),
        height = this.height,
        fanbase = this.fanbase,
        emoji = this.emoji,
        youtubeChannel = this.youtubeChannel,
        twitterHandle = this.twitterHandle,
        isActive = this.isActive
    )

// Member, MemberDto, MemberEventPublisher는 모두 별도 파일에 정의됨