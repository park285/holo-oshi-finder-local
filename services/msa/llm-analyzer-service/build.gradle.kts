plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
}

group = "com.holo.oshi"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    
    // Spring Cloud
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-reactor-resilience4j")
    
    // Resilience4j
    implementation("io.github.resilience4j:resilience4j-spring-boot3")
    implementation("io.github.resilience4j:resilience4j-reactor")
    implementation("io.github.resilience4j:resilience4j-kotlin:2.2.0")
    
    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    
    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    
    // Kotlin 로깅
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    
    // AI/LLM 라이브러리 (모놀리식과 동일)
    implementation("com.aallam.openai:openai-client:4.0.0")
    
    // Ktor HTTP 클라이언트 (Reactor WebClient 대체)
    implementation("io.ktor:ktor-client-core:3.0.2")
    implementation("io.ktor:ktor-client-cio:3.0.2")
    implementation("io.ktor:ktor-client-content-negotiation:3.0.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.2")
    implementation("io.ktor:ktor-client-logging:3.0.2")
    
    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    
    // HTTP 클라이언트
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    
    // 관찰성 - Netflix/Google 수준 분산 추적
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    implementation("io.zipkin.reporter2:zipkin-reporter-brave")
    implementation("org.springframework.boot:spring-boot-starter-logging")
    
    // 유틸리티
    implementation("org.springframework.boot:spring-boot-configuration-processor")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.springframework.cloud:spring-cloud-starter-contract-stub-runner")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.0.0")
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
    environment("OPENAI_API_KEY", System.getenv("OPENAI_API_KEY") ?: "")
}