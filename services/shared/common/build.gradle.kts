import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
}

group = "com.holo.oshi"
version = "1.0.0"

repositories {
    mavenCentral()
}

extra["springCloudVersion"] = "2025.0.0"
extra["kotlinLoggingVersion"] = "3.0.5"

dependencies {
    // Spring Boot (Core Dependencies Only)
    api("org.springframework.boot:spring-boot-starter")
    api("org.springframework.boot:spring-boot-starter-webflux")
    api("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    api("org.springframework.boot:spring-boot-starter-validation")
    api("org.springframework.boot:spring-boot-autoconfigure")
    
    // Kotlin Coroutines
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-reactive")
    
    // Arrow-kt for Functional Programming (MSA 필수)
    api("io.arrow-kt:arrow-core:1.2.4")
    api("io.arrow-kt:arrow-fx-coroutines:1.2.4")
    
    // Jackson JSON Processing
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.module:jackson-module-kotlin")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    
    // Logging
    api("io.github.microutils:kotlin-logging-jvm:${property("kotlinLoggingVersion")}")
    api("ch.qos.logback:logback-classic")
    
    // Spring Framework
    api("org.springframework:spring-context")
    api("org.springframework:spring-webflux")
    api("org.springframework.data:spring-data-redis")
    
    // Validation
    api("jakarta.validation:jakarta.validation-api")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.5.5")
    }
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_23)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// JAR 생성 설정
tasks.named<Jar>("jar") {
    enabled = true
    archiveClassifier.set("")
}