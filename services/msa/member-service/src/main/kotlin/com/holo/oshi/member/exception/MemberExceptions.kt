package com.holo.oshi.member.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/**
 * 멤버 도메인 예외 클래스들
 * - 명확한 에러 타입 구분
 * - HTTP 상태 코드 자동 매핑
 */

sealed class MemberDomainException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause) {
    constructor(message: String) : this(message, null)
}

@ResponseStatus(HttpStatus.NOT_FOUND)
data class MemberNotFoundException(
    val memberId: Long
) : MemberDomainException("회원을 찾을 수 없습니다: ID=$memberId")

@ResponseStatus(HttpStatus.CONFLICT)
data class MemberAlreadyExistsException(
    val nameEn: String
) : MemberDomainException("이미 존재하는 회원입니다: $nameEn")

@ResponseStatus(HttpStatus.BAD_REQUEST)
data class InvalidMemberDataException(
    val field: String,
    val value: String?,
    val reason: String
) : MemberDomainException("잘못된 회원 데이터: $field='$value' ($reason)")

@ResponseStatus(HttpStatus.CONFLICT)
data class MemberUpdateConflictException(
    val memberId: Long,
    val conflictField: String
) : MemberDomainException("회원 업데이트 충돌: ID=$memberId, 필드=$conflictField")

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
data class MemberEventPublishException(
    val memberId: Long,
    val eventType: String,
    override val cause: Throwable
) : MemberDomainException("회원 이벤트 발행 실패: ID=$memberId, 이벤트=$eventType", cause)

@ResponseStatus(HttpStatus.BAD_REQUEST)
data class MemberValidationException(
    val violations: Map<String, String>
) : MemberDomainException("회원 데이터 검증 실패: ${violations.entries.joinToString { "${it.key}: ${it.value}" }}")

/**
 * 결과 타입 (Railway-oriented programming)
 */
sealed class MemberResult<out T> {
    data class Success<T>(val data: T) : MemberResult<T>()
    data class Failure(val exception: MemberDomainException) : MemberResult<Nothing>()
    
    inline fun <R> map(transform: (T) -> R): MemberResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Failure -> this
    }
    
    inline fun <R> flatMap(transform: (T) -> MemberResult<R>): MemberResult<R> = when (this) {
        is Success -> transform(data)
        is Failure -> this
    }
    
    inline fun onSuccess(action: (T) -> Unit): MemberResult<T> {
        if (this is Success) action(data)
        return this
    }
    
    inline fun onFailure(action: (MemberDomainException) -> Unit): MemberResult<T> {
        if (this is Failure) action(exception)
        return this
    }
    
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Failure -> throw exception
    }
    
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }
}

/**
 * 확장 함수들
 */
class GenericMemberException(message: String, cause: Throwable? = null) : MemberDomainException(message, cause)

fun <T> runCatchingMember(block: () -> T): MemberResult<T> = try {
    MemberResult.Success(block())
} catch (e: MemberDomainException) {
    MemberResult.Failure(e)
} catch (e: Exception) {
    MemberResult.Failure(GenericMemberException("알 수 없는 오류", e))
}

suspend fun <T> runCatchingMemberSuspend(block: suspend () -> T): MemberResult<T> = try {
    MemberResult.Success(block())
} catch (e: MemberDomainException) {
    MemberResult.Failure(e)
} catch (e: Exception) {
    MemberResult.Failure(GenericMemberException("알 수 없는 오류", e))
}

fun <T, R> MemberResult<T>.mapCatching(transform: (T) -> R): MemberResult<R> = try {
    map(transform)
} catch (e: MemberDomainException) {
    MemberResult.Failure(e)
} catch (e: Exception) {
    MemberResult.Failure(GenericMemberException("변환 중 오류", e))
}