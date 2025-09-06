package com.holo.oshi.notification.model

import kotlinx.serialization.Serializable

@Serializable
sealed class NotificationError {
    abstract val message: String
    abstract val code: String
    @Serializable
    data object RedisConnectionError : NotificationError() {
        override val message = "Redis 연결에 실패했습니다"
        override val code = "REDIS_CONNECTION_ERROR"
    }
    
    @Serializable
    data object NotificationNotFound : NotificationError() {
        override val message = "알림을 찾을 수 없습니다"
        override val code = "NOTIFICATION_NOT_FOUND"
    }
    
    @Serializable
    data object InvalidNotificationData : NotificationError() {
        override val message = "잘못된 알림 데이터입니다"
        override val code = "INVALID_NOTIFICATION_DATA"
    }
    
    @Serializable
    data object SerializationError : NotificationError() {
        override val message = "알림 직렬화에 실패했습니다"
        override val code = "SERIALIZATION_ERROR"
    }
    
    @Serializable
    data object DeserializationError : NotificationError() {
        override val message = "알림 역직렬화에 실패했습니다"
        override val code = "DESERIALIZATION_ERROR"
    }
    
    @Serializable
    data object ExpiredNotification : NotificationError() {
        override val message = "만료된 알림입니다"
        override val code = "EXPIRED_NOTIFICATION"
    }
    
    @Serializable
    data class CustomError(
        override val message: String,
        override val code: String = "CUSTOM_ERROR"
    ) : NotificationError()
}