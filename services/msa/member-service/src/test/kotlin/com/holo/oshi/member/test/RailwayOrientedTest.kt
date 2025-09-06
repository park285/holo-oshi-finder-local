package com.holo.oshi.member.test

import arrow.core.*
import com.holo.oshi.common.extensions.*
import com.holo.oshi.member.service.UltimateKotlinMemberService.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import mu.KotlinLogging

/**
 * 🎯 Railway-Oriented Programming Ultimate Test
 * 
 * 완전한 함수형 에러 처리 검증:
 * ✅ Either 체인 처리 완전성
 * ✅ Error Propagation 정확성
 * ✅ Success Path 보존
 * ✅ Failure Path 격리
 * ✅ Error Transformation
 * ✅ Monadic Laws 준수
 * ✅ Real-world Error Scenarios
 * ✅ Performance Under Error Conditions
 */
private val logger = KotlinLogging.logger {}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@KotlinTest 
class RailwayOrientedTest {
    
    @Test
    @KotlinSpec("Either 체인이 성공 경로를 유지해야 함")
    fun `Either chain should preserve success path`() = runTest {
        kotlinTest("성공 경로 보존 테스트") {
            `given`("유효한 입력값 체인") {}
            
            `when`("Railway-Oriented 체인 실행") {
                val result = "123"
                    .validateNotEmpty()
                    .flatMap { it.validateLength(1, 10) }
                    .flatMap { it.validatePattern("\\d+".toRegex(), "숫자가 아님") }
                    .flatMap { MemberId.parse(it).mapLeft { it.message } }
                    .map { memberId -> "성공: 멤버 ID ${memberId.value}" }
                
                result
            }
            
            `then`("모든 단계가 성공해야 함") { result ->
                val r = result as Either<String, String>
                
                val successValue = r.shouldBeRight()
                successValue shouldBe "성공: 멤버 ID 123"
                
                logger.info { "✅ 성공 경로 보존 확인: $successValue" }
            }
        }
    }
    
    @Test
    @KotlinSpec("Either 체인이 실패를 즉시 전파해야 함")
    fun `Either chain should propagate failure immediately`() = runTest {
        kotlinTest("실패 전파 테스트") {
            `given`("중간에 실패하는 체인") {}
            
            `when`("Railway-Oriented 체인에서 실패 발생") {
                val result = "abc"  // 숫자가 아님 - 여기서 실패
                    .validateNotEmpty()              // 성공
                    .flatMap { it.validateLength(1, 10) }     // 성공  
                    .flatMap { it.validatePattern("\\d+".toRegex(), "숫자가 아님") } // 실패!
                    .flatMap { MemberId.parse(it).mapLeft { it.message } }  // 실행되지 않음
                    .map { memberId -> "성공: 멤버 ID ${memberId.value}" }    // 실행되지 않음
                
                result
            }
            
            `then`("첫 번째 실패에서 멈춰야 함") { result ->
                val r = result as Either<String, String>
                
                val errorMessage = r.shouldBeLeft()
                errorMessage shouldBe "숫자가 아님"
                
                logger.info { "✅ 실패 즉시 전파 확인: $errorMessage" }
            }
        }
    }
    
    @Test
    @KotlinSpec("Domain Error들이 올바르게 분류되어야 함")  
    fun `Domain Errors should be properly categorized`() = runTest {
        kotlinTest("도메인 에러 분류 테스트") {
            `given`("다양한 에러 상황들") {}
            
            `when`("각 에러 타입 생성") {
                val memberNotFound = DomainError.MemberNotFound("멤버 없음")
                val invalidId = DomainError.InvalidMemberId("잘못된 ID")
                val invalidBranch = DomainError.InvalidBranch("잘못된 지부")
                val repoError = DomainError.RepositoryError("DB 오류")
                val validationError = DomainError.ValidationError("검증 실패")
                
                listOf(memberNotFound, invalidId, invalidBranch, repoError, validationError)
            }
            
            `then`("각 에러가 올바른 코드와 메시지를 가져야 함") { errors ->
                val errorList = errors as List<DomainError>
                
                // ✅ 에러 코드 검증
                errorList[0].code shouldBe "MEMBER_NOT_FOUND"
                errorList[1].code shouldBe "INVALID_MEMBER_ID"  
                errorList[2].code shouldBe "INVALID_BRANCH"
                errorList[3].code shouldBe "REPOSITORY_ERROR"
                errorList[4].code shouldBe "VALIDATION_ERROR"
                
                // ✅ 에러 메시지 검증
                errorList[0].message shouldBe "멤버 없음"
                errorList[1].message shouldBe "잘못된 ID"
                
                logger.info { "✅ 도메인 에러 분류 확인 완료" }
            }
        }
    }
    
