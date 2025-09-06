package com.holo.oshi.notification.service

import com.holo.oshi.notification.model.*
import com.holo.oshi.notification.extensions.*
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class RedisNotificationService {
    private val redisClient = RedisClient.create("redis://localhost:6380")
    private val connection: StatefulRedisConnection<String, String> = redisClient.connect()
    private val commands: RedisAsyncCommands<String, String> = connection.async()
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    suspend fun save(notification: Notification): NotificationResult<Unit> = 
        withContext(Dispatchers.IO) {
            runCatching {
                val key = RedisKey.notification(notification.id).value
                val jsonData = json.encodeToString(notification)
                
                commands.hset(key, buildMap {
                    put("data", jsonData)
                    put("userId", notification.userId?.value.orEmpty())
                    put("type", notification.type.name)
                    put("status", notification.status.name)
                    put("createdAt", notification.createdAt.toString())
                    put("priority", notification.priority.name)
                }).await()
                
                notification.expiresAt?.let { expiry ->
                    val ttl = expiry.epochSeconds - Clock.System.now().epochSeconds
                    if (ttl > 0) commands.expire(key, ttl).await()
                }
                
                commands.zadd("notifications:timeline", notification.createdAt.epochSeconds.toDouble(), notification.id.value).await()
                
                notification.userId?.let { userId ->
                    commands.zadd(RedisKey.userTimeline(userId).value, notification.createdAt.epochSeconds.toDouble(), notification.id.value).await()
                }
                
                logger.info("알림 저장: ${notification.title.value}")
                
            }.fold(
                onSuccess = { NotificationResult.Success(Unit) },
                onFailure = { NotificationResult.Failure(NotificationError.RedisConnectionError) }
            )
        }
    
    suspend fun notifications(
        userId: UserId? = null,
        status: NotificationStatus? = null,
        limit: Int = 50
    ): NotificationResult<List<Notification>> = withContext(Dispatchers.IO) {
        runCatching {
            val timelineKey = userId.toTimelineKey().redisKey.value
            val ids = commands.zrevrange(timelineKey, 0, (limit - 1).toLong()).await()
            
            ids.mapNotNull { id ->
                runCatching {
                    val key = "notification:$id"
                    commands.hgetall(key).await()["data"]?.let { jsonData ->
                        json.decodeFromString<Notification>(jsonData)
                    }
                }.getOrNull()
            }
            .let { notifications ->
                when (status) {
                    null -> notifications
                    else -> notifications.filter { it.status == status }
                }
            }
            .take(limit)
            
        }.fold(
            onSuccess = { NotificationResult.Success(it) },
            onFailure = { NotificationResult.Failure(NotificationError.RedisConnectionError) }
        )
    }
    
    suspend fun updateStatus(
        id: NotificationId, 
        status: NotificationStatus
    ): NotificationResult<Notification> = withContext(Dispatchers.IO) {
        runCatching {
            val key = RedisKey.notification(id).value
            val jsonData = commands.hget(key, "data").await() 
                ?: return@runCatching NotificationResult.Failure(NotificationError.NotificationNotFound)
            
            json.decodeFromString<Notification>(jsonData)
                .run {
                    when (status) {
                        NotificationStatus.DELIVERED -> markAsDelivered()
                        NotificationStatus.READ -> markAsRead()
                        NotificationStatus.FAILED -> markAsFailed()
                        else -> copy(status = status)
                    }
                }
                .also { updated ->
                    val updatedJson = json.encodeToString(updated)
                    commands.hset(key, "data", updatedJson).await()
                    commands.hset(key, "status", updated.status.name).await()
                }
                .let { NotificationResult.Success(it) }
            
        }.getOrElse { NotificationResult.Failure(NotificationError.RedisConnectionError) }
    }
    
    suspend fun cleanupExpired(): NotificationResult<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val now = Clock.System.now().epochSeconds.toDouble()
            commands.zrangebyscore("notifications:timeline", io.lettuce.core.Range.create(Double.NEGATIVE_INFINITY, now))
                .await()
                .fold(0) { count, id ->
                    runCatching {
                        val key = "notification:$id"
                        commands.hget(key, "data").await()?.let { jsonData ->
                            json.decodeFromString<Notification>(jsonData)
                                .takeIf { it.isExpired }
                                ?.also {
                                    commands.del(key).await()
                                    commands.zrem("notifications:timeline", id).await()
                                    it.userId?.let { userId ->
                                        commands.zrem(RedisKey.userTimeline(userId).value, id).await()
                                    }
                                }
                                ?.let { count + 1 } ?: count
                        } ?: count
                    }.getOrElse { count }
                }
                .also { cleanedCount ->
                    if (cleanedCount > 0) logger.info("만료 알림 정리: ${cleanedCount}개")
                }
            
        }.fold(
            onSuccess = { NotificationResult.Success(it) },
            onFailure = { NotificationResult.Failure(NotificationError.RedisConnectionError) }
        )
    }
    
    fun close() {
        connection.close()
        redisClient.close()
    }
}