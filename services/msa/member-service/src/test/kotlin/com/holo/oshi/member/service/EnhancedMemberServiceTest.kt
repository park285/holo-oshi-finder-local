package com.holo.oshi.member.service

import com.holo.oshi.member.model.*
import com.holo.oshi.member.repository.MemberRepository
import com.holo.oshi.member.repository.MemberEnrichedDataRepository
import com.holo.oshi.member.event.MemberEventPublisher
import com.holo.oshi.member.service.EnhancedMemberService.Companion.MemberId
import com.holo.oshi.member.service.EnhancedMemberService.Companion.BranchCode
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate

/**
 * - MockK for mocking
 * - Coroutine test support
 * - Property-based testing concepts
 */
class EnhancedMemberServiceTest {
    
    @MockK
    private lateinit var memberRepository: MemberRepository
    
    @MockK
    private lateinit var enrichedDataRepository: MemberEnrichedDataRepository
    
    @MockK
    private lateinit var memberEventPublisher: MemberEventPublisher
    
    private lateinit var service: EnhancedMemberService
    
    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        service = EnhancedMemberService(
            memberRepository,
            enrichedDataRepository,
            memberEventPublisher
        )
    }
    
    @Test
    fun `getMemberById should return member when exists`() = runTest {
        // Given
        val memberId = MemberId(1L)
        val member = createTestMember(id = 1L)
        
        every { memberRepository.findById(1L) } returns Mono.just(member)
        
        // When
        val result = service.getMemberById(memberId)
        
        // Then
        assertThat(result.isSuccess).isTrue()
        result.getOrNull()?.let { dto ->
            assertThat(dto.id).isEqualTo(1L)
            assertThat(dto.nameEn).isEqualTo("Tokino Sora")
            assertThat(dto.branch).isEqualTo("jp")
        }
        
        verify(exactly = 1) { memberRepository.findById(1L) }
    }
    
    @Test
    fun `getMemberById should return error when member not found`() = runTest {
        // Given
        val memberId = MemberId(999L)
        
        every { memberRepository.findById(999L) } returns Mono.empty()
        
        // When
        val result = service.getMemberById(memberId)
        
        // Then
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(MemberNotFoundException::class.java)
    }
    
    @Test
    fun `getAllMembersAsFlow should stream active members`() = runTest {
        // Given
        val members = listOf(
            createTestMember(id = 1L, nameEn = "Tokino Sora"),
            createTestMember(id = 2L, nameEn = "Shirakami Fubuki"),
            createTestMember(id = 3L, nameEn = "Inugami Korone")
        )
        
        every { memberRepository.findByIsActiveTrue() } returns Flux.fromIterable(members)
        
        // When
        val results = service.getAllMembersAsFlow(activeOnly = true).toList()
        
        // Then
        assertThat(results).hasSize(3)
        assertThat(results.map { it.nameEn }).containsExactly(
            "Tokino Sora",
            "Shirakami Fubuki",
            "Inugami Korone"
        )
    }
    
    @Test
    fun `getMembersByBranch should validate branch code`() = runTest {
        // Given
        val validBranch = BranchCode("jp")
        val members = listOf(
            createTestMember(id = 1L, branch = "jp"),
            createTestMember(id = 2L, branch = "jp")
        )
        
        every { memberRepository.findByBranch("jp") } returns Flux.fromIterable(members)
        
        // When
        val results = service.getMembersByBranch(validBranch)
        
        // Then
        assertThat(results).hasSize(2)
        assertThat(results.all { it.branch == "jp" }).isTrue()
    }
    
    @Test
    fun `invalid branch code should throw exception`() {
        // When/Then
        assertThrows<IllegalArgumentException> {
            BranchCode("invalid")
        }
    }
    
    @Test
    fun `createMember should publish event on success`() = runTest {
        // Given
        val request = CreateMemberRequest(
            nameEn = "Test Member",
            nameJp = "テストメンバー",
            generation = "gen0",
            branch = "jp"
        )
        
        val savedMember = createTestMember(id = 100L, nameEn = "Test Member")
        
        every { memberRepository.save(any()) } returns Mono.just(savedMember)
        every { memberEventPublisher.publishMemberCreated(any()) } returns true
        
        // When
        val result = service.createMember(request)
        
        // Then
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.nameEn).isEqualTo("Test Member")
        
        verify { memberEventPublisher.publishMemberCreated(savedMember) }
    }
    
    @Test
    fun `updateMember should track changes correctly`() = runTest {
        // Given
        val memberId = MemberId(1L)
        val existingMember = createTestMember(
            id = 1L,
            nameEn = "Old Name",
            branch = "jp"
        )
        
        val updateRequest = UpdateMemberRequest(
            nameEn = "New Name",
            branch = "en"
        )
        
        val updatedMember = existingMember.copy(
            nameEn = "New Name",
            branch = "en"
        )
        
        every { memberRepository.findById(1L) } returns Mono.just(existingMember)
        every { memberRepository.save(any()) } returns Mono.just(updatedMember)
        every { memberEventPublisher.publishMemberUpdated(any(), any(), any()) } returns true
        
        // When
        val result = service.updateMember(memberId, updateRequest)
        
        // Then
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.nameEn).isEqualTo("New Name")
        assertThat(result.getOrNull()?.branch).isEqualTo("en")
        
        verify { 
            memberEventPublisher.publishMemberUpdated(
                currentMember = updatedMember,
                previousMember = existingMember,
                changedFields = match { it.contains("nameEn") && it.contains("branch") }
            )
        }
    }
    
    @Test
    fun `searchMembers should filter by criteria`() = runTest {
        // Given
        val criteria = SearchCriteria(
            branch = "jp",
            activeOnly = true
        )
        
        val enrichedData = listOf(
            MemberEnrichedData(
                id = 1,
                memberId = 1,
                singingSkill = 9,
                gamingSkill = 5,
                englishProficiency = 3
            )
        )
        
        val member = createTestMember(id = 1L)
        
        every { enrichedDataRepository.findByBranch("jp") } returns Flux.fromIterable(enrichedData)
        every { memberRepository.findById(1L) } returns Mono.just(member)
        
        // When
        val results = service.searchMembers(criteria)
        
        // Then
        assertThat(results).hasSize(1)
        assertThat(results.first().member.id).isEqualTo(1L)
    }
    
    // Helper function
    private fun createTestMember(
        id: Long = 1L,
        nameEn: String = "Tokino Sora",
        nameJp: String? = "ときのそら",
        generation: String? = "gen0",
        branch: String? = "jp",
        isActive: Boolean = true
    ) = Member(
        id = id,
        nameEn = nameEn,
        nameJp = nameJp,
        generation = generation,
        branch = branch,
        unit = null,
        debutDate = LocalDate.of(2017, 9, 7),
        birthday = LocalDate.of(2000, 5, 15),
        height = 160,
        fanbase = "Soratomo",
        emoji = "🐻",
        youtubeChannel = "@TokinoSora",
        twitterHandle = "@tokino_sora",
        isActive = isActive,
        graduationDate = null,
        graduationType = null,
        graduationReason = null
    )
}

/**
 * Property-based testing concepts
 */
class MemberServicePropertyTest {
    
    @Test
    fun `member ID should always be positive`() {
        // Property: All valid member IDs must be > 0
        val validIds = listOf(1L, 100L, Long.MAX_VALUE)
        validIds.forEach { id ->
            assertThat(MemberId(id).value).isGreaterThan(0)
        }
        
        val invalidIds = listOf(0L, -1L, Long.MIN_VALUE)
        invalidIds.forEach { id ->
            assertThrows<IllegalArgumentException> {
                MemberId(id)
            }
        }
    }
    
    @Test
    fun `branch code should only accept valid values`() {
        // Property: Branch codes must be in the valid set
        val validBranches = listOf("jp", "en", "id")
        validBranches.forEach { branch ->
            val code = BranchCode(branch)
            assertThat(code.value).isIn(BranchCode.VALID_BRANCHES)
        }
        
        val invalidBranches = listOf("", "kr", "cn", "invalid")
        invalidBranches.forEach { branch ->
            assertThrows<IllegalArgumentException> {
                BranchCode(branch)
            }
        }
    }
}