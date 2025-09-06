package com.holo.oshi.notification.model

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

@Serializable(with = NotificationIdSerializer::class)
@JvmInline
value class NotificationId(val value: String) {
    init { 
        require(value.isNotBlank()) { "NotificationId cannot be blank" } 
        require(value.length <= 36) { "NotificationId too long: ${value.length}" }
    }
    
    companion object {
        fun generate(): NotificationId = NotificationId(
            "${System.currentTimeMillis()}-${(1000..9999).random()}"
        )
    }
}

@Serializer(forClass = NotificationId::class)
object NotificationIdSerializer : KSerializer<NotificationId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("NotificationId", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: NotificationId) = encoder.encodeString(value.value)
    override fun deserialize(decoder: Decoder): NotificationId = NotificationId(decoder.decodeString())
}

@Serializable(with = UserIdSerializer::class)
@JvmInline  
value class UserId(val value: String) {
    init { 
        require(value.isNotBlank()) { "UserId cannot be blank" }
        require(value.length <= 50) { "UserId too long" }
    }
}

@Serializer(forClass = UserId::class)
object UserIdSerializer : KSerializer<UserId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UserId", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: UserId) = encoder.encodeString(value.value)
    override fun deserialize(decoder: Decoder): UserId = UserId(decoder.decodeString())
}

@Serializable(with = NotificationTitleSerializer::class)
@JvmInline
value class NotificationTitle(val value: String) {
    init {
        require(value.isNotBlank()) { "Title cannot be blank" }
        require(value.length <= 200) { "Title too long: ${value.length}" }
    }
}

@Serializer(forClass = NotificationTitle::class)
object NotificationTitleSerializer : KSerializer<NotificationTitle> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("NotificationTitle", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: NotificationTitle) = encoder.encodeString(value.value)
    override fun deserialize(decoder: Decoder): NotificationTitle = NotificationTitle(decoder.decodeString())
}

@Serializable(with = NotificationMessageSerializer::class)
@JvmInline
value class NotificationMessage(val value: String) {
    init {
        require(value.isNotBlank()) { "Message cannot be blank" }
        require(value.length <= 1000) { "Message too long: ${value.length}" }
    }
}

@Serializer(forClass = NotificationMessage::class)
object NotificationMessageSerializer : KSerializer<NotificationMessage> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("NotificationMessage", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: NotificationMessage) = encoder.encodeString(value.value)
    override fun deserialize(decoder: Decoder): NotificationMessage = NotificationMessage(decoder.decodeString())
}

@Serializable(with = EventDataSerializer::class)
@JvmInline
value class EventData(val value: Map<String, String>) {
    fun get(key: String): String? = value[key]
    val isEmpty: Boolean get() = value.isEmpty()
    val size: Int get() = value.size
}

@Serializer(forClass = EventData::class)
object EventDataSerializer : KSerializer<EventData> {
    override val descriptor: SerialDescriptor = 
        PrimitiveSerialDescriptor("EventData", PrimitiveKind.STRING)
        
    override fun serialize(encoder: Encoder, value: EventData) {
        val jsonString = value.value.entries.joinToString(",") { (k, v) -> "\"$k\":\"$v\"" }
        encoder.encodeString("{$jsonString}")
    }
    
    override fun deserialize(decoder: Decoder): EventData {
        val jsonString = decoder.decodeString()
        val map = if (jsonString.startsWith("{") && jsonString.endsWith("}")) {
            val content = jsonString.substring(1, jsonString.length - 1)
            if (content.isBlank()) emptyMap()
            else content.split(",").associate { pair ->
                val (key, value) = pair.split(":", limit = 2)
                key.trim('"') to value.trim('"')
            }
        } else emptyMap()
        return EventData(map)
    }
}

@Serializable(with = ConnectionIdSerializer::class)
@JvmInline
value class ConnectionId(val value: String) {
    init { require(value.isNotBlank()) { "ConnectionId cannot be blank" } }
    
    companion object {
        fun generate(): ConnectionId = ConnectionId(
            "conn-${System.currentTimeMillis()}-${(100..999).random()}"
        )
    }
}

@Serializer(forClass = ConnectionId::class)
object ConnectionIdSerializer : KSerializer<ConnectionId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ConnectionId", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: ConnectionId) = encoder.encodeString(value.value)
    override fun deserialize(decoder: Decoder): ConnectionId = ConnectionId(decoder.decodeString())
}