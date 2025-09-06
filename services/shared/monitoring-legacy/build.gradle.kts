plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
}

group = "com.holo.oshi"
version = "1.0.0"
java.sourceCompatibility = JavaVersion.VERSION_23

repositories {
    mavenCentral()
}

dependencies {
    // Micrometer Core
    api("io.micrometer:micrometer-registry-prometheus")
    api("io.micrometer:micrometer-tracing-bridge-brave")
    api("io.zipkin.reporter2:zipkin-reporter-brave")
    
    // Kotlin Coroutines 통합
    api("io.micrometer:micrometer-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-configuration-processor")
    
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
    
    // Logging
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.micrometer:micrometer-test")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_23)
    }
}