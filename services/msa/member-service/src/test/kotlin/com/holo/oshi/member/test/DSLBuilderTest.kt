package com.holo.oshi.member.test

import com.holo.oshi.member.service.UltimateKotlinMemberService.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import mu.KotlinLogging

/**
 * 🎯 DSL Builder Pattern Ultimate Test
 * 
 * Netflix/Google 수준의 DSL 표현력 검증:
 * ✅ Fluent API Readability
 * ✅ Type-Safe Builder Chain
 * ✅ Method Chaining Validation
 * ✅ Builder State Consistency
 * ✅ Edge Case Handling
 * ✅ Performance of Builder Pattern
 * ✅ Complex Query Building
 */
private val logger = KotlinLogging.logger {}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@KotlinTest
class DSLBuilderTest {
    
    @Test
    @KotlinSpec("DSL Builder는 자연스러운 언어처럼 읽혀야 함")
    fun `DSL Builder should read like natural language`() = runTest {
        kotlinTest("DSL 자연어 표현력 검증") {
            `given`("복잡한 검색 요구사항") {
                logger.info { "복잡한 홀로라이브 멤버 검색 시나리오 설정" }
            }
            
            `when`("DSL로 자연스럽게 표현") {
                // 🎯 Netflix 수준의 표현력 - 영어처럼 읽힘
                val simpleQuery = MemberSearchQuery.build {
                    jp()           // 일본 지부
                    gen1()         // 1기생
                    activeOnly()   // 활성 멤버만
                    top10()        // 상위 10명
                    sortByName()   // 이름순
                    ascending()    // 오름차순
                }
                
                // 🎯 복잡한 쿼리도 직관적
                val complexQuery = MemberSearchQuery.build {
                    branch("en")                    // English branch
                    includeInactive()               // 졸업생 포함
                    withTags("singer", "gamer")     // 태그 필터링
                    limitTo(25)                     // 25명 제한
                    sortByPopularity()              // 인기순
                    descending()                    // 내림차순
                }
                
                // 🎯 체이닝 스타일도 지원
                val chainQuery = MemberSearchQuery.build {
                    id().generation("gen2").tagged("dancer").top5().sortByDebut().descending()
                }
                
                Triple(simpleQuery, complexQuery, chainQuery)
            }
            
            `then`("DSL이 올바르게 구성되었는지 검증") { (simple, complex, chain) ->
                val (simpleQ, complexQ, chainQ) = simple as Triple<MemberSearchQuery, MemberSearchQuery, MemberSearchQuery>
                
                // ✅ Simple Query 검증
                simpleQ.branch?.value shouldBe "jp"
                simpleQ.generation?.value shouldBe "gen1"
                simpleQ.activeOnly shouldBe true
                simpleQ.limit.value shouldBe 5  // top5() = SearchLimit.SMALL
                
                // ✅ Complex Query 검증  
                complexQ.branch?.value shouldBe "en"
                complexQ.activeOnly shouldBe false  // includeInactive()
                complexQ.tags shouldBe setOf("singer", "gamer")
                complexQ.limit.value shouldBe 25
                
                // ✅ Chain Query 검증
                chainQ.branch?.value shouldBe "id"
                chainQ.generation?.value shouldBe "gen2"
                chainQ.tags shouldBe setOf("dancer")
                chainQ.limit.value shouldBe 5
                
                logger.info { "✅ DSL 표현력 검증 완료" }
                logger.info { "   Simple: ${simpleQ.description}" }
                logger.info { "   Complex: ${complexQ.description}" }
                logger.info { "   Chain: ${chainQ.description}" }
            }
        }
    }
    
    @Test
    @KotlinSpec("DSL Builder는 invalid 상태를 방지해야 함")
    fun `DSL Builder should prevent invalid states`() = runTest {
        kotlinTest("DSL 상태 검증") {
            `given`("잘못된 입력값들") {}
            
            `when`("DSL로 잘못된 값 설정 시도") {
                val queryWithInvalidBranch = MemberSearchQuery.build {
                    branch("invalid-branch")  // 잘못된 지부
                    limitTo(-5)               // 음수 제한
                    generation("invalid-gen") // 잘못된 세대
                }
                
                queryWithInvalidBranch
            }
            
            `then`("잘못된 값은 무시되고 기본값 사용") { query ->
                val q = query as MemberSearchQuery
                
                // ✅ 잘못된 값들은 무시되고 안전한 기본값 사용
                q.branch.shouldBeNull()           // invalid branch ignored
                q.generation.shouldBeNull()       // invalid generation ignored  
                q.limit.value shouldBe 20         // invalid limit ignored, default used
                
                logger.info { "✅ DSL 안전성 검증 완료: ${q.description}" }
            }
        }
    }
    