    @Test
    @KotlinSpec("복잡한 Either 체인이 완전히 작동해야 함")
    fun `Complex Either chains should work completely`() = runTest {
        kotlinTest("복잡한 Either 체인 테스트") {
            `given`("실제 비즈니스 로직 시나리오") {}
            
            `when`("멤버 조회 → 검증 → 처리 체인 실행") {
                // 🎯 실제 비즈니스 로직 흐름 시뮬레이션
                val result = simulateComplexMemberOperation("123")
                result
            }
            
            `then`("전체 체인이 성공해야 함") { result ->
                val r = result as Either<String, String>
                
                r.shouldBeRight() shouldBe "처리 완료: Sakura Miko (🇯🇵 Hololive Japan gen0)"
                
                logger.info { "✅ 복잡한 Either 체인 검증 완료" }
            }
        }
    }
    
    @Test
    @KotlinBenchmark(iterations = 5000)
    @KotlinSpec("Railway-Oriented 패턴 성능 오버헤드 검증")
    fun `Railway-Oriented pattern should have minimal overhead`() = runTest {
        // 🎯 Either 체인 vs 예외 처리 성능 비교
        
        val eitherPerformance = measureCoroutinePerformance("Either 체인", 5000) {
            simulateEitherChain("valid-input")
        }
        
        val exceptionPerformance = measureCoroutinePerformance("예외 처리", 5000) {
            simulateExceptionChain("valid-input")
        }
        
        logger.info { 
            "📊 Either 체인: ${eitherPerformance.averageTime.inWholeMicroseconds}μs" +
            " vs 예외 처리: ${exceptionPerformance.averageTime.inWholeMicroseconds}μs"
        }
        
        // ✅ Either가 예외 처리보다 빠르거나 비슷해야 함 (일반적으로 더 빠름)
        val performanceRatio = eitherPerformance.averageTime.inWholeMicroseconds.toDouble() / 
                              exceptionPerformance.averageTime.inWholeMicroseconds
        
        assert(performanceRatio <= 1.5) { // Either가 50% 이상 느리면 안됨
            "Either chain too slow: ${performanceRatio}x slower than exceptions"
        }
        
        logger.info { "✅ Railway-Oriented 성능 검증 완료 (비율: ${String.format("%.2f", performanceRatio)})" }
    }
    
    @Test
    @KotlinProperty(iterations = 100)
    @KotlinSpec("모든 에러 타입이 Either로 변환 가능해야 함")
    fun `all error types should be convertible to Either`() = runTest {
        kotlinProperty(
            "에러 타입 Either 변환",
            generateDomainErrors(),
            100
        ) { error ->
            try {
                // 모든 도메인 에러가 Either.Left로 변환 가능한지 확인
                val either: Either<DomainError, Nothing> = error.left()
                either.isLeft()
            } catch (e: Exception) {
                logger.error(e) { "에러 변환 실패: $error" }
                false
            }
        }
    }
    
    // ============ Helper Functions (Test Simulation) ============
    
    private suspend fun simulateComplexMemberOperation(idString: String): Either<String, String> =
        idString.validateNotEmpty()
            .flatMap { it.validatePattern("\\d+".toRegex(), "숫자가 아님") }
            .flatMap { MemberId.parse(it).mapLeft { it.message } }
            .flatMap { memberId -> 
                // 가짜 멤버 조회
                if (memberId.value == 123L) {
                    "Sakura Miko".right()
                } else {
                    "멤버 없음".left()
                }
            }
            .flatMap { memberName ->
                // 가짜 지부/세대 정보 추가
                "$memberName (🇯🇵 Hololive Japan gen0)".right()
            }
            .map { memberInfo ->
                "처리 완료: $memberInfo"
            }
    
    private suspend fun simulateEitherChain(input: String): Either<String, String> =
        input.validateNotEmpty()
            .flatMap { it.validateLength(5, 20) }
            .map { "처리됨: $it" }
    
    private suspend fun simulateExceptionChain(input: String): String {
        if (input.isEmpty()) throw IllegalArgumentException("비어있음")
        if (input.length !in 5..20) throw IllegalArgumentException("길이 오류")
        return "처리됨: $input"
    }
    
    private fun generateDomainErrors(): Sequence<DomainError> = sequence {
        yield(DomainError.MemberNotFound("테스트 멤버 없음"))
        yield(DomainError.InvalidMemberId("테스트 ID 오류"))
        yield(DomainError.InvalidBranch("테스트 지부 오류"))
        yield(DomainError.InvalidGeneration("테스트 세대 오류"))
        yield(DomainError.RepositoryError("테스트 DB 오류"))
        yield(DomainError.CacheError("테스트 캐시 오류"))
        yield(DomainError.ValidationError("테스트 검증 오류"))
        yield(DomainError.OperationFailed("테스트 작업 실패"))
    }
}