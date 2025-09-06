/**
 * Netflix/Google 수준 MSA 공통 의존성 정의
 * 모든 마이크로서비스가 사용하는 표준 의존성 모음
 */

// 버전 정의
extra["kotlinVersion"] = "2.2.10"
extra["springBootVersion"] = "3.5.5"
extra["springCloudVersion"] = "2025.0.0"
extra["kotlinLoggingVersion"] = "3.0.5"
extra["arrowVersion"] = "1.2.4"
extra["kotestVersion"] = "5.8.0"
extra["mockkVersion"] = "1.13.8"

// 공통 의존성 함수들
fun DependencyHandler.addSpringBootCore() {
    add("implementation", "org.springframework.boot:spring-boot-starter-webflux")
    add("implementation", "org.springframework.boot:spring-boot-starter-actuator")
    add("implementation", "org.springframework.boot:spring-boot-starter-validation")
}

fun DependencyHandler.addSpringCloud() {
    add("implementation", "org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    add("implementation", "org.springframework.cloud:spring-cloud-starter-loadbalancer")
    add("implementation", "org.springframework.cloud:spring-cloud-starter-circuitbreaker-reactor-resilience4j")
}

fun DependencyHandler.addKotlinCore() {
    add("implementation", "org.jetbrains.kotlin:kotlin-reflect")
    add("implementation", "org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    add("implementation", "org.jetbrains.kotlinx:kotlinx-coroutines-core")
    add("implementation", "com.fasterxml.jackson.module:jackson-module-kotlin")
    add("implementation", "io.projectreactor.kotlin:reactor-kotlin-extensions")
}

fun DependencyHandler.addKotlinLogging() {
    add("implementation", "io.github.microutils:kotlin-logging-jvm:${project.extra["kotlinLoggingVersion"]}")
    add("implementation", "org.slf4j:slf4j-api")
}

fun DependencyHandler.addArrowKt() {
    add("implementation", "io.arrow-kt:arrow-core:${project.extra["arrowVersion"]}")
    add("implementation", "io.arrow-kt:arrow-fx-coroutines:${project.extra["arrowVersion"]}")
}

fun DependencyHandler.addRedis() {
    add("implementation", "org.springframework.boot:spring-boot-starter-data-redis-reactive")
}

fun DependencyHandler.addMessaging() {
    add("implementation", "org.springframework.cloud:spring-cloud-starter-stream-rabbit")
}

fun DependencyHandler.addMonitoring() {
    add("implementation", "io.micrometer:micrometer-registry-prometheus")
    add("runtimeOnly", "io.micrometer:micrometer-registry-prometheus")
}

fun DependencyHandler.addKotlinTesting() {
    add("testImplementation", "org.springframework.boot:spring-boot-starter-test")
    add("testImplementation", "io.projectreactor:reactor-test")
    add("testImplementation", "io.kotest:kotest-runner-junit5:${project.extra["kotestVersion"]}")
    add("testImplementation", "io.kotest:kotest-assertions-core:${project.extra["kotestVersion"]}")
    add("testImplementation", "io.kotest:kotest-property:${project.extra["kotestVersion"]}")
    add("testImplementation", "io.mockk:mockk:${project.extra["mockkVersion"]}")
    add("testImplementation", "org.jetbrains.kotlinx:kotlinx-coroutines-test")
}