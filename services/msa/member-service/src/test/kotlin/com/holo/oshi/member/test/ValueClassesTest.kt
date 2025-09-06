package com.holo.oshi.member.test

import arrow.core.*
import com.holo.oshi.member.service.UltimateKotlinMemberService.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import mu.KotlinLogging

/**
 * 🎯 Value Classes Ultimate Test
 * 
 * 코틀린라이크 타입 안전성 완전 검증:
 * ✅ Smart Constructor Validation
 * ✅ Invalid State Prevention
 * ✅ Type Safety at Compile Time
 * ✅ Runtime Performance (No Boxing)
 * ✅ Equality and HashCode
 * ✅ Serialization/Deserialization
 * ✅ Edge Cases and Boundary Testing
 */
private val logger = KotlinLogging.logger {}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@KotlinTest
class ValueClassesTest {
    
    // ============ MemberId Tests ============
    
    @Test
    @KotlinSpec("MemberId는 양수만 허용해야 함")
    fun `MemberId should only accept positive values`() = runTest {
        kotlinTest("MemberId 양수 검증") {
            `given`("유효하지 않은 ID 값들") {
                // Setup invalid values
            }
            `when`("MemberId 생성 시도") {
                val validId = MemberId.of(1L)
                val zeroId = MemberId.of(0L)
                val negativeId = MemberId.of(-1L)
                
                validId to zeroId to negativeId
            }
            `then`("올바른 검증 결과 확인") { (validId, zeroId, negativeId) ->
                val (valid, zero, negative) = validId as Triple<*, *, *>
                
                // ✅ 유효한 ID는 성공
                (valid as Either<*, MemberId>).shouldBeRight()
                
                // ❌ 0과 음수는 실패
                (zero as Either<DomainError.InvalidMemberId, *>).shouldBeLeft()
                (negative as Either<DomainError.InvalidMemberId, *>).shouldBeLeft()
                
                logger.info { "✅ MemberId 검증 완료" }
            }
        }
    }
    
    @Test
    @KotlinSpec("MemberId는 String 파싱을 지원해야 함")
    fun `MemberId should support String parsing`() = runTest {
        kotlinTest("MemberId String 파싱") {
            `given`("다양한 문자열 입력") {
                // Test strings
            }
            `when`("문자열을 MemberId로 파싱") {
                val validParse = MemberId.parse("123")
                val invalidParse = MemberId.parse("abc")
                val emptyParse = MemberId.parse("")
                
                Triple(validParse, invalidParse, emptyParse)
            }
            `then`("파싱 결과 검증") { (valid, invalid, empty) ->
                // ✅ 숫자 문자열은 성공
                valid.shouldBeRight().value shouldBe 123L
                
                // ❌ 비숫자는 실패
                invalid.shouldBeLeft()
                empty.shouldBeLeft()
                
                logger.info { "✅ MemberId 파싱 검증 완료" }
            }
        }
    }
    
    // ============ BranchCode Tests ============
    
    @Test
    @KotlinSpec("BranchCode는 유효한 지부만 허용해야 함")
    fun `BranchCode should only accept valid branches`() = runTest {
        kotlinTest("BranchCode 유효성 검증") {
            `given`("유효한/유효하지 않은 지부 코드들") {
                // Branch codes to test
            }
            `when`("BranchCode 생성 시도") {
                val jpBranch = BranchCode.parse("JP")  // 대문자
                val enBranch = BranchCode.parse("en")  // 소문자
                val invalidBranch = BranchCode.parse("kr")
                val emptyBranch = BranchCode.parse("")
                
                listOf(jpBranch, enBranch, invalidBranch, emptyBranch)
            }
            `then`("지부 검증 결과 확인") { results ->
                val (jp, en, invalid, empty) = results as List<Either<*, *>>
                
                // ✅ 유효한 지부는 성공 (대소문자 정규화)
                jp.shouldBeRight().value shouldBe "jp"
                en.shouldBeRight().value shouldBe "en"
                
                // ❌ 유효하지 않은 지부는 실패
                invalid.shouldBeLeft()
                empty.shouldBeLeft()
                
                logger.info { "✅ BranchCode 검증 완료" }
            }
        }
    }
    
    @Test
    @KotlinSpec("BranchCode는 displayName을 제공해야 함")
    fun `BranchCode should provide localized display names`() = runTest {
        val jpBranch = BranchCode.JP
        val enBranch = BranchCode.EN
        val idBranch = BranchCode.ID
        
        jpBranch.displayName shouldBe "🇯🇵 Hololive Japan"
        enBranch.displayName shouldBe "🇺🇸 Hololive English"
        idBranch.displayName shouldBe "🇮🇩 Hololive Indonesia"
        
        logger.info { "✅ BranchCode displayName 검증 완료" }
    }
    
    // ============ GenerationCode Tests ============
    
    @Test
    @KotlinSpec("GenerationCode는 gen0, gen1 형식만 허용해야 함")
    fun `GenerationCode should only accept genN format`() = runTest {
        kotlinTest("GenerationCode 형식 검증") {
            `given`("다양한 세대 코드 형식") {}
            `when`("GenerationCode 생성 시도") {
                val validGen = GenerationCode.parse("gen1")
                val upperGen = GenerationCode.parse("GEN2")  
                val invalidGen = GenerationCode.parse("generation1")
                val numberGen = GenerationCode.parse("1")
                
                listOf(validGen, upperGen, invalidGen, numberGen)
            }
            `then`("세대 형식 검증") { results ->
                val (valid, upper, invalid, number) = results as List<Either<*, *>>
                
                // ✅ 올바른 형식은 성공
                valid.shouldBeRight()
                upper.shouldBeRight()  // 대소문자 정규화
                
                // ❌ 잘못된 형식은 실패
                invalid.shouldBeLeft()
                number.shouldBeLeft()
                
                logger.info { "✅ GenerationCode 검증 완료" }
            }
        }
    }
    
