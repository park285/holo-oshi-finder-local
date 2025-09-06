package com.holo.oshi.notification.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = RedisKeySerializer::class)
@JvmInline
value class RedisKey(val value: String) {
    init {
        require(value.isNotBlank()) { "Redis key cannot be blank" }
        require(value.length <= 250) { "Redis key too long: ${value.length}" }
        require(!value.contains(" ")) { "Redis key cannot contain spaces" }
    }
    
    companion object {
        // 알림 관련 키들
        fun notification(id: NotificationId): RedisKey = 
            RedisKey("notification:${id.value}")
            
        fun userTimeline(userId: UserId): RedisKey = 
            RedisKey("user:${userId.value}:timeline")
            
        fun globalTimeline(): RedisKey = 
            RedisKey("notifications:timeline")
            
        // 상태별 키들
        fun pendingNotifications(): RedisKey = 
            RedisKey("notifications:pending")
            
        fun failedNotifications(): RedisKey = 
            RedisKey("notifications:failed")
            
        // 통계 키들
        fun notificationStats(): RedisKey = 
            RedisKey("stats:notifications")
            
        fun userStats(userId: UserId): RedisKey = 
            RedisKey("stats:user:${userId.value}")
            
        // 캐시 키들
        fun notificationCache(id: NotificationId): RedisKey = 
            RedisKey("cache:notification:${id.value}")
            
        fun userNotificationsCache(userId: UserId): RedisKey = 
            RedisKey("cache:user:${userId.value}:notifications")
    }
}

@Serializer(forClass = RedisKey::class)
object RedisKeySerializer : kotlinx.serialization.KSerializer<RedisKey> {
    override val descriptor: SerialDescriptor = 
        PrimitiveSerialDescriptor("RedisKey", PrimitiveKind.STRING)
        
    override fun serialize(encoder: Encoder, value: RedisKey) = 
        encoder.encodeString(value.value)
        
    override fun deserialize(decoder: Decoder): RedisKey = 
        RedisKey(decoder.decodeString())
}