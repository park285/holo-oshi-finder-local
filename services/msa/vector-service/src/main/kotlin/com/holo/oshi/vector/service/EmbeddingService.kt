package com.holo.oshi.vector.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

/**
 * Embedding Service - Gemini API Integration
 * WebClient 기반 구현 (Ktor 직렬화 문제 해결)
 */
@Service
class EmbeddingService(
    @Value("\${google.api.key}") 
    private val apiKey: String
) {
    private val logger = LoggerFactory.getLogger(EmbeddingService::class.java)
    
    private val webClient = WebClient.builder()
        .codecs { it.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) }
        .build()
    
    /**
     * Gemini API를 사용하여 텍스트 임베딩 생성
     */
    suspend fun generateEmbedding(
        text: String, 
        taskType: String = "RETRIEVAL_QUERY"
    ): List<Double> = withContext(Dispatchers.IO) {
        try {
            // Google Gemini text-embedding-001 모델 사용 (1536차원)
            val geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/text-embedding-001:embedContent?key=$apiKey"
            
            val requestBody = buildJsonObject {
                put("model", JsonPrimitive("models/text-embedding-001"))
                putJsonObject("content") {
                    putJsonArray("parts") {
                        addJsonObject {
                            put("text", JsonPrimitive(text))
                        }
                    }
                }
                put("taskType", JsonPrimitive(taskType))
                put("outputDimensionality", JsonPrimitive(1536))
            }.toString()
            
            logger.debug("Sending request to Gemini API for text: ${text.take(50)}...")
            
            val response = webClient
                .post()
                .uri(geminiUrl)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .awaitBody<String>()
            
            val jsonResponse = Json { ignoreUnknownKeys = true }.parseToJsonElement(response).jsonObject
            val embeddingObject = jsonResponse["embedding"]?.jsonObject
            val values = embeddingObject?.get("values")?.jsonArray
            
            val embedding = values?.map { 
                it.jsonPrimitive.doubleOrNull ?: 0.0 
            } ?: emptyList()
            
            if (embedding.isEmpty()) {
                logger.warn("Empty embedding received, using zero vector")
                return@withContext List(1536) { 0.0 }
            }
            
            logger.info("임베딩 생성 성공: dimension=${embedding.size}")
            
            // 1536차원 확인
            if (embedding.size != 1536) {
                logger.warn("Expected 1536 dimensions but got ${embedding.size}, padding/truncating")
                when {
                    embedding.size < 1536 -> embedding + List(1536 - embedding.size) { 0.0 }
                    embedding.size > 1536 -> embedding.take(1536)
                    else -> embedding
                }
            } else {
                embedding
            }
            
        } catch (error: Exception) {
            logger.error("임베딩 생성 실패: ${error.message}", error)
            logger.debug("Using API Key: ${apiKey.take(10)}...")
            List(1536) { 0.0 }
        }
    }
    
    /**
     * 배치 임베딩 생성
     */
    suspend fun generateBatchEmbeddings(
        texts: List<String>,
        taskType: String = "RETRIEVAL_QUERY"
    ): List<List<Double>> = withContext(Dispatchers.IO) {
        texts.map { text ->
            generateEmbedding(text, taskType)
        }
    }
}