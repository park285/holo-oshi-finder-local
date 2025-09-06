package com.holo.oshi.member.event

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.util.*

/**
 * 멤버 도메인 이벤트 기본 클래스
 * Event-Driven 아키텍처의 핵심 구성요소
 */
sealed class MemberEvent {
    abstract val eventId: String
    abstract val memberId: Int
    abstract val eventType: String
    abstract val timestamp: Instant
    abstract val source: String
    abstract val version: String
    
    /**
     * 멤버 생성 이벤트
     */
    data class MemberCreated(
        @param:JsonProperty("event_id") override val eventId: String = UUID.randomUUID().toString(),
        @param:JsonProperty("member_id") override val memberId: Int,
        @param:JsonProperty("member_data") val memberData: MemberEventData,
        @param:JsonProperty("event_type") override val eventType: String = "MEMBER_CREATED",
        @param:JsonProperty("timestamp") override val timestamp: Instant = Instant.now(),
        @param:JsonProperty("source") override val source: String = "member-service",
        @param:JsonProperty("version") override val version: String = "1.0"
    ) : MemberEvent()
    
    /**
     * 멤버 업데이트 이벤트
     */
    data class MemberUpdated(
        @param:JsonProperty("event_id") override val eventId: String = UUID.randomUUID().toString(),
        @param:JsonProperty("member_id") override val memberId: Int,
        @param:JsonProperty("member_data") val memberData: MemberEventData,
        @param:JsonProperty("previous_data") val previousData: MemberEventData? = null,
        @param:JsonProperty("changed_fields") val changedFields: List<String>,
        @param:JsonProperty("event_type") override val eventType: String = "MEMBER_UPDATED",
        @param:JsonProperty("timestamp") override val timestamp: Instant = Instant.now(),
        @param:JsonProperty("source") override val source: String = "member-service",
        @param:JsonProperty("version") override val version: String = "1.0"
    ) : MemberEvent()
    
    /**
     * 멤버 삭제 이벤트
     */
    data class MemberDeleted(
        @param:JsonProperty("event_id") override val eventId: String = UUID.randomUUID().toString(),
        @param:JsonProperty("member_id") override val memberId: Int,
        @param:JsonProperty("event_type") override val eventType: String = "MEMBER_DELETED",
        @param:JsonProperty("timestamp") override val timestamp: Instant = Instant.now(),
        @param:JsonProperty("source") override val source: String = "member-service",
        @param:JsonProperty("version") override val version: String = "1.0",
        @param:JsonProperty("reason") val reason: String = "MANUAL_DELETE"
    ) : MemberEvent()
}

/**
 * 이벤트에 포함되는 멤버 데이터
 */
data class MemberEventData(
    @param:JsonProperty("id") val id: Int,
    @param:JsonProperty("name") val name: String,
    @param:JsonProperty("branch") val branch: String,
    @param:JsonProperty("generation") val generation: String,
    @param:JsonProperty("is_active") val isActive: Boolean,
    @param:JsonProperty("debut_date") val debutDate: String?,
    @param:JsonProperty("graduation_date") val graduationDate: String?,
    @param:JsonProperty("description") val description: String?,
    @param:JsonProperty("traits") val traits: List<String> = emptyList(),
    @param:JsonProperty("colors") val colors: List<String> = emptyList(),
    @param:JsonProperty("requires_reindexing") val requiresReindexing: Boolean = true
)

/**
 * 이벤트 메타데이터 - 추적 및 디버깅용
 */
data class EventMetadata(
    @param:JsonProperty("trace_id") val traceId: String = UUID.randomUUID().toString(),
    @param:JsonProperty("user_id") val userId: String? = null,
    @param:JsonProperty("correlation_id") val correlationId: String = UUID.randomUUID().toString(),
    @param:JsonProperty("retry_count") val retryCount: Int = 0,
    @param:JsonProperty("max_retries") val maxRetries: Int = 3
)