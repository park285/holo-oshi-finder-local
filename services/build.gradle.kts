import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.5.5" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    kotlin("jvm") version "2.2.10" apply false
    kotlin("plugin.spring") version "2.2.10" apply false
    kotlin("plugin.jpa") version "2.2.10" apply false
    kotlin("plugin.serialization") version "2.2.10" apply false
}

allprojects {
    group = "com.holo.oshi"
    version = "1.0.0"
    
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "java")
    
    // JDK 23 Toolchain 설정 (전체 프로젝트)
    configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(23)
        }
    }
    
    // Kotlin JDK 23 설정
    tasks.withType<KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_23)
            freeCompilerArgs.addAll(
                "-Xjsr305=strict",
                "-Xjvm-default=all",
                "-opt-in=kotlin.ExperimentalStdlibApi",
                "-opt-in=kotlin.RequiresOptIn",
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-opt-in=kotlinx.coroutines.FlowPreview",
                "-Xcontext-parameters",
                "-Xannotation-default-target=param-property",
                "-Xenable-incremental-compilation=true"
            )
            languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
            apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
        }
    }
    
    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

configure(subprojects.filter { it.name != "common" }) {
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
}