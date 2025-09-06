plugins {
    kotlin("jvm") version "2.2.10"
    kotlin("plugin.serialization") version "2.2.10"
    id("application")
}

group = "com.holo.oshi"
version = "1.0.0"
java.sourceCompatibility = JavaVersion.VERSION_23

repositories {
    mavenCentral()
}

dependencies {
    // Ktor Server 완전한 기능
    implementation("io.ktor:ktor-server-core:3.0.2")
    implementation("io.ktor:ktor-server-netty:3.0.2")
    implementation("io.ktor:ktor-server-websockets:3.0.2")
    implementation("io.ktor:ktor-server-content-negotiation:3.0.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.2")
    implementation("io.ktor:ktor-server-cors:3.0.2")
    implementation("io.ktor:ktor-server-call-logging:3.0.2")
    implementation("io.ktor:ktor-server-status-pages:3.0.2")
    
    // Kotlinx 완전한 기능
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    
    // Redis - Lettuce (올바른 의존성)
    implementation("io.lettuce:lettuce-core:6.6.0.RELEASE")
    
    // RabbitMQ
    implementation("com.rabbitmq:amqp-client:5.25.0")
    
    // JSON Processing
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.2")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
}

application {
    mainClass.set("com.holo.oshi.notification.ApplicationKt")
}