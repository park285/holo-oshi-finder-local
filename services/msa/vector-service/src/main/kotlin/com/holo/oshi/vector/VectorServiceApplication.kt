package com.holo.oshi.vector

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories

@SpringBootApplication
@EnableDiscoveryClient
@EnableR2dbcRepositories
@EnableCaching
class VectorServiceApplication

fun main(args: Array<String>) {
    runApplication<VectorServiceApplication>(*args)
}