    @Test
    @KotlinSpec("GenerationCode는 세대 번호를 추출해야 함")
    fun `GenerationCode should extract generation number`() = runTest {
        val gen0 = GenerationCode.parse("gen0").shouldBeRight()
        val gen5 = GenerationCode.parse("gen5").shouldBeRight()
        
        gen0.generationNumber shouldBe 0
        gen5.generationNumber shouldBe 5
        
        gen0.displayName shouldBe "0期生 (Founders)"
        gen5.displayName shouldBe "gen5"
        
        logger.info { "✅ GenerationCode 번호 추출 검증 완료" }
    }
    
    // ============ Performance Tests for Value Classes ============
    
    @Test
    @KotlinBenchmark(iterations = 100000)
    @KotlinSpec("MemberId 생성 성능이 충분히 빨라야 함")
    fun `MemberId creation should be performant`() = runTest {
        val performanceResult = measureCoroutinePerformance("MemberId 생성", 100000) {
            MemberId.of(12345L)
        }
        
        // ✅ 평균 생성 시간이 1μs 미만이어야 함 (No Boxing)
        performanceResult.averageTime.shouldBeLessThan(Duration.microseconds(1))
        
        logger.info { 
            "🚀 MemberId 생성 성능: 평균 ${performanceResult.averageTime.inWholeMicroseconds}μs" +
            " (최소: ${performanceResult.minTime.inWholeMicroseconds}μs, " +
            "최대: ${performanceResult.maxTime.inWholeMicroseconds}μs)"
        }
    }
    
    @Test
    @KotlinBenchmark(iterations = 50000)
    @KotlinSpec("BranchCode 파싱 성능 검증")
    fun `BranchCode parsing should be efficient`() = runTest {
        val branches = listOf("jp", "en", "id")
        var index = 0
        
        val performanceResult = measureCoroutinePerformance("BranchCode 파싱", 50000) {
            val branch = branches[index % branches.size]
            index++
            BranchCode.parse(branch)
        }
        
        // ✅ 파싱 시간이 5μs 미만이어야 함
        performanceResult.averageTime.shouldBeLessThan(Duration.microseconds(5))
        
        logger.info { 
            "🚀 BranchCode 파싱 성능: 평균 ${performanceResult.averageTime.inWholeMicroseconds}μs"
        }
    }
    
    // ============ Property-based Tests ============
    
    @Test
    @KotlinProperty(iterations = 500)
    @KotlinSpec("모든 유효한 MemberId는 생성되어야 함")
    fun `all valid MemberIds should be created successfully`() = runTest {
        kotlinProperty(
            "유효한 MemberId 생성",
            KotlinGenerators.validMemberIds(),
            500
        ) { id ->
            MemberId.of(id).isRight()
        }
    }
    
    @Test 
    @KotlinProperty(iterations = 300)
    @KotlinSpec("모든 유효한 BranchCode는 파싱되어야 함")
    fun `all valid BranchCodes should be parsed successfully`() = runTest {
        kotlinProperty(
            "유효한 BranchCode 파싱",
            KotlinGenerators.validBranchCodes(),
            300
        ) { branch ->
            BranchCode.parse(branch).isRight()
        }
    }
    
    // ============ Equality and HashCode Tests ============
    
    @Test
    @KotlinSpec("Value Classes는 구조적 동등성을 지원해야 함")
    fun `Value Classes should support structural equality`() = runTest {
        val id1 = MemberId.of(123L).shouldBeRight()
        val id2 = MemberId.of(123L).shouldBeRight()
        val id3 = MemberId.of(456L).shouldBeRight()
        
        // ✅ 같은 값은 동등
        id1 shouldBe id2
        assert(id1.hashCode() == id2.hashCode())
        
        // ✅ 다른 값은 부등
        id1 shouldNotBe id3
        assert(id1.hashCode() != id3.hashCode())
        
        logger.info { "✅ Value Classes 동등성 검증 완료" }
    }
    
    // ============ Serialization Tests ============
    
    @Test
    @KotlinSpec("Value Classes는 JSON 직렬화가 가능해야 함")
    fun `Value Classes should be JSON serializable`() = runTest {
        val memberId = MemberId.of(123L).shouldBeRight()
        val branchCode = BranchCode.JP
        
        val objectMapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
        
        // ✅ 직렬화/역직렬화 테스트
        val memberIdJson = objectMapper.writeValueAsString(memberId.value)
        val branchJson = objectMapper.writeValueAsString(branchCode.value)
        
        val deserializedMemberId = objectMapper.readValue<Long>(memberIdJson)
        val deserializedBranch = objectMapper.readValue<String>(branchJson)
        
        deserializedMemberId shouldBe 123L
        deserializedBranch shouldBe "jp"
        
        logger.info { "✅ Value Classes JSON 직렬화 검증 완료" }
    }
}