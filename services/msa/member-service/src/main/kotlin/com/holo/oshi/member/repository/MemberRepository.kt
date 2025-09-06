package com.holo.oshi.member.repository

import com.holo.oshi.member.model.Member
import com.holo.oshi.member.model.MemberEnrichedData
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface MemberRepository : CoroutineCrudRepository<Member, Long> {
    // id 기반 조회 (Primary Key 사용)
    suspend fun findByIsActiveTrue(): Flow<Member>
    
    suspend fun findByBranch(branch: String): Flow<Member>
    
    suspend fun findByGeneration(generation: String): Flow<Member>
    
    @Query("""
        SELECT * FROM members 
        WHERE branch = :branch 
        AND generation = :generation 
        AND is_active = true
    """)
    suspend fun findByBranchAndGeneration(branch: String, generation: String): Flow<Member>  // Int -> String 수정
    
    @Query("""
        SELECT * FROM members 
        WHERE unit = :unit 
        AND is_active = true
    """)
    suspend fun findByUnit(unit: String): Flow<Member>
}

interface MemberEnrichedDataRepository : CoroutineCrudRepository<MemberEnrichedData, Long> {
    suspend fun findByMemberId(memberId: Int): MemberEnrichedData?  // String -> Int 수정
    
    @Query("""
        SELECT * FROM member_enriched_data 
        WHERE member_id IN (
            SELECT id FROM members 
            WHERE is_active = true
        )
    """)
    suspend fun findAllActiveEnrichedData(): Flow<MemberEnrichedData>
    
    @Query("""
        SELECT med.* FROM member_enriched_data med
        JOIN members m ON med.member_id = m.id
        WHERE m.branch = :branch
    """)
    suspend fun findByBranch(branch: String): Flow<MemberEnrichedData>
}