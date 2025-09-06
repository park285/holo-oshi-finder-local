package com.holo.oshi.notification.model

import kotlinx.serialization.Serializable

@Serializable
sealed class NotificationResult<out T> {
    @Serializable
    data class Success<T>(val data: T) : NotificationResult<T>()
    
    @Serializable
    data class Failure(val error: NotificationError) : NotificationResult<Nothing>()
    
    inline fun <R> map(transform: (T) -> R): NotificationResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Failure -> this
    }
    
    inline fun <R> flatMap(transform: (T) -> NotificationResult<R>): NotificationResult<R> = when (this) {
        is Success -> transform(data)
        is Failure -> this
    }
    
    inline fun onSuccess(action: (T) -> Unit): NotificationResult<T> = also {
        if (this is Success) action(data)
    }
    
    inline fun onFailure(action: (NotificationError) -> Unit): NotificationResult<T> = also {
        if (this is Failure) action(error)
    }
    
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }
    
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Failure -> throw NotificationException(error.message)
    }
}

class NotificationException(message: String) : Exception(message)