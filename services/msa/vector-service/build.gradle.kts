import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.serialization") version "2.2.10"
}

group = "com.holo.oshi"
version = "1.0.0"

repositories {
    mavenCentral()
}

extra["springCloudVersion"] = "2025.0.0"
extra["kotlinLoggingVersion"] = "3.0.5"

dependencies {
    
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    
    // Spring Cloud
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-reactor-resilience4j")
    
    // Arrow-kt for Functional Programming (Netflix/Uber 수준)
    implementation("io.arrow-kt:arrow-core:1.2.4")
    implementation("io.arrow-kt:arrow-fx-coroutines:1.2.4")
    
    // Kotlin Core
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive") // 핵심 누락 의존성 추가!
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    
    // Kotlin Logging
    implementation("io.github.microutils:kotlin-logging-jvm:${property("kotlinLoggingVersion")}")
    
    // Ktor 제거 - Spring WebClient 사용
    
    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    
    // Database - R2DBC only
    implementation("org.postgresql:r2dbc-postgresql")
    
    // pgvector support
    implementation("com.pgvector:pgvector:0.1.6")
    
    // Redis for caching
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    
    // Messaging (MSA 메시징 인프라)
    implementation("org.springframework.cloud:spring-cloud-starter-stream-rabbit")
    
    // Monitoring - Netflix/Google 수준 관찰성
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    implementation("io.zipkin.reporter2:zipkin-reporter-brave")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_23)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// 환경변수 전달 설정
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    environment("GOOGLE_API_KEY", System.getenv("GOOGLE_API_KEY") ?: "")
    environment("GOOGLE_PROJECT_ID", System.getenv("GOOGLE_PROJECT_ID") ?: "holo-oshi-finder")
}