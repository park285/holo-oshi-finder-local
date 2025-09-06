package com.holo.oshi.member.controller

import arrow.core.*
import com.holo.oshi.member.service.KotlinMemberService
import com.holo.oshi.member.model.*
import com.holo.oshi.common.model.MemberId
import com.holo.oshi.common.model.BranchCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.http.MediaType
import mu.KotlinLogging

/**
 * Simple Kotlin Member Controller
 * 
 * 코틀린라이크 REST API:
 * - 기존 DTO와 완전 호환
 * - 간단하고 확실한 구현
 * - Type-Safe Operations
 * - Railway-Oriented Error Handling
 * - Extension Functions
 */
@RestController
@RequestMapping("/api/members")
class SimpleMemberController(
    private val memberService: KotlinMemberService
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }
    
    /**
     * DSL 기반 멤버 검색
     */
    @GetMapping
    suspend fun searchMembers(
        @RequestParam(required = false) branch: String?,
        @RequestParam(required = false) generation: String?,
        @RequestParam(defaultValue = "true") activeOnly: Boolean,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<List<MemberDto>> =
        try {
            val result = memberService.searchMembers {
                branch?.let { branch(it) }
                generation?.let { generation(it) }
                if (!activeOnly) includeInactive()
                limitTo(limit)
            }
            
            when (result) {
                is Either.Right -> ResponseEntity.ok(result.value)
                is Either.Left -> {
                    logger.error { "검색 실패: ${result.value}" }
                    ResponseEntity.badRequest().build()
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "검색 중 예외 발생" }
            ResponseEntity.internalServerError().build()
        }
    
    /**
     * 단일 멤버 조회
     */
    @GetMapping("/{id}")
    suspend fun getMemberById(@PathVariable id: Long): ResponseEntity<MemberDto> =
        try {
            val memberId = MemberId(id) // 기존 ValueClasses 사용
            
            val result = memberService.getMemberById(memberId)
            when (result) {
                is Either.Right -> ResponseEntity.ok(result.value)
                is Either.Left -> ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            logger.error(e) { "멤버 조회 중 예외: $id" }
            ResponseEntity.internalServerError().build()
        }
    
    /**
     * 지부별 멤버 조회
     */
    @GetMapping("/branch/{branchCode}")
    suspend fun getMembersByBranch(@PathVariable branchCode: String): ResponseEntity<List<MemberDto>> =
        try {
            val branchCodeValue = BranchCode(branchCode.lowercase()) // 기존 ValueClasses 사용
            
            val result = memberService.searchMembers {
                branch(branchCodeValue.value)
            }
            
            when (result) {
                is Either.Right -> ResponseEntity.ok(result.value)
                is Either.Left -> ResponseEntity.badRequest().build()
            }
        } catch (e: Exception) {
            logger.error(e) { "지부별 조회 중 예외: $branchCode" }
            ResponseEntity.internalServerError().build()
        }
    
    /**
     * 실시간 활성 멤버 스트림
     */
    @GetMapping("/stream/active", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    suspend fun streamActiveMembers(): Flow<String> =
        memberService.streamActiveMembers()
            .map { member ->
                "data: ${member.nameEn}\n\n"
            }
    
    /**
     * 헬스체크
     */
    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> =
        ResponseEntity.ok(mapOf(
            "status" to "UP",
            "service" to "member-service",
            "timestamp" to kotlinx.datetime.Clock.System.now().toString()
        ))
}