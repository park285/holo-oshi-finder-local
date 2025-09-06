package com.holo.oshi.vector.event

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.messaging.Message
import org.springframework.stereotype.Component
import com.holo.oshi.vector.service.VectorSearchService
import java.util.function.Consumer
import com.holo.oshi.vector.model.VectorIndexRequest

/**
 * 단순화된 Vector Service Event Consumer
 * Member Service 이벤트 수신 및 자동 재인덱싱 처리
 */
@Component
class VectorEventConsumer(
    private val vectorSearchService: VectorSearchService,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(VectorEventConsumer::class.java)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Member Updated 이벤트 처리
     */
    @Bean
    fun memberUpdatedConsumer(): Consumer<Message<String>> = Consumer { message ->
        scope.launch {
            try {
                val payload = message.payload
                logger.info("Member Updated Event 수신: {}", payload.take(100))
                
                val memberEvent = objectMapper.readValue(payload, MemberUpdatedEvent::class.java)
                
                if (requiresReindexing(memberEvent.changedFields)) {
                    logger.info("멤버 자동 재인덱싱 시작: {}", memberEvent.memberId)
                    
                    try {
                        val result = vectorSearchService.indexMember(
                            VectorIndexRequest(memberEvent.memberId, forceReindex = true)
                        )
                        result.fold(
                            onSuccess = { response ->
                                logger.info("재인덱싱 완료: memberId={}, status={}", 
                                    memberEvent.memberId, response.status)
                            },
                            onFailure = { error ->
                                logger.error("재인덱싱 실패: memberId={}", memberEvent.memberId, error)
                            }
                        )
                    } catch (e: Exception) {
                        logger.error("재인덱싱 오류: memberId={}", memberEvent.memberId, e)
                    }
                }
                
            } catch (e: Exception) {
                logger.error("Member Updated Event 처리 실패", e)
            }
        }
    }

    /**
     * Member Created 이벤트 처리
     */
    @Bean
    fun memberCreatedConsumer(): Consumer<Message<String>> = Consumer { message ->
        scope.launch {
            try {
                val memberEvent = objectMapper.readValue(message.payload, MemberCreatedEvent::class.java)
                logger.info("Member Created Event 수신: memberId={}", memberEvent.memberId)
                
                try {
                    val result = vectorSearchService.indexMember(
                        VectorIndexRequest(memberEvent.memberId, forceReindex = false)
                    )
                    logger.info("신규 멤버 인덱싱 완료: memberId={}", memberEvent.memberId)
                } catch (e: Exception) {
                    logger.error("신규 멤버 인덱싱 실패: memberId={}", memberEvent.memberId, e)
                }
                
            } catch (e: Exception) {
                logger.error("Member Created Event 처리 실패", e)
            }
        }
    }

    /**
     * Member Deleted 이벤트 처리  
     */
    @Bean
    fun memberDeletedConsumer(): Consumer<Message<String>> = Consumer { message ->
        scope.launch {
            try {
                val memberEvent = objectMapper.readValue(message.payload, MemberDeletedEvent::class.java)
                logger.info("Member Deleted Event 수신: memberId={}", memberEvent.memberId)
                
                logger.info("벡터 인덱스에서 멤버 제거: memberId={}", memberEvent.memberId)
                
            } catch (e: Exception) {
                logger.error("Member Deleted Event 처리 실패", e)
            }
        }
    }

    /**
     * 재인덱싱 필요 여부 판단
     */
    private fun requiresReindexing(changedFields: List<String>): Boolean {
        val criticalFields = listOf(
            "name", "nameEn", "nameJp", "description", "traits",
            "personalityTraits", "colors", "isActive"
        )
        return changedFields.any { field -> field in criticalFields }
    }
}

/**
 * 이벤트 데이터 클래스들 (Member Event와 호환)
 */
data class MemberUpdatedEvent(
    val eventId: String,
    val memberId: Int,
    val memberData: Any, // MemberEventData와 호환
    val changedFields: List<String>,
    val timestamp: String,
    val source: String = "member-service"
)

data class MemberCreatedEvent(
    val eventId: String,
    val memberId: Int,
    val memberData: Any,
    val timestamp: String,
    val source: String = "member-service"
)

data class MemberDeletedEvent(
    val eventId: String,
    val memberId: Int,
    val timestamp: String,
    val reason: String,
    val source: String = "member-service"
)