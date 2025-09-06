package com.holo.oshi.common.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(ServiceProperties::class)
class ServiceConfig

@ConfigurationProperties(prefix = "services")
data class ServiceProperties(
    val pythonNlp: ServiceEndpoint = ServiceEndpoint(),
    val geminiClassifier: ServiceEndpoint = ServiceEndpoint(),
    val pgvector: ServiceEndpoint = ServiceEndpoint(),
    val llmAnalyzer: ServiceEndpoint = ServiceEndpoint(),
    val elasticsearch: ServiceEndpoint = ServiceEndpoint(),
    val monitoring: ServiceEndpoint = ServiceEndpoint()
) {
    data class ServiceEndpoint(
        var url: String = "",
        var timeout: Long = 30000,
        var retryAttempts: Int = 3,
        var healthCheck: String = "/health"
    )
}

@ConfigurationProperties(prefix = "database")
data class DatabaseProperties(
    val postgres: PostgresConfig = PostgresConfig(),
    val redis: RedisConfig = RedisConfig(),
    val elasticsearch: ElasticsearchConfig = ElasticsearchConfig()
) {
    data class PostgresConfig(
        var host: String = "localhost",
        var port: Int = 5432,
        var database: String = "holo_oshi_db",
        var username: String = "holo_user",
        var password: String = "",
        var poolSize: Int = 10
    )

    data class RedisConfig(
        var host: String = "localhost",
        var port: Int = 6379,
        var password: String = "",
        var database: Int = 0,
        var timeout: Long = 2000,
        var poolMaxActive: Int = 8
    )

    data class ElasticsearchConfig(
        var host: String = "localhost",
        var port: Int = 9200,
        var scheme: String = "http",
        var username: String = "",
        var password: String = "",
        var indexName: String = "holo-members"
    )
}

@ConfigurationProperties(prefix = "api")
data class ApiProperties(
    val google: GoogleApiConfig = GoogleApiConfig(),
    val jina: JinaApiConfig = JinaApiConfig()
) {
    data class GoogleApiConfig(
        var apiKey: String = "",
        var projectId: String = "holo-oshi-finder",
        var location: String = "us-central1",
        var model: String = "gemini-1.5-flash",
        var maxTokens: Int = 2048,
        var temperature: Double = 0.7
    )

    data class JinaApiConfig(
        var apiKey: String = "",
        var apiUrl: String = "https://api.jina.ai/v1/embeddings",
        var model: String = "jina-embeddings-v2-base-en",
        var dimension: Int = 768
    )
}

@ConfigurationProperties(prefix = "application")
data class ApplicationProperties(
    val environment: String = "development",
    val logLevel: String = "INFO",
    val cors: CorsConfig = CorsConfig(),
    val metrics: MetricsConfig = MetricsConfig()
) {
    data class CorsConfig(
        var allowedOrigins: List<String> = listOf("*"),
        var allowedMethods: List<String> = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS"),
        var allowedHeaders: List<String> = listOf("*"),
        var allowCredentials: Boolean = true
    )

    data class MetricsConfig(
        var enabled: Boolean = true,
        var exportInterval: Long = 60000,
        var batchSize: Int = 100
    )
}