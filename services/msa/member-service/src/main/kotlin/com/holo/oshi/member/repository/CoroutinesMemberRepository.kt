package com.holo.oshi.member.repository

import com.holo.oshi.common.model.*
import com.holo.oshi.member.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface CoroutinesMemberRepository : CoroutineCrudRepository<Member, Long> {
    
    fun findByIsActiveTrue(): Flow<Member>
    
    /**
     * 브랜치별 조회
     */
    suspend fun findByBranch(branch: String): Flow<Member>
    
    /**
     * 세대별 조회 with Value Class
     */
    suspend fun findByGeneration(generation: String): Flow<Member>
    
    // ========== 복합 조건 조회 ==========
    
    /**
     * 브랜치 + 세대 조합 조회
     */
    @Query("""
        SELECT * FROM members 
        WHERE branch = :branch 
        AND generation = :generation 
        AND is_active = true
        ORDER BY id
    """)
    fun findByBranchAndGeneration(
        branch: String, 
        generation: String
    ): Flow<Member>
    
    /**
     * 유닛별 활성 멤버 조회
     */
    @Query("""
        SELECT * FROM members 
        WHERE unit = :unit 
        AND is_active = true
        ORDER BY debut_date, id
    """)
    fun findByUnit(unit: String): Flow<Member>
    
    /**
     * 데뷔일 기준 조회
     */
    @Query("""
        SELECT * FROM members 
        WHERE debut_date BETWEEN :startDate AND :endDate
        AND is_active = :activeOnly
        ORDER BY debut_date
    """)
    fun findByDebutDateRange(
        startDate: String,
        endDate: String,
        activeOnly: Boolean = true
    ): Flow<Member>
    
    /**
     * 태그 기반 검색
     */
    @Query("""
        SELECT * FROM members 
        WHERE :tag = ANY(tags)
        AND is_active = :activeOnly
    """)
    fun findByTag(
        tag: String,
        activeOnly: Boolean = true
    ): Flow<Member>
    
    /**
     * 멤버 존재 여부 확인
     */
    suspend fun existsByNameEnAndBranch(
        nameEn: String,
        branch: String
    ): Boolean
    
    /**
     * 활성 멤버 수 조회
     */
    @Query("SELECT COUNT(*) FROM members WHERE is_active = true")
    suspend fun countActiveMembers(): Long
    
    /**
     * 브랜치별 멤버 수 조회
     */
    @Query("""
        SELECT branch, COUNT(*) as count 
        FROM members 
        WHERE is_active = true 
        GROUP BY branch
    """)
    suspend fun countMembersByBranch(): Flow<BranchCount>
}

/**
 * Enriched Data Repository - Coroutines 버전
 */
interface CoroutinesEnrichedDataRepository : CoroutineCrudRepository<MemberEnrichedData, Long> {
    
    /**
     * 멤버 ID로 enriched data 조회
     */
    suspend fun findByMemberId(memberId: Int): MemberEnrichedData?
    
    /**
     * 활성 멤버의 enriched data 조회
     */
    @Query("""
        SELECT * FROM member_enriched_data 
        WHERE member_id IN (
            SELECT id FROM members 
            WHERE is_active = true
        )
        ORDER BY member_id
    """)
    fun findAllActiveEnrichedData(): Flow<MemberEnrichedData>
    
    /**
     * 브랜치별 enriched data 조회
     */
    @Query("""
        SELECT med.* FROM member_enriched_data med
        JOIN members m ON med.member_id = m.id
        WHERE m.branch = :branch
        AND m.is_active = true
        ORDER BY med.member_id
    """)
    fun findByBranch(branch: String): Flow<MemberEnrichedData>
    
    /**
     * 트레잇 점수 기준 검색
     */
    @Query("""
        SELECT med.* FROM member_enriched_data med
        JOIN members m ON med.member_id = m.id
        WHERE m.is_active = true
        AND (
            (:minSinging IS NULL OR med.singing_skill >= :minSinging) AND
            (:minDancing IS NULL OR med.dancing_skill >= :minDancing) AND
            (:minVariety IS NULL OR med.variety_skill >= :minVariety)
        )
        ORDER BY 
            (COALESCE(med.singing_skill, 0) + 
             COALESCE(med.dancing_skill, 0) + 
             COALESCE(med.variety_skill, 0)) DESC
    """)
    fun findByMinimumTraits(
        minSinging: Int? = null,
        minDancing: Int? = null,
        minVariety: Int? = null
    ): Flow<MemberEnrichedData>
    
    /**
     * 배치 업데이트용 조회
     */
    @Query("""
        SELECT med.* FROM member_enriched_data med
        WHERE med.updated_at < :lastUpdateTime
        ORDER BY med.member_id
        LIMIT :limit
    """)
    fun findStaleData(
        lastUpdateTime: String,
        limit: Int = 100
    ): Flow<MemberEnrichedData>
}

/**
 * 브랜치별 카운트 결과
 */
data class BranchCount(
    val branch: String,
    val count: Long
)

/**
 * Repository Extension Functions
 */

/**
 * Value Class를 사용한 조회
 */
suspend fun CoroutinesMemberRepository.findByMemberId(
    id: MemberId
): Member? = findById(id.value)

suspend fun CoroutinesMemberRepository.findByBranchCode(
    branch: BranchCode
): Flow<Member> = findByBranch(branch.value)

suspend fun CoroutinesMemberRepository.findByGenerationCode(
    generation: GenerationCode
): Flow<Member> = findByGeneration(generation.value)

/**
 * 복합 검색 with Value Classes
 */
suspend fun CoroutinesMemberRepository.search(
    branch: BranchCode? = null,
    generation: GenerationCode? = null,
    activeOnly: Boolean = true
): Flow<Member> {
    return when {
        branch != null && generation != null -> 
            findByBranchAndGeneration(branch.value, generation.value)
        branch != null -> 
            findByBranchCode(branch)
        generation != null -> 
            findByGenerationCode(generation)
        activeOnly -> 
            findByIsActiveTrue()
        else -> 
            findAll()
    }.let { flow ->
        if (activeOnly) {
            flow.filter { it.isActive }
        } else {
            flow
        }
    }
}

/**
 * 통계 조회 Extensions
 */
suspend fun CoroutinesMemberRepository.getStatistics(): MemberStatistics {
    val totalCount = count()
    val activeCount = countActiveMembers()
    val branchCounts = countMembersByBranch()
        .collect { map ->
            mutableMapOf<String, Long>().apply {
                this[map.branch] = map.count
            }
        }
    
    return MemberStatistics(
        total = totalCount,
        active = activeCount,
        inactive = totalCount - activeCount,
        byBranch = emptyMap() // branchCounts 처리 필요
    )
}

/**
 * 멤버 통계 데이터
 */
data class MemberStatistics(
    val total: Long,
    val active: Long,
    val inactive: Long,
    val byBranch: Map<String, Long>
)