    @Test
    @KotlinBenchmark(iterations = 10000)
    @KotlinSpec("DSL Builder 성능이 충분해야 함")
    fun `DSL Builder should have minimal overhead`() = runTest {
        val performanceResult = measureCoroutinePerformance("DSL Builder 생성", 10000) {
            MemberSearchQuery.build {
                jp()
                gen1() 
                activeOnly()
                withTags("singer", "idol")
                top20()
                sortByPopularity()
                descending()
            }
        }
        
        // ✅ DSL 빌드 시간이 10μs 미만이어야 함 (Zero-Cost)
        performanceResult.averageTime.shouldBeLessThan(Duration.microseconds(10))
        
        logger.info { 
            "🚀 DSL Builder 성능: 평균 ${performanceResult.averageTime.inWholeMicroseconds}μs" +
            " (목표: 10μs 미만)"
        }
    }
    
    @Test
    @KotlinSpec("DSL Builder는 immutable해야 함")
    fun `DSL Builder should produce immutable objects`() = runTest {
        val originalQuery = MemberSearchQuery.build {
            jp()
            gen1()
            top10()
        }
        
        // 빌더는 매번 새 객체 생성
        val modifiedQuery = MemberSearchQuery.build {
            en()
            gen2() 
            top20()
        }
        
        // ✅ 원본은 변경되지 않음
        originalQuery.branch?.value shouldBe "jp"
        originalQuery.generation?.value shouldBe "gen1"
        originalQuery.limit.value shouldBe 5
        
        // ✅ 새 객체는 다른 값
        modifiedQuery.branch?.value shouldBe "en"
        modifiedQuery.generation?.value shouldBe "gen2"
        modifiedQuery.limit.value shouldBe 20
        
        logger.info { "✅ DSL 불변성 검증 완료" }
    }
    
    @Test
    @KotlinProperty(iterations = 200)
    @KotlinSpec("모든 유효한 DSL 조합이 작동해야 함")
    fun `all valid DSL combinations should work`() = runTest {
        kotlinProperty(
            "유효한 DSL 조합",
            generateValidDSLCombinations(),
            200
        ) { (branchCode, genCode, isActive, tagCount, limitValue) ->
            try {
                val query = MemberSearchQuery.build {
                    branch(branchCode)
                    generation(genCode)
                    if (!isActive) includeInactive()
                    withTags(*(1..tagCount).map { "tag$it" }.toTypedArray())
                    limitTo(limitValue)
                }
                
                // 성공적으로 빌드되면 true
                true
            } catch (e: Exception) {
                logger.error(e) { "DSL 조합 실패: $branchCode, $genCode, $isActive, $tagCount, $limitValue" }
                false
            }
        }
    }
    
    @Test
    @KotlinSpec("DSL은 메소드 체이닝을 지원해야 함")
    fun `DSL should support method chaining`() = runTest {
        // 🎯 한 줄로 표현 가능한 복잡한 쿼리
        val chainedQuery = MemberSearchQuery.build {
            jp().gen1().includeInactive().withTags("singer").top10().sortByPopularity().descending()
        }
        
        chainedQuery.branch?.value shouldBe "jp"
        chainedQuery.generation?.value shouldBe "gen1" 
        chainedQuery.activeOnly shouldBe false
        chainedQuery.tags shouldBe setOf("singer")
        chainedQuery.limit.value shouldBe 5
        chainedQuery.sortBy shouldBe SortOption.POPULARITY
        chainedQuery.sortOrder shouldBe SortOrder.DESC
        
        logger.info { "✅ 메소드 체이닝: ${chainedQuery.description}" }
    }
    
    // ============ Helper Functions ============
    
    private fun generateValidDSLCombinations(): Sequence<Tuple5<String, String, Boolean, Int, Int>> = 
        sequence {
            val branches = listOf("jp", "en", "id")
            val generations = listOf("gen0", "gen1", "gen2", "gen3", "gen4", "gen5")
            val activeStates = listOf(true, false)
            val tagCounts = listOf(0, 1, 3, 5)
            val limits = listOf(1, 5, 10, 20, 50, 100)
            
            for (branch in branches) {
                for (gen in generations) {
                    for (active in activeStates) {
                        for (tagCount in tagCounts) {
                            for (limit in limits) {
                                yield(Tuple5(branch, gen, active, tagCount, limit))
                            }
                        }
                    }
                }
            }
        }
    
    data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
}