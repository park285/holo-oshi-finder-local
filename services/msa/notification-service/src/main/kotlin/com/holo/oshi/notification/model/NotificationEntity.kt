package com.holo.oshi.notification.model

import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@Serializable
enum class NotificationPriority(val level: Int) {
    LOW(1), NORMAL(2), HIGH(3), URGENT(4), CRITICAL(5);
    
    companion object {
        fun fromLevel(level: Int): NotificationPriority = 
            entries.find { it.level == level } ?: NORMAL
    }
}

@Serializable
enum class NotificationStatus {
    PENDING, SENT, DELIVERED, READ, FAILED, EXPIRED;
    
    val isActive: Boolean get() = this in setOf(PENDING, SENT, DELIVERED)
    val isCompleted: Boolean get() = this in setOf(READ, FAILED, EXPIRED)
}

@Serializable
enum class NotificationTypeEnum {
    SEARCH_COMPLETED,
    LLM_ANALYSIS_COMPLETED,
    VECTOR_SEARCH_COMPLETED,
    MEMBER_ADDED,
    MEMBER_UPDATED,
    SYSTEM_STATUS,
    PERFORMANCE_ALERT,
    ERROR_OCCURRED
}

@Serializable
data class Notification(
    val id: NotificationId = NotificationId.generate(),
    val type: NotificationTypeEnum,
    val title: NotificationTitle,
    val message: NotificationMessage,
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val status: NotificationStatus = NotificationStatus.PENDING,
    val data: EventData = EventData(emptyMap()),
    val userId: UserId? = null,
    val createdAt: Instant = Clock.System.now(),
    val deliveredAt: Instant? = null,
    val readAt: Instant? = null,
    val expiresAt: Instant? = null
) {
    val isExpired: Boolean 
        get() = expiresAt?.let { Clock.System.now() > it } ?: false
    
    val isRead: Boolean 
        get() = readAt != null
    
    val isDelivered: Boolean 
        get() = deliveredAt != null
    
    fun markAsDelivered(): Notification = copy(
        status = NotificationStatus.DELIVERED,
        deliveredAt = Clock.System.now()
    )
    
    fun markAsRead(): Notification = copy(
        status = NotificationStatus.READ,
        readAt = Clock.System.now()
    )
    
    fun markAsFailed(): Notification = copy(
        status = NotificationStatus.FAILED
    )
}

fun createNotification(
    type: NotificationTypeEnum,
    title: String,
    message: String,
    priority: NotificationPriority = NotificationPriority.NORMAL,
    userId: String? = null,
    data: Map<String, String> = emptyMap(),
    expiresIn: kotlin.time.Duration? = null
): Notification = Notification(
    type = type,
    title = NotificationTitle(title),
    message = NotificationMessage(message),
    priority = priority,
    userId = userId?.let(::UserId),
    data = EventData(data),
    expiresAt = expiresIn?.let { Clock.System.now().plus(it) }
)