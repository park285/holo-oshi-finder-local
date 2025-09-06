package com.holo.oshi.notification.extensions

import com.holo.oshi.notification.model.*

// UserId 확장 함수들
fun UserId?.toTimelineKey(): TimelineKeyInfo = when (this) {
    null -> TimelineKeyInfo(RedisKey.globalTimeline())
    else -> TimelineKeyInfo(RedisKey.userTimeline(this))
}

// 타임라인 키 정보를 담는 data class
data class TimelineKeyInfo(val redisKey: RedisKey) {
    fun asString(): String = redisKey.value
}

// String 확장 함수들 (컨버터용)
fun String?.orEmpty(): String = this ?: ""

// Notification 확장 함수들
fun Notification.toRedisHash(): Map<String, String> = mapOf(
    "id" to id.value,
    "type" to type.name,
    "title" to title.value,
    "message" to message.value,
    "priority" to priority.name,
    "status" to status.name,
    "userId" to (userId?.value.orEmpty()),
    "createdAt" to createdAt.toString(),
    "deliveredAt" to (deliveredAt?.toString().orEmpty()),
    "readAt" to (readAt?.toString().orEmpty()),
    "expiresAt" to (expiresAt?.toString().orEmpty())
)

// NotificationResult 확장 함수들
suspend fun <T> NotificationResult<T>.onSuccessSuspend(
    action: suspend (T) -> Unit
): NotificationResult<T> = also {
    if (this is NotificationResult.Success) action(data)
}

suspend fun <T> NotificationResult<T>.onFailureSuspend(
    action: suspend (NotificationError) -> Unit
): NotificationResult<T> = also {
    if (this is NotificationResult.Failure) action(error)
}

// Collection 확장 함수들
fun <T> List<T>.takeSafely(count: Int): List<T> = 
    if (count <= 0) emptyList() else take(count)