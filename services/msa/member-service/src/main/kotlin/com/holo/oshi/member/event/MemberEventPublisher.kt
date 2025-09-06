package com.holo.oshi.member.event

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.integration.support.MessageBuilder
import org.springframework.messaging.Message
import org.springframework.stereotype.Component
import com.holo.oshi.member.model.Member


@Component
class MemberEventPublisher(
    private val streamBridge: StreamBridge
) {
    private val logger = LoggerFactory.getLogger(MemberEventPublisher::class.java)

    /**
     * 멤버 생성 이벤트 발행
     */
    suspend fun publishMemberCreated(member: Member): Boolean {
        return try {
            val event = MemberEvent.MemberCreated(
                memberId = member.id?.toInt() ?: 0,
                memberData = member.toEventData()
            )
            
            val result = publishEvent("member-created-out-0", event, "member.created")
            
            if (result) {
                logger.info("Member Created Event Published: memberId={}, eventId={}", 
                    member.id, event.eventId)
            } else {
                logger.error("Failed to publish Member Created Event: memberId={}", member.id)
            }
            
            result
        } catch (e: Exception) {
            logger.error("Exception publishing Member Created Event: memberId={}", member.id, e)
            false
        }
    }

    /**
     * 멤버 업데이트 이벤트 발행
     */
    suspend fun publishMemberUpdated(
        currentMember: Member, 
        previousMember: Member? = null,
        changedFields: List<String> = emptyList()
    ): Boolean {
        return try {
            // 실제 변경사항이 있는지 검증
            val actualChangedFields = if (changedFields.isNotEmpty()) {
                changedFields
            } else {
                detectChangedFields(currentMember, previousMember)
            }
            
            if (actualChangedFields.isEmpty()) {
                logger.debug("No changes detected for member: {}", currentMember.id)
                return true
            }
            
            val event = MemberEvent.MemberUpdated(
                memberId = currentMember.id?.toInt() ?: 0,
                memberData = currentMember.toEventData(),
                previousData = previousMember?.toEventData(),
                changedFields = actualChangedFields
            )
            
            val result = publishEvent("member-updated-out-0", event, "member.updated")
            
            if (result) {
                logger.info("Member Updated Event Published: memberId={}, changedFields={}, eventId={}", 
                    currentMember.id, actualChangedFields, event.eventId)
            } else {
                logger.error("Failed to publish Member Updated Event: memberId={}", currentMember.id)
            }
            
            result
        } catch (e: Exception) {
            logger.error("Exception publishing Member Updated Event: memberId={}", currentMember.id, e)
            false
        }
    }

    /**
     * 멤버 검색 이벤트 발행 (DSL 지원)
     */
    suspend fun publishMemberSearched(query: Any): Boolean {
        return try {
            logger.debug("Member search performed: {}", query.toString())
            // 검색 이벤트는 간단히 로깅만 (필요시 확장)
            true
        } catch (e: Exception) {
            logger.error("Exception publishing Member Searched Event", e)
            false
        }
    }

    /**
     * 멤버 삭제 이벤트 발행
     */
    suspend fun publishMemberDeleted(memberId: Int, reason: String = "MANUAL_DELETE"): Boolean {
        return try {
            val event = MemberEvent.MemberDeleted(
                memberId = memberId,
                reason = reason
            )
            
            val result = publishEvent("member-deleted-out-0", event, "member.deleted")
            
            if (result) {
                logger.info("Member Deleted Event Published: memberId={}, reason={}, eventId={}", 
                    memberId, reason, event.eventId)
            } else {
                logger.error("Failed to publish Member Deleted Event: memberId={}", memberId)
            }
            
            result
        } catch (e: Exception) {
            logger.error("Exception publishing Member Deleted Event: memberId={}", memberId, e)
            false
        }
    }

    /**
     * 공통 이벤트 발행 로직 
     */
    private suspend fun publishEvent(binding: String, event: MemberEvent, routingKey: String): Boolean {
        return try {
            // 메시지 헤더에 추적 정보 추가
            val message: Message<MemberEvent> = MessageBuilder
                .withPayload(event)
                .setHeader("routingKey", routingKey)
                .setHeader("eventType", event.eventType)
                .setHeader("eventId", event.eventId)
                .setHeader("source", event.source)
                .setHeader("version", event.version)
                .setHeader("contentType", "application/json")
                .build()
            
            // Pure Coroutines 비동기 발행
            withContext(Dispatchers.IO) {
                val success = streamBridge.send(binding, message)
                if (success) {
                    logger.debug("Event sent successfully: binding={}, eventId={}", binding, event.eventId)
                } else {
                    logger.warn("Event send returned false: binding={}, eventId={}", binding, event.eventId)
                }
                success
            }
        } catch (e: Exception) {
            logger.error("Critical error in publishEvent: binding={}, eventId={}", binding, event.eventId, e)
            false
        }
    }

    /**
     * 멤버 변경 필드 감지 (Deep Comparison)
     */
    private fun detectChangedFields(current: Member, previous: Member?): List<String> {
        if (previous == null) return emptyList()
        
        val changes = mutableListOf<String>()
        
        if (current.nameEn != previous.nameEn) changes.add("name_en")
        if (current.nameJp != previous.nameJp) changes.add("name_jp")
        if (current.branch != previous.branch) changes.add("branch") 
        if (current.generation != previous.generation) changes.add("generation")
        if (current.isActive != previous.isActive) changes.add("is_active")
        if (current.debutDate != previous.debutDate) changes.add("debut_date")
        if (current.graduationDate != previous.graduationDate) changes.add("graduation_date")
        if (current.personalityTraits != previous.personalityTraits) changes.add("personality_traits")
        
        return changes
    }
}

/**
 * Member Entity를 Event Data로 변환
 */
private fun Member.toEventData(): MemberEventData {
    return MemberEventData(
        id = this.id?.toInt() ?: 0,
        name = this.nameEn,
        branch = this.branch ?: "",
        generation = this.generation ?: "",
        isActive = this.isActive,
        debutDate = this.debutDate?.toString(),
        graduationDate = this.graduationDate?.toString(),
        description = this.personalityTraits,
        traits = emptyList(), // TODO: traits 테이블에서 로드
        colors = emptyList(), // TODO: colors 테이블에서 로드
        requiresReindexing = true
    )